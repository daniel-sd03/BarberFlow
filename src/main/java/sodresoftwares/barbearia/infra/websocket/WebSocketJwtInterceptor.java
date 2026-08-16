package sodresoftwares.barbearia.infra.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import sodresoftwares.barbearia.infra.security.TokenService;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.replace("Bearer ", "");

                var decodedJWT = tokenService.validateAndDecodeToken(token);

                if (decodedJWT != null) {
                    var authentication = tokenService.getAuthentication(decodedJWT);

                    if (authentication != null) {
                        accessor.setUser(authentication);

                    } else {
                    log.warn("WebSocket connection rejected: user not found");
                    throw new IllegalArgumentException("User not found");

                    }
                } else {
                log.warn("WebSocket connection rejected due to invalid JWT");
                throw new IllegalArgumentException("Invalid JWT token");

                }
            } else {
                log.warn("WebSocket connection rejected: missing Authorization header");
                throw new IllegalArgumentException("Missing or invalid JWT token in WebSocket CONNECT");

            }
        }
        return message;
    }
}