package com.kenstudy.miracle_hotel_payment_service.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

        @Bean
        public Gson gson() {
            return new GsonBuilder().setPrettyPrinting().create();
        }

}
