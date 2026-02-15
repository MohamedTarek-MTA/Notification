# Senior Engineer Code Review: Notification System

## Architecture Overview

**Current flow:** Producer (external) → Kafka → Consumer (this service) → MongoDB → Controller (WebFlux) → Client.

---

## 1. Logic Issues

### 1.1 Document collection name (FIXED)

- **Issue:** `@Document(collation = "notifications")` used `collation` instead of `collection`. In Spring Data MongoDB, `collation` is for string comparison rules; the collection name is set by `collection`. Documents were going to the default collection name (e.g. `notification`), not `notifications`.
- **Fix applied:** Changed to `@Document(collection = "notifications")`.

### 1.2 Kafka consumer deserialization

- **Issue:** `application.properties` uses `StringDeserializer` for the consumer, but `NotificationConsumer.consume(NotificationDTO dto)` expects a `NotificationDTO`. With the current config the broker sends bytes that are deserialized as `String`; passing that into a method that expects `NotificationDTO` will fail (e.g. conversion/type error).
- **Fix:** Use JSON deserialization for the consumer value, and align producer (in the other service) to send JSON.

**application.properties:**

```properties
# Consumer: use JSON for value
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

**Consumer (optional explicit config):**

```java
@KafkaListener(topics = "notification-topic")
public void consume(NotificationDTO dto) {
    // ... existing logic
}
```

Ensure the producer (in the other app) uses `JsonSerializer` and sends the same JSON shape as `NotificationDTO`.

### 1.3 Fire-and-forget in consumer

- **Issue:** `notificationRepository.save(notification).subscribe();` subscribes with no error or completion handling. Failures (DB down, validation, etc.) are invisible and Kafka will still commit the offset, so the message is effectively lost.
- **Fix:** Handle errors and consider offset commit behavior.

**Option A – Block and handle (simplest):**

```java
@KafkaListener(topics = "notification-topic")
public void consume(NotificationDTO dto) {
    Notification notification = mapToNotification(dto);
    notificationRepository.save(notification)
            .doOnError(e -> log.error("Failed to save notification", e))
            .block(); // or use a different strategy (see below)
}
```

**Option B – Return Mono and use reactive listener (recommended):**  
Use a reactive Kafka listener so that the framework subscribes for you and can tie offset commit to successful completion. Then you can return `Mono<Void>` and avoid manual `.subscribe()` and `.block()` in the listener.

### 1.4 JWT claim for user identity

- **Issue:** Controller uses `jwt.getClaimAsString("id")` for `userId`. Many OAuth2/JWT setups use `sub` (subject) for user identity. If your IdP puts the user id in `sub` (or another claim), `getClaimAsString("id")` can be null and all notifications would be filtered to “no user.”
- **Fix:** Align with your IdP. For example, if user id is in `sub`:

```java
// Prefer sub if that's where your IdP puts user id
String userId = jwt.getClaimAsString("id");
if (userId == null) {
    userId = jwt.getSubject();
}
```

Or standardize on one claim (e.g. `sub` or a custom `userId`) in both the IdP and this service.

---

## 2. Performance Issues

### 2.1 Pagination

- **Issue:** `getUserNotifications` returns `Flux<Notification>` with no limit. A user with many notifications can pull a very large result set and increase memory and latency.
- **Fix:** Add pagination (e.g. `Pageable` / limit + offset or limit + cursor) in repository and controller, and return a bounded stream or a page DTO.

**Repository:**

```java
Flux<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
```

**Controller:**

```java
@GetMapping
public Flux<Notification> getMyNotifications(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "0") int page) {
    return notificationService.getUserNotifications(
            jwt.getClaimAsString("id"),
            PageRequest.of(page, size));
}
```

### 2.2 Indexes

- **Current:** `userId` and TTL on `createdAt` are indexed, which is good for “my notifications” and expiry.
- **Suggestion:** If you often query “unread count” or “unread for user,” a compound index can help, e.g. `{ userId: 1, read: 1 }`. Add via `@CompoundIndex` or MongoDB shell.

### 2.3 Reactive Kafka consumer

- **Current:** `@KafkaListener` with a blocking-style `subscribe()`/`block()` mixes reactive and blocking. For high throughput, a reactive Kafka consumer (e.g. `ReactorKafkaReceiver`) allows non-blocking, backpressure-friendly consumption and fits WebFlux better.

---

## 3. Security Issues

### 3.1 Secret key logging (FIXED)

- **Issue:** `System.out.println("Secret Key is : "+secretKey);` in `SecurityConfig` logs the JWT secret. Anyone with log access can forge tokens.
- **Fix applied:** Removed the `System.out.println` line.

### 3.2 Secrets in application.properties

- **Issue:** `spring.jwt.secretKey`, SSL keystore password, and other secrets are in plain text in `application.properties`. They get committed to Git and are visible to anyone with repo access.
- **Fix:**  
  - Use environment variables or a secret manager (e.g. `SPRING_JWT_SECRETKEY`, `SERVER_SSL_KEY_STORE_PASSWORD`).  
  - Exclude real secrets from Git; keep a `application.properties.example` with placeholders.  
  - In production, never commit production secrets.

### 3.3 User can only access their own notifications (current design)

- **How it works:**  
  - All notification APIs are under `/api/v1/**`, which is `authenticated()`.  
  - Controller passes only the authenticated user’s id (from JWT) to the service: `getUserNotifications(jwt.getClaimAsString("id"))`, `getNumberOfUnreadNotificationsByUserId(...)`, and `markAsReadByNotificationId(id, jwt.getClaimAsString("id"))`.  
  - Service layer filters by `userId`: e.g. `markAsReadByNotificationId` uses `filter(notification -> userId.equals(notification.getUserId()))`.  
- **Verdict:** Users can only see and act on their own notifications as long as:  
  - You never take `userId` from the request body or path for these operations.  
  - JWT is validated and the claim you use for “current user” is correct (see JWT claim section above).

**Recommendation:**  
- Do not add a “get notification by id” API that takes an id from the path without checking ownership. If you do, always resolve the notification and then check `notification.getUserId().equals(currentUserId)`.  
- Optionally add a small security test: call “get my notifications” and “mark as read” with a token for user A and assert you never see or update user B’s data.

---

## 4. Is WebFlux + Kafka + MongoDB a Good Fit?

**Short answer: Yes**, for an async, scalable notification pipeline.

| Component   | Role |
|------------|------|
| **Kafka**  | Decouples producers (many services) from this notification service; buffers traffic; allows replay and multiple consumers later (e.g. analytics, push). |
| **MongoDB**| Good for document-shaped notifications, flexible schema, and TTL (you already use it for 30d expiry). |
| **WebFlux**| Fits reactive repository and non-blocking I/O; scales well for many concurrent “get my notifications” calls. |

**Caveats:**

- **Exactly-once:** Currently at-least-once: consumer can commit offset after save; if save succeeds and commit fails, you might process the same message again (duplicate notification). If you need exactly-once, you’d need idempotency (e.g. dedup key in the message + unique index or “upsert by idempotency key”) or transactional outbox patterns.
- **Ordering:** Order per user is preserved only if the producer sends to a partition keyed by `userId` and you have a single consumer per partition. Otherwise, ordering is per partition, not per user.
- **Producer:** This repo only has the consumer. The app that produces must send JSON that matches `NotificationDTO` and use the same topic and (recommended) `JsonSerializer`.

---

## 5. Ensuring Users Access Only Their Notifications – Summary

1. **Already in place:**  
   - All notification endpoints require authentication.  
   - `userId` is taken only from the JWT (`getClaimAsString("id")` or similar), not from request body/path.  
   - Service uses this `userId` for reads and for `markAsRead` (with a filter by `notification.getUserId()`).

2. **To harden:**  
   - Fix JWT claim (e.g. fallback to `sub` or standardize on one claim).  
   - Never expose an API that returns a notification by id without checking `notification.getUserId().equals(currentUserId)`.  
   - Add pagination and indexes so the “my notifications” path stays fast and bounded.

3. **Optional:**  
   - Add integration tests that call the API with two users and assert no cross-user data.  
   - Consider a simple audit log or metric when notifications are read, to detect misuse.

---

## 6. Action Checklist

- [x] Fix `@Document(collection = "notifications")`.
- [x] Remove `System.out.println` of secret in `SecurityConfig`.
- [ ] Switch consumer to JSON deserializer and ensure producer sends JSON.
- [ ] Replace fire-and-forget `.subscribe()` in consumer with error handling and/or reactive Kafka listener.
- [ ] Align JWT user id claim with IdP (`id` vs `sub`).
- [ ] Add pagination to “get my notifications.”
- [ ] Move secrets to environment variables / secret manager and stop committing them.
- [ ] (Optional) Add compound index `{ userId, read }` and reactive Kafka consumer for scalability.

Once these are done, the design (WebFlux + Kafka + MongoDB, with consumer saving and controller reading by authenticated user) is sound and production-ready with clear ownership of notifications per user.
