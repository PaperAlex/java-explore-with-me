package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;


@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(final BadRequestException e) {
        log.error(e.getMessage());
        return ErrorResponse.builder()
                .message(e.getMessage())
                .reason("Incorrectly made request.")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleThrowable(final InternalServerException e) {
        log.info(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage());
        return ErrorResponse.builder()
                .message(e.getMessage())
                .status("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }
}

