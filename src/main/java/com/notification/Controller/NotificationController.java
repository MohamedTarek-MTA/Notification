package com.notification.Controller;

import com.notification.Document.Notification;
import com.notification.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Flux<Notification> getMyNotifications(@AuthenticationPrincipal Jwt jwt){
        return notificationService.getUserNotifications(jwt.getClaimAsString("id"));
    }

    @GetMapping("/number-of-unread-notifications")
    public Mono<Long> getTotalNumberOf(@AuthenticationPrincipal Jwt jwt){
        return notificationService.getNumberOfUnreadNotificationsByUserId(jwt.getClaimAsString("id"));
    }

    @PatchMapping("/notification/{id}/mark-as-read")
    public Mono<Void> markAsRead(@PathVariable String id,@AuthenticationPrincipal Jwt jwt){
        return notificationService.markAsReadByNotificationId(id,jwt.getClaimAsString("id"));
    }
}
