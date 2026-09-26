package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT tm FROM TeamMember tm " +
            "WHERE tm.user.id = :userId " +
            "AND tm.isActive = true")
    Optional<TeamMember> findActiveByUserId(@Param("userId") String userId);

    @Query("SELECT tm FROM TeamMember tm " +
            "JOIN FETCH tm.business b " +
            "WHERE tm.user.id = :userId " +
            "AND tm.isActive = true " +
            "AND b.isActive = true")
    Optional<TeamMember> findActiveByUserIdWithBusiness(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE TeamMember tm " +
            "SET tm.isActive = false " +
            "WHERE tm.business.id = :businessId " +
            "AND tm.isActive = true")
    void deactivateAllByBusinessId(@Param("businessId") String businessId);

    Optional<TeamMember> findByBusinessIdAndUserId(String businessId, String userId);

    Optional<TeamMember> findByUserId(String userId);

    Optional<TeamMember> findByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessIdAndRole(String loggedUserId, String id, TeamRole owner);

    boolean existsByUserId(String userId);

    boolean existsByUserIdAndIsActiveTrue(String userId);
}