package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.gov.madinah.meetings.domain.TaskComment;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
