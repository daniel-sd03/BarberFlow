package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sodresoftwares.barbearia.model.TeamMember;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

    @Query("SELECT t FROM TeamMember t " +
            "JOIN FETCH t.business " +
            "WHERE t.user.id = :userId")
    Optional<TeamMember> findByUserIdWithBusiness(@Param("userId") String userId);

    Optional<TeamMember> findByUserId(String userId);

    Optional<TeamMember> findByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessIdAndRole(String loggedUserId, String id, String owner);
}