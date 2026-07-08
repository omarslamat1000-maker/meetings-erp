package sa.gov.madinah.meetings.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.gov.madinah.meetings.domain.*;
import sa.gov.madinah.meetings.domain.enums.*;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.repo.*;
import sa.gov.madinah.meetings.security.CurrentUser;

import java.time.LocalDate;
import java.util.List;

/** خدمة إدارة المهام: العرض حسب الصلاحية، سير العمل، المشاركون، الإغلاق. */
@Service
public class TaskService {

    private final TaskRepository taskRepo;
    private final TaskParticipantRepository participantRepo;
    private final TaskCommentRepository commentRepo;
    private final TaskWorkflowHistoryRepository historyRepo;
    private final AuditService audit;
    private final CurrentUser currentUser;
    private final StatusNormalizer normalizer;
    private final MeetingService meetingService;
    private final EscalationRepository escalationRepo;
    private final DepartmentService departmentService;

    public TaskService(TaskRepository taskRepo, TaskParticipantRepository participantRepo,
                       TaskCommentRepository commentRepo, TaskWorkflowHistoryRepository historyRepo,
                       AuditService audit, CurrentUser currentUser, StatusNormalizer normalizer,
                       MeetingService meetingService, EscalationRepository escalationRepo,
                       DepartmentService departmentService) {
        this.taskRepo = taskRepo;
        this.participantRepo = participantRepo;
        this.commentRepo = commentRepo;
        this.historyRepo = historyRepo;
        this.audit = audit;
        this.currentUser = currentUser;
        this.normalizer = normalizer;
        this.meetingService = meetingService;
        this.escalationRepo = escalationRepo;
        this.departmentService = departmentService;
    }

    /** مجموعة معرّفات جهة المستخدم وكل الجهات التابعة لها (التسلسل الإداري). */
    private java.util.Set<Long> deptScope(User u) {
        if (u == null || u.getDepartment() == null) return java.util.Set.of();
        return departmentService.selfAndDescendantIds(u.getDepartment().getId());
    }

    // ---------------- العرض حسب الصلاحية ----------------

    public List<Task> list(TaskFilter filter) {
        User u = currentUser.user();
        Specification<Task> spec = Specification
                .where(TaskSpecifications.scope(u, deptScope(u)))
                .and(TaskSpecifications.fromFilter(filter));
        return taskRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    public List<Task> listShared() {
        TaskFilter f = new TaskFilter();
        f.setShared(true);
        return list(f);
    }

    public Task get(Long id) {
        Task t = taskRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("المهمة غير موجودة"));
        if (!canView(t)) {
            throw new org.springframework.security.access.AccessDeniedException("لا تملك صلاحية عرض هذه المهمة");
        }
        return t;
    }

    /** فحص صلاحية العرض على مهمة محددة (مدير النظام يرى الكل، وغيره ضمن نطاقه فقط). */
    public boolean canView(Task t) {
        User u = currentUser.user();
        if (u == null || t == null) return false;
        Role r = u.getRole();
        if (r == Role.ADMIN) return true;

        // مشارك شخصيًا في المهمة
        boolean isNamedParticipant = t.getParticipants().stream()
                .anyMatch(p -> p.getUser() != null && u.getId().equals(p.getUser().getId()));
        if (isNamedParticipant) return true;

        if (r == Role.RESPONSIBLE) {
            if (t.getResponsibleUser() != null && u.getId().equals(t.getResponsibleUser().getId())) return true;
            if (t.getMainResponsibleName() != null && u.getFullName() != null) {
                String needle = u.getFullName().replace("م.", "").replace("د.", "").trim();
                if (!needle.isBlank() && t.getMainResponsibleName().contains(needle)) return true;
            }
            return false;
        }
        // الجهة / المشاهد / المتابعة التنفيذية: جهتهم وكل الجهات التابعة + المشتركة التي جهتهم طرف فيها
        java.util.Set<Long> deptIds = deptScope(u);
        if (!deptIds.isEmpty()) {
            if (t.getDepartment() != null && deptIds.contains(t.getDepartment().getId())) return true;
            return t.getParticipants().stream()
                    .anyMatch(p -> p.getDepartment() != null && deptIds.contains(p.getDepartment().getId()));
        }
        return false;
    }

