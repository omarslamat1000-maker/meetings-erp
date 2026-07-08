package sa.gov.madinah.meetings.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sa.gov.madinah.meetings.service.DashboardService;
import sa.gov.madinah.meetings.service.UserService;

@Controller
public class AuthController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public AuthController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        // تهيئة الجلسة ورمز الحماية CSRF مبكرًا قبل عرض الصفحة الكبيرة
        // (لتفادي تثبيت الاستجابة قبل إنشاء الجلسة)
        request.getSession(true);
        Object csrf = request.getAttribute(CsrfToken.class.getName());
        if (csrf instanceof CsrfToken token) {
            token.getToken();
        }
        // معاينة المؤشرات الإجمالية بجانب نموذج الدخول (مثل المنصة المرجعية)
        model.addAttribute("d", dashboardService.buildAggregate());
        // قائمة الحسابات لاختيارها من القائمة المنسدلة
        model.addAttribute("accounts", userService.findResponsibles());
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
