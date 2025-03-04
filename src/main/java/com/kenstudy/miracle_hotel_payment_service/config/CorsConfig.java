package com.kenstudy.miracle_hotel_payment_service.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebMvc
public class CorsConfig {
    private final static Long MAX_AGE = 3600L;
    private final static int CORS_FILTER_ORDER= -102;
    @Bean
    public FilterRegistrationBean corsFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://dev.kenstudy.com",
                "http://hotel.kenstudy.com"
        ));
        config.addAllowedMethod(HttpMethod.OPTIONS.name());
        config.setAllowedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT));
        config.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.PUT.name()));

        config.setMaxAge(MAX_AGE);
        source.registerCorsConfiguration("/**",config);

        FilterRegistrationBean bean = new  FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(CORS_FILTER_ORDER);
        return bean;

    }

}

