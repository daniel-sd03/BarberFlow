package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sodresoftwares.barbearia.dto.ProfessionalResponseDTO;
import sodresoftwares.barbearia.dto.UpdateProfessionalDTO;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.ProfessionalService;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @PatchMapping("/me")
    public ResponseEntity<ProfessionalResponseDTO> updateMyProfessionalProfile(
            @AuthenticationPrincipal User loggedInUser,
            @Valid @RequestBody UpdateProfessionalDTO dto) {

        ProfessionalResponseDTO updatedProfile = professionalService.updateProfessionalProfile(loggedInUser.getId(), dto);
        return ResponseEntity.ok(updatedProfile);
    }
}