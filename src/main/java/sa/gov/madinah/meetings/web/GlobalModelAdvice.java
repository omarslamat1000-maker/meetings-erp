package sa.gov.madinah.meetings.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sa.gov.madinah.meetings.domain.enums.*;
import sa.gov.madinah.meetings.security.AppUserDetails;
import sa.gov.madinah.meetings.security.CurrentUser;
import sa.gov.madinah.meetings.service.NotificationService;

/** يوفّر خصائص مشتركة لجميع الصفحات (المستخدم الحالي، القوائم المرجعية). */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUser currentUser;
    private final NotificationService notificationService;

    public GlobalModelAdvice(CurrentUser currentUser, NotificationService notificationService) {
        this.currentUser = currentUser;
        this.notificationService = notificationService;
    }

    @ModelAttribute("notifCount")
    public int notifCount() {
        return currentUser.user() == null ? 0 : notificationService.badgeCount();
    }

    @ModelAttribute("appName")
    public String appName() {
        return "منصة متابعة منظومة اجتماعات نائب الأمين";
    }

    @ModelAttribute("me")
    public AppUserDetails me() {
        return currentUser.details();
    }

    @ModelAttribute("myRole")
    public String myRole() {
        var u = currentUser.user();
        return u == null ? null : u.getRole().name();
    }

    @ModelAttribute("allStatuses")
    public TaskStatus[] statuses() {
        return TaskStatus.values();
    }

    @ModelAttribute("allWorkflow")
    public WorkflowStatus[] workflow() {
        return WorkflowStatus.values();
    }

    @ModelAttribute("allPriorities")
    public Priority[] priorities() {
        return Priority.values();
    }

    @ModelAttribute("allEscalationStatuses")
    public EscalationStatus[] escalationStatuses() {
        return EscalationStatus.values();
    }

    @ModelAttribute("allRoles")
    public Role[] roles() {
        return Role.values();
    }

    @ModelAttribute("allParticipantStatuses")
    public ParticipantStatus[] participantStatuses() {
        return ParticipantStatus.values();
    }
}
