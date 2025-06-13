package com.example.cleopatra.service;

import com.example.cleopatra.dto.ReportDTO.CreateReportDTO;
import com.example.cleopatra.dto.ReportDTO.ReportResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostReportService {

  void createReport(CreateReportDTO dto, Long reporterId, String reporterIp);

    Page<ReportResponseDTO> getAllReports(Pageable pageable);

    Page<ReportResponseDTO> getPendingReports(Pageable pageable);

    void resolveReport(Long reportId, Long adminId, String adminComment, String actionTaken);


    boolean hasUserReportedPost(Long id, Long currentUserId);
}
