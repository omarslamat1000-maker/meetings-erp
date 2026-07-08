package sa.gov.madinah.meetings.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** صف واحد في معاينة استيراد Excel. */
@Getter
@Setter
@NoArgsConstructor
public class ImportRowPreview {
    private int rowNumber;
    private Integer taskNumber;
    private String department;
    private String meetingTitle;
    private String meetingDate;
    private String taskTitle;
    private String responsible;
    private String status;          // الحالة الموحّدة (عربي)
    private String rawStatus;       // النص الأصلي
    private String escalation;
    private String feedback;
    private boolean shared;
    private String outcome;         // جديد / مكرر / خطأ
    private String note;
}
