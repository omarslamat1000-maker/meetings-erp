package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.gov.madinah.meetings.domain.Escalation;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;

import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    List<Escalation> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<Escalation> findAllByOrderByCreatedAtDesc();
    List<Escalation> findByEscalationStatusOrderByCreatedAtDesc(EscalationStatus status);
}
