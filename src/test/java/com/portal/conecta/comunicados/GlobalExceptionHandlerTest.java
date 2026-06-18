package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.shared.exception.ApiError;
import com.portal.conecta.comunicados.shared.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/posts");
    }

    @Test
    void shouldReturnFriendlyMessage_WhenOriginIsInvalid() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "postFilterRequest");
        bindingResult.addError(new FieldError(
                "postFilterRequest",
                "origin",
                "TEACHER",
                false,
                null,
                null,
                "Failed to convert value of type 'java.lang.String' to required type "
                        + "'com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin'"
        ));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = handler.handleMethodArgumentNotValid(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Origem inválida. Valores aceitos: WEG, SENAI, BOTH.");
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().getFirst().field()).isEqualTo("origin");
        assertThat(response.getBody().errors().getFirst().message())
                .isEqualTo("Origem inválida. Valores aceitos: WEG, SENAI, BOTH.");
        assertThat(response.getBody().message()).doesNotContain("AnnouncementOrigin");
    }

    @Test
    void shouldReturnFriendlyMessage_WhenQueryParamTypeMismatchIsEnum() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("origin");
        when(exception.getRequiredType()).thenReturn((Class) com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin.class);

        ResponseEntity<ApiError> response = handler.handleMethodArgumentTypeMismatch(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Origem inválida. Valores aceitos: WEG, SENAI, BOTH.");
    }
}
