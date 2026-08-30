package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.business.BusinessResponseDTO;
import sodresoftwares.barbearia.dto.business.CreateBusinessDTO;
import sodresoftwares.barbearia.dto.business.UpdateBusinessDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.Business;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.model.TeamRole;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.BusinessRepository;
import sodresoftwares.barbearia.repositories.TeamMemberRepository;
import sodresoftwares.barbearia.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public BusinessResponseDTO getMyBusinessProfile(String userId) {

        Business business = businessRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "BUSINESS_NOT_FOUND",
                        "Business profile not found for this user."
                ));

        return BusinessResponseDTO.fromEntity(business);
    }

    @Transactional
    public void createBusiness(String userId, CreateBusinessDTO data) {

        if (businessRepository.existsByUserId(userId)) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_ALREADY_EXISTS",
                    "This user already owns a registered business."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found."
                ));


        Business newBusiness = Business.builder()
                .user(user)
                .name(data.name())
                .build();

        Business savedBusiness = businessRepository.save(newBusiness);

        TeamMember ownerMember = TeamMember.builder()
                .business(savedBusiness)
                .name(user.getName())
                .user(user)
                .role(TeamRole.OWNER)
                .build();

        teamMemberRepository.save(ownerMember);

        log.info("Business and Owner Team Member registered successfully ");
    }

    @Transactional
    public BusinessResponseDTO updateBusinessProfile(String userId, UpdateBusinessDTO dto) {
        Business business = businessRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "BUSINESS_NOT_FOUND",
                        "Business profile not found for this user."
                ));

        if (dto.name() != null && !dto.name().isBlank()) {
            business.setName(dto.name().trim());
        }

        Business updatedBusiness = businessRepository.save(business);

        log.info("Business profile updated successfully");
        return BusinessResponseDTO.fromEntity(updatedBusiness);
    }
}