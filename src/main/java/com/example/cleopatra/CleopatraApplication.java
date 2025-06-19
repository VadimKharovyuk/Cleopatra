package com.example.cleopatra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableAsync
public class CleopatraApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleopatraApplication.class, args);



//        curl https://cleopatra-brcc.onrender.com/health
//        curl https://cleopatra-brcc.onrender.com/diagnostic/memory



//        Could not autowire. No beans of 'JavaMailSender' type foun
//        EmailQueueManager


    }

}
