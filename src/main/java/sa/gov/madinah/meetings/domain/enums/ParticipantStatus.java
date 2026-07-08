package sa.gov.madinah.meetings.domain.enums;

/** حالة إنجاز الجهة/المسؤول المشارك في مهمة مشتركة. */
public enum ParticipantStatus {
    NOT_STARTED("لم يبدأ", "gray"),
    IN_PROGRESS("جاري العمل", "blue"),
    COMPLETED("مكتمل", "green");

    private final String arabic;
    private final String color;

    ParticipantStatus(String arabic, String color) {
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
