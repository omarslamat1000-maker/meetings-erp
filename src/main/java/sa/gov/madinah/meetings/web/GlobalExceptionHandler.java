package sa.gov.madinah.meetings.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** معالجة الأخطاء المتوقعة على مستوى جميع المتحكمات وعرض صفحة تنبيه ودّية. */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String accessDenied(Model model) {
        model.addAttribute("message", "لا تملك الصلاحية لتنفيذ هذا الإجراء.");
        return "error-page";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String badRequest(RuntimeException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error-page";
    }
}
