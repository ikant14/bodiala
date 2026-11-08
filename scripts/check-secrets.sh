#!/usr/bin/env bash
# Pre-push credential check (AGENTS.md §6). Scans the commits you are about to publish and never
# prints a secret value.
#
#   bash C:/Users/user/IdeaProjects/bodiala/scripts/check-secrets.sh <repo-path> [<range>]
#   e.g. bash .../check-secrets.sh . origin/main..HEAD
#
# Exit 1 (do not push) when:
#   - a real value from bodiala's git-ignored .env (any *KEY*, *SECRET*, *PASSWORD*, *TOKEN*,
#     *USER_NAME* or *AGENT_CODE* entry) appears in an added line; or
#   - a file that must never be committed is added (.env, keys, keystores, H2/SQLite databases).
# Lines that merely look like credentials (key = "...") are listed as WARNINGs for you to judge:
# placeholders such as stub-key or test-only passwords are fine, real values are not.
set -u
repo=${1:?usage: check-secrets.sh <repo-path> [<range>]}
range=${2:-origin/main..HEAD}
envfile="C:/Users/user/IdeaProjects/bodiala/.env"
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

if [ -f "$envfile" ]; then
  grep -E '^[A-Z0-9_]*(KEY|SECRET|PASSWORD|TOKEN|USER_NAME|AGENT_CODE)[A-Z0-9_]*=' "$envfile" \
    | cut -d= -f2- | sed -e 's/\r$//' -e 's/^["'"'"']//' -e 's/["'"'"']$//' | awk 'length($0) >= 6' > "$tmp"
fi

fail=0
echo "Scanning $repo $range ($(git -C "$repo" rev-list --count "$range") commits; $(wc -l < "$tmp") .env values)"

bad_files=$(git -C "$repo" log --format= --name-only --diff-filter=A "$range" | sort -u \
  | grep -E '(^|/)\.env$|\.env\.(local|dev|prod|staging)$|\.(pem|key|p12|pfx|jks|keystore|mv\.db|lock\.db|sqlite|db)$|(^|/)id_(rsa|ed25519|ecdsa)')
if [ -n "$bad_files" ]; then
  echo "FAIL: files that must not be committed:"; echo "$bad_files" | sed 's/^/  /'; fail=1
fi

if [ -s "$tmp" ]; then
  hits=$(git -C "$repo" log -p --format= "$range" | grep '^+' | grep -c -F -f "$tmp")
  if [ "$hits" -gt 0 ]; then
    echo "FAIL: $hits added line(s) contain a real value from .env (not shown)."; fail=1
  fi
fi

pattern='BEGIN (RSA |OPENSSH |EC |DSA |PGP )?PRIVATE KEY|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36}|xox[baprs]-[A-Za-z0-9-]{10,}|sk_(live|test)_[A-Za-z0-9]{10,}|(api[_-]?key|secret|passw(or)?d|token)["'"'"']?\s*[:=]\s*["'"'"']?[A-Za-z0-9+/_.$@!#%^&*-]{8,}'
warn=$(git -C "$repo" log -p --format= "$range" | awk '/^\+\+\+ /{f=$2} /^\+[^+]/{print f": "$0}' \
  | grep -E -i "$pattern" | cut -c1-140)
if [ -n "$warn" ]; then
  echo "WARNING: lines that look like credentials. Check each is a placeholder or test-only value:"
  echo "$warn" | sed 's/^/  /'
fi

[ "$fail" -eq 0 ] && echo "OK: no real credentials or forbidden files in $range."
exit "$fail"
