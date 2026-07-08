package sa.gov.madinah.meetings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.repo.DepartmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository repo;
    private final AuditService audit;

    public DepartmentService(DepartmentRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public List<Department> findAll() {
        return repo.findAllByOrderByNameAsc();
    }

    public List<Department> findActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public Optional<Department> findById(Long id) {
        return repo.findById(id);
    }

    /** إيجاد جهة بالاسم أو إنشاؤها (يُستخدم أثناء الاستيراد). */
    @Transactional
    public Department findOrCreate(String name) {
        if (name == null || name.isBlank()) {
            name = "غير محدد";
        }
        final String clean = name.trim();
        return repo.findByName(clean).orElseGet(() -> {
            Department d = new Department();
            d.setName(clean);
            d.setActive(true);
            return repo.save(d);
        });
    }

    @Transactional
    public Department save(Department d) {
        boolean isNew = d.getId() == null;
        Department saved = repo.save(d);
        audit.log(isNew ? "إنشاء جهة" : "تعديل جهة", "Department", saved.getId(), null, saved.getName());
        return saved;
    }

    @Transactional
    public void toggleActive(Long id) {
        repo.findById(id).ifPresent(d -> {
            d.setActive(!d.isActive());
            repo.save(d);
            audit.log("تغيير حالة الجهة", "Department", id, null, d.isActive() ? "مفعّلة" : "معطّلة");
        });
    }

    public long count() {
        return repo.count();
    }

    /** مجموعة معرّفات الجهة نفسها وكل الجهات التابعة لها في التسلسل الإداري. */
    public java.util.Set<Long> selfAndDescendantIds(Long deptId) {
        java.util.Set<Long> result = new java.util.HashSet<>();
        if (deptId == null) return result;
        java.util.Map<Long, java.util.List<Long>> childrenOf = new java.util.HashMap<>();
        for (Department d : repo.findAll()) {
            if (d.getParent() != null) {
                childrenOf.computeIfAbsent(d.getParent().getId(), k -> new java.util.ArrayList<>()).add(d.getId());
            }
        }
        java.util.Deque<Long> stack = new java.util.ArrayDeque<>();
        stack.push(deptId);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (!result.add(cur)) continue;
            java.util.List<Long> ch = childrenOf.get(cur);
            if (ch != null) ch.forEach(stack::push);
        }
        return result;
    }
}
