package sa.gov.madinah.meetings.domain.enums;

/** أدوار المستخدمين في النظام. */
public enum Role {
    ADMIN("مدير النظام"),
    DEPARTMENT("حساب جهة"),
    RESPONSIBLE("حساب مسؤول"),
    VIEWER("مشاهد"),
    EXECUTIVE("متابعة تنفيذية");

    private final String arabic;

    Role(String arabic) {
        this.arabic = arabic;
    }

    public String getArabic() {
        return arabic;
    }

    /** الاسم المستخدم داخل Spring Security (ROLE_ADMIN ...). */
    public String authority() {
        return "ROLE_" + name();
    }
}
