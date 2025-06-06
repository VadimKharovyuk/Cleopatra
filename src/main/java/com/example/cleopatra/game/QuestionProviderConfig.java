package com.example.cleopatra.game;

import com.example.cleopatra.game.QuestionProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
@Configuration
public class QuestionProviderConfig {

//    @Bean
//    @Primary
//    public QuestionProvider questionProvider(RestTemplate restTemplate, FallbackQuestionProvider fallbackProvider) {
//        return new LazyOpenTDBQuestionProvider(restTemplate, fallbackProvider);
//    }
@Bean
@Primary
public QuestionProvider questionProvider(RestTemplate restTemplate,
                                         FallbackQuestionProvider fallbackProvider,
                                         TranslationService translationService) {
    return new LazyOpenTDBQuestionProvider(restTemplate, fallbackProvider, translationService);
}
}