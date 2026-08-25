package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.team.QuickCreateMemberDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Business;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.model.TeamRole;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public void quickCreateMember(String loggedUserId, QuickCreateMemberDTO dto) {

        Business business = getBusinessForOwner(loggedUserId);

        TeamMember teamMember = TeamMember.builder()
                .business(business)
                .name(dto.name())
                .user(null)
                .role(TeamRole.STAFF)
                .isActive(true)
                .build();

        teamMemberRepository.save(teamMember);
        log.info("Team member created without a linked user account.");
    }

    @Transactional
    public void removeMember(String loggedUserId, String memberIdToRemove) {
        Business business = getBusinessForOwner(loggedUserId);

        TeamMember memberToRemove = teamMemberRepository.findById(memberIdToRemove)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "MEMBER_NOT_FOUND",
                        "Team member not found."
                ));

        if (!memberToRemove.getBusiness().getId().equals(business.getId())) {
            throw new AppException(
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED",
                    "This member does not belong to your business."
            );
        }

        if (memberToRemove.getRole() == TeamRole.OWNER) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "CANNOT_REMOVE_OWNER",
                    "The business owner cannot be removed."
            );
        }

        memberToRemove.setIsActive(false);
        teamMemberRepository.save(memberToRemove);
        log.info("Team member deactivated.");
    }


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
}