package sa.gov.madinah.meetings.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sa.gov.madinah.meetings.domain.AuditLog;
import sa.gov.madinah.meetings.repo.AuditLogRepository;
import sa.gov.madinah.meetings.security.CurrentUser;

/** خدمة سجل التدقيق. */
@Service
public class AuditService {

    private final AuditLogRepository repo;
    private final CurrentUser currentUser;

    public AuditService(AuditLogRepository repo, CurrentUser currentUser) {
        this.repo = repo;
        this.currentUser = currentUser;
    }

    public void log(String action, String entity, Long entityId, String oldValue, String newValue) {
        AuditLog l = new AuditLog();
        l.setUserId(currentUser.id());
        l.setUsername(currentUser.username());
        l.setAction(action);
        l.setEntityName(entity);
        l.setEntityId(entityId);
        l.setOldValue(trim(oldValue));
        l.setNewValue(trim(newValue));
        repo.save(l);
    }

    private String trim(String v) {
        if (v == null) return null;
        return v.length() > 1990 ? v.substring(0, 1990) : v;
    }

    public Page<AuditLog> list(Pageable pageable) {
        return repo.findAllByOrderByCreatedAtDesc(pageable);
    }
}
