package sa.gov.madinah.meetings.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sa.gov.madinah.meetings.service.NotificationService;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("n", notificationService.summary());
        model.addAttribute("active", "notifications");
        return "notifications";
    }
}
