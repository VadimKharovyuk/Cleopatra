package com.example.cleopatra.service.impl;

import com.example.cleopatra.service.BirthdayService;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BirthdayServiceImpl implements BirthdayService {

    private final SubscriptionService subscriptionService ;
}
