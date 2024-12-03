package com.micro.learningplatform.config;

import com.micro.learningplatform.api.ApiVersionIntreceptop;
import com.micro.learningplatform.api.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class ApiConfiguration implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /*
     Konfiguracija za osnovne postavke api-a centralizira postavke i olaksava njihovu izmjenu
     */


    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // podrzavamo json i protobuf za optimalne performance
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML)
                .mediaType("protobuf", new MediaType("application", "x-protobuf"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Dodajemo presretač za verzioniranje
        registry.addInterceptor(new ApiVersionIntreceptop());

        // Dodajemo presretač za rate limiting
        registry.addInterceptor(rateLimitInterceptor);

    }
}
