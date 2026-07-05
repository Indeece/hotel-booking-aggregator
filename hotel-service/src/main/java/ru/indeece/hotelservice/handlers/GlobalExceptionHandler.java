package ru.indeece.hotelservice.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.indeece.hotelservice.exceptions.ResourceNotFoundException;

import java.nio.file.AccessDeniedException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("code", "HOTEL_NOT_FOUND");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );

        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty("code", "FORBIDDEN_ACCESS");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}