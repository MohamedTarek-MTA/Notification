package com.notification.Consumer;

import com.notification.DTO.NotificationDTO;
import com.notification.Document.Notification;
import com.notification.Repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "notification-topic")
    public void consume(NotificationDTO dto){
        Notification notification = new Notification(
                null,
                dto.getUserId(),
                dto.getFromUserId(),
                dto.getTargetType(),
                dto.getMessage(),
                dto.getAppName(),
                false,
                Instant.now()
        );
        notificationRepository.save(notification).subscribe();
    }

}
