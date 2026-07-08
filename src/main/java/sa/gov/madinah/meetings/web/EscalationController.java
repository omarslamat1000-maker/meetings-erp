package sa.gov.madinah.meetings.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.Priority;
import sa.gov.madinah.meetings.service.EscalationService;
import sa.gov.madinah.meetings.service.TaskService;

@Controller
@RequestMapping("/escalations")
public class EscalationController {

    private final EscalationService escalationService;
    private final TaskService taskService;

    public EscalationController(EscalationService escalationService, TaskService taskService) {
        this.escalationService = escalationService;
        this.taskService = taskService;
    }

    @GetMapping
    public String list(Model model) {
        // تقييد حسب نطاق المستخدم: يرى فقط تصعيدات المهام التي يملك صلاحية عرضها
        var tasks = escalationService.escalatedTasks().stream()
                .filter(taskService::canView).toList();
        var escalations = escalationService.all().stream()
                .filter(e -> taskService.canView(e.getTask())).toList();
        model.addAttribute("tasks", tasks);
        model.addAttribute("escalations", escalations);
        model.addAttribute("active", "escalations");
        return "escalations/list";
    }

    @PostMapping("/task/{taskId}/add")
    public String add(@PathVariable Long taskId, @RequestParam Priority level,
                      @RequestParam(required = false) String reason,
                      @RequestParam(required = false) String action, RedirectAttributes ra) {
        escalationService.escalate(taskId, level, reason, action);
        ra.addFlashAttribute("toast", "تم إضافة إجراء التصعيد");
        return "redirect:/escalations";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam EscalationStatus status,
                               @RequestParam(required = false) String action, RedirectAttributes ra) {
        escalationService.updateStatus(id, status, action);
        ra.addFlashAttribute("toast", "تم تحديث حالة التصعيد");
        return "redirect:/escalations";
    }
}
