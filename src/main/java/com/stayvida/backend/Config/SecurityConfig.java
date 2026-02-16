package com.stayvida.backend.Config;

import com.stayvida.backend.security.CustomAuthEntryPoint;
import com.stayvida.backend.security.CustomOAuth2SuccessHandler;
import com.stayvida.backend.security.JwtAuthFilter;
import com.stayvida.backend.security.SupabaseJwtFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

        private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
        private final JwtAuthFilter jwtAuthFilter;
        // private final SupabaseJwtFilter supabaseJwtFilter;

        // @Autowired
        public SecurityConfig(CustomOAuth2SuccessHandler customOAuth2SuccessHandler,
                        JwtAuthFilter jwtAuthFilter) {
                this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
                this.jwtAuthFilter = jwtAuthFilter;
        }

        @Autowired
        private CustomAuthEntryPoint customAuthEntryPoint;
        @Autowired
        private SupabaseJwtFilter supabaseJwtFilter; // 🟦 For admin dashboard

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // ✅ Enable CORS and disable CSRF
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())

                                // ✅ Stateless — no sessions stored
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // ✅ Define accessible endpoints
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/get-otp",
                                                                "/api/auth/*",
                                                                "/api/verify-otp",
                                                                "/api/signup",
                                                                "/api/login",
                                                                "/api/login/google-auth",
                                                                "/google",
                                                                "/api/hotels/search",
                                                                "/api/hotels/featurelist",
                                                                "/auth/google/callback",
                                                                "/testjson",
                                                                "/home/**",
                                                                "/me",
                                                                "/login",
                                                                "/logout-success",
                                                                "/image/**",
                                                                "/css/**",
                                                                "/api/hotels/register",
                                                                "/api/hotels/upload-image",
                                                                "/api/hotel/*/rooms",
                                                                "/api/hotels/*/rooms",
                                                                "/api/hotels/**",
                                                                "/api/hotels/update-verification",
                                                                "/otplogin/**",
                                                                "/api/rating/create",
                                                                "/api/rating/hotel/**",
                                                                "/api/locations/list",
                                                                "/api/events/add",
                                                                "/api/events/search",
                                                                "/api/contact/submit",
                                                                "/api/contact/all",
                                                                "/api/events/list",
                                                                "/api/events/details",
                                                                "/test/upload",
                                                                "/api/admin/**",
                                                                "/api/auth/login",
                                                                "/rating/hotel/**",
                                                                "/error",
                                                                "/api/payments/**",
                                                                "/api/profile/{bookingId}/details")
                                                .permitAll()
                                                .anyRequest().authenticated()

                                )

                                // ✅ Disable default login types
                                .httpBasic(httpBasic -> httpBasic.disable())
                                .formLogin(form -> form.disable())

                                // ✅ Add this line — handles 401s without redirecting
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(customAuthEntryPoint)
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setContentType("application/json");
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.getWriter().write("{\"error\": \"Access denied.\"}");
                                                }))

                                // ✅ Enable OAuth2 login (Google)
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .successHandler(customOAuth2SuccessHandler))

                                // ✅ Add JWT filter BEFORE username/password filter
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(supabaseJwtFilter, JwtAuthFilter.class);

                // ✅ Logout handler
                // .logout(logout -> logout
                // .logoutUrl("/logout")
                // .logoutSuccessUrl("/logout-success?logout")
                // .invalidateHttpSession(true)
                // .clearAuthentication(true)
                // .deleteCookies("JSESSIONID")
                // .permitAll()
                // )
                ;

                return http.build();

        }

        @Value("${CORS}")
        private String corsAllowedOrigins;

        // ✅ CORS policy
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                                .map(String::trim)
                                .toList();
                CorsConfiguration configuration = new CorsConfiguration();
                // configuration.setAllowedOriginPatterns(List.of(
                // "http://localhost:5173",
                // "http://localhost:5174",
                // "http://localhost:5175",
                // "https://sv-website-frontend-uyt5qvn33-stay-vidas-projects.vercel.app/",
                // "https://sv-website-frontend.vercel.app/"
                // ));
                configuration.setAllowedOriginPatterns(allowedOrigins);
                configuration.setAllowedMethods(List.of("GET", "PATCH", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        // ✅ Static files ignored from security
        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
                return (web) -> web.ignoring()
                                .requestMatchers("/image/**", "/css/**", "/js/**");
        }
}
