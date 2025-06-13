package com.example.cleopatra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableScheduling
@SpringBootApplication
@EnableAsync
public class CleopatraApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleopatraApplication.class, args);





//public enum ProfilePrivacyLevel {
//    PUBLIC,           // Все могут видеть
//    SUBSCRIBERS_ONLY, // Только подписчики
//    PRIVATE          // Никто (только сам пользователь)
//}
//
//




//// В User добавить:
//        @Enumerated(EnumType.STRING)
//        @Column(name = "wall_access_level")
//        @Builder.Default
//        private WallAccessLevel wallAccessLevel = WallAccessLevel.PUBLIC;
//
//        enum WallAccessLevel {
//            PUBLIC,      // Все могут писать на стене
//            FRIENDS,     // Только подписчики могут писать
//            PRIVATE,     // Никто не может писать (только владелец)
//            DISABLED     // Стена отключена
//        }



//
//        // Закреплен ли пост
//        @Column(name = "is_pinned")
//        @Builder.Default
//        private Boolean isPinned = false;
//
//        // Дата закрепления
//        @Column(name = "pinned_at")
//        private LocalDateTime pinnedAt;

    }

}