    /** الاجتماعات التي يحق للمستخدم الحالي الاطلاع عليها (اجتماعاته + ما به مهام ضمن نطاقه). */
    public java.util.List<sa.gov.madinah.meetings.domain.Meeting> visibleMeetings() {
        User u = currentUser.user();
        if (u == null) return java.util.List.of();
        if (u.getRole() == Role.ADMIN) return meetingService.findAll();

        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (Task t : list(new TaskFilter())) {
            if (t.getMeeting() != null) ids.add(t.getMeeting().getId());
        }
        for (Long did : deptScope(u)) {
            for (var m : meetingService.findByDepartment(did)) {
                ids.add(m.getId());
            }
        }
        return meetingService.findAll().stream()
                .filter(m -> ids.contains(m.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /** هل يحق للمستخدم الحالي الاطلاع على هذا الاجتماع؟ */
    public boolean canViewMeeting(sa.gov.madinah.meetings.domain.Meeting m) {
        User u = currentUser.user();
        if (u == null || m == null) return false;
        if (u.getRole() == Role.ADMIN) return true;
        java.util.Set<Long> deptIds = deptScope(u);
        if (m.getDepartment() != null && deptIds.contains(m.getDepartment().getId())) return true;
        // أو إن كان به أي مهمة ضمن نطاق المستخدم
        return taskRepo.findByMeetingIdOrderByTaskNumberAsc(m.getId()).stream().anyMatch(this::canView);
    }

    /** هل يمكن للمستخدم الحالي تعديل هذه المهمة (وليس مجرد عرضها)؟ */
    public boolean canEdit(Task t) {
        User u = currentUser.user();
        if (u == null) return false;
        Role r = u.getRole();
        if (r == Role.VIEWER) return false;
        if (r == Role.ADMIN) return true;
        // الجهة والمسؤول: يعدّلان ضمن نطاقهما فقط
        if (r == Role.DEPARTMENT || r == Role.RESPONSIBLE) return canView(t);
        // متابعة تنفيذية: ملاحظات فقط (لا تعديل جوهري)
        return false;
    }

    // ---------------- الحفظ والتعديل ----------------

    @Transactional
    public Task save(Task t) {
        boolean isNew = t.getId() == null;
        // مزامنة نسبة الإنجاز مع الحالة إن لم تُحدَّد يدويًا
        if (t.getProgressPercentage() == null) {
            t.setProgressPercentage(normalizer.progressFor(t.getStatus()));
        }
        if (isNew && t.getCreatedBy() == null) {
            t.setCreatedBy(currentUser.username());
        }
        // كشف المهمة المشتركة من اسم المسؤول (وجود +)
        if (t.getMainResponsibleName() != null && t.getMainResponsibleName().contains("+")) {
            t.setShared(true);
        }
        // اشتقاق حالة سير العمل للمهمة الجديدة بما يوافق حالتها
        if (isNew && t.getWorkflowStatus() == WorkflowStatus.CREATED
                && t.getStatus() != TaskStatus.NOT_STARTED) {
            t.setWorkflowStatus(normalizer.deriveWorkflow(t.getStatus()));
        }
        Task saved = taskRepo.save(t);
        if (saved.getMeeting() != null) {
            meetingService.refreshTaskCount(saved.getMeeting().getId());
        }
        audit.log(isNew ? "إنشاء مهمة" : "تعديل مهمة", "Task", saved.getId(), null, saved.getTitle());
        return saved;
    }

    /**
     * حفظ مهمة مع عدة جهات: الجهة الأولى رئيسية، والباقي جهات مشاركة (مهمة مشتركة).
     * إن كانت القائمة فارغة تبقى جهة المهمة كما هي.
     */
    @Transactional
    public Task saveWithDepartments(Task t, java.util.List<Department> departments) {
        if (departments != null && !departments.isEmpty()) {
            t.setDepartment(departments.get(0));
            if (departments.size() > 1) {
                t.setShared(true);
            }
        }
        Task saved = save(t);
        if (departments != null && departments.size() > 1) {
            for (int i = 1; i < departments.size(); i++) {
                Department d = departments.get(i);
                boolean exists = saved.getParticipants().stream()
                        .anyMatch(p -> p.getDepartment() != null && d.getId().equals(p.getDepartment().getId()));
                if (exists) continue;
                TaskParticipant p = new TaskParticipant();
                p.setTask(saved);
                p.setDepartment(d);
                p.setParticipantName(d.getName());
                participantRepo.save(p);
            }
        }
        return saved;
    }

    /**
     * حفظ مهمة مع تحديد كونها مشتركة وإضافة الجهات المشاركة.
     * تُضاف الجهات الجديدة فقط (لا تُحذف القائمة الحالية) لتفادي فقدان مشاركين مُضافين يدويًا.
     */
    @Transactional
    public Task saveTaskWithSharing(Task t, boolean shared, java.util.List<Department> participantDepts) {
        boolean hasParticipants = participantDepts != null && !participantDepts.isEmpty();
        if (shared || hasParticipants) {
            t.setShared(true);
        }
        Task saved = save(t);
        if (hasParticipants) {
            java.util.List<TaskParticipant> existing = participantRepo.findByTaskId(saved.getId());
            for (Department d : participantDepts) {
                if (saved.getDepartment() != null && d.getId().equals(saved.getDepartment().getId())) continue;
                boolean exists = existing.stream()
                        .anyMatch(p -> p.getDepartment() != null && d.getId().equals(p.getDepartment().getId()));
                if (exists) continue;
                TaskParticipant p = new TaskParticipant();
                p.setTask(saved);
                p.setDepartment(d);
                p.setParticipantName(d.getName());
                participantRepo.save(p);
            }
        }
        return saved;
    }

    /** معرّفات الجهات المشاركة الحالية في المهمة (لعرضها في نموذج التعديل). */
    public java.util.List<Long> participantDepartmentIds(Long taskId) {
        return participantRepo.findByTaskId(taskId).stream()
                .filter(p -> p.getDepartment() != null)
                .map(p -> p.getDepartment().getId())
                .distinct().toList();
    }

    /** تحديث الحالة المعيارية مع ضبط نسبة الإنجاز. */
    @Transactional
    public Task updateStatus(Long id, TaskStatus status, String feedback) {
        Task t = get(id);
        requireEdit(t);
        TaskStatus old = t.getStatus();
        t.setStatus(status);
        t.setProgressPercentage(normalizer.progressFor(status));
        if (feedback != null && !feedback.isBlank()) {
            t.setFeedback(feedback);
        }
        if (status == TaskStatus.COMPLETED && t.getCompletionDate() == null) {
            t.setCompletionDate(LocalDate.now());
        }
        Task saved = taskRepo.save(t);
        audit.log("تغيير حالة المهمة", "Task", id,
                old == null ? null : old.getArabic(), status.getArabic());
        return saved;
    }

    // ---------------- سير العمل ----------------

    /** الانتقال إلى حالة سير عمل جديدة مع تطبيق القواعد وحفظ السجل. */
    @Transactional
    public Task transition(Long id, WorkflowStatus target, String reason) {
        Task t = get(id);
        requireEdit(t);
        WorkflowStatus current = t.getWorkflowStatus();

        // قاعدة: لا يمكن الإغلاق دون اعتماد الإفادة (إلا لمدير النظام)
        boolean isAdmin = currentUser.user() != null && currentUser.user().getRole() == Role.ADMIN;
        if (target == WorkflowStatus.CLOSED) {
            if (!isAdmin && current != WorkflowStatus.APPROVED && current != WorkflowStatus.COMPLETED) {
                throw new IllegalStateException("لا يمكن إغلاق المهمة قبل اعتماد الإفادة أو اكتمالها.");
            }
            // قاعدة: المهمة المشتركة لا تُغلق إلا باكتمال إجراءات الجهات المشاركة أو باعتماد مدير النظام
            if (t.isShared() && !isAdmin && !allParticipantsApproved(t)) {
                throw new IllegalStateException(
                        "المهمة مشتركة: يجب اكتمال واعتماد إجراءات جميع الجهات المشاركة قبل الإغلاق، أو اعتماد مدير النظام.");
            }
        }

        t.setWorkflowStatus(target);
        // مزامنة الحالة المعيارية مع بعض حالات سير العمل
        switch (target) {
            case COMPLETED -> { t.setStatus(TaskStatus.COMPLETED); t.setProgressPercentage(100);
                                if (t.getCompletionDate() == null) t.setCompletionDate(LocalDate.now()); }
            case PARTIALLY_COMPLETED -> { t.setStatus(TaskStatus.PARTIAL); t.setProgressPercentage(50); }
            case IN_PROGRESS -> { if (t.getStatus() == TaskStatus.NOT_STARTED) { t.setStatus(TaskStatus.IN_PROGRESS); t.setProgressPercentage(30);} }
            case DELAYED -> t.setStatus(TaskStatus.DELAYED);
            case ESCALATED -> { if (t.getEscalationStatus() == EscalationStatus.NONE) t.setEscalationStatus(EscalationStatus.OPEN); }
            default -> { }
        }
        Task saved = taskRepo.save(t);

        TaskWorkflowHistory h = new TaskWorkflowHistory();
        h.setTask(saved);
        h.setOldStatus(current);
        h.setNewStatus(target);
        h.setChangedBy(currentUser.username());
        h.setChangeReason(reason);
        historyRepo.save(h);

        audit.log("انتقال سير العمل", "Task", id,
                current == null ? null : current.getArabic(), target.getArabic());
        return saved;
    }

    private boolean allParticipantsApproved(Task t) {
        List<TaskParticipant> ps = participantRepo.findByTaskId(t.getId());
        if (ps.isEmpty()) return true;
        return ps.stream().allMatch(p -> p.isApproved()
                && p.getParticipantStatus() == ParticipantStatus.COMPLETED);
    }

    // ---------------- المشاركون (المهام المشتركة) ----------------

    @Transactional
    public TaskParticipant saveParticipant(TaskParticipant p) {
        Task t = get(p.getTask().getId());
        requireEdit(t);
        TaskParticipant saved = participantRepo.save(p);
        if (!t.isShared()) {
            t.setShared(true);
            taskRepo.save(t);
        }
        audit.log("تحديث مشارك في مهمة مشتركة", "TaskParticipant", saved.getId(), null,
                saved.getParticipantName());
        return saved;
    }

    @Transactional
    public void updateParticipantStatus(Long participantId, ParticipantStatus status, Integer progress,
                                        String feedback, Boolean approved) {
        TaskParticipant p = participantRepo.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("المشارك غير موجود"));
        requireEdit(p.getTask());
        if (status != null) p.setParticipantStatus(status);
        if (progress != null) p.setParticipantProgress(progress);
        if (feedback != null) p.setParticipantFeedback(feedback);
        if (approved != null) {
            // الاعتماد النهائي للمشارك: لمدير النظام أو الجهة نفسها
            p.setApproved(approved);
        }
        participantRepo.save(p);
        audit.log("تحديث حالة إجراء جهة مشاركة", "TaskParticipant", participantId, null,
                p.getParticipantStatus().getArabic());
    }

    // ---------------- الملاحظات ----------------

    @Transactional
    public TaskComment addComment(Long taskId, String text) {
        Task t = get(taskId);
        User u = currentUser.user();
        // المتابعة التنفيذية والمشاهد: المتابعة التنفيذية تستطيع الملاحظات، المشاهد لا
        if (u != null && u.getRole() == Role.VIEWER) {
            throw new org.springframework.security.access.AccessDeniedException("المشاهد لا يمكنه إضافة ملاحظات");
        }
        TaskComment c = new TaskComment();
        c.setTask(t);
        c.setUser(u);
        c.setComment(text);
        TaskComment saved = commentRepo.save(c);
        audit.log("إضافة ملاحظة", "TaskComment", saved.getId(), null, text);
        return saved;
    }

    public List<TaskComment> comments(Long taskId) {
        return commentRepo.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    public List<TaskWorkflowHistory> history(Long taskId) {
        return historyRepo.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    public List<TaskParticipant> participants(Long taskId) {
        return participantRepo.findByTaskId(taskId);
    }

    // ---------------- المهام المتأخرة (تحديث تلقائي) ----------------

    @Transactional
    public int markOverdueTasks() {
        int count = 0;
        for (Task t : taskRepo.findAll()) {
            if (t.getDueDate() != null && t.getStatus() != TaskStatus.COMPLETED
                    && t.getWorkflowStatus() != WorkflowStatus.CLOSED
                    && t.getDueDate().isBefore(LocalDate.now())
                    && t.getStatus() != TaskStatus.DELAYED) {
                t.setStatus(TaskStatus.DELAYED);
                t.setProgressPercentage(0);
                taskRepo.save(t);
                count++;
            }
        }
        return count;
    }

    private void requireEdit(Task t) {
        if (!canEdit(t)) {
            throw new org.springframework.security.access.AccessDeniedException("لا تملك صلاحية تعديل هذه المهمة");
        }
    }

    @Transactional
    public void delete(Long id) {
        Task t = taskRepo.findById(id).orElseThrow();
        Long meetingId = t.getMeeting() == null ? null : t.getMeeting().getId();
        // حذف التصعيدات المرتبطة أولًا (لا تُحذف تلقائيًا) — الباقي يُحذف بالـ cascade
        escalationRepo.deleteAll(escalationRepo.findByTaskIdOrderByCreatedAtDesc(id));
        taskRepo.deleteById(id);
        if (meetingId != null) meetingService.refreshTaskCount(meetingId);
        audit.log("حذف مهمة", "Task", id, t.getTitle(), null);
    }

    public long count() {
        return taskRepo.count();
    }
}
