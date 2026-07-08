package sa.gov.madinah.meetings.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** صفحة رفض الوصول (يُوجَّه إليها من Spring Security). */
@Controller
public class ErrorPagesController {

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("message", "لا تملك الصلاحية للوصول إلى هذه الصفحة أو تنفيذ هذا الإجراء.");
        return "error-page";
    }
}
