package sodresoftwares.barbearia.dto;

import sodresoftwares.barbearia.model.Business;

public record BusinessResponseDTO(
        String id,
        String name,
        Boolean isActive,
        UserResponseDTO user
) {
    public static BusinessResponseDTO fromEntity(Business business) {
        return new BusinessResponseDTO(
                business.getId(),
                business.getName(),
                business.getIsActive(),
                business.getUser() != null ? UserResponseDTO.fromEntity(business.getUser()) : null
        );
    }
}