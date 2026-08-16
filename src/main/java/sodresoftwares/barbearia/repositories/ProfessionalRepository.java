package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sodresoftwares.barbearia.model.Professional;

import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<Professional, String> {
    @Query("SELECT p FROM Professional " +
            "p JOIN FETCH p.user " +
            "WHERE p.user.id = :userId")
    Optional<Professional> findByUserId(@Param("userId") String userId);
}