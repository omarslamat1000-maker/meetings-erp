package sa.gov.madinah.meetings.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.User;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.Role;
import sa.gov.madinah.meetings.domain.enums.WorkflowStatus;
import sa.gov.madinah.meetings.dto.TaskFilter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** بناء استعلامات المهام (فلاتر + تحديد الصلاحيات حسب الجهة والمسؤول). */
public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> fromFilter(TaskFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (f == null) return cb.conjunction();

            if (f.getQ() != null && !f.getQ().isBlank()) {
                String like = "%" + f.getQ().trim() + "%";
                var meetingJoin = root.join("meeting", jakarta.persistence.criteria.JoinType.LEFT);
                ps.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("feedback"), like),
                        cb.like(root.get("mainResponsibleName"), like),
                        cb.like(root.get("escalationStatusText"), like),
                        cb.like(meetingJoin.get("title"), like)
                ));
            }
            if (f.getDateFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(
                        root.join("meeting", jakarta.persistence.criteria.JoinType.LEFT).get("meetingDate"),
                        f.getDateFrom()));
            }
            if (f.getDateTo() != null) {
                ps.add(cb.lessThanOrEqualTo(
                        root.join("meeting", jakarta.persistence.criteria.JoinType.LEFT).get("meetingDate"),
                        f.getDateTo()));
            }
            if (f.getDepartmentId() != null) {
                ps.add(cb.equal(root.get("department").get("id"), f.getDepartmentId()));
            }
            if (f.getResponsible() != null && !f.getResponsible().isBlank()) {
                ps.add(cb.like(root.get("mainResponsibleName"), "%" + f.getResponsible().trim() + "%"));
            }
            if (f.getStatus() != null) {
                ps.add(cb.equal(root.get("status"), f.getStatus()));
            }
            if (f.getStatuses() != null && !f.getStatuses().isEmpty()) {
                ps.add(root.get("status").in(f.getStatuses()));
            }
            if (f.getMeetingId() != null) {
                ps.add(cb.equal(root.get("meeting").get("id"), f.getMeetingId()));
            }
            if (Boolean.TRUE.equals(f.getEscalated())) {
                ps.add(cb.and(
                        cb.notEqual(root.get("escalationStatus"), EscalationStatus.NONE),
                        cb.notEqual(root.get("escalationStatus"), EscalationStatus.CLOSED)
                ));
            }
            if (Boolean.TRUE.equals(f.getShared())) {
                ps.add(cb.isTrue(root.get("shared")));
            }
            if (Boolean.TRUE.equals(f.getDelayed())) {
                ps.add(cb.or(
                        cb.equal(root.get("status"), sa.gov.madinah.meetings.domain.enums.TaskStatus.DELAYED),
                        cb.and(
                                cb.isNotNull(root.get("dueDate")),
                                cb.lessThan(root.get("dueDate"), LocalDate.now()),
                                cb.notEqual(root.get("status"), sa.gov.madinah.meetings.domain.enums.TaskStatus.COMPLETED)
                        )
                ));
            }
            if (Boolean.TRUE.equals(f.getNoFeedback())) {
                ps.add(cb.or(cb.isNull(root.get("feedback")), cb.equal(cb.trim(root.get("feedback")), "")));
            }
            if (Boolean.TRUE.equals(f.getAwaitingApproval())) {
                ps.add(cb.equal(root.get("workflowStatus"), WorkflowStatus.UNDER_REVIEW));
            }
            if (Boolean.TRUE.equals(f.getClosed())) {
                ps.add(cb.equal(root.get("workflowStatus"), WorkflowStatus.CLOSED));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /**
     * تحديد الصلاحيات: يرى المستخدم المهام ضمن نطاقه فقط.
     * - ADMIN / VIEWER / EXECUTIVE: كل المهام.
     * - DEPARTMENT: مهام جهته + المهام المشتركة التي جهته طرف فيها.
     * - RESPONSIBLE: المهام المسندة له بالاسم أو كـ responsibleUser + المشتركة التي هو طرف فيها.
     */
    public static Specification<Task> scope(User user, java.util.Set<Long> deptIds) {
        return (root, query, cb) -> {
            if (user == null) return cb.disjunction();
            Role role = user.getRole();
            // مدير النظام وحده يرى كل شيء
            if (role == Role.ADMIN) {
                return cb.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            var participants = root.join("participants", jakarta.persistence.criteria.JoinType.LEFT);
            List<Predicate> or = new ArrayList<>();

            if (role == Role.RESPONSIBLE) {
                // المسؤول: المهام المسندة له بالاسم أو كحساب + المشتركة التي يشارك فيها
                or.add(cb.equal(root.get("responsibleUser").get("id"), user.getId()));
                if (user.getFullName() != null) {
                    String namePart = user.getFullName().replace("م.", "").replace("د.", "").trim();
                    if (!namePart.isBlank()) {
                        or.add(cb.like(root.get("mainResponsibleName"), "%" + namePart + "%"));
                    }
                }
                or.add(cb.equal(participants.get("user").get("id"), user.getId()));
            } else {
                // الجهة والمشاهد والمتابعة التنفيذية: مهام جهتهم وكل الجهات التابعة لها في التسلسل الإداري
                if (deptIds != null && !deptIds.isEmpty()) {
                    or.add(root.get("department").get("id").in(deptIds));
                    or.add(participants.get("department").get("id").in(deptIds));
                }
                // أو إن كان مضافًا شخصيًا كمشارك
                or.add(cb.equal(participants.get("user").get("id"), user.getId()));
            }

            if (or.isEmpty()) return cb.disjunction();
            return cb.or(or.toArray(new Predicate[0]));
        };
    }
}
