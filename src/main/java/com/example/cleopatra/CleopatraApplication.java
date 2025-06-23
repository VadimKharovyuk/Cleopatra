package com.example.cleopatra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableAsync
public class CleopatraApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleopatraApplication.class, args);


    ///форум /комент
//
//        Лайки/дизлайки
//        Рейтинги пользователей
//        Расширенная модерация
//        Редактирование комментариев
//        Упоминания пользователей (@username)

        /// нотификация если ответили на клментарий в форуме





    }

}
