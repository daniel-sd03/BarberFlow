package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.QueueSession;

import java.util.Optional;

@Repository
public interface QueueSessionRepository extends JpaRepository<QueueSession, String> {

    @Query("SELECT s FROM QueueSession s " +
            "JOIN FETCH s.professional p " +
            "WHERE p.user.id = :userId")
    Optional<QueueSession> findByProfessionalUserIdWithProfessional(@Param("userId") String userId);

    @Query("SELECT s FROM QueueSession s " +
            "JOIN FETCH s.professional " +
            "WHERE s.ticketCode = :ticketCode")
    Optional<QueueSession> findByTicketCodeWithProfessional(@Param("ticketCode") String ticketCode);

    @Query("SELECT s FROM QueueSession s " +
            "JOIN FETCH s.professional p " +
            "JOIN FETCH p.user " +
            "WHERE s.id = :id")
    Optional<QueueSession> findByIdWithProfessionalAndUser(@Param("id") String id);

    Optional<QueueSession> findByProfessionalUserId(String userId);
    boolean existsByTicketCode(String ticketCode);
    boolean existsByProfessionalUserId(String userId);
}