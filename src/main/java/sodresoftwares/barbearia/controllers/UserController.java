package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @AuthenticationPrincipal User loggedInUser,
            @Valid @RequestBody UpdateUserDTO dto) {

        UserResponseDTO updatedProfile = userService.updateUserProfile(loggedInUser.getId(), dto);
        return ResponseEntity.ok(updatedProfile);
    }
}