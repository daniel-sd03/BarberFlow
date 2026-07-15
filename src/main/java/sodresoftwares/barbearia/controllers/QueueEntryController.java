package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.JoinQueueDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.QueueEntryService;

@RestController
@RequestMapping("/api/queue-entries")
@RequiredArgsConstructor
public class QueueEntryController {

    private final QueueEntryService queueEntryService;

    @PostMapping("/join")
    public ResponseEntity<QueueEntryResponseDTO> joinQueue(
            @RequestBody @Valid JoinQueueDTO dto,
            @AuthenticationPrincipal User loggedInUser) {
        QueueEntryResponseDTO response = queueEntryService.joinQueue(dto,loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active/me")
    public ResponseEntity<QueueEntryResponseDTO> getActiveEntry(
            @AuthenticationPrincipal User loggedInUser) {
        return queueEntryService.findActiveEntryByUserId(loggedInUser.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/session/{sessionId}/call-next")
    public ResponseEntity<QueueEntryResponseDTO> callNext(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User loggedInUser) {

        QueueEntryResponseDTO response = queueEntryService.callNext(sessionId, loggedInUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{entryId}/start")
    public ResponseEntity<QueueEntryResponseDTO> startService(
            @PathVariable String entryId,
            @AuthenticationPrincipal User loggedInUser) {

        QueueEntryResponseDTO response = queueEntryService.startService(entryId, loggedInUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{entryId}/finish")
    public ResponseEntity<Void> finishService(
            @PathVariable String entryId,
            @AuthenticationPrincipal User loggedInUser) {

        queueEntryService.finishService(entryId, loggedInUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entryId}/cancel")
    public ResponseEntity<Void> cancelEntry(
            @PathVariable String entryId,
            @AuthenticationPrincipal User loggedInUser) {

        queueEntryService.cancelEntry(entryId, loggedInUser.getId());
        return ResponseEntity.noContent().build();
    }
}