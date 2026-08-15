package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;
    private final String[] details;

    public ErrorResponse(String error, String[] message) {
        this.error = error;
        this.details = message;
    }

    public ErrorResponse(String error) {
        this.error = error;
        this.details = null;
    }
}
