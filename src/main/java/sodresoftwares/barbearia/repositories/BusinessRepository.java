package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sodresoftwares.barbearia.model.Business;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, String> {
    @Query("SELECT b FROM Business b " +
            "JOIN FETCH b.user " +
            "WHERE b.user.id = :userId")
    Optional<Business> findByUserIdWithUser(@Param("userId") String userId);

    boolean existsByUserId(String userId);
}