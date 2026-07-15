package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.ProfessionalDashboardDTO;
import sodresoftwares.barbearia.dto.ToggleQueueDTO;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.QueueSessionService;

@RestController
@RequestMapping("/api/queue-sessions")
@RequiredArgsConstructor
public class QueueSessionController {

    private final QueueSessionService queueSessionService;

    @PostMapping("/toggle")
    public ResponseEntity<QueueSession> toggleQueue(
            @AuthenticationPrincipal User loggedInUser,
            @RequestBody @Valid ToggleQueueDTO dto) {

        QueueSession session = queueSessionService.toggleQueue(
                loggedInUser.getId(),
                dto.activate(),
                dto.customPrefix()
        );
        return ResponseEntity.ok(session);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ProfessionalDashboardDTO> getDashboard(
            @AuthenticationPrincipal User loggedInUser) {
        ProfessionalDashboardDTO dashboard = queueSessionService.getDashboardData(loggedInUser.getId());
        return ResponseEntity.ok(dashboard);
    }
}