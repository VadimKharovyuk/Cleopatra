package com.example.cleopatra.service;

import com.example.cleopatra.dto.VisitDto.VisitDtoCard;
import com.example.cleopatra.dto.VisitDto.VisitListDto;
import com.example.cleopatra.dto.VisitDto.VisitStatsDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitService {

    void recordVisit(Long visitedUserId, Long visitorId, String ipAddress, String userAgent);

    VisitListDto getRecentVisitors(Long userId, Pageable pageable) ;

    List<VisitDtoCard> getUniqueVisitors(Long userId, LocalDateTime from, LocalDateTime to) ;


    VisitStatsDto getVisitStats(Long id);


}
