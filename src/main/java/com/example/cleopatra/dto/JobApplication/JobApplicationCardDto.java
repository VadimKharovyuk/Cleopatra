package com.example.cleopatra.dto.JobApplication;

import com.example.cleopatra.enums.PerformerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationCardDto {
    private Long id;
    private String name;
    private PerformerProfile profile;
    private Integer workExperience;
    private BigDecimal minSalary;
    private Integer age;
}
