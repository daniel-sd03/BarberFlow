package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.QueueSession;

import java.util.Optional;

@Repository
public interface QueueSessionRepository extends JpaRepository<QueueSession, String> {
    Optional<QueueSession> findByProfessionalId(String professionalId);
    boolean existsByTicketCode(String ticketCode);
    boolean existsByProfessionalId(String professionalId);
}