package sa.gov.madinah.meetings.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sa.gov.madinah.meetings.domain.*;
import sa.gov.madinah.meetings.domain.enums.*;
import sa.gov.madinah.meetings.repo.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** بيانات أولية: الجهات، المستخدمون التجريبيون، وعينة اجتماعات ومهام. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepo;
    private final UserRepository userRepo;
    private final MeetingRepository meetingRepo;
    private final TaskRepository taskRepo;
    private final TaskParticipantRepository participantRepo;
    private final EscalationRepository escalationRepo;
    private final PasswordEncoder encoder;

    public DataSeeder(DepartmentRepository departmentRepo, UserRepository userRepo,
                      MeetingRepository meetingRepo, TaskRepository taskRepo,
                      TaskParticipantRepository participantRepo, EscalationRepository escalationRepo,
                      PasswordEncoder encoder) {
        this.departmentRepo = departmentRepo;
        this.userRepo = userRepo;
        this.meetingRepo = meetingRepo;
        this.taskRepo = taskRepo;
        this.participantRepo = participantRepo;
        this.escalationRepo = escalationRepo;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // اشتقاق تاريخ الاجتماع الفعلي من النص للاجتماعات القديمة (يعمل في كل تشغيل)
        meetingRepo.findAll().forEach(m -> {
            if (m.getMeetingDate() == null && m.getMeetingDateText() != null) {
                var d = sa.gov.madinah.meetings.service.MeetingService.parseArabicDate(m.getMeetingDateText());
                if (d != null) { m.setMeetingDate(d); meetingRepo.save(m); }
            }
        });

        // إن كانت القاعدة مهيأة مسبقًا (يوجد حساب admin) نكتفي بتحديث الهيكل ونخرج
        if (userRepo.existsByUsername("admin")) {
            ensureInfrastructureOrg();
            return;
        }

        // ---------- الجهات ----------
        String[] deptNames = {
                "الحدائق", "الدراسات والتصاميم", "الشؤون الفنية", "مركز متابعة العمليات البلدية",
                "تنسيق المشروعات", "الطرق", "شبكات تصريف مياه الأمطار", "المخططات الخاصة",
                "الوكلاء المساعدين", "أنسنة الطرق", "مكتب P3O"
        };
        for (String n : deptNames) {
            Department d = new Department();
            d.setName(n);
            d.setActive(true);
            departmentRepo.save(d);
        }
        Department roads = departmentRepo.findByName("الطرق").orElseThrow();
        Department studies = departmentRepo.findByName("الدراسات والتصاميم").orElseThrow();
        Department gardens = departmentRepo.findByName("الحدائق").orElseThrow();
        Department technical = departmentRepo.findByName("الشؤون الفنية").orElseThrow();
        Department center = departmentRepo.findByName("مركز متابعة العمليات البلدية").orElseThrow();
        Department rain = departmentRepo.findByName("شبكات تصريف مياه الأمطار").orElseThrow();

        // ---------- المستخدمون ----------
        createUser("مدير النظام", "admin", "Admin@12345", "admin@amanahmadinah.gov.sa", Role.ADMIN, null);
        createUser("حساب وكالة الطرق", "roads", "Roads@12345", "roads@amanahmadinah.gov.sa", Role.DEPARTMENT, roads);
        createUser("م. مستور المعبدي", "user1", "User@12345", "user1@amanahmadinah.gov.sa", Role.RESPONSIBLE, roads);
        createUser("مشاهد تنفيذي", "viewer", "Viewer@12345", "viewer@amanahmadinah.gov.sa", Role.VIEWER, null);
        createUser("متابعة تنفيذية", "exec", "Exec@12345", "exec@amanahmadinah.gov.sa", Role.EXECUTIVE, null);
        // مسؤولون إضافيون لمطابقة أسماء البيانات
        createUser("م. وسام بالخيور", "wesam", "User@12345", null, Role.RESPONSIBLE, studies);
        createUser("م. البراء محروس", "albaraa", "User@12345", null, Role.RESPONSIBLE, center);
        createUser("د. عمر محمد", "omar", "User@12345", null, Role.RESPONSIBLE, technical);

        User mastour = userRepo.findByUsername("user1").orElse(null);
        User wesam = userRepo.findByUsername("wesam").orElse(null);

        // ---------- عينة اجتماعات ومهام ----------
        Meeting m1 = meeting("اجتماع الوكالة المساعدة للطرق", roads, "8-5-2026م");
        Meeting m2 = meeting("اجتماع الدراسات والتصاميم - التقاطعات المرورية", studies, "31-4-2026م");
        Meeting m3 = meeting("اجتماع مركز متابعة العمليات البلدية", center, "1-5-2026م");
        Meeting m4 = meeting("اجتماع شبكات تصريف مياه الأمطار - الحالة المطرية", rain, "2-5-2026م");
        Meeting m5 = meeting("اجتماع الحدائق - جدول الافتتاح", gardens, "3-5-2026م");

        task(m1, roads, 1, "استكمال تنفيذ الطرق الداخلية القريبة من منارات طريق راشد السلمي",
                "م. مستور المعبدي", mastour, TaskStatus.IN_PROGRESS,
                "تم تزويدهم بالتصميم من ضمن مداخل ومخارج الهجرة", LocalDate.now().plusDays(10));

        task(m1, roads, 2, "حساب تكلفة تنفيذ طريق عثمان بن عفان مع الدائري (النور مول)",
                "م. مستور المعبدي", mastour, TaskStatus.COMPLETED,
                "تم الانتهاء من أعمال القطع وجاري البدء بأعمال الإسفلت", LocalDate.now().minusDays(3));

        task(m2, studies, 1, "تقديم مقترحات لتقاطع عثمان بن عفان مع الدائري الثاني",
                "م. وسام بالخيور", wesam, TaskStatus.PARTIAL,
                "تم اعتماد المقترح والموافقة عليه من قبل المرور", LocalDate.now().plusDays(5));

        task(m3, center, 1, "البدء بإعداد الخطة التفصيلية لأول 90 يوم في المركز",
                "م. البراء محروس", null, TaskStatus.NOT_STARTED, null, LocalDate.now().plusDays(20));

        // مهمة متأخرة
        Task overdue = task(m5, gardens, 1, "طرح عقد استكمال (سداد) حديقة الغروب",
                "م. وائل", null, TaskStatus.DELAYED,
                "بانتظار استكمال الإجراءات", LocalDate.now().minusDays(7));

        // مهمة مصعّدة
        Task escalated = task(m4, rain, 1,
                "معالجة المواقع الحرجة للحالة المطرية التي لا توجد بها شبكات تصريف",
                "م. إبراهيم الخطابي", null, TaskStatus.IN_PROGRESS,
                "جاري التنسيق مع إدارة الكوارث", LocalDate.now().plusDays(2));
        escalated.setEscalationStatus(EscalationStatus.OPEN);
        escalated.setEscalationLevel(Priority.CRITICAL);
        escalated.setEscalationStatusText("تصعيد وكالة البنية التحتية");
        escalated.setWorkflowStatus(WorkflowStatus.ESCALATED);
        taskRepo.save(escalated);

        Escalation esc = new Escalation();
        esc.setTask(escalated);
        esc.setEscalationLevel(Priority.CRITICAL);
        esc.setEscalationReason("عدم وجود شبكات تصريف في مواقع حرجة قد تشكل خطرًا خلال الحالة المطرية");
        esc.setEscalationAction("رفع الموضوع لوكالة البنية التحتية وإصدار خطاب للمرور لإغلاق الطرق الحرجة");
        esc.setEscalationStatus(EscalationStatus.OPEN);
        esc.setEscalatedBy("admin");
        escalationRepo.save(esc);

        // مهمة مشتركة بين أكثر من جهة ومسؤول
        Task shared = task(m1, roads, 3, "تنفيذ عكس المدخل بجوار ظل التوفير وعمل مخرج بجوار النور مول",
                "م. مستور المعبدي + م. وسام بالخيور", mastour, TaskStatus.IN_PROGRESS,
                "بانتظار الدراسة من الشؤون الفنية", LocalDate.now().plusDays(15));
        shared.setShared(true);
        taskRepo.save(shared);

        participant(shared, roads, mastour, "م. مستور المعبدي",
                "تنفيذ عكس المدخل", ParticipantStatus.IN_PROGRESS, 40, false);
        participant(shared, studies, wesam, "م. وسام بالخيور",
                "إعداد الدراسة التصميمية للمخرج", ParticipantStatus.NOT_STARTED, 0, false);
        participant(shared, technical, null, "الشؤون الفنية",
                "مراجعة الدراسة فنيًا", ParticipantStatus.NOT_STARTED, 0, false);

        // تحديث عدد المهام لكل اجتماع
        for (Meeting m : meetingRepo.findAll()) {
            m.setTaskCount((int) taskRepo.countByMeetingId(m.getId()));
            meetingRepo.save(m);
        }

        // إنشاء هيكل منظومة البنية التحتية بعد البذر التجريبي (ليرتبط بجهات المهام)
        ensureInfrastructureOrg();
    }

    /** إنشاء هيكل منظومة البنية التحتية (الجهات + التسلسل + الحسابات) دون الإدارات العامة. */
    private void ensureInfrastructureOrg() {
        // 1) جهات التسلسل الإداري العليا
        Department naib     = orgDept("نائب الأمين للبنية التحتية", null);
        Department wakeel   = orgDept("وكيل الأمين للبنية التحتية", naib);
        Department agHadaeq = orgDept("الوكالة المساعدة للحدائق والأنسنة", wakeel);
        Department agTuruq  = orgDept("الوكالة المساعدة للطرق", wakeel);
        Department agDirasat= orgDept("الوكالة المساعدة للدراسات والتصاميم", wakeel);
        Department agFanniy = orgDept("الوكالة المساعدة للشؤون الفنية", wakeel);

        // 2) ربط جهات المهام المستوردة بالتسلسل الإداري
        setParent(new String[]{"الحدائق", "أنسنة الطرق"}, agHadaeq);
        setParent(new String[]{"الطرق", "شبكات تصريف مياه الأمطار", "شبكات تصريف مياه الامطار"}, agTuruq);
        setParent(new String[]{"الدراسات والتصاميم", "الدرسات و التصاميم"}, agDirasat);
        setParent(new String[]{"الشؤون الفنية", "وكالة الشؤون الفنية", "مكتب P3O"}, agFanniy);
        setParent(new String[]{"مركز متابعة العمليات البلدية", "المخططات الخاصة", "تنسيق المشروعات"}, naib);
        setParent(new String[]{"الوكلاء المساعدين", "الوكالاء المساعدين"}, wakeel);

        // 3) الحسابات وفق الهيكل (الدور: حساب جهة مع الرؤية الهرمية)
        orgUser("م. منصور الخريف", "mansour", naib);
        orgUser("م. سلطان الحاسري", "sultan", wakeel);
        orgUser("م. البراء محروس", "baraa", deptOr("مركز متابعة العمليات البلدية", naib));
        orgUser("م. محمد التركي", "turki", deptOr("المخططات الخاصة", naib));
        orgUser("م. محمد زرق", "zarq", deptOr("تنسيق المشروعات", naib));
        orgUser("م. إبراهيم الخطابي", "khattabi", agHadaeq);
        orgUser("م. مستور المعبدي", "maabadi", agTuruq);
        orgUser("م. وسام بالخيور", "wissam", agDirasat);
        orgUser("م. صالح الحديثي", "hadithi", agFanniy);
    }

    private Department orgDept(String name, Department parent) {
        Department d = departmentRepo.findByName(name).orElseGet(() -> {
            Department nd = new Department();
            nd.setName(name);
            nd.setActive(true);
            return nd;
        });
        d.setParent(parent);
        return departmentRepo.save(d);
    }

    private Department deptOr(String name, Department fallback) {
        return departmentRepo.findByName(name).orElse(fallback);
    }

    private void setParent(String[] names, Department parent) {
        for (String n : names) {
            departmentRepo.findByName(n).ifPresent(d -> {
                d.setParent(parent);
                departmentRepo.save(d);
            });
        }
    }

    private void orgUser(String fullName, String username, Department dept) {
        if (userRepo.existsByUsername(username)) {
            userRepo.findByUsername(username).ifPresent(u -> {
                u.setDepartment(dept);
                userRepo.save(u);
            });
            return;
        }
        User u = new User();
        u.setFullName(fullName);
        u.setUsername(username);
        u.setPasswordHash(encoder.encode("Amanah@1446"));
        u.setEmail(username + "@amanahmadinah.gov.sa");
        u.setRole(Role.DEPARTMENT);
        u.setDepartment(dept);
        u.setActive(true);
        userRepo.save(u);
    }

    private void createUser(String fullName, String username, String password, String email,
                            Role role, Department dept) {
        User u = new User();
        u.setFullName(fullName);
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setEmail(email);
        u.setRole(role);
        u.setDepartment(dept);
        u.setActive(true);
        userRepo.save(u);
    }

    private Meeting meeting(String title, Department dept, String dateText) {
        Meeting m = new Meeting();
        m.setTitle(title);
        m.setDepartment(dept);
        m.setMeetingDateText(dateText);
        m.setMeetingDate(sa.gov.madinah.meetings.service.MeetingService.parseArabicDate(dateText));
        m.setCreatedBy("admin");
        return meetingRepo.save(m);
    }

    private Task task(Meeting meeting, Department dept, int num, String title, String responsible,
                      User responsibleUser, TaskStatus status, String feedback, LocalDate due) {
        Task t = new Task();
        t.setMeeting(meeting);
        t.setDepartment(dept);
        t.setTaskNumber(num);
        t.setTitle(title);
        t.setMainResponsibleName(responsible);
        t.setResponsibleUser(responsibleUser);
        t.setStatus(status);
        t.setProgressPercentage(status.getWeight());
        t.setWorkflowStatus(deriveWf(status));
        t.setFeedback(feedback);
        t.setDueDate(due);
        t.setPriority(Priority.NORMAL);
        t.setCreatedBy("admin");
        if (status == TaskStatus.COMPLETED) t.setCompletionDate(LocalDate.now().minusDays(2));
        return taskRepo.save(t);
    }

    private WorkflowStatus deriveWf(TaskStatus s) {
        return switch (s) {
            case COMPLETED -> WorkflowStatus.COMPLETED;
            case PARTIAL -> WorkflowStatus.PARTIALLY_COMPLETED;
            case IN_PROGRESS -> WorkflowStatus.IN_PROGRESS;
            case DELAYED -> WorkflowStatus.DELAYED;
            case NOT_STARTED -> WorkflowStatus.ASSIGNED;
        };
    }

    private void participant(Task task, Department dept, User user, String name, String action,
                             ParticipantStatus status, int progress, boolean approved) {
        TaskParticipant p = new TaskParticipant();
        p.setTask(task);
        p.setDepartment(dept);
        p.setUser(user);
        p.setParticipantName(name);
        p.setActionRequired(action);
        p.setResponsibilityDescription(action);
        p.setParticipantStatus(status);
        p.setParticipantProgress(progress);
        p.setApproved(approved);
        participantRepo.save(p);
    }
}
