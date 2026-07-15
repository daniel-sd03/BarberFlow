package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.model.QueueEntryStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, String> {

    @Query("""
        SELECT q FROM QueueEntry q
        JOIN FETCH q.user
        WHERE q.queueSession.id = :sessionId
        AND q.status IN ('WAITING', 'CALLED', 'IN_SERVICE')
        ORDER BY q.joinedAt ASC
    """)
    List<QueueEntry> findActiveEntriesBySessionId(@Param("sessionId") String sessionId);

    boolean existsByUserIdAndStatusIn(String userId, List<QueueEntryStatus> statuses);

    Optional<QueueEntry> findByUserIdAndStatusIn(String userId, List<QueueEntryStatus> statuses);
}