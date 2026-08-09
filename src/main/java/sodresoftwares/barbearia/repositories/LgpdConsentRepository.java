package sodresoftwares.barbearia.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sodresoftwares.barbearia.model.LgpdConsent;

import java.util.Optional;

@Repository
public interface LgpdConsentRepository extends JpaRepository<LgpdConsent, String> {
    Optional<LgpdConsent> findFirstByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndTermVersion(String userId, String termVersion);
}