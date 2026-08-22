package sodresoftwares.barbearia.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.ChangePasswordDTO;
import sodresoftwares.barbearia.dto.RegisterDTO;
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile(
            @AuthenticationPrincipal User loggedInUser) {
        UserResponseDTO profile = userService.getMyProfile(loggedInUser.getId());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/client")
    public ResponseEntity<Void> register(
            @RequestBody @Valid RegisterDTO data,
            HttpServletRequest request) {
        userService.registerClient(data,request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/professional")
    public ResponseEntity<Void> registerProfessional(
            @RequestBody @Valid RegisterDTO data,
            HttpServletRequest request) {
        userService.registerProfessional(data, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @AuthenticationPrincipal User loggedInUser,
            @Valid @RequestBody UpdateUserDTO dto) {
        UserResponseDTO updatedProfile = userService.updateUserProfile(loggedInUser.getId(), dto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal User loggedInUser,
            @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(loggedInUser.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal User loggedUser) {
        userService.deleteMyAccount(loggedUser.getId());
        return ResponseEntity.noContent().build();
    }
}