package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.BusinessDashboardDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.dto.TeamMemberDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.mappers.QueueMapper;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TeamMemberRepository teamMemberRepository;
    private final QueueSessionRepository queueSessionRepository;
    private final QueueCacheService queueCacheService;
    private final QueueMapper queueMapper;

    public BusinessDashboardDTO getProfessionalDashboard(String loggedUserId) {

        Optional<TeamMember> loggedMemberOpt = teamMemberRepository.findByUserIdWithBusiness(loggedUserId);

        if (loggedMemberOpt.isEmpty()) {
            return new BusinessDashboardDTO(
                    null, null, null, null,
                    null, null, false, null, List.of(), List.of()
            );
        }

        TeamMember loggedMember = loggedMemberOpt.get();
        String businessId = loggedMember.getBusiness().getId();
        String businessName = loggedMember.getBusiness().getName();

        List<TeamMemberDTO> teamDtos = teamMemberRepository.findAllByBusinessIdWithUser(businessId)
                .stream()
                .map(member -> new TeamMemberDTO(
                        member.getId(),
                        member.getUser().getName(),
                        member.getRole()
                )).toList();

        Optional<QueueSession> sessionOpt = queueSessionRepository.findByBusinessIdWithBusiness(businessId);

        if (sessionOpt.isEmpty()) {
            return new BusinessDashboardDTO(
                    businessId, businessName, loggedMember.getId(), loggedMember.getRole(),
                    null, null, false, null, List.of(), teamDtos
            );
        }

        QueueSession session = sessionOpt.get();
        List<QueueEntryResponseDTO> activeQueueDtos = queueMapper.toDtoList(
                queueCacheService.getActiveEntries(session.getId())
        );

        return new BusinessDashboardDTO(
                businessId, businessName, loggedMember.getId(), loggedMember.getRole(),
                session.getId(), session.getTicketCode(), session.getIsActive(),
                session.getToleranceMinutes(), activeQueueDtos, teamDtos
        );
    }
}