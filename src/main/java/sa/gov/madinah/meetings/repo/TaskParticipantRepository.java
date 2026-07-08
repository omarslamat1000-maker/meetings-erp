package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import sa.gov.madinah.meetings.domain.TaskParticipant;

import java.util.List;

public interface TaskParticipantRepository extends JpaRepository<TaskParticipant, Long> {
    List<TaskParticipant> findByTaskId(Long taskId);
    List<TaskParticipant> findByUserId(Long userId);
    List<TaskParticipant> findByDepartmentId(Long departmentId);
}
