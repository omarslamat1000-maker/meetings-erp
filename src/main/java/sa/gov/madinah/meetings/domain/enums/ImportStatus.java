package sa.gov.madinah.meetings.domain.enums;

/** حالة عملية استيراد ملف Excel. */
public enum ImportStatus {
    PENDING("بانتظار الاعتماد"),
    APPROVED("معتمد"),
    REJECTED("مرفوض");

    private final String arabic;

    ImportStatus(String arabic) {
        this.arabic = arabic;
    }

    public String getArabic() {
        return arabic;
    }
}
