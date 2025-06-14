package com.example.cleopatra.service;

import com.example.cleopatra.dto.BirthdayUser.BirthdayPageResponse;
import org.springframework.data.domain.Pageable;

public interface BirthdayService {


     BirthdayPageResponse getSubscriptionsBirthdays(Long userId, Pageable pageable);
}
