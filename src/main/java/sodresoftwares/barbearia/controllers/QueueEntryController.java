package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.JoinQueueDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.services.QueueEntryService;

@RestController
@RequestMapping("/api/queue-entries")
@RequiredArgsConstructor
public class QueueEntryController {

    private final QueueEntryService queueEntryService;

    @PostMapping("/join")
    public ResponseEntity<QueueEntryResponseDTO> joinQueue(@RequestBody @Valid JoinQueueDTO dto) {
        QueueEntryResponseDTO response = queueEntryService.joinQueue(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active/user/{userId}")
    public ResponseEntity<QueueEntryResponseDTO> getActiveEntry(@PathVariable String userId) {
        return queueEntryService.findActiveEntryByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}