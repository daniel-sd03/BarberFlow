package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.TeamMember;
import sodresoftwares.barbearia.model.TeamRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

    @Query("SELECT t FROM TeamMember t " +
            "JOIN FETCH t.business " +
            "WHERE t.user.id = :userId")
    Optional<TeamMember> findByUserIdWithBusiness(@Param("userId") String userId);

    @Query("SELECT tm FROM TeamMember tm " +
            "LEFT JOIN FETCH tm.user " +
            "WHERE tm.business.id = :businessId " +
            "AND tm.isActive = true")
    List<TeamMember> findAllByBusinessIdAndIsActiveTrueWithUser(@Param("businessId") String businessId);

    Optional<TeamMember> findByUserId(String userId);

    Optional<TeamMember> findByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessIdAndRole(String loggedUserId, String id, TeamRole owner);

    boolean existsByUserId(String userId);
}