package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.QueueSession;

import java.util.Optional;

@Repository
public interface QueueSessionRepository extends JpaRepository<QueueSession, String> {
    Optional<QueueSession> findByProfessionalUserId(String userId);
    boolean existsByTicketCode(String ticketCode);
    boolean existsByProfessionalUserId(String userId);
    Optional<QueueSession> findByTicketCode(String ticketCode);
}