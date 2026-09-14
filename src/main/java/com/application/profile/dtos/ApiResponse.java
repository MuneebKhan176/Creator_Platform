package com.application.profile.dtos;

/**
 * Mirrors the {success, message, data} shape your frontend's api.ts already
 * expects. I didn't have the source of authentication.dtos.ApiResponse to
 * confirm its exact static-factory API, so this is a small self-contained
 * copy scoped to the profile module — swap it out for your existing class
 * later if the shapes line up.
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}