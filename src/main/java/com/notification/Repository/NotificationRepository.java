package com.notification.Repository;

import com.notification.Document.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends ReactiveMongoRepository<Notification,String> {
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(String userId);


    Mono<Long> countByUserIdAndReadFalse(String userId);

}
