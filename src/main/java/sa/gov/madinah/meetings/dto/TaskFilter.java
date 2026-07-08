package sa.gov.madinah.meetings.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;

import java.time.LocalDate;
import java.util.List;

/** معايير البحث والفلترة لقائمة المهام. */
@Getter
@Setter
public class TaskFilter {
    private String q;                 // بحث عام (الاجتماع أو المهمة)
    private Long departmentId;        // جهة الاجتماع
    private String responsible;       // المسؤول
    private TaskStatus status;        // الحالة (اختيار مفرد - لبقية الصفحات)
    private List<TaskStatus> statuses;// الحالة (اختيار متعدد - في التقارير)
    private Long meetingId;           // محضر الاجتماع

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;       // تاريخ الاجتماع من
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;         // تاريخ الاجتماع إلى
    private Boolean escalated;        // حالة التصعيد
    private Boolean shared;           // المهام المشتركة
    private Boolean delayed;          // المتأخرة فقط
    private Boolean noFeedback;       // دون إفادة
    private Boolean awaitingApproval; // بانتظار الاعتماد
    private Boolean closed;           // المغلقة
}
