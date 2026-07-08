package sa.gov.madinah.meetings.domain.enums;

/**
 * الحالة المعيارية الموحّدة للمهمة (بعد التنظيف والتوحيد).
 * لكل حالة وزن إنجاز يُستخدم في حساب نسبة الإنجاز.
 */
public enum TaskStatus {
    NOT_STARTED("لم يبدأ", 0, "gray"),
    IN_PROGRESS("جاري العمل", 30, "blue"),
    PARTIAL("تسليم جزئي", 50, "gold"),
    DELAYED("متأخر", 0, "red"),
    COMPLETED("تم الانتهاء", 100, "green");

    private final String arabic;
    private final int weight;
    private final String color;

    TaskStatus(String arabic, int weight, String color) {
        this.arabic = arabic;
        this.weight = weight;
        this.color = color;
    }

    public String getArabic() {
        return arabic;
    }

    public int getWeight() {
        return weight;
    }

    public String getColor() {
        return color;
    }
}
