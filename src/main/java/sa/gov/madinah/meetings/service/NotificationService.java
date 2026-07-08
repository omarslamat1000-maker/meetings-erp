package sa.gov.madinah.meetings.service;

import org.springframework.stereotype.Service;
import sa.gov.madinah.meetings.domain.Meeting;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.dto.NotificationData;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** يبني إشعارات المستخدم الحالي ضمن نطاق صلاحياته. */
@Service
public class NotificationService {

    private static final int LIMIT = 25;
    private static final int RECENT_DAYS = 7;

    private final TaskService taskService;
    private final CurrentUser currentUser;

    public NotificationService(TaskService taskService, CurrentUser currentUser) {
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    public NotificationData summary() {
        NotificationData d = new NotificationData();
        if (currentUser.user() == null) return d;

        List<Task> tasks = taskService.list(new TaskFilter()); // مقيّدة بالنطاق ومرتبة بآخر تحديث
        LocalDateTime threshold = LocalDateTime.now().minusDays(RECENT_DAYS);

        d.setRecentTasks(tasks.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(threshold))
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .limit(LIMIT).toList());

        d.setDelayedTasks(tasks.stream().filter(Task::isDelayed).limit(LIMIT).toList());
        d.setEscalatedTasks(tasks.stream().filter(Task::isEscalated).limit(LIMIT).toList());
        d.setSharedTasks(tasks.stream().filter(Task::isShared).limit(LIMIT).toList());

        d.setRecentMeetings(taskService.visibleMeetings().stream()
                .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(threshold))
                .sorted(Comparator.comparing(Meeting::getCreatedAt).reversed())
                .limit(LIMIT).toList());

        return d;
    }

    /** عدد الإشعارات لعرضه في الشارة (بحماية ضد الأخطاء لعدم كسر الصفحات). */
    public int badgeCount() {
        try {
            return summary().getTotal();
        } catch (Exception e) {
            return 0;
        }
    }
}
