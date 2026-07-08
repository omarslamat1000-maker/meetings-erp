package sa.gov.madinah.meetings.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import sa.gov.madinah.meetings.domain.User;

/** أداة للوصول إلى المستخدم الحالي المسجّل دخوله. */
@Component
public class CurrentUser {

    public AppUserDetails details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails aud) {
            return aud;
        }
        return null;
    }

    public User user() {
        AppUserDetails d = details();
        return d == null ? null : d.getUser();
    }

    public String username() {
        AppUserDetails d = details();
        return d == null ? "system" : d.getUsername();
    }

    public Long id() {
        AppUserDetails d = details();
        return d == null ? null : d.getId();
    }
}
