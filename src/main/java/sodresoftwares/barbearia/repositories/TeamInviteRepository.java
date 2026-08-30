package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.InviteStatus;
import sodresoftwares.barbearia.model.TeamInvite;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, String> {

    boolean existsByEmailAndBusinessIdAndStatus(String email, String businessId, InviteStatus status);

    @Query("SELECT ti FROM TeamInvite ti " +
            "JOIN FETCH ti.business " +
            "WHERE ti.email = :email " +
            "AND ti.status = :status")
    List<TeamInvite> findAllByEmailAndStatusWithBusiness(@Param("email") String email, @Param("status") InviteStatus status);

    @Query("SELECT ti FROM TeamInvite ti " +
            "JOIN FETCH ti.business " +
            "WHERE ti.id = :id")
    Optional<TeamInvite> findByIdWithBusiness(@Param("id") String id);
}