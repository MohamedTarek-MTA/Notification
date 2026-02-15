package com.notification.Controller;

import com.notification.Document.Notification;
import com.notification.Service.NotificationService;
//import com.notification.Util.JwtUtils;
import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping(value = "/{userId}", produces = "text/event-stream")
    public Flux<Notification> getMyNotifications(@PathVariable String userId) {
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/number-of-unread-notifications/{userId}")
    public Mono<Long> getTotalNumberOf(@PathVariable String userId) {
        return notificationService.getNumberOfUnreadNotificationsByUserId(userId);
    }

    @PatchMapping("/notification/{notificationId}/mark-as-read/{userId}")
    public Mono<Void> markAsRead(@PathVariable String notificationId,@PathVariable String userId) {
        return notificationService.markAsReadByNotificationId(notificationId,userId);
    }
}
