package sodresoftwares.barbearia.dto;

import sodresoftwares.barbearia.model.user.User;

public record UserResponseDTO(
        String id,
        String name,
        String login,
        String phone,
        String role
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getLogin(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().name() : null
        );
    }
}