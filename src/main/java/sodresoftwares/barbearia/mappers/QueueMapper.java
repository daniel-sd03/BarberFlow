package sodresoftwares.barbearia.mappers;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import sodresoftwares.barbearia.dto.QueueEntryResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.QueueEntry;

import java.util.ArrayList;
import java.util.List;

@Component
public class QueueMapper {

    public List<QueueEntryResponseDTO> toDtoList(List<QueueEntry> activeEntries) {
        List<QueueEntryResponseDTO> dtos = new ArrayList<>();
        for (int i = 0; i < activeEntries.size(); i++) {
            QueueEntry entry = activeEntries.get(i);
            dtos.add(new QueueEntryResponseDTO(
                    entry.getId(),
                    i + 1,
                    entry.getUser().getId(),
                    entry.getUser().getName(),
                    entry.getServiceName(),
                    entry.getStatus(),
                    entry.getCalledAt()
            ));
        }
        return dtos;
    }

    public QueueEntryResponseDTO toSingleDto(QueueEntry targetEntry, List<QueueEntry> activeEntries) {
        for (int i = 0; i < activeEntries.size(); i++) {
            if (activeEntries.get(i).getId().equals(targetEntry.getId())) {
                return new QueueEntryResponseDTO(
                        targetEntry.getId(),
                        i + 1,
                        targetEntry.getUser().getId(),
                        targetEntry.getUser().getName(),
                        targetEntry.getServiceName(),
                        targetEntry.getStatus(),
                        targetEntry.getCalledAt()
                );
            }
        }
        throw new AppException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ENTRY_NOT_IN_ACTIVE_QUEUE",
                "Queue entry was not found in the active queue."
        );
    }
}