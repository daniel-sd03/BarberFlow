package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.ProfessionalResponseDTO;
import sodresoftwares.barbearia.dto.UpdateProfessionalDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Professional;
import sodresoftwares.barbearia.repositories.ProfessionalRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;

    @Transactional(readOnly = true)
    public ProfessionalResponseDTO getMyProfessionalProfile(String userId) {

        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Professional not found");
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "PROFESSIONAL_NOT_FOUND",
                            "Professional profile not found for this user."
                    );
                });

        return ProfessionalResponseDTO.fromEntity(professional);
    }

    @Transactional
    public ProfessionalResponseDTO updateProfessionalProfile(String userId, UpdateProfessionalDTO dto) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Business profile update failed. Professional not found");
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "PROFESSIONAL_NOT_FOUND",
                            "Professional profile not found for this user."
                    );
                });

        if (dto.businessName() != null && !dto.businessName().isBlank()) {
            professional.setBusinessName(dto.businessName().trim());
        }

        Professional updatedProfessional = professionalRepository.save(professional);

        log.info("Business profile updated successfully");
        return ProfessionalResponseDTO.fromEntity(updatedProfessional);
    }
}