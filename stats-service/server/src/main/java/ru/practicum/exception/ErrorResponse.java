package ru.practicum.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private final List<String> errors;
    private final String message;
    private final String reason;
    private final String status;
    private final LocalDateTime timestamp;
}
