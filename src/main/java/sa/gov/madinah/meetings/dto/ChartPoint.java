package sa.gov.madinah.meetings.dto;

/** نقطة بيانات لرسم بياني (تسمية + قيمة + لون اختياري). */
public record ChartPoint(String label, double value, String color) {
    public ChartPoint(String label, double value) {
        this(label, value, null);
    }
}
