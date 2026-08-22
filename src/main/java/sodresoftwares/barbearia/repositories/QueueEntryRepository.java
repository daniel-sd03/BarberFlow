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
        JOIN FETCH q.queueSession
        LEFT JOIN FETCH q.servedByMember sbm
        LEFT JOIN FETCH sbm.user
        WHERE q.queueSession.id = :sessionId
        AND q.status IN ('WAITING', 'CALLED', 'IN_SERVICE')
        ORDER BY q.joinedAt ASC
    """)
    List<QueueEntry> findActiveEntriesBySessionId(@Param("sessionId") String sessionId);

    @Query("""
        SELECT q FROM QueueEntry q
        JOIN FETCH q.user
        WHERE q.id = :id
    """)
    Optional<QueueEntry> findByIdWithUser(@Param("id") String id);

    @Query("SELECT e FROM QueueEntry e " +
            "JOIN FETCH e.user u " +
            "JOIN FETCH e.queueSession s " +
            "JOIN FETCH s.business b " +
            "JOIN FETCH b.user " +
            "LEFT JOIN FETCH e.servedByMember sbm " +
            "LEFT JOIN FETCH sbm.user " +
            "WHERE e.id = :id")
    Optional<QueueEntry> findByIdWithFullGraph(@Param("id") String id);

    boolean existsByUserIdAndStatusIn(String userId, List<QueueEntryStatus> statuses);
    Optional<QueueEntry> findByUserIdAndStatusIn(String userId, List<QueueEntryStatus> statuses);
    Optional<QueueEntry> findFirstByUserIdOrderByJoinedAtDesc(String userId);
    boolean existsByServedByMemberIdAndStatusIn(String memberId, List<QueueEntryStatus> statuses);
}