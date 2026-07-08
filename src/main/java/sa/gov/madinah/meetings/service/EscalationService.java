package sa.gov.madinah.meetings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.gov.madinah.meetings.domain.Escalation;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.Priority;
import sa.gov.madinah.meetings.domain.enums.WorkflowStatus;
import sa.gov.madinah.meetings.repo.EscalationRepository;
import sa.gov.madinah.meetings.repo.TaskRepository;
import sa.gov.madinah.meetings.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EscalationService {

    private final EscalationRepository repo;
    private final TaskRepository taskRepo;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public EscalationService(EscalationRepository repo, TaskRepository taskRepo,
                             AuditService audit, CurrentUser currentUser) {
        this.repo = repo;
        this.taskRepo = taskRepo;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    /** المهام المصعّدة (نشطة). */
    public List<Task> escalatedTasks() {
        return taskRepo.findByEscalationStatusNotOrderByUpdatedAtDesc(EscalationStatus.NONE);
    }

    public List<Escalation> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<Escalation> forTask(Long taskId) {
        return repo.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    /** إضافة إجراء تصعيد لمهمة. */
    @Transactional
    public Escalation escalate(Long taskId, Priority level, String reason, String action) {
        Task t = taskRepo.findById(taskId).orElseThrow(() -> new IllegalArgumentException("المهمة غير موجودة"));
        Escalation e = new Escalation();
        e.setTask(t);
        e.setEscalationLevel(level);
        e.setEscalationReason(reason);
        e.setEscalationAction(action);
        e.setEscalationStatus(EscalationStatus.OPEN);
        e.setEscalatedBy(currentUser.username());
        Escalation saved = repo.save(e);

        // تحديث المهمة
        t.setEscalationStatus(EscalationStatus.OPEN);
        t.setEscalationLevel(level);
        t.setWorkflowStatus(WorkflowStatus.ESCALATED);
        if (t.getEscalationStatusText() == null || t.getEscalationStatusText().isBlank()) {
            t.setEscalationStatusText(level.getArabic());
        }
        taskRepo.save(t);

        audit.log("إضافة تصعيد", "Escalation", saved.getId(), null,
                t.getTitle() + " (" + level.getArabic() + ")");
        return saved;
    }

    /** تغيير حالة التصعيد ومعالجته. */
    @Transactional
    public void updateStatus(Long escalationId, EscalationStatus status, String action) {
        Escalation e = repo.findById(escalationId).orElseThrow();
        e.setEscalationStatus(status);
        if (action != null && !action.isBlank()) e.setEscalationAction(action);
        if (status == EscalationStatus.RESOLVED || status == EscalationStatus.CLOSED) {
            e.setResolvedBy(currentUser.username());
            e.setResolvedAt(LocalDateTime.now());
        }
        repo.save(e);

        // مزامنة المهمة
        Task t = e.getTask();
        t.setEscalationStatus(status);
        taskRepo.save(t);

        audit.log("تحديث حالة التصعيد", "Escalation", escalationId, null, status.getArabic());
    }

    public long countActive() {
        return taskRepo.countEscalatedActive();
    }
}
