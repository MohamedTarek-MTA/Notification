//package com.notification.Util;
//
//import org.springframework.security.oauth2.jwt.Jwt;
//
///**
// * Extracts user identity from JWT. Use this so the same logic works whether
// * your issuer puts the user id in "id", "sub", or "userId".
// */
//public final class JwtUtils {
//
//    private JwtUtils() {}
//
//    /**
//     * Preferred claim names for user id, in order. First non-null value is returned.
//     * Many issuers use "sub" (subject); some use a custom "id" or "userId".
//     */
//    private static final String[] USER_ID_CLAIMS = {"id", "userId", "user_id", "sub"};
//
//    /**
//     * Get the current user's id from the JWT. Tries common claim names so it works
//     * whether the issuer uses "id", "userId", or standard "sub".
//     *
//     * @param jwt the authenticated JWT (must not be null)
//     * @return user id, or null if no known claim is present
//     */
//    public static String getUserId(Jwt jwt) {
//        if (jwt == null) {
//            return null;
//        }
//        for (String claimName : USER_ID_CLAIMS) {
//            String value = jwt.getClaimAsString(claimName);
//            if (value != null && !value.isBlank()) {
//                return value;
//            }
//        }
//        return jwt.getSubject();
//    }
//}
