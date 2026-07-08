package sa.gov.madinah.meetings.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.TaskParticipant;
import sa.gov.madinah.meetings.domain.enums.ParticipantStatus;
import sa.gov.madinah.meetings.service.DepartmentService;
import sa.gov.madinah.meetings.service.TaskService;
import sa.gov.madinah.meetings.service.UserService;

@Controller
@RequestMapping("/shared-tasks")
public class SharedTaskController {

    private final TaskService taskService;
    private final DepartmentService departmentService;
    private final UserService userService;

    public SharedTaskController(TaskService taskService, DepartmentService departmentService,
                                UserService userService) {
        this.taskService = taskService;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tasks", taskService.listShared());
        model.addAttribute("active", "shared");
        return "shared-tasks/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Task t = taskService.get(id);
        model.addAttribute("task", t);
        model.addAttribute("participants", taskService.participants(id));
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("users", userService.findResponsibles());
        model.addAttribute("canEdit", taskService.canEdit(t));
        model.addAttribute("active", "shared");
        return "shared-tasks/detail";
    }

    @PostMapping("/{id}/participant")
    public String addParticipant(@PathVariable Long id,
                                 @RequestParam(required = false) Long departmentId,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam String participantName,
                                 @RequestParam(required = false) String actionRequired,
                                 RedirectAttributes ra) {
        Task t = taskService.get(id);
        TaskParticipant p = new TaskParticipant();
        p.setTask(t);
        if (departmentId != null) departmentService.findById(departmentId).ifPresent(p::setDepartment);
        if (userId != null) userService.findById(userId).ifPresent(p::setUser);
        p.setParticipantName(participantName);
        p.setActionRequired(actionRequired);
        p.setResponsibilityDescription(actionRequired);
        taskService.saveParticipant(p);
        ra.addFlashAttribute("toast", "تمت إضافة الجهة المشاركة");
        return "redirect:/shared-tasks/" + id;
    }

    @PostMapping("/{id}/participant/{pid}/update")
    public String updateParticipant(@PathVariable Long id, @PathVariable Long pid,
                                    @RequestParam(required = false) ParticipantStatus status,
                                    @RequestParam(required = false) Integer progress,
                                    @RequestParam(required = false) String feedback,
                                    @RequestParam(required = false) Boolean approved,
                                    RedirectAttributes ra) {
        taskService.updateParticipantStatus(pid, status, progress, feedback, approved);
        ra.addFlashAttribute("toast", "تم تحديث إجراء الجهة المشاركة");
        return "redirect:/shared-tasks/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, @RequestParam(required = false) String reason,
                        RedirectAttributes ra) {
        try {
            taskService.transition(id, sa.gov.madinah.meetings.domain.enums.WorkflowStatus.CLOSED, reason);
            ra.addFlashAttribute("toast", "تم إغلاق المهمة المشتركة");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/shared-tasks/" + id;
    }
}
