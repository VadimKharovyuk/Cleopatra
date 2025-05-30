package com.example.cleopatra.controller;

import com.example.cleopatra.dto.JobApplication.JobApplicationDto;
import com.example.cleopatra.service.JobApplicationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RestTest {
    private final JobApplicationService jobApplicationService;

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationDto> getJobApplication(@PathVariable Long id) {
        try {
            JobApplicationDto application = jobApplicationService.getApplicationDetails(id);
            return ResponseEntity.ok(application);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
