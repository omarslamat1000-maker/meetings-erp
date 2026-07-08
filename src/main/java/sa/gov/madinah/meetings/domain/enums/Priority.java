package sa.gov.madinah.meetings.domain.enums;

/** أولوية المهمة / مستوى التصعيد. */
public enum Priority {
    NORMAL("عادي", "gray"),
    IMPORTANT("مهم", "blue"),
    URGENT("عاجل", "gold"),
    CRITICAL("حرج", "red");

    private final String arabic;
    private final String color;

    Priority(String arabic, String color) {
        this.arabic = arabic;
        this.color = color;
    }

    public String getArabic() {
        return arabic;
    }

    public String getColor() {
        return color;
    }
}
