package com.micro.learningplatform.config;

import com.micro.learningplatform.security.CustomOAuth2UserService;
import com.micro.learningplatform.security.CustomUserDetailsService;
import com.micro.learningplatform.security.OAuth2AuthenticationFailureHandler;
import com.micro.learningplatform.security.OAuth2AuthenticationSuccessHandler;
import com.micro.learningplatform.security.jwt.JwtAuthenticationEntryPoint;
import com.micro.learningplatform.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LogManager.getLogger(SecurityConfig.class);
    /** Ovdje je klasa "srce" nase centralne sigurnosti tj nase sigurnosne konfiguracije
     * Definira kako app tretira zarlicite http zahtijeve i kako se porvodi autehntifikacija
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
            new AntPathRequestMatcher("/login/oauth2/code/**"), // i gogole/github
            new AntPathRequestMatcher("/error"),
            new AntPathRequestMatcher("/login")
    };

    private static final RequestMatcher[] API_DOCS_ENDPOINTS = {
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/v3/api-docs.yaml")  //  podrška za YAML format

    };


    //TODO razmilsit o uvodenju rivatnih metoda koje za logicke blokove unutram securityFilterChain npr:
      //configureAuthorization, configureOAuth2, configureSessionManagement kao bi smanjili velicina metoda i povecala citljivost
      //


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable) // jer koritimo jwt
                .authorizeHttpRequests(auth -> auth
                        // dopustam javnim endpoint pristup svima
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(API_DOCS_ENDPOINTS).permitAll()
                        // role based authentifikacija
                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/v1/instructor/**").hasRole("INSTRUCTOR")
                                .requestMatchers("/api/v1/courses/**").hasAnyRole("USER", "INSTRUCTOR", "ADMIN")
                                .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                /* nasa custom auth2 service klasa za oauth2
                                  1. customOAuth2UserService ucitava i kreira korisnike nakon uspijsne oauth2 authetifikcije
                                  2. successHandler -> generira jwt tokene nakon tokene nakon uspijesne autentifiaijce
                                  3. fauilureHanlder upravlja greskama
                                 */
                                .userService(customOAuth2UserService)
                        )
                        // ako je uspijesno authent klas preuzima kontrolu
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                .sessionManagement(session -> session
                        /* jer imamo jwt koji je statles i oAuth2 koji mora proci kroz cijeli svoj flow
                           1. o2auth zahtijeva sesiju tijekom authentifikaciskog proces
                           2. nakoon uspijesne autehtigikacije generiramo jwt tokene
                           3. za buduce zahtijeve s jwt tokenima sesija se ne kreira
                         */
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                .authenticationProvider(authenticationProvider())  // Koristimo metodu umjesto injectirane instance
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // kordinira proces autenfikacije
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /* Ova metoda je kljucna za autentifikaciju jer konfigurira kako spring security validirati korsinicke kredenciale
     *  DaoAuthenticationProvider mozemo ga zamislit kao most izmedu korisnickih podatka koje dobivamo iz baze i enkripcije
     *
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // sada ukljucujemo CorsConfigurationSource u security filter chain umjesto rucnog krairanj corsFilter()
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // kljucno za komunikacije fornt i back-enda gdje app cesto komunicirajmu preko razlicitih domea
    //TODO razmilsiti o prebacivanju corss-a u yaml i razmotirit sigurnosti implikacije za ("*")

    /*
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

     */

}
