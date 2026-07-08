package sa.gov.madinah.meetings.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.domain.User;
import sa.gov.madinah.meetings.domain.enums.Role;
import sa.gov.madinah.meetings.repo.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository repo, PasswordEncoder encoder, AuditService audit) {
        this.repo = repo;
        this.encoder = encoder;
        this.audit = audit;
    }

    public List<User> findAll() {
        return repo.findAllByOrderByFullNameAsc();
    }

    public List<User> findResponsibles() {
        return repo.findByActiveTrueOrderByFullNameAsc();
    }

    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return repo.existsByUsername(username);
    }

    /** إنشاء أو تعديل مستخدم. إذا كانت rawPassword فارغة عند التعديل، تبقى كلمة المرور كما هي. */
    @Transactional
    public User save(User user, String rawPassword, Department department) {
        boolean isNew = user.getId() == null;
        user.setDepartment(department);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(encoder.encode(rawPassword));
        }
        User saved = repo.save(user);
        audit.log(isNew ? "إنشاء مستخدم" : "تعديل مستخدم", "User", saved.getId(), null,
                saved.getUsername() + " / " + saved.getRole().getArabic());
        return saved;
    }

    @Transactional
    public User create(String fullName, String username, String rawPassword, String email,
                       Role role, Department department) {
        User u = new User();
        u.setFullName(fullName);
        u.setUsername(username);
        u.setEmail(email);
        u.setRole(role);
        u.setActive(true);
        return save(u, rawPassword, department);
    }

    @Transactional
    public void toggleActive(Long id) {
        repo.findById(id).ifPresent(u -> {
            u.setActive(!u.isActive());
            repo.save(u);
            audit.log("تغيير حالة المستخدم", "User", id, null, u.isActive() ? "مفعّل" : "معطّل");
        });
    }

    public long count() {
        return repo.count();
    }

    /** مطابقة اسم مسؤول نصي بمستخدم في النظام (تطابق جزئي بسيط). */
    public Optional<User> matchResponsibleByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String needle = name.replace("م.", "").replace("د.", "").trim();
        return repo.findAll().stream()
                .filter(u -> {
                    String full = u.getFullName() == null ? "" : u.getFullName().replace("م.", "").replace("د.", "").trim();
                    return !needle.isBlank() && (full.contains(needle) || needle.contains(full));
                })
                .findFirst();
    }
}
