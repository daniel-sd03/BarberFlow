package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.QueueSession;

import java.util.Optional;

@Repository
public interface QueueSessionRepository extends JpaRepository<QueueSession, String> {
    @Query("SELECT q FROM QueueSession q " +
            "JOIN FETCH q.business " +
            "WHERE q.business.id = :businessId")
    Optional<QueueSession> findByBusinessIdWithBusiness(@Param("businessId") String businessId);

    @Query("SELECT q FROM QueueSession q " +
            "JOIN FETCH q.business " +
            "WHERE q.ticketCode = :ticketCode")
    Optional<QueueSession> findByTicketCodeWithBusiness(@Param("ticketCode") String ticketCode);

    @Query("SELECT s FROM QueueSession s " +
            "JOIN FETCH s.business b " +
            "JOIN FETCH b.user " +
            "WHERE s.id = :id")
    Optional<QueueSession> findByIdWithBusinessAndUser(@Param("id") String id);

    Optional<QueueSession> findByBusinessId(String businessId);
    boolean existsByTicketCode(String ticketCode);
    boolean existsByBusinessId(String businessId);
}