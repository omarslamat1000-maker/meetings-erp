package sa.gov.madinah.meetings.domain.enums;

/** حالة التصعيد. */
public enum EscalationStatus {
    NONE("لا يوجد"),
    OPEN("مفتوح"),
    IN_PROGRESS("قيد المعالجة"),
    RESOLVED("تمت المعالجة"),
    CLOSED("مغلق");

    private final String arabic;

    EscalationStatus(String arabic) {
        this.arabic = arabic;
    }

    public String getArabic() {
        return arabic;
    }
}
