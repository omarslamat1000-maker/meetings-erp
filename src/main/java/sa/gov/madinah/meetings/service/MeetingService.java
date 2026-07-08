package sa.gov.madinah.meetings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.domain.Meeting;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.repo.EscalationRepository;
import sa.gov.madinah.meetings.repo.MeetingRepository;
import sa.gov.madinah.meetings.repo.TaskRepository;
import sa.gov.madinah.meetings.security.CurrentUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MeetingService {

    private final MeetingRepository repo;
    private final TaskRepository taskRepository;
    private final EscalationRepository escalationRepository;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public MeetingService(MeetingRepository repo, TaskRepository taskRepository,
                          EscalationRepository escalationRepository,
                          AuditService audit, CurrentUser currentUser) {
        this.repo = repo;
        this.taskRepository = taskRepository;
        this.escalationRepository = escalationRepository;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    public List<Meeting> findAll() {
        return repo.findAllOrderByDate();
    }

    public Optional<Meeting> findById(Long id) {
        return repo.findById(id);
    }

    public List<Meeting> findByDepartment(Long departmentId) {
        return repo.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
    }

    public long countTasks(Long meetingId) {
        return taskRepository.countByMeetingId(meetingId);
    }

    /** إيجاد اجتماع بعنوان المحضر + الجهة أو إنشاؤه (يُستخدم في الاستيراد). */
    @Transactional
    public Meeting findOrCreate(String title, Department department, String meetingDateText) {
        if (title == null || title.isBlank()) {
            title = "بدون محضر";
        }
        final String clean = title.trim();
        Long deptId = department == null ? null : department.getId();
        Optional<Meeting> existing = deptId == null
                ? repo.findFirstByTitle(clean)
                : repo.findByTitleAndDepartmentId(clean, deptId);
        return existing.orElseGet(() -> {
            Meeting m = new Meeting();
            m.setTitle(clean);
            m.setDepartment(department);
            m.setMeetingDateText(meetingDateText);
            m.setMeetingDate(parseArabicDate(meetingDateText));
            m.setCreatedBy(currentUser.username());
            return repo.save(m);
        });
    }

    /** محاولة تحويل نص تاريخ الاجتماع إلى تاريخ فعلي (d-M-yyyy أو yyyy-M-d). */
    public static LocalDate parseArabicDate(String text) {
        if (text == null || text.isBlank()) return null;
        String[] parts = text.replaceAll("[^0-9/\\-]", " ").trim().split("[\\s/\\-]+");
        java.util.List<Integer> nums = new java.util.ArrayList<>();
        for (String p : parts) {
            if (p.isBlank()) continue;
            try { nums.add(Integer.parseInt(p)); } catch (NumberFormatException ignored) { }
        }
        if (nums.size() < 3) return null;
        int a = nums.get(0), b = nums.get(1), c = nums.get(2);
        try {
            if (a > 1000) return LocalDate.of(a, b, c);            // yyyy-M-d
            int year = c < 100 ? 2000 + c : c;
            return LocalDate.of(year, b, a);                       // d-M-yyyy
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Meeting save(Meeting m) {
        boolean isNew = m.getId() == null;
        if (isNew && m.getCreatedBy() == null) {
            m.setCreatedBy(currentUser.username());
        }
        // اشتقاق التاريخ الفعلي من النص إن لم يُحدَّد
        if (m.getMeetingDate() == null && m.getMeetingDateText() != null) {
            m.setMeetingDate(parseArabicDate(m.getMeetingDateText()));
        }
        Meeting saved = repo.save(m);
        audit.log(isNew ? "إنشاء اجتماع" : "تعديل اجتماع", "Meeting", saved.getId(), null, saved.getTitle());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id).ifPresent(m -> {
            // حذف مهام الاجتماع أولًا (مع تصعيداتها) لتفادي قيود المفاتيح الأجنبية
            List<Task> tasks = taskRepository.findByMeetingIdOrderByTaskNumberAsc(id);
            for (Task t : tasks) {
                escalationRepository.deleteAll(escalationRepository.findByTaskIdOrderByCreatedAtDesc(t.getId()));
            }
            // حذف المهام يُسقط المشاركين والتعليقات وسجل سير العمل تلقائيًا (cascade + orphanRemoval)
            taskRepository.deleteAll(tasks);
            repo.deleteById(id);
            audit.log("حذف اجتماع", "Meeting", id, m.getTitle(), null);
        });
    }

    @Transactional
    public void refreshTaskCount(Long meetingId) {
        repo.findById(meetingId).ifPresent(m -> {
            m.setTaskCount((int) taskRepository.countByMeetingId(meetingId));
            repo.save(m);
        });
    }

    public long count() {
        return repo.count();
    }
}
