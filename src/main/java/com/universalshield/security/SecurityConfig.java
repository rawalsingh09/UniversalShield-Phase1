package com.universalshield.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalshield.config.AppProperties;
import com.universalshield.dto.ApiError;
import com.universalshield.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

/**
 * Security configuration:
 * <ul>
 *   <li>Session-cookie authentication (HttpOnly, SameSite=Lax, Secure in production).</li>
 *   <li>CSRF protection with the double-submit cookie pattern for the JavaScript frontend.</li>
 *   <li>API returns JSON 401/403 instead of redirects.</li>
 *   <li>Strict Content-Security-Policy and other security headers.</li>
 *   <li>CORS disabled unless origins are explicitly configured.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; "
            + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public RateLimiter rateLimiter(Clock clock) {
        return new RateLimiter(clock);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AppProperties props, ObjectMapper mapper,
                                           RateLimiter limiter, SecurityContextRepository contextRepository,
                                           @Value("${universalshield.api-docs.public:true}") boolean docsPublic) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieCustomizer(c -> c.sameSite("Lax").path("/"));

        http
                .securityContext(c -> c.securityContextRepository(contextRepository))
                .csrf(c -> c.csrfTokenRepository(csrfRepo).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(limiter, props.security().rateLimit(), mapper), AnonymousAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
                .authorizeHttpRequests(a -> {
                    a.requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.svg", "/css/**", "/js/**", "/assets/**").permitAll();
                    a.requestMatchers(HttpMethod.GET, "/api/health", "/api/detectors", "/api/auth/csrf", "/api/methodology").permitAll();
                    a.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll();
                    a.requestMatchers("/error").permitAll();
                    if (docsPublic) {
                        a.requestMatchers("/api/openapi", "/api/openapi/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    a.requestMatchers("/api/**").authenticated();
                    a.anyRequest().denyAll();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> write(res, mapper, HttpStatus.UNAUTHORIZED,
                                ApiError.of("UNAUTHORIZED", "Please sign in to continue.")))
                        .accessDeniedHandler((req, res, ex) -> write(res, mapper, HttpStatus.FORBIDDEN, ex instanceof CsrfException
                                ? ApiError.of("CSRF_INVALID", "Your security token is missing or expired. Reload the page and try again.")
                                : ApiError.of("FORBIDDEN", "You are not allowed to do this."))))
                .logout(l -> l.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> {
                            res.setStatus(200);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            mapper.writeValue(res.getOutputStream(), ApiResponse.ok("Signed out"));
                        }))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()")));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = props.security().corsAllowedOrigins();
        origins = origins == null ? List.of() : origins.stream().filter(o -> o != null && !o.isBlank()).toList();
        if (!origins.isEmpty()) {
            CorsConfiguration cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(origins);
            cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
            cfg.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
            cfg.setAllowCredentials(true);
            cfg.setMaxAge(3600L);
            source.registerCorsConfiguration("/api/**", cfg);
        }
        return source;
    }

    private static void write(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, ApiError error) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), ApiResponse.fail(error));
    }
}
