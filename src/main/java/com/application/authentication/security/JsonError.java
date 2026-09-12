package com.application.authentication.security;

/**
 * Hand-rolled {"success":false,"message":"..."} JSON, matching the shape of
 * ApiResponse.error(...), for the two spots (auth entry point, access denied
 * handler) that run outside normal Spring MVC message conversion and so can't
 * just return an ApiResponse object.
 */
final class JsonError {

    private JsonError() {
    }

    static String of(String message) {
        return "{\"success\":false,\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}