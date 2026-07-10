package com.portal.conecta.comunicados.shared.exception;

import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementConflictException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileContentTypeNotAllowedException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileLimitExceededException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementFileTooLargeException;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementMustBeInTheFutureException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.tag.domain.exception.TagNotFoundException;
import com.portal.conecta.comunicados.module.tag.domain.exception.TagPermissionDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

    @ExceptionHandler(AnnouncementMustBeInTheFutureException.class)
    public ResponseEntity<ApiError> handleAnnouncementMustBeInTheFuture(
            AnnouncementMustBeInTheFutureException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception, request);
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
                .orElse("Requisição inválida.");

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
        String field = exception.getName();
        Class<?> requiredType = exception.getRequiredType();
        String message = isEnumType(requiredType)
                ? enumConversionMessage(field, requiredType)
                : "Parâmetro '%s' inválido.".formatted(field);

        List<ApiError.FieldErrorDetail> errors = List.of(new ApiError.FieldErrorDetail(field, message));

        return ResponseEntity
                .badRequest()
                .body(ApiError.validation(HttpStatus.BAD_REQUEST, message, path(request), errors));
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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, exception.getMessage(), request);
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

    @ExceptionHandler(AnnouncementFileNotFoundException.class)
    public ResponseEntity<ApiError> handleAnnouncementFileNotFound(
            AnnouncementFileNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception, request);
    }

    @ExceptionHandler(AnnouncementFileLimitExceededException.class)
    public ResponseEntity<ApiError> handleAnnouncementFileLimitExceeded(
            AnnouncementFileLimitExceededException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception, request);
    }

    @ExceptionHandler(AnnouncementFileContentTypeNotAllowedException.class)
    public ResponseEntity<ApiError> handleAnnouncementFileContentTypeNotAllowed(
            AnnouncementFileContentTypeNotAllowedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception, request);
    }

    @ExceptionHandler(AnnouncementFileTooLargeException.class)
    public ResponseEntity<ApiError> handleAnnouncementFileTooLarge(
            AnnouncementFileTooLargeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, exception, request);
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
                        resolveFieldMessage(fieldError)
                ))
                .toList();

        String message = errors.stream()
                .map(ApiError.FieldErrorDetail::message)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Requisição inválida.");

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

    private String resolveFieldMessage(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage();

        if (isTypeConversionMessage(defaultMessage)) {
            return queryParameterConversionMessage(fieldError.getField());
        }

        return Objects.requireNonNullElse(defaultMessage, "Valor inválido.");
    }

    private boolean isTypeConversionMessage(String message) {
        return message != null && message.contains("Failed to convert");
    }

    private String queryParameterConversionMessage(String field) {
        return switch (field) {
            case "origin" -> enumConversionMessage(field, AnnouncementOrigin.class);
            case "classId" -> "Identificador de turma inválido. Informe um UUID válido.";
            case "publishedFrom", "publishedTo" ->
                    "Data inválida. Use o formato ISO-8601 (ex.: 2026-06-12T10:00:00Z).";
            case "page" -> "Página inválida. Informe um número inteiro maior ou igual a 0.";
            case "size" -> "Tamanho da página inválido. Informe um número entre 1 e 100.";
            default -> "Valor inválido para o parâmetro '%s'.".formatted(field);
        };
    }

    private boolean isEnumType(Class<?> type) {
        return type != null && type.isEnum();
    }

    private String enumConversionMessage(String field, Class<?> enumType) {
        if (AnnouncementOrigin.class.equals(enumType) || "origin".equals(field)) {
            return "Origem inválida. Valores aceitos: WEG, SENAI, BOTH.";
        }

        String allowedValues = Arrays.stream(enumType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        return "Valor inválido para '%s'. Valores aceitos: %s.".formatted(field, allowedValues);
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
