package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.ToggleQueueDTO;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.services.QueueSessionService;

@RestController
@RequestMapping("/api/queue-sessions")
@RequiredArgsConstructor
public class QueueSessionController {

    private final QueueSessionService queueSessionService;

    @PostMapping("/{professionalId}/toggle")
    public ResponseEntity<QueueSession> toggleQueue(
            @PathVariable String professionalId,
            @RequestBody @Valid ToggleQueueDTO dto) {

        QueueSession session = queueSessionService.toggleQueue(
                professionalId,
                dto.activate(),
                dto.customPrefix()
        );
        return ResponseEntity.ok(session);
    }
}