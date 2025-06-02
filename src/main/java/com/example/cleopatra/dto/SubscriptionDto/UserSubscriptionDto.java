package com.example.cleopatra.dto.SubscriptionDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private String city;
    private LocalDateTime subscribedAt;
}
