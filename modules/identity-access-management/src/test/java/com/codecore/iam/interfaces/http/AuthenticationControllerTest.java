package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.domain.exception.InvalidCredentialsException;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock
    private AuthenticateIdentityUseCase authenticateIdentityUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        AuthenticationController controller = new AuthenticationController(authenticateIdentityUseCase);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new IamHttpExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200AndDelegateToUseCase() {
        when(authenticateIdentityUseCase.execute(any()))
                .thenReturn(Mono.just(new AuthenticationResponse("jwt-token", "Bearer", 900L)));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("user@codecore.local", "ValidPass1!"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("jwt-token")
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isEqualTo(900);

        verify(authenticateIdentityUseCase).execute(any());
    }

    @Test
    void shouldReturn401WhenUseCaseRejectsCredentials() {
        when(authenticateIdentityUseCase.execute(any()))
                .thenReturn(Mono.error(new InvalidCredentialsException("Invalid credentials")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("user@codecore.local", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
