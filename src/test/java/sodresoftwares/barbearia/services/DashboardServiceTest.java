package sodresoftwares.barbearia.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sodresoftwares.barbearia.dto.BusinessDashboardDTO;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.mappers.QueueMapper;
import sodresoftwares.barbearia.model.Business;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueSession;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.QueueSessionRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private QueueSessionRepository queueSessionRepository;

    @Mock
    private QueueCacheService queueCacheService;

    @Mock
    private QueueMapper queueMapper;

    @InjectMocks
    private DashboardService dashboardService;

    private final String LOGGED_USER_ID = "user-123";
    private final String BUSINESS_ID = "biz-123";
    private final String SESSION_ID = "session-123";

    private TeamMember teamMember;
    private QueueSession queueSession;

    @BeforeEach
    void setUp() {
        Business business = Business.builder()
                .id(BUSINESS_ID)
                .name("Barbearia Teste")
                .build();

        User mockUser = sodresoftwares.barbearia.model.user.User.builder()
                .id(LOGGED_USER_ID)
                .name("Zé Barbeiro")
                .build();

        teamMember = TeamMember.builder()
                .id("member-123")
                .role("OWNER")
                .business(business)
                .user(mockUser)
                .build();

        queueSession = QueueSession.builder()
                .id(SESSION_ID)
                .business(business)
                .ticketCode("CODE99")
                .isActive(true)
                .toleranceMinutes(15)
                .build();
    }

    // ==================== DASHBOARD PROFESSIONAL ====================

    @Test
    @DisplayName("Should return empty dashboard when user is not associated with any team")
    void getProfessionalDashboard_NoTeamMember() {
        // Arrange
        when(teamMemberRepository.findByUserIdWithBusiness(LOGGED_USER_ID)).thenReturn(Optional.empty());

        // Act
        BusinessDashboardDTO result = dashboardService.getProfessionalDashboard(LOGGED_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.businessId()).isNull();
        assertThat(result.loggedMemberRole()).isNull();
        assertThat(result.sessionId()).isNull();
        assertThat(result.activeQueue()).isEmpty();

        verifyNoInteractions(queueSessionRepository, queueCacheService, queueMapper);
    }

    @Test
    @DisplayName("Should return business data but no session data when queue session does not exist")
    void getProfessionalDashboard_NoActiveSession() {
        // Arrange
        when(teamMemberRepository.findByUserIdWithBusiness(LOGGED_USER_ID)).thenReturn(Optional.of(teamMember));
        when(teamMemberRepository.findAllByBusinessIdWithUser(BUSINESS_ID)).thenReturn(List.of(teamMember));
        when(queueSessionRepository.findByBusinessIdWithBusiness(BUSINESS_ID)).thenReturn(Optional.empty());

        // Act
        BusinessDashboardDTO result = dashboardService.getProfessionalDashboard(LOGGED_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(result.businessName()).isEqualTo("Barbearia Teste");
        assertThat(result.loggedMemberRole()).isEqualTo("OWNER");
        assertThat(result.sessionId()).isNull();
        assertThat(result.activeQueue()).isEmpty();

        verifyNoInteractions(queueCacheService, queueMapper);
    }

    @Test
    @DisplayName("Should return full dashboard data when team member and active session exist")
    void getProfessionalDashboard_FullData() {
        // Arrange
        when(teamMemberRepository.findByUserIdWithBusiness(LOGGED_USER_ID)).thenReturn(Optional.of(teamMember));
        when(teamMemberRepository.findAllByBusinessIdWithUser(BUSINESS_ID)).thenReturn(List.of(teamMember));
        when(queueSessionRepository.findByBusinessIdWithBusiness(BUSINESS_ID)).thenReturn(Optional.of(queueSession));

        List<QueueEntry> mockEntries = List.of(new QueueEntry());
        when(queueCacheService.getActiveEntries(SESSION_ID)).thenReturn(mockEntries);

        List<QueueEntryResponseDTO> mockDtos = List.of(mock(QueueEntryResponseDTO.class));
        when(queueMapper.toDtoList(anyList())).thenReturn(mockDtos);

        // Act
        BusinessDashboardDTO result = dashboardService.getProfessionalDashboard(LOGGED_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(result.sessionId()).isEqualTo(SESSION_ID);
        assertThat(result.ticketCode()).isEqualTo("CODE99");
        assertThat(result.isActive()).isTrue();
        assertThat(result.toleranceMinutes()).isEqualTo(15);
        assertThat(result.activeQueue()).hasSize(1);

        verify(queueCacheService).getActiveEntries(SESSION_ID);
        verify(queueMapper).toDtoList(mockEntries);
    }
}