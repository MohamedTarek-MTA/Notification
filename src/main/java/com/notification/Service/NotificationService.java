package com.notification.Service;

import com.notification.Document.Notification;
import com.notification.Repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public Flux<Notification> getUserNotifications(String userId){
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Mono<Long> getNumberOfUnreadNotificationsByUserId(String userId){
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public Mono<Void> markAsReadByNotificationId(String notificationId){
        return notificationRepository.findById(notificationId).flatMap(
                notification -> {
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                }
        ).then();
    }
}
