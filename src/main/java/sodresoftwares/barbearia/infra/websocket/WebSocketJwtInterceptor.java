package sodresoftwares.barbearia.infra.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import sodresoftwares.barbearia.infra.security.TokenService;
import sodresoftwares.barbearia.repositories.UserRepository;

@Component
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.replace("Bearer ", "");

                String subject = tokenService.validateToken(token);

                if (!subject.isEmpty()) {
                    UserDetails user = userRepository.findByLogin(subject);

                    if (user != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        accessor.setUser(authentication);
                    }
                }
            } else {
                throw new IllegalArgumentException("Missing or invalid JWT token in WebSocket CONNECT");
            }
        }
        return message;
    }
}