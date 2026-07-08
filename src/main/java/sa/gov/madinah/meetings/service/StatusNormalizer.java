package sa.gov.madinah.meetings.service;

import org.springframework.stereotype.Component;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.Priority;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;
import sa.gov.madinah.meetings.domain.enums.WorkflowStatus;

/**
 * توحيد الحالات النصية القادمة من ملف Excel إلى حالات معيارية.
 * يطبّق قواعد التوحيد المطلوبة (لما يبدا = لم يبدأ، جاري = جاري العمل ...).
 */
@Component
public class StatusNormalizer {

    /** تنظيف النص: إزالة المسافات الزائدة وتوحيد المحارف. */
    public String clean(String raw) {
        if (raw == null) return null;
        String s = raw.replace(' ', ' ')      // مسافة غير قابلة للكسر
                      .replace('\t', ' ')
                      .replace('\n', ' ')
                      .replace('\r', ' ')
                      .trim()
                      .replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    /** توحيد الحالة النصية إلى TaskStatus معيارية. */
    public TaskStatus normalizeStatus(String raw) {
        String s = clean(raw);
        if (s == null) return TaskStatus.NOT_STARTED;

        // تسليم جزئي
        if (s.contains("جزئي")) return TaskStatus.PARTIAL;
        // متأخر / متعثر
        if (s.contains("متأخر") || s.contains("متعثر") || s.contains("متاخر")) return TaskStatus.DELAYED;
        // لم يبدأ (لما يبدا / لم يبدا / لم يبدأ بعد)
        if (s.contains("لم يبد") || s.contains("لما يبد") || s.contains("لم يبدأ")
                || s.contains("لن يبدأ") || s.contains("قيد الانتظار")) return TaskStatus.NOT_STARTED;
        // جاري العمل (جاري / قيد الإنجاز / قيد التنفيذ)
        if (s.contains("جاري") || s.contains("قيد الإنجاز") || s.contains("قيد الانجاز")
                || s.contains("قيد التنفيذ") || s.startsWith("جار")) return TaskStatus.IN_PROGRESS;
        // مكتمل (تم / منجز / مكتمل / انتهى / تمت)
        if (s.contains("تم الانتهاء") || s.contains("منجز") || s.contains("مكتمل")
                || s.contains("انته") || s.startsWith("تم") || s.contains("تمت") || s.equals("تم")) {
            return TaskStatus.COMPLETED;
        }
        // القيمة الافتراضية: لم يبدأ
        return TaskStatus.NOT_STARTED;
    }

    /** اشتقاق حالة سير العمل الأولية من الحالة المعيارية. */
    public WorkflowStatus deriveWorkflow(TaskStatus status) {
        return switch (status) {
            case COMPLETED -> WorkflowStatus.COMPLETED;
            case PARTIAL -> WorkflowStatus.PARTIALLY_COMPLETED;
            case IN_PROGRESS -> WorkflowStatus.IN_PROGRESS;
            case DELAYED -> WorkflowStatus.DELAYED;
            case NOT_STARTED -> WorkflowStatus.ASSIGNED;
        };
    }

    /** نسبة الإنجاز المقابلة للحالة. */
    public int progressFor(TaskStatus status) {
        return status.getWeight();
    }

    /** توحيد نص حالة التصعيد. */
    public EscalationStatus normalizeEscalation(String raw) {
        String s = clean(raw);
        if (s == null) return EscalationStatus.NONE;
        if (s.contains("لا يوجد") || s.equals("-")) return EscalationStatus.NONE;
        if (s.contains("مغلق") || s.contains("أغلق") || s.contains("اغلق")) return EscalationStatus.CLOSED;
        if (s.contains("عولج") || s.contains("تمت المعالجة") || s.contains("تم المعالجة")) return EscalationStatus.RESOLVED;
        if (s.contains("قيد")) return EscalationStatus.IN_PROGRESS;
        if (s.contains("تصعيد") || s.contains("مصعد") || s.contains("عاجل") || s.contains("حرج")) {
            return EscalationStatus.OPEN;
        }
        // أي نص آخر غير فارغ يُعامل كتصعيد مفتوح
        return EscalationStatus.OPEN;
    }

    /** اشتقاق مستوى الأولوية من نص التصعيد. */
    public Priority deriveLevel(String raw) {
        String s = clean(raw);
        if (s == null) return Priority.NORMAL;
        if (s.contains("حرج")) return Priority.CRITICAL;
        if (s.contains("عاجل")) return Priority.URGENT;
        if (s.contains("مهم") || s.contains("تصعيد")) return Priority.IMPORTANT;
        return Priority.NORMAL;
    }
}
