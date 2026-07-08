package sa.gov.madinah.meetings.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** حزمة مؤشرات لوحة التحكم التنفيذية. */
@Getter
@Setter
public class DashboardData {
    private long totalMeetings;
    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
    private long partialTasks;
    private long delayedTasks;
    private long notStartedTasks;
    private long escalatedTasks;
    private long sharedTasks;

    private int overallProgress;
    private String topDepartment;
    private String topResponsible;

    private LocalDateTime lastUpdate;
    private LocalDateTime lastImport;

    // توزيعات للرسوم
    private List<ChartPoint> statusDistribution;
    private List<ChartPoint> departmentDistribution;
    private List<ChartPoint> responsibleDistribution;   // أعلى 10
    private List<ChartPoint> meetingDistribution;        // أعلى 10
    private List<ChartPoint> progressByDepartment;
    private List<ChartPoint> progressByResponsible;      // أعلى 10
}
