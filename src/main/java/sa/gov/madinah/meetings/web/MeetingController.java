package sa.gov.madinah.meetings.web;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.domain.Meeting;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.service.DepartmentService;
import sa.gov.madinah.meetings.service.MeetingService;
import sa.gov.madinah.meetings.service.TaskService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final DepartmentService departmentService;
    private final TaskService taskService;

    public MeetingController(MeetingService meetingService, DepartmentService departmentService,
                             TaskService taskService) {
        this.meetingService = meetingService;
        this.departmentService = departmentService;
        this.taskService = taskService;
    }

    @GetMapping
    public String list(@ModelAttribute TaskFilter filter, Model model) {
        model.addAttribute("meetings", filterMeetings(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("meetingOptions", taskService.visibleMeetings());
        model.addAttribute("active", "meetings");
        return "meetings/list";
    }

    /** جزء جدول الاجتماعات للتحديث التفاعلي. */
    @GetMapping("/fragment")
    public String fragment(@ModelAttribute TaskFilter filter, Model model) {
        model.addAttribute("meetings", filterMeetings(filter));
        return "meetings/list :: meetingsBody";
    }

    /** فلترة الاجتماعات المرئية للمستخدم حسب المعايير. */
    private java.util.List<Meeting> filterMeetings(TaskFilter f) {
        java.util.List<Meeting> meetings = taskService.visibleMeetings();
        if (f.getDepartmentId() != null) {
            meetings = meetings.stream().filter(m -> m.getDepartment() != null
                    && f.getDepartmentId().equals(m.getDepartment().getId())).toList();
        }
        if (f.getMeetingId() != null) {
            meetings = meetings.stream().filter(m -> f.getMeetingId().equals(m.getId())).toList();
        }
        if (f.getDateFrom() != null) {
            meetings = meetings.stream().filter(m -> m.getMeetingDate() != null
                    && !m.getMeetingDate().isBefore(f.getDateFrom())).toList();
        }
        if (f.getDateTo() != null) {
            meetings = meetings.stream().filter(m -> m.getMeetingDate() != null
                    && !m.getMeetingDate().isAfter(f.getDateTo())).toList();
        }
        boolean contentFilter = (f.getQ() != null && !f.getQ().isBlank())
                || (f.getResponsible() != null && !f.getResponsible().isBlank())
                || f.getStatus() != null;
        if (contentFilter) {
            java.util.Set<Long> ids = taskService.list(f).stream()
                    .map(t -> t.getMeeting() == null ? null : t.getMeeting().getId())
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            String q = f.getQ() == null ? null : f.getQ().trim();
            meetings = meetings.stream().filter(m -> ids.contains(m.getId())
                    || (q != null && m.getTitle() != null && m.getTitle().contains(q))).toList();
        }
        return meetings;
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE','DEPARTMENT')")
    public String createForm(Model model) {
        model.addAttribute("meeting", new Meeting());
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("active", "meetings");
        return "meetings/form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE','DEPARTMENT')")
    public String editForm(@PathVariable Long id, Model model) {
        Meeting m = meetingService.findById(id).orElseThrow();
        model.addAttribute("meeting", m);
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("active", "meetings");
        return "meetings/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE','DEPARTMENT')")
    public String save(@Valid @ModelAttribute("meeting") Meeting meeting, BindingResult br,
                       @RequestParam(required = false) Long departmentId,
                       // صفوف المهام المرتبطة بالاجتماع (اختيارية)
                       @RequestParam(required = false) List<String> taskTitle,
                       @RequestParam(required = false) List<String> taskResponsible,
                       @RequestParam(required = false) List<String> taskDepartmentIds,
                       @RequestParam(required = false) List<String> taskStatus,
                       @RequestParam(required = false) List<String> taskDueDate,
                       @RequestParam(required = false) List<String> taskFeedback,
                       Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("departments", departmentService.findActive());
            model.addAttribute("active", "meetings");
            return "meetings/form";
        }
        if (departmentId != null) {
            departmentService.findById(departmentId).ifPresent(meeting::setDepartment);
        }
        Meeting saved = meetingService.save(meeting);

        // إنشاء المهام المدخلة وربطها بالاجتماع
        int created = createMeetingTasks(saved, taskTitle, taskResponsible, taskDepartmentIds,
                taskStatus, taskDueDate, taskFeedback);

        ra.addFlashAttribute("toast", created > 0
                ? "تم حفظ الاجتماع وإضافة " + created + " مهمة مرتبطة به"
                : "تم حفظ الاجتماع بنجاح");
        return "redirect:/meetings/" + saved.getId();
    }

    /** إنشاء المهام المدخلة في نموذج الاجتماع وربطها به. يُرجع عدد المهام المنشأة. */
    private int createMeetingTasks(Meeting meeting, List<String> titles, List<String> responsibles,
                                   List<String> deptIds, List<String> statuses,
                                   List<String> dueDates, List<String> feedbacks) {
        if (titles == null) return 0;
        int created = 0;
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            if (title == null || title.isBlank()) continue; // تجاهل الصفوف الفارغة

            Task t = new Task();
            t.setMeeting(meeting);
            t.setTitle(title.trim());
            t.setTaskNumber(created + 1);
            t.setMainResponsibleName(at(responsibles, i));

            // الحالة
            String st = at(statuses, i);
            TaskStatus status = TaskStatus.NOT_STARTED;
            if (st != null && !st.isBlank()) {
                try { status = TaskStatus.valueOf(st); } catch (IllegalArgumentException ignored) { }
            }
            t.setStatus(status);

            // تاريخ الاستحقاق
            String due = at(dueDates, i);
            if (due != null && !due.isBlank()) {
                try { t.setDueDate(LocalDate.parse(due)); } catch (Exception ignored) { }
            }

            t.setFeedback(at(feedbacks, i));

            // الجهات: المختارة (أكثر من جهة = مهمة مشتركة) أو جهة الاجتماع افتراضيًا
            List<Department> depts = resolveDepartments(at(deptIds, i));
            if (depts.isEmpty() && meeting.getDepartment() != null) {
                depts = new ArrayList<>(List.of(meeting.getDepartment()));
            }
            taskService.saveWithDepartments(t, depts);
            created++;
        }
        return created;
    }

    /** تحويل نص معرّفات الجهات المفصولة بفواصل إلى كائنات جهات. */
    private List<Department> resolveDepartments(String csv) {
        List<Department> list = new ArrayList<>();
        if (csv == null || csv.isBlank()) return list;
        for (String s : csv.split(",")) {
            if (s.isBlank()) continue;
            try { departmentService.findById(Long.parseLong(s.trim())).ifPresent(list::add); }
            catch (NumberFormatException ignored) { }
        }
        return list;
    }

    private String at(List<String> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Meeting m = meetingService.findById(id).orElseThrow();
        if (!taskService.canViewMeeting(m)) {
            throw new org.springframework.security.access.AccessDeniedException("لا تملك صلاحية عرض هذا الاجتماع");
        }
        TaskFilter f = new TaskFilter();
        f.setMeetingId(id);
        model.addAttribute("meeting", m);
        model.addAttribute("tasks", taskService.list(f));
        model.addAttribute("active", "meetings");
        return "meetings/detail";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        meetingService.delete(id);
        ra.addFlashAttribute("toast", "تم حذف الاجتماع");
        return "redirect:/meetings";
    }
}
