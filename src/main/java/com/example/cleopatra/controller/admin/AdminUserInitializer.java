//package com.example.cleopatra.controller.admin;
//
//import com.example.cleopatra.enums.Gender;
//import com.example.cleopatra.enums.Role;
//import com.example.cleopatra.model.User;
//import com.example.cleopatra.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//@RequiredArgsConstructor
//public class AdminUserInitializer implements ApplicationRunner {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        createAdminIfNotExists();
//    }
//
//    private void createAdminIfNotExists() {
//        String adminEmail = "vadimKh17@gmail.com";
//
//        if (!userRepository.existsByEmail(adminEmail)) {
//            User admin = User.builder()
//                    .email(adminEmail)
//                    .password(passwordEncoder.encode("admin"))
//                    .role(Role.ADMIN)
//                    .gender(Gender.MALE)
//                    .showOnlineStatus(true)
//                    .followersCount(0L)
//                    .followingCount(0L)
//                    .totalVisits(null)
//                    .isOnline(false)
//                    .receiveVisitNotifications(true)
//                    .createdAt(LocalDateTime.now())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//
//            userRepository.save(admin);
//
//            System.out.println("Создан администратор: " + adminEmail);
//        }
//    }
//}
