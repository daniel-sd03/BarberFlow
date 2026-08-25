package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.team.CreateTeamInviteDTO;
import sodresoftwares.barbearia.dto.team.TeamInviteResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Business;
import sodresoftwares.barbearia.model.InviteStatus;
import sodresoftwares.barbearia.model.TeamInvite;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.model.TeamRole;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.TeamInviteRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInviteService {

    private final TeamInviteRepository teamInviteRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public List<TeamInviteResponseDTO> getMyPendingInvites(String userEmail) {
        return teamInviteRepository.findAllByEmailAndStatus(userEmail, InviteStatus.PENDING)
                .stream()
                .map(invite -> new TeamInviteResponseDTO(
                        invite.getId(),
                        invite.getBusiness().getId(),
                        invite.getBusiness().getName(),
                        invite.getEmail(),
                        invite.getRole(),
                        invite.getExpiresAt()
                )).toList();
    }

    @Transactional
    public void sendInvite(String loggedUserId, CreateTeamInviteDTO dto) {
        Business business = getBusinessForOwner(loggedUserId);

        boolean alreadyInvited = teamInviteRepository.existsByEmailAndBusinessIdAndStatus(
                dto.email(), business.getId(), InviteStatus.PENDING
        );

        if (alreadyInvited) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "INVITE_ALREADY_SENT",
                    "There is already a pending invite for this email."
            );
        }

        TeamInvite invite = TeamInvite.builder()
                .business(business)
                .email(dto.email())
                .role(TeamRole.STAFF)
                .status(InviteStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        teamInviteRepository.save(invite);
        log.info("Team invite generated.");
    }

    @Transactional
    public void acceptInvite(String inviteId, String loggedUserId, String loggedUserEmail) {

        if (teamMemberRepository.existsByUserId(loggedUserId)) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "ALREADY_IN_TEAM",
                    "You are already part of a team. Leave your current team to accept a new invite."
            );
        }

        TeamInvite invite = getValidInvite(inviteId, loggedUserEmail);
        User user = userRepository.findById(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found."
                ));

        invite.setStatus(InviteStatus.ACCEPTED);
        teamInviteRepository.save(invite);

        TeamMember newMember = TeamMember.builder()
                .business(invite.getBusiness())
                .user(user)
                .name(user.getName())
                .role(invite.getRole())
                .isActive(true)
                .build();

        teamMemberRepository.save(newMember);
        log.info("Team invite accepted.");
    }

    @Transactional
    public void declineInvite(String inviteId, String loggedUserEmail) {
        TeamInvite invite = getValidInvite(inviteId, loggedUserEmail);
        invite.setStatus(InviteStatus.DECLINED);
        teamInviteRepository.save(invite);
        log.info("Team invite declined.");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Business getBusinessForOwner(String loggedUserId) {
        TeamMember member = teamMemberRepository.findByUserId(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "TEAM_MEMBER_NOT_FOUND",
                        "User is not associated with any team/business."
                ));

        if (member.getRole() != TeamRole.OWNER) {
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "Only the business owner can perform this action."
            );
        }

        return member.getBusiness();
    }

    private TeamInvite getValidInvite(String inviteId, String email) {
        TeamInvite invite = teamInviteRepository.findById(inviteId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "INVITE_NOT_FOUND",
                        "Invite not found."
                ));

        if (!invite.getEmail().equalsIgnoreCase(email)) {
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "This invite does not belong to you."
            );
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INVITE",
                    "This invite has already been processed or is invalid."
            );
        }

        if (invite.getExpiresAt().isBefore(Instant.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            teamInviteRepository.save(invite);
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVITE_EXPIRED",
                    "This invite has expired."
            );
        }

        return invite;
    }
}