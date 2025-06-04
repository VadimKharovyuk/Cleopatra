package com.example.cleopatra.model;

import com.example.cleopatra.enums.DeliveryStatus;
import com.example.cleopatra.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Отправитель сообщения
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Получатель сообщения
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Текст сообщения
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;


    // Прочитано ли сообщение
    @Column(name = "is_read")
    private Boolean isRead = false;

    // Время прочтения
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Удалено ли сообщение отправителем
    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;

    // Удалено ли сообщение получателем
    @Column(name = "deleted_by_recipient")
    private Boolean deletedByRecipient = false;

    // Отредактировано ли сообщение
    @Column(name = "is_edited")
    private Boolean isEdited = false;

    // ✅ Добавляем тип сообщения
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20)
    private MessageType messageType = MessageType.TEXT;


    // ID сообщения на которое отвечаем (для replies)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    // Статус доставки
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    // Время создания
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Время обновления
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.SENT;
        }
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
    }


}
