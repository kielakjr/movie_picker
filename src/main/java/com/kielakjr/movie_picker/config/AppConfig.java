package com.kielakjr.movie_picker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.DoubleSupplier;

@Configuration
public class AppConfig {

    @Bean
    public DoubleSupplier explorationRandom() {
        return Math::random;
    }
}
