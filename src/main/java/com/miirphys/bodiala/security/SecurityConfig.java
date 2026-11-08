package com.miirphys.bodiala.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Stateless, deny-by-default security for the whole API (SEC-1).
 *
 * <ul>
 *   <li>The rules come from {@link RouteMatrix}, applied in order; anything unlisted is denied.</li>
 *   <li>No sessions and no cookies: staff send HTTP Basic credentials on every request.</li>
 *   <li>CSRF protection is off because nothing authenticates by an ambient credential. To keep it
 *       that way, a 401 carries <b>no</b> {@code WWW-Authenticate: Basic} challenge, so a browser never
 *       shows its login prompt and never caches staff credentials that a cross-site form could reuse.
 *       Callers (Swagger UI, curl, a future agent screen) send the {@code Authorization} header
 *       themselves.</li>
 *   <li>401/403 bodies are RFC 7807 problem JSON, like the rest of the API's errors.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, e) ->
                writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication required.");
        AccessDeniedHandler forbidden = (request, response, e) ->
                writeProblem(response, HttpStatus.FORBIDDEN, "Not allowed.");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .authorizeHttpRequests(auth -> {
                    // The container's error dispatch (e.g. a public route's 404) must keep its status.
                    // A direct request to /error is still a REQUEST dispatch and stays denied.
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    for (RouteMatrix.Route route : RouteMatrix.ROUTES) {
                        var rule = route.method() == null
                                ? auth.requestMatchers(route.pattern())
                                : auth.requestMatchers(route.method(), route.pattern());
                        switch (route.access()) {
                            case PUBLIC, BOOKING_TOKEN -> rule.permitAll(); // BOOKING_TOKEN: until SEC-2
                            case STAFF -> rule.hasRole(StaffProperties.ROLE);
                        }
                    }
                    auth.anyRequest().denyAll();
                });
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Holds the single staff user, or nobody (every staff route then returns 401). */
    @Bean
    UserDetailsService staffUsers(StaffProperties staff, PasswordEncoder encoder) {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        staff.toUser(encoder).ifPresentOrElse(users::createUser, () -> log.info(
                "No staff password set (BODIALA_STAFF_PASSWORD): staff-only routes will return 401."));
        return users;
    }

    private static void writeProblem(HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase()
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}");
    }
}
