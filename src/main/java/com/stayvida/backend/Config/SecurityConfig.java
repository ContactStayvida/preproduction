package com.stayvida.backend.Config;

import com.stayvida.backend.security.CustomOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.config.Customizer;

@Configuration
public class SecurityConfig {


    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    // ✅ Spring will inject your @Component success handler
    public SecurityConfig(CustomOAuth2SuccessHandler customOAuth2SuccessHandler) {
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/signup",
                    "/api/login",
                    "/api/login/google-auth",
                    "/google",
                    "/api/hotels/search",
                    "/api/featurelist",
                    "/auth/google//callback",
                    "/testjson",
                    "/home/",
                    "/me",
                    "/login",
                    "/logout-success",
                    "/image/**",
                    "/css/**",
                    "/api/hotels/register",
                    "/api/hotels/upload-image",
                    "/api/hotels/*/rooms",
                    "/api/hotels/**",
                    "/api/hotels/udate-verification"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable()) // disable HTTP Basic auth
            .formLogin(form -> form.disable()) // disable form login
               .oauth2Login(oauth2 -> oauth2
            //    .loginPage("/oauth2/authorization/google")
               .loginPage("/login")

                // ✅ now use the injected bean
                .successHandler(customOAuth2SuccessHandler)
            )
          .logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/logout-success?logout")
    .invalidateHttpSession(true)
    .clearAuthentication(true)
    .deleteCookies("JSESSIONID")  // clear your app session cookie
    .permitAll()
);//switch this to home page/loginpage later
        return http.build();
    }   
}
