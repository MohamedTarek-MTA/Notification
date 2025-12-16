package com.notification.DTO;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String id;

    private String userId;
    private String targetType;
    private String message;
    private String appName;
    private Instant createdAt;
    private boolean read;
}
