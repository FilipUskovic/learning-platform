package com.micro.learningplatform.config;

import com.micro.learningplatform.security.CustomOAuth2UserService;
import com.micro.learningplatform.security.CustomUserDetailsService;
import com.micro.learningplatform.security.OAuth2AuthenticationFailureHandler;
import com.micro.learningplatform.security.OAuth2AuthenticationSuccessHandler;
import com.micro.learningplatform.security.jwt.JwtAuthenticationEntryPoint;
import com.micro.learningplatform.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Ovdje nam
     */

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    // imam bolju organiziranost endpointa za konfgiruacjuje ovako
    public static final RequestMatcher[] PUBLIC_ENDPOINTS = {
            new AntPathRequestMatcher("/api/v1/auth/**"),
            new AntPathRequestMatcher("/oauth2/**"),
            new AntPathRequestMatcher("/oauth2/authorization/**"), // za 02auth
            new AntPathRequestMatcher("/login/oauth2/code/**"), // i gogole
         //   new AntPathRequestMatcher("/api/v1/auth/oauth2/callback/**"), // dodao i  ovo
            new AntPathRequestMatcher("/error"),
            new AntPathRequestMatcher("/login")
    };

    private static final RequestMatcher[] SWAGGER_ENDPOINTS = {
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**")
    };


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                        // role based authentifikacija
                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/v1/instructor/**").hasRole("INSTRUCTOR")
                                .requestMatchers("/api/v1/courses/**").hasAnyRole("USER", "INSTRUCTOR", "ADMIN")
                                .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                        /*
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorize"))
                         */

                )
                .sessionManagement(session -> session
                      //  .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())  // Dodaj EntryPoint
                )
                .authenticationProvider(authenticationProvider())  // Koristimo metodu umjesto injectirane instance
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }


    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000"); // Frontend URL
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }



    /*
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                              //  "/api/v1/auth/register",
                             //   "/api/v1/auth",
                                "/api/v1/auth/**",
                                "/oauth2/**",
                                "/error",
                                "/login",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Dodajemo role-based autorizaciju
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/instructor/**").hasRole("INSTRUCTOR")
                        .requestMatchers("/api/v1/courses/**").hasAnyRole("USER", "INSTRUCTOR", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())  // Koristimo metodu umjesto injectirane instance
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
     */


}
