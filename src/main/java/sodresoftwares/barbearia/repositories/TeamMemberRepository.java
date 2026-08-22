package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sodresoftwares.barbearia.model.TeamMember;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {

    @Query("SELECT t FROM TeamMember t " +
            "JOIN FETCH t.business " +
            "WHERE t.user.id = :userId")
    Optional<TeamMember> findByUserIdWithBusiness(@Param("userId") String userId);

    @Query("SELECT t FROM TeamMember t " +
            "JOIN FETCH t.user " +
            "WHERE t.business.id = :businessId")
    List<TeamMember> findAllByBusinessIdWithUser(@Param("businessId") String businessId);

    Optional<TeamMember> findByUserId(String userId);

    Optional<TeamMember> findByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessId(String userId, String businessId);

    boolean existsByUserIdAndBusinessIdAndRole(String loggedUserId, String id, String owner);

    boolean existsByUserId(String userId);
}