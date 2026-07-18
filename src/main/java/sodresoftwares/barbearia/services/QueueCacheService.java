package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import sodresoftwares.barbearia.model.QueueEntry;
import sodresoftwares.barbearia.repositories.QueueEntryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueCacheService {

    private final QueueEntryRepository queueEntryRepository;

    @Cacheable(value = "activeEntries", key = "#sessionId")
    public List<QueueEntry> getActiveEntries(String sessionId) {
        return queueEntryRepository.findActiveEntriesBySessionId(sessionId);
    }

    @CacheEvict(value = "activeEntries", key = "#sessionId")
    public void evict(String sessionId) {
    }
}