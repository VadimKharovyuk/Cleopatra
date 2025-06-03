package com.example.cleopatra.maper;

import com.example.cleopatra.dto.VisitDto.VisitDtoCard;
import com.example.cleopatra.dto.VisitDto.VisitListDto;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.Visit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class VisitMapper {

    public VisitDtoCard toVisitDtoCard(Visit visit) {
        if (visit == null) {
            return null;
        }

        try {
            User visitor = visit.getVisitor();

            return VisitDtoCard.builder()
                    .id(visit.getId())
                    .visitorId(visitor != null ? visitor.getId() : null)
                    .visitorFirstName(visitor != null ? visitor.getFirstName() : null)
                    .visitorLastName(visitor != null ? visitor.getLastName() : null)
                    .visitorImageUrl(visitor != null ? visitor.getImageUrl() : null)
                    .visitorCity(visitor != null ? visitor.getCity() : null)
//                    .visitorIsOnline(visitor != null ? visitor.getIsOnline() : false)
                    .visitedAt(visit.getVisitedAt())
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при маппинге Visit в VisitDtoCard: {}", e.getMessage());
            return null;
        }
    }

    public List<VisitDtoCard> toVisitDtoCardList(List<Visit> visits) {
        if (visits == null || visits.isEmpty()) {
            return new ArrayList<>();
        }

        return visits.stream()
                .map(this::toVisitDtoCard)
                .filter(Objects::nonNull) // Исключаем null результаты
                .collect(Collectors.toList());
    }

    public VisitListDto toVisitListDto(Page<Visit> visitPage) {
        if (visitPage == null) {
            return createEmptyVisitListDto(0, 20);
        }

        try {
            List<VisitDtoCard> visitCards = toVisitDtoCardList(visitPage.getContent());

            return VisitListDto.builder()
                    .visits(visitCards)
                    .currentPage(visitPage.getNumber())
                    .totalPages(visitPage.getTotalPages())
                    .totalElements(visitPage.getTotalElements())
                    .hasNext(visitPage.hasNext())
                    .hasPrevious(visitPage.hasPrevious())
                    .pageSize(visitPage.getSize())
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при маппинге Page<Visit> в VisitListDto: {}", e.getMessage());
            return createEmptyVisitListDto(
                    visitPage.getNumber(),
                    visitPage.getSize()
            );
        }
    }

    public VisitListDto createEmptyVisitListDto(int page, int size) {
        return VisitListDto.builder()
                .visits(new ArrayList<>())
                .currentPage(page)
                .totalPages(0)
                .totalElements(0L)
                .hasNext(false)
                .hasPrevious(false)
                .pageSize(size)
                .build();
    }
}

