package com.notification.Document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collation = "notifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;
    private String fromUserId;
    private String targetType;
    private String message;
    private String appName;
    private boolean read;

    @Indexed(expireAfter = "30d")
    private Instant createdAt;
}


//// Just Comment !!