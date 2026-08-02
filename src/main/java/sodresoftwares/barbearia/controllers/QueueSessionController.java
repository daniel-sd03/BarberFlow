package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.*;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.QueueSessionService;

@RestController
@RequestMapping("/queue-sessions")
@RequiredArgsConstructor
public class QueueSessionController {

    private final QueueSessionService queueSessionService;

    @GetMapping("/me/dashboard")
    public ResponseEntity<ProfessionalDashboardDTO> getDashboard(
            @AuthenticationPrincipal User loggedInUser) {
        ProfessionalDashboardDTO dashboard = queueSessionService.getDashboardData(loggedInUser.getId());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<QueueSessionUserResponseDTO> getSessionByCode(
            @PathVariable String ticketCode) {
        QueueSessionUserResponseDTO response = queueSessionService.getSessionInfoByCode(ticketCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<QueueSessionProfResponseDTO> createSession(
            @AuthenticationPrincipal User loggedInUser) {
        QueueSessionProfResponseDTO session = queueSessionService.createQueueSession(loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PostMapping("/me/ticket-code")
    public ResponseEntity<QueueSessionProfResponseDTO> refreshTicketCode(
            @AuthenticationPrincipal User loggedInUser) {
        QueueSessionProfResponseDTO updatedSession = queueSessionService.refreshTicketCode(loggedInUser.getId());
        return ResponseEntity.ok(updatedSession);
    }

    @PatchMapping("/me")
    public ResponseEntity<QueueSessionProfResponseDTO> updateSession(
            @AuthenticationPrincipal User loggedInUser,
            @Valid @RequestBody UpdateQueueSessionDTO request) {
        QueueSessionProfResponseDTO updatedSession =
                queueSessionService.updateSessionSettings(loggedInUser.getId(), request);
        return ResponseEntity.ok(updatedSession);
    }

    @PatchMapping("/me/status")
    public ResponseEntity<QueueSessionProfResponseDTO> updateStatus(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid UpdateQueueStatusDTO dto) {
        QueueSessionProfResponseDTO session = queueSessionService.updateQueueStatus(loggedInUser.getId(),dto.activate());
        return ResponseEntity.ok(session);
    }
}