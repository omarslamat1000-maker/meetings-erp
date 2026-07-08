package sa.gov.madinah.meetings.domain.enums;

/** حالات سير العمل الرسمي للمهمة (Workflow). */
public enum WorkflowStatus {
    CREATED("تم الإنشاء", 1),
    ASSIGNED("تم الإسناد", 2),
    IN_PROGRESS("قيد التنفيذ", 3),
    UNDER_REVIEW("مراجعة الإفادة", 4),
    APPROVED("اعتماد الإفادة", 5),
    PARTIALLY_COMPLETED("تسليم جزئي", 6),
    ESCALATED("تصعيد", 7),
    DELAYED("متأخر", 8),
    COMPLETED("مكتمل", 9),
    CLOSED("مغلق", 10);

    private final String arabic;
    private final int order;

    WorkflowStatus(String arabic, int order) {
        this.arabic = arabic;
        this.order = order;
    }

    public String getArabic() {
        return arabic;
    }

    public int getOrder() {
        return order;
    }
}
