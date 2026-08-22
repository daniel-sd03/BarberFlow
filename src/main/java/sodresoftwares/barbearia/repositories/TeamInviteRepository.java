package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.InviteStatus;
import sodresoftwares.barbearia.model.TeamInvite;

import java.util.List;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, String> {

    boolean existsByEmailAndBusinessIdAndStatus(String email, String businessId, InviteStatus status);

    List<TeamInvite> findAllByEmailAndStatus(String email, InviteStatus status);
}