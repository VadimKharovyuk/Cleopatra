package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UserResponse;


public interface UserService {

    UserResponse createUser(RegisterDto  registerDto);
}
