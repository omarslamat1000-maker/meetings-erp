package sa.gov.madinah.meetings.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.domain.Meeting;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.enums.Priority;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;
import sa.gov.madinah.meetings.domain.enums.WorkflowStatus;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.service.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final MeetingService meetingService;
    private final DepartmentService departmentService;
    private final UserService userService;

    public TaskController(TaskService taskService, MeetingService meetingService,
                          DepartmentService departmentService, UserService userService) {
        this.taskService = taskService;
        this.meetingService = meetingService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@ModelAttribute TaskFilter filter, Model model) {
        model.addAttribute("tasks", taskService.list(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("meetings", taskService.visibleMeetings());
        model.addAttribute("active", "tasks");
        return "tasks/list";
    }

    /** جزء جدول المهام للتحديث التفاعلي عبر AJAX. */
    @GetMapping("/fragment")
    public String fragment(@ModelAttribute TaskFilter filter, Model model) {
        model.addAttribute("tasks", taskService.list(filter));
        model.addAttribute("filter", filter);
        return "tasks/list :: tasksBody";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Task t = taskService.get(id);
        model.addAttribute("task", t);
        model.addAttribute("comments", taskService.comments(id));
        model.addAttribute("history", taskService.history(id));
        model.addAttribute("participants", taskService.participants(id));
        model.addAttribute("canEdit", taskService.canEdit(t));
        model.addAttribute("active", "tasks");
        return "tasks/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','EXECUTIVE','DEPARTMENT')")
    public String createForm(Model model) {
        model.addAttribute("task", new Task());
        model.addAttribute("participantDeptIds", List.of());
        fillFormRefs(model);
        model.addAttribute("active", "tasks");
        return "tasks/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Task t = taskService.get(id);
        model.addAttribute("task", t);
        model.addAttribute("participantDeptIds", taskService.participantDepartmentIds(id));
        fillFormRefs(model);
        model.addAttribute("active", "tasks");
        return "tasks/form";
    }

    private void fillFormRefs(Model model) {
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("meetings", taskService.visibleMeetings());
        model.addAttribute("users", userService.findResponsibles());
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Task task,
                       @RequestParam(required = false) Long meetingId,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) Long responsibleUserId,
                       @RequestParam(required = false, defaultValue = "false") boolean shared,
                       @RequestParam(required = false) String participantDepartmentIds,
                       RedirectAttributes ra) {
        if (meetingId != null) meetingService.findById(meetingId).ifPresent(task::setMeeting);
        if (departmentId != null) departmentService.findById(departmentId).ifPresent(task::setDepartment);
        if (responsibleUserId != null) userService.findById(responsibleUserId).ifPresent(task::setResponsibleUser);
        List<Department> participants = resolveDepartments(participantDepartmentIds);
        Task saved = taskService.saveTaskWithSharing(task, shared, participants);
        ra.addFlashAttribute("toast", (shared || !participants.isEmpty())
                ? "تم حفظ المهمة المشتركة وربط الجهات المشاركة"
                : "تم حفظ المهمة بنجاح");
        return "redirect:/tasks/" + saved.getId();
    }

    /** تحويل قائمة معرّفات الجهات (نص مفصول بفواصل) إلى كائنات جهات مرتّبة. */
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

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam TaskStatus status,
                               @RequestParam(required = false) String feedback, RedirectAttributes ra) {
        taskService.updateStatus(id, status, feedback);
        ra.addFlashAttribute("toast", "تم تحديث حالة المهمة");
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/{id}/workflow")
    public String transition(@PathVariable Long id, @RequestParam WorkflowStatus target,
                             @RequestParam(required = false) String reason, RedirectAttributes ra) {
        try {
            taskService.transition(id, target, reason);
            ra.addFlashAttribute("toast", "تم نقل المهمة إلى: " + target.getArabic());
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/{id}/comment")
    public String comment(@PathVariable Long id, @RequestParam String comment, RedirectAttributes ra) {
        taskService.addComment(id, comment);
        ra.addFlashAttribute("toast", "تمت إضافة الملاحظة");
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        taskService.delete(id);
        ra.addFlashAttribute("toast", "تم حذف المهمة");
        return "redirect:/tasks";
    }
}
