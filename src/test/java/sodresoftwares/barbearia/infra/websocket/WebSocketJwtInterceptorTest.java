package sodresoftwares.barbearia.infra.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import sodresoftwares.barbearia.infra.security.TokenService;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketJwtInterceptor Tests")
class WebSocketJwtInterceptorTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private DecodedJWT decodedJWT;

    @InjectMocks
    private WebSocketJwtInterceptor interceptor;

    @Test
    @DisplayName("Should authenticate user when valid token is provided on CONNECT")
    void shouldAuthenticateUserOnConnect() {
        // Arrange
        String validToken = "valid-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + validToken);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        User testUser = User.builder().id("123").login("test@test.com").role(UserRole.USER).build();
        var auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        when(tokenService.validateAndDecodeToken(validToken)).thenReturn(decodedJWT);
        when(tokenService.getAuthentication(decodedJWT)).thenReturn(auth);

        // Act
        Message<?> resultMessage = interceptor.preSend(message, messageChannel);

        // Assert
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(resultMessage);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser()).isEqualTo(auth);
    }

    @Test
    @DisplayName("Should throw exception when Authorization header is missing on CONNECT")
    void shouldThrowExceptionWhenTokenIsMissing() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertThat(exception.getMessage()).contains("Missing or invalid JWT token");
    }

    @Test
    @DisplayName("Should throw exception when token is invalid on CONNECT")
    void shouldThrowExceptionWhenTokenIsInvalid() {
        String invalidToken = "invalid-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + invalidToken);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(tokenService.validateAndDecodeToken(invalidToken)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertThat(exception.getMessage()).contains("Invalid JWT token");
    }

    @Test
    @DisplayName("Should ignore non-CONNECT commands")
    void shouldIgnoreNonConnectCommands() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> resultMessage = interceptor.preSend(message, messageChannel);

        assertThat(resultMessage).isEqualTo(message);
        verifyNoInteractions(tokenService);
    }
}