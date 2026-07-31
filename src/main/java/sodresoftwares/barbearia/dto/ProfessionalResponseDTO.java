package sodresoftwares.barbearia.dto;

import sodresoftwares.barbearia.model.Professional;

public record ProfessionalResponseDTO(
        String id,
        String businessName,
        Boolean isActive,
        UserResponseDTO user
) {
    public static ProfessionalResponseDTO fromEntity(Professional professional) {
        return new ProfessionalResponseDTO(
                professional.getId(),
                professional.getBusinessName(),
                professional.getIsActive(),
                professional.getUser() != null ? UserResponseDTO.fromEntity(professional.getUser()) : null
        );
    }
}