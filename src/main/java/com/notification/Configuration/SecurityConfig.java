//package com.notification.Configuration;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.Resource;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
//import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.util.StreamUtils;
//
//import javax.crypto.SecretKey;
//import javax.crypto.spec.SecretKeySpec;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.security.KeyFactory;
//import java.security.interfaces.RSAPublicKey;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//@Configuration
//@EnableWebFluxSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        return http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//                .authorizeExchange(exchanges -> exchanges
//                        .pathMatchers(
//                                "/swagger-ui.html",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**"
//                        ).permitAll()
//                        .pathMatchers("/api/v1/**").authenticated()
//                        .anyExchange().permitAll()
//                )
//                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(jwt -> {})
//                )
//                .build();
//    }
//
//    /** Optional: e.g. classpath:public.pem — use when JWTs are signed with RSA (e.g. from same cert as server). */
//    @Value("${spring.jwt.public-key-location:}")
//    private String publicKeyLocation;
//
//    @Value("${spring.jwt.secretKey:}")
//    private String secretKey;
//
//    @Bean
//    public ReactiveJwtDecoder jwtDecoder(Resource publicKeyResource) throws Exception {
//        // Prefer RSA public key when token is signed by the same certificate (e.g. auth service using .pfx private key)
//        if (publicKeyResource.exists() && publicKeyResource.isReadable() && publicKeyResource.contentLength() > 0) {
//            RSAPublicKey rsaPublicKey = loadRsaPublicKeyFromPem(publicKeyResource);
//            return NimbusReactiveJwtDecoder.withPublicKey(rsaPublicKey).build();
//        }
//        // Fallback: HMAC (symmetric) — issuer must sign with the same secret
//        if (secretKey == null || secretKey.isBlank()) {
//            throw new IllegalStateException(
//                    "Configure either spring.jwt.public-key-location (RSA) or spring.jwt.secretKey (HMAC) for JWT validation.");
//        }
//        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
//        SecretKey key = new SecretKeySpec(decodedKey, "HmacSHA512");
//        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
//    }
//
//    /**
//     * Bean only created when public-key-location is set, so Resource is optional.
//     * When not set, inject a non-existing Resource to avoid failure.
//     */
//    @Bean
//    public Resource publicKeyResource(
//            @Value("${spring.jwt.public-key-location:}") String location) {
//        if (location == null || location.isBlank()) {
//            return new org.springframework.core.io.ByteArrayResource(new byte[0]);
//        }
//        return new org.springframework.core.io.DefaultResourceLoader().getResource(location);
//    }
//
//    private static RSAPublicKey loadRsaPublicKeyFromPem(Resource resource) throws Exception {
//        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
//        String pem = tryDecodeUtf8(bytes);
//        if (pem == null) {
//            pem = new String(bytes, StandardCharsets.UTF_16LE);
//        }
//        pem = pem.replace("\uFEFF", "");
//        String base64 = pem
//                .replace("-----BEGIN PUBLIC KEY-----", "")
//                .replace("-----END PUBLIC KEY-----", "")
//                .replaceAll("\\s+", "");
//        byte[] decoded = Base64.getDecoder().decode(base64);
//        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
//        KeyFactory kf = KeyFactory.getInstance("RSA");
//        return (RSAPublicKey) kf.generatePublic(spec);
//    }
//
//    /** Decode as UTF-8; return null if invalid (e.g. file is UTF-16). */
//    private static String tryDecodeUtf8(byte[] bytes) {
//        try {
//            String s = new String(bytes, StandardCharsets.UTF_8);
//            if (s.contains("BEGIN PUBLIC KEY")) return s;
//            return null;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//}
