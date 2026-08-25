package sodresoftwares.barbearia.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sodresoftwares.barbearia.dto.team.QuickCreateMemberDTO;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.services.TeamMemberService;

@RestController
@RequestMapping("/team-members")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    @PostMapping("/quick-create")
    public ResponseEntity<Void> quickCreateMember(
            @AuthenticationPrincipal User loggedUser,
            @Valid @RequestBody QuickCreateMemberDTO dto) {
        teamMemberService.quickCreateMember(loggedUser.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal User loggedUser,
            @PathVariable("memberId") String memberId) {
        teamMemberService.removeMember(loggedUser.getId(), memberId);
        return ResponseEntity.noContent().build();
    }
}