package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.*;
import sodresoftwares.barbearia.services.AuthenticationService;
import sodresoftwares.barbearia.services.PasswordResetService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationService authenticationService;
	private final PasswordResetService passwordResetService;

	@PostMapping("/login")
	public ResponseEntity<TokenResponseDTO> login(
			@RequestBody @Valid AuthenticationDTO data) {
		TokenResponseDTO response = authenticationService.login(data);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/password-resets")
	public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody ForgotPasswordDTO dto) {
		passwordResetService.requestPasswordReset(dto.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/password-resets/validate")
	public ResponseEntity<Void> validateResetToken(@Valid @RequestBody ValidateTokenDTO dto) {
		passwordResetService.validateToken(dto.email(), dto.code());
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/passwords")
	public ResponseEntity<Void> updatePassword(@Valid @RequestBody ResetPasswordDTO dto) {
		passwordResetService.resetPassword(dto);
		return ResponseEntity.noContent().build();
	}
}
