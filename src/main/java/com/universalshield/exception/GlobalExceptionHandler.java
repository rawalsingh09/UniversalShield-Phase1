package com.universalshield.exception;

import com.universalshield.dto.ApiError;
import com.universalshield.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Converts every exception into the standard {@link ApiResponse} error envelope with an
 * appropriate status code. Unexpected errors are logged by type only and return a generic
 * message, so stack traces and input data never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> api(ApiException e) {
        return respond(e.getStatus(), ApiError.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException e) {
        List<ApiError.FieldProblem> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldProblem(f.getField(), f.getDefaultMessage())).toList();
        return respond(HttpStatus.BAD_REQUEST, new ApiError("VALIDATION_FAILED", "Some fields are invalid.", fields));
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> constraint(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("VALIDATION_FAILED", "A request parameter is invalid."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("MALFORMED_REQUEST",
                "The request body is missing or is not valid JSON of the expected shape."));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> badParam(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("INVALID_PARAMETER", "A request parameter is missing or has the wrong type."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, ApiError.of("FILE_TOO_LARGE", "The uploaded file is too large."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> method(HttpRequestMethodNotSupportedException e) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ApiError.of("METHOD_NOT_ALLOWED", "This HTTP method is not supported here."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> mediaType(HttpMediaTypeNotSupportedException e) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiError.of("UNSUPPORTED_MEDIA_TYPE", "Unsupported content type."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> noResource(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, ApiError.of("NOT_FOUND", "Resource not found."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> unauthenticated(AuthenticationException e) {
        return respond(HttpStatus.UNAUTHORIZED, ApiError.of("UNAUTHORIZED", "Authentication is required."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
        return respond(HttpStatus.FORBIDDEN, ApiError.of("FORBIDDEN", "You are not allowed to do this."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        log.error("Unexpected error: {}", e.getClass().getName(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiError.of("INTERNAL_ERROR",
                "Something went wrong on our side. Please try again."));
    }

    private static ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, ApiError error) {
        return ResponseEntity.status(status).body(ApiResponse.fail(error));
    }
}
