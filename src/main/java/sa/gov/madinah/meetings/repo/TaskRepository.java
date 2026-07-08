package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    long countByStatus(TaskStatus status);

    long countBySharedTrue();

    long countByMeetingId(Long meetingId);

    List<Task> findByMeetingIdOrderByTaskNumberAsc(Long meetingId);

    List<Task> findBySharedTrueOrderByUpdatedAtDesc();

    @Query("select count(t) from Task t where t.escalationStatus <> sa.gov.madinah.meetings.domain.enums.EscalationStatus.NONE " +
           "and t.escalationStatus <> sa.gov.madinah.meetings.domain.enums.EscalationStatus.CLOSED")
    long countEscalatedActive();

    // ---------- التوزيعات (Dashboard) ----------

    @Query("select t.status, count(t) from Task t group by t.status")
    List<Object[]> countGroupByStatus();

    @Query("select coalesce(d.name,'غير محدد'), count(t) from Task t left join t.department d group by d.name order by count(t) desc")
    List<Object[]> countGroupByDepartment();

    @Query("select coalesce(t.mainResponsibleName,'غير محدد'), count(t) from Task t group by t.mainResponsibleName order by count(t) desc")
    List<Object[]> countGroupByResponsible();

    @Query("select coalesce(m.title,'غير محدد'), count(t) from Task t left join t.meeting m group by m.title order by count(t) desc")
    List<Object[]> countGroupByMeeting();

    // نسبة الإنجاز حسب الجهة: مجموع الأوزان / العدد
    @Query("select coalesce(d.name,'غير محدد'), avg(t.progressPercentage), count(t) " +
           "from Task t left join t.department d group by d.name order by count(t) desc")
    List<Object[]> progressByDepartment();

    @Query("select coalesce(t.mainResponsibleName,'غير محدد'), avg(t.progressPercentage), count(t) " +
           "from Task t group by t.mainResponsibleName order by count(t) desc")
    List<Object[]> progressByResponsible();

    @Query("select avg(t.progressPercentage) from Task t")
    Double overallProgress();

    List<Task> findByEscalationStatusNotOrderByUpdatedAtDesc(EscalationStatus status);
}
