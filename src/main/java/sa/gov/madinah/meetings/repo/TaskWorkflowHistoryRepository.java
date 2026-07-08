package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.gov.madinah.meetings.domain.TaskWorkflowHistory;

import java.util.List;

public interface TaskWorkflowHistoryRepository extends JpaRepository<TaskWorkflowHistory, Long> {
    List<TaskWorkflowHistory> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
