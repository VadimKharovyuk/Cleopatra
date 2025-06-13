package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.ReportDTO.CreateReportDTO;
import com.example.cleopatra.dto.ReportDTO.ReportResponseDTO;
import com.example.cleopatra.enums.ReportReason;
import com.example.cleopatra.enums.ReportStatus;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.PostReport;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.PostReportRepository;
import com.example.cleopatra.repository.PostRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.PostReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostReportServiceImpl implements PostReportService {

    private final PostReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public void createReport(CreateReportDTO dto, Long reporterId, String reporterIp) {
        // Проверяем, не жаловался ли уже этот пользователь
        if (reportRepository.existsByPostIdAndReporterId(dto.getPostId(), reporterId)) {
            throw new RuntimeException("Вы уже жаловались на этот пост");
        }

        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя жаловаться на свой пост
        if (post.getAuthor().getId().equals(reporterId)) {
            throw new RuntimeException("Нельзя жаловаться на собственный пост");
        }

        PostReport report = PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .reporterIp(reporterIp)
                .priority(calculatePriority(dto.getReason()))
                .build();

        reportRepository.save(report);

        Long currentCount = post.getReportsCount();
        if (currentCount == null) {
            currentCount = 0L;
        }
        post.setReportsCount(currentCount + 1);
        postRepository.save(post);

        log.info("Создана жалоба на пост {} от пользователя {}", dto.getPostId(), reporterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDTO> getAllReports(Pageable pageable) {
        return reportRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDTO> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public void resolveReport(Long reportId, Long adminId, String adminComment, String actionTaken) {
        PostReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Жалоба не найдена"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Админ не найден"));

        report.setStatus(ReportStatus.RESOLVED);
        report.setReviewedBy(admin);
        report.setAdminComment(adminComment);
        report.setActionTaken(actionTaken);
        report.setReviewedAt(LocalDateTime.now());

        reportRepository.save(report);

        log.info("Жалоба {} решена админом {}", reportId, adminId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReportedPost(Long postId, Long userId) {
        // Проверяем входные параметры
        if (postId == null || userId == null) {
            log.warn("Некорректные параметры для проверки жалобы: postId={}, userId={}", postId, userId);
            return false;
        }

        try {
            return reportRepository.existsByPostIdAndReporterId(postId, userId);
        } catch (Exception e) {
            log.error("Ошибка при проверке статуса жалобы для поста {} пользователем {}: {}",
                    postId, userId, e.getMessage(), e);
            return false;
        }
    }

    private int calculatePriority(ReportReason reason) {
        return switch (reason) {
            case VIOLENCE -> 5; // Высокий приоритет - насилие
            case ADULT_CONTENT -> 4; // Высокий приоритет - контент 18+
            case FRAUD -> 4; // Высокий приоритет - мошенничество
            case INAPPROPRIATE -> 3; // Средний приоритет - неподходящий контент
            case SPAM -> 2; // Низкий приоритет - спам
            case OTHER -> 1; // Самый низкий приоритет - другое
        };
    }


    private ReportResponseDTO mapToDTO(PostReport report) {
        return ReportResponseDTO.builder()
                .id(report.getId())
                .postId(report.getPost().getId())
                .postContent(truncateContent(report.getPost().getContent()))
                .postAuthorName(report.getPost().getAuthor().getFirstName() + " " +
                        report.getPost().getAuthor().getLastName())
                .reporterName(report.getReporter().getFirstName() + " " +
                        report.getReporter().getLastName())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .priority(report.getPriority())
                .adminComment(report.getAdminComment())
                .actionTaken(report.getActionTaken())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .reviewedByName(report.getReviewedBy() != null ?
                        report.getReviewedBy().getFirstName() + " " +
                                report.getReviewedBy().getLastName() : null)
                .build();
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 100 ?
                content.substring(0, 100) + "..." : content;
    }
    public String getPriorityColor(int priority) {
        return switch (priority) {
            case 5 -> "danger"; // Красный
            case 4 -> "warning"; // Оранжевый
            case 3 -> "info"; // Синий
            case 2 -> "secondary"; // Серый
            default -> "light"; // Светло-серый
        };
    }

    public String getPriorityText(int priority) {
        return switch (priority) {
            case 5 -> "Критический";
            case 4 -> "Высокий";
            case 3 -> "Средний";
            case 2 -> "Низкий";
            default -> "Минимальный";
        };
    }
}
