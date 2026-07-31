package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.ProfessionalResponseDTO;
import sodresoftwares.barbearia.dto.UpdateProfessionalDTO;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.ProfessionalService;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @GetMapping("/me")
    public ResponseEntity<ProfessionalResponseDTO> getMyProfessionalProfile(
            @AuthenticationPrincipal User loggedInUser) {
        ProfessionalResponseDTO profile = professionalService.getMyProfessionalProfile(loggedInUser.getId());
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfessionalResponseDTO> updateMyProfessionalProfile(
            @AuthenticationPrincipal User loggedInUser,
            @Valid @RequestBody UpdateProfessionalDTO dto) {

        ProfessionalResponseDTO updatedProfile = professionalService.updateProfessionalProfile(loggedInUser.getId(), dto);
        return ResponseEntity.ok(updatedProfile);
    }
}