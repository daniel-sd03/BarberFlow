package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.barbearia.model.Professional;

import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<Professional, String> {
    Optional<Professional> findByUserId(String userId);
}