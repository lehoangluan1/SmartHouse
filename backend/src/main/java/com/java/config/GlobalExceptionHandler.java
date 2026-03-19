package com.java.config;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========================
    // Business Exceptions
    // ========================

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Data not found", ex.getMessage(), null);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), null);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleForbidden(Exception ex) {
        log.warn("Forbidden: {}", ex.getMessage(), ex);
        return build(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action",
                ex.getMessage(),
                null);
    }

    // ========================
    // Validation
    // ========================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", message);

        return build(HttpStatus.BAD_REQUEST, "Invalid input data", message, details);
    }

    // ========================
    // JSON / Jackson
    // ========================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonParse(HttpMessageNotReadableException ex) {
        log.warn("JSON parse error: {}", ex.getMessage(), ex);

        String message = "Malformed JSON request";
        if (ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON format",
                message,
                null
        );
    }
    
    // ========================
    // Number / Type Convert
    // ========================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("Type mismatch: {}", message, ex);

        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter type",
                message,
                null
        );
    }

    @ExceptionHandler({
            ConversionFailedException.class,
            NumberFormatException.class,
            TypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleConvertException(Exception ex) {
        log.warn("Conversion error: {}", ex.getMessage(), ex);

        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter format",
                "Failed to convert value to required type",
                null
        );
    }

    // ========================
    // AMQP / RabbitMQ
    // ========================

    @ExceptionHandler({
            AmqpException.class,
            ListenerExecutionFailedException.class,
            AmqpRejectAndDontRequeueException.class
    })
    public ResponseEntity<ApiError> handleAmqpException(Exception ex) {
        log.error("AMQP error: {}", ex.getMessage(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Message queue error",
                "Error occurred while processing message",
                null
        );
    }

    // ========================
    // Fallback
    // ========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Please try again later or contact the administrator",
                null
        );
    }

    // ========================
    // Builder
    // ========================

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> details
    ) {
        return ResponseEntity.status(status).body(
                ApiError.builder()
                        .code(status.name())
                        .error(error)
                        .message(message)
                        .details(details)
                        .timestamp(OffsetDateTime.now())
                        .build()
        );
    }
}