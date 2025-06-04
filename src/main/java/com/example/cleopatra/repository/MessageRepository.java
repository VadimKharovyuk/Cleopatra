package com.example.cleopatra.repository;

import com.example.cleopatra.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Получить сообщения между двумя пользователями
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender.id = :userId1 AND m.recipient.id = :userId2 AND m.deletedBySender = false) OR " +
            "(m.sender.id = :userId2 AND m.recipient.id = :userId1 AND m.deletedByRecipient = false)) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findConversation(@Param("userId1") Long userId1,
                                   @Param("userId2") Long userId2,
                                   Pageable pageable);

    // Получить непрочитанные сообщения от конкретного отправителя
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :recipientId " +
            "AND m.sender.id = :senderId AND m.isRead = false " +
            "AND m.deletedByRecipient = false")
    List<Message> findUnreadMessagesFromSender(@Param("senderId") Long senderId,
                                               @Param("recipientId") Long recipientId);

    // Получить все непрочитанные сообщения для пользователя
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :userId " +
            "AND m.isRead = false AND m.deletedByRecipient = false")
    List<Message> findUnreadMessages(@Param("userId") Long userId);

    // Количество непрочитанных сообщений
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId " +
            "AND m.isRead = false AND m.deletedByRecipient = false")
    Long countUnreadMessages(@Param("userId") Long userId);

    // Последнее сообщение в конверсации
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender.id = :userId1 AND m.recipient.id = :userId2 AND m.deletedBySender = false) OR " +
            "(m.sender.id = :userId2 AND m.recipient.id = :userId1 AND m.deletedByRecipient = false)) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findLastMessage(@Param("userId1") Long userId1,
                                  @Param("userId2") Long userId2,
                                  Pageable pageable);

    // ✅ ИСПРАВЛЕННЫЙ запрос для получения конверсаций
    // Вместо сложного native query используем простой JPQL
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :userId OR m.recipient.id = :userId) " +
            "AND ((m.sender.id = :userId AND m.deletedBySender = false) OR " +
            "(m.recipient.id = :userId AND m.deletedByRecipient = false)) " +
            "AND m.id IN (" +
            "  SELECT MAX(m2.id) FROM Message m2 WHERE " +
            "  ((m2.sender.id = :userId AND m2.recipient.id = m.recipient.id AND m.sender.id = :userId) OR " +
            "   (m2.sender.id = :userId AND m2.recipient.id = m.sender.id AND m.recipient.id = :userId) OR " +
            "   (m2.sender.id = m.sender.id AND m2.recipient.id = :userId AND m.recipient.id = :userId) OR " +
            "   (m2.sender.id = m.recipient.id AND m2.recipient.id = :userId AND m.sender.id = :userId)) " +
            "  GROUP BY CASE WHEN m2.sender.id = :userId THEN m2.recipient.id ELSE m2.sender.id END" +
            ") " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findUserConversations(@Param("userId") Long userId, Pageable pageable);


    // Поиск сообщений по различным критериям
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :userId OR m.recipient.id = :userId) " +
            "AND (:searchText IS NULL OR LOWER(m.content) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (:otherUserId IS NULL OR " +
            "(m.sender.id = :otherUserId AND m.recipient.id = :userId) OR " +
            "(m.sender.id = :userId AND m.recipient.id = :otherUserId)) " +
            "AND (:dateFrom IS NULL OR m.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR m.createdAt <= :dateTo) " +
            "AND (:unreadOnly IS NULL OR :unreadOnly = false OR " +
            "(m.recipient.id = :userId AND m.isRead = false)) " +
            "AND ((m.sender.id = :userId AND m.deletedBySender = false) OR " +
            "(m.recipient.id = :userId AND m.deletedByRecipient = false))")
    Page<Message> searchMessages(@Param("userId") Long userId,
                                 @Param("searchText") String searchText,
                                 @Param("otherUserId") Long otherUserId,
                                 @Param("dateFrom") LocalDateTime dateFrom,
                                 @Param("dateTo") LocalDateTime dateTo,
                                 @Param("unreadOnly") Boolean unreadOnly,
                                 Pageable pageable);

    // Получить количество сообщений в конверсации
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
            "((m.sender.id = :userId1 AND m.recipient.id = :userId2 AND m.deletedBySender = false) OR " +
            "(m.sender.id = :userId2 AND m.recipient.id = :userId1 AND m.deletedByRecipient = false))")
    Long countConversationMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender.id = :userId")
    Long countSentMessages(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient.id = :userId")
    Long countReceivedMessages(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT CASE WHEN m.sender.id = :userId THEN m.recipient.id ELSE m.sender.id END) " +
            "FROM Message m WHERE m.sender.id = :userId OR m.recipient.id = :userId")
    Long countConversations(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.recipient.id = :userId AND m.isRead = false " +
            "AND m.deletedByRecipient = false ORDER BY m.createdAt DESC")
    List<Message> findRecentUnreadMessages(@Param("userId") Long userId, Pageable pageable);
    }