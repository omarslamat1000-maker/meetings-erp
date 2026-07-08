package sa.gov.madinah.meetings.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import sa.gov.madinah.meetings.domain.User;
import sa.gov.madinah.meetings.repo.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;

/** يسجّل تاريخ آخر دخول للمستخدم عند نجاح تسجيل الدخول. */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof AppUserDetails aud) {
            User u = userRepository.findById(aud.getId()).orElse(null);
            if (u != null) {
                u.setLastLogin(LocalDateTime.now());
                userRepository.save(u);
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
