package com.example.cleopatra.repository;

import com.example.cleopatra.enums.NewsType;
import com.example.cleopatra.model.ProjectNews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectNewsRepository extends JpaRepository<ProjectNews, Long> {

    /**
     * Получение всех опубликованных новостей с пагинацией (для публичного API)
     */
    Slice<ProjectNews> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    /**
     * Получение всех новостей с пагинацией (для админки)
     */
    Slice<ProjectNews> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Фильтрация по типу новости (опубликованные)
     */
    Slice<ProjectNews> findByIsPublishedTrueAndNewsTypeOrderByPublishedAtDesc(
            NewsType newsType, Pageable pageable);

    /**
     * Фильтрация по типу новости (все для админки)
     */
    Slice<ProjectNews> findByNewsTypeOrderByCreatedAtDesc(NewsType newsType, Pageable pageable);

    /**
     * Поиск по заголовку и описанию (опубликованные)
     */
    @Query("SELECT n FROM ProjectNews n WHERE n.isPublished = true AND " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "LOWER(n.description) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY n.publishedAt DESC")
    Slice<ProjectNews> findPublishedBySearchQuery(@Param("searchQuery") String searchQuery, Pageable pageable);

    /**
     * Поиск по заголовку и описанию (все для админки)
     */
    @Query("SELECT n FROM ProjectNews n WHERE " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "LOWER(n.description) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY n.createdAt DESC")
    Slice<ProjectNews> findBySearchQuery(@Param("searchQuery") String searchQuery, Pageable pageable);

    /**
     * Комбинированный поиск: тип + текстовый поиск (опубликованные)
     */
    @Query("SELECT n FROM ProjectNews n WHERE n.isPublished = true AND n.newsType = :newsType AND " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "LOWER(n.description) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "ORDER BY n.publishedAt DESC")
    Slice<ProjectNews> findPublishedByNewsTypeAndSearchQuery(
            @Param("newsType") NewsType newsType,
            @Param("searchQuery") String searchQuery,
            Pageable pageable);

    /**
     * Получение новости по ID (только опубликованной)
     */
    Optional<ProjectNews> findByIdAndIsPublishedTrue(Long id);

    /**
     * Получение последних опубликованных новостей без пагинации (для виджетов)
     */
    List<ProjectNews> findTop5ByIsPublishedTrueOrderByPublishedAtDesc();

    /**
     * Подсчет опубликованных новостей по типу
     */
    long countByIsPublishedTrueAndNewsType(NewsType newsType);

    /**
     * Подсчет всех опубликованных новостей
     */
    long countByIsPublishedTrue();

    /**
     * Увеличение счетчика просмотров
     */
    @Modifying
    @Query("UPDATE ProjectNews n SET n.viewsCount = n.viewsCount + 1 WHERE n.id = :newsId")
    void incrementViewCount(@Param("newsId") Long newsId);

    /**
     * Получение новостей конкретного автора
     */
    Slice<ProjectNews> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * Получение черновиков (неопубликованных новостей)
     */
    Slice<ProjectNews> findByIsPublishedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Фильтрация по статусу публикации и типу (черновики по типу)
     */
    Slice<ProjectNews> findByIsPublishedFalseAndNewsTypeOrderByCreatedAtDesc(NewsType newsType, Pageable pageable);


    Slice<ProjectNews> findByIsPublishedOrderByCreatedAtDesc(Boolean isPublished, Pageable pageable);
}