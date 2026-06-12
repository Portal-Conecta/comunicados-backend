package com.portal.conecta.comunicados.shared.exception;

import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.tag.domain.exception.TagNotFoundException;
import com.portal.conecta.comunicados.module.tag.domain.exception.TagPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementConflictException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ApiError> handleTagNotFound(
            TagNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(TagPermissionDeniedException.class)
    public ResponseEntity<ApiError> handleTagPermissionDenied(
            TagPermissionDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedUserException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception, request);
    }

    @ExceptionHandler(AnnouncementPermissionDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(
            AnnouncementPermissionDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(AnnouncementNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            AnnouncementNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        String constraintName = extractConstraintName(exception);

        log.warn("Data integrity violation without mapped constraint. Constraint: {}", constraintName, exception);

        if (constraintName != null && constraintName.startsWith("uk_")) {
            return buildResponse(HttpStatus.CONFLICT, "Resource already exists.", request);
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "Data integrity violation.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return buildValidationResponse(exception.getBindingResult().getFieldErrors(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(
            BindException exception,
            HttpServletRequest request
    ) {
        return buildValidationResponse(exception.getBindingResult().getFieldErrors(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiError.FieldErrorDetail> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();

                    if (field.contains(".")) {
                        field = field.substring(field.lastIndexOf('.') + 1);
                    }

                    return new ApiError.FieldErrorDetail(
                            field,
                            violation.getMessage()
                    );
                })
                .toList();

        String message = errors.stream()
                .map(ApiError.FieldErrorDetail::message)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Invalid request.");

        return ResponseEntity
                .badRequest()
                .body(ApiError.validation(
                        HttpStatus.BAD_REQUEST,
                        message,
                        path(request),
                        errors
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter '%s'.".formatted(exception.getName());

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String message = "Required parameter '%s' is missing.".formatted(exception.getParameterName());

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        log.warn("Invalid request body.", exception);

        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request body.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = Objects.requireNonNullElse(exception.getReason(), exception.getMessage());

        return buildResponse(status, message, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.error("Runtime exception intercepted: ", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected error occurred: ", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request
        );
    }

    @ExceptionHandler(AnnouncementConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            AnnouncementConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception, request);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(status, exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(status)
                .body(ApiError.of(status, message, path(request)));
    }

    private ResponseEntity<ApiError> buildValidationResponse(
            List<FieldError> fieldErrors,
            HttpServletRequest request
    ) {
        List<ApiError.FieldErrorDetail> errors = fieldErrors.stream()
                .map(fieldError -> new ApiError.FieldErrorDetail(
                        fieldError.getField(),
                        Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Invalid value.")
                ))
                .toList();

        String message = errors.stream()
                .map(ApiError.FieldErrorDetail::message)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Invalid request.");

        return ResponseEntity
                .badRequest()
                .body(ApiError.validation(
                        HttpStatus.BAD_REQUEST,
                        message,
                        path(request),
                        errors
                ));
    }

    private String extractConstraintName(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolationException) {
                return constraintViolationException.getConstraintName();
            }

            current = current.getCause();
        }

        return null;
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
