package com.miirphys.bodiala.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
import tools.jackson.databind.json.JsonMapper;

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
 *   <li>401/403 bodies are a Spring {@link ProblemDetail} written with the app's JSON mapper, so they
 *       have the same RFC 7807 shape as {@code ApiExceptionHandler}'s errors.</li>
 *   <li><b>A wrong or stale {@code Authorization: Basic} header gets 401 even on a PUBLIC route</b>
 *       (e.g. after a mistyped Authorize in Swagger UI): the Basic filter rejects bad credentials before
 *       the route matrix is consulted. Drop the header (Swagger: Logout) and the public route works
 *       again. This is Spring Security's normal behaviour and is kept on purpose: a bad credential is
 *       never silently downgraded to anonymous.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JsonMapper json) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, e) ->
                writeProblem(json, request, response, HttpStatus.UNAUTHORIZED, "Authentication required.");
        AccessDeniedHandler forbidden = (request, response, e) ->
                writeProblem(json, request, response, HttpStatus.FORBIDDEN, "Not allowed.");

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

    /**
     * Writes the same body an {@code @ExceptionHandler} returning a {@link ProblemDetail} would: Spring MVC
     * fills in {@code instance} with the request path, so this does too.
     */
    private static void writeProblem(JsonMapper json, HttpServletRequest request, HttpServletResponse response,
                                     HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), problem);
    }
}
