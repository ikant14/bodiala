package com.miirphys.bodiala.provider.rezlive;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the RezLive.com (XMLHUB / RezTez) B2B travel API.
 *
 * <p>Bound from the {@code rezlive.*} properties. The {@code apiKey} is sent as the
 * {@code x-api-key} HTTP header; {@code agentCode} and {@code userName} go into the
 * in-body {@code <Authentication>} block of every XML request.
 */
@ConfigurationProperties(prefix = "rezlive")
public class RezLiveProperties {

    /** Action base URL, e.g. {@code http://test.xmlhub.com/testpanel.php/action}. */
    private String baseUrl = "http://test.xmlhub.com/testpanel.php/action";

    /** AgentCode (a.k.a. "User Code") assigned by XMLHUB. */
    private String agentCode = "";

    /** UserName self-generated at registration. */
    private String userName = "";

    /** The {@code x-api-key} value issued by XMLHUB. */
    private String apiKey = "";

    /** Directory holding the manually-downloaded CSV master files for ingestion. */
    private String staticDataDir = "./data/rezlive";

    /**
     * Auto-import the CSV master files on startup — but only when the static-data directory exists
     * and the local cache is still empty (so it seeds a fresh DB without re-importing every restart).
     */
    private boolean importOnStartup = true;

    /** True only when all three API credentials are present. */
    public boolean hasCredentials() {
        return notBlank(agentCode) && notBlank(userName) && notBlank(apiKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getStaticDataDir() {
        return staticDataDir;
    }

    public void setStaticDataDir(String staticDataDir) {
        this.staticDataDir = staticDataDir;
    }

    public boolean isImportOnStartup() {
        return importOnStartup;
    }

    public void setImportOnStartup(boolean importOnStartup) {
        this.importOnStartup = importOnStartup;
    }
}
