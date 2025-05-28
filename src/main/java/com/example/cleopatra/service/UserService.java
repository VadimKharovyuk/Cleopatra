package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

public interface UserService {

    UserResponse createUser(RegisterDto  registerDto);
}
