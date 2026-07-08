package sa.gov.madinah.meetings.service;

import org.springframework.stereotype.Service;
import sa.gov.madinah.meetings.domain.ExcelImportLog;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.domain.enums.ImportStatus;
import sa.gov.madinah.meetings.domain.enums.Role;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;
import sa.gov.madinah.meetings.dto.ChartPoint;
import sa.gov.madinah.meetings.dto.DashboardData;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.repo.ExcelImportLogRepository;
import sa.gov.madinah.meetings.repo.MeetingRepository;
import sa.gov.madinah.meetings.repo.TaskRepository;
import sa.gov.madinah.meetings.security.CurrentUser;

import java.util.*;
import java.util.stream.Collectors;

/** بناء مؤشرات لوحة التحكم. مدير النظام يرى مؤشرات كل البيانات، وغيره ضمن نطاقه فقط. */
@Service
public class DashboardService {

    private final TaskRepository taskRepo;
    private final MeetingRepository meetingRepo;
    private final ExcelImportLogRepository importRepo;
    private final CurrentUser currentUser;
    private final TaskService taskService;

    public DashboardService(TaskRepository taskRepo, MeetingRepository meetingRepo,
                            ExcelImportLogRepository importRepo, CurrentUser currentUser,
                            TaskService taskService) {
        this.taskRepo = taskRepo;
        this.meetingRepo = meetingRepo;
        this.importRepo = importRepo;
        this.currentUser = currentUser;
        this.taskService = taskService;
    }

    public DashboardData build() {
        var u = currentUser.user();
        if (u != null && u.getRole() == Role.ADMIN) {
            return buildGlobal();
        }
        return buildScoped(taskService.list(new TaskFilter()));
    }

    /** مؤشرات إجمالية عامة لعرضها في صفحة الدخول (قبل المصادقة). */
    public DashboardData buildAggregate() {
        return buildGlobal();
    }

    /** بناء المؤشرات من قائمة مهام محددة (تُستخدم في التقارير المفلترة). */
    public DashboardData summarize(List<Task> tasks) {
        return buildScoped(tasks);
    }

    // ================= مؤشرات مدير النظام (كل البيانات) =================
    private DashboardData buildGlobal() {
        DashboardData d = new DashboardData();

        d.setTotalMeetings(meetingRepo.count());
        d.setTotalTasks(taskRepo.count());
        d.setCompletedTasks(taskRepo.countByStatus(TaskStatus.COMPLETED));
        d.setInProgressTasks(taskRepo.countByStatus(TaskStatus.IN_PROGRESS));
        d.setPartialTasks(taskRepo.countByStatus(TaskStatus.PARTIAL));
        d.setDelayedTasks(taskRepo.countByStatus(TaskStatus.DELAYED));
        d.setNotStartedTasks(taskRepo.countByStatus(TaskStatus.NOT_STARTED));
        d.setEscalatedTasks(taskRepo.countEscalatedActive());
        d.setSharedTasks(taskRepo.countBySharedTrue());

        Double overall = taskRepo.overallProgress();
        d.setOverallProgress(overall == null ? 0 : (int) Math.round(overall));

        d.setStatusDistribution(statusPoints(
                taskRepo.countByStatus(TaskStatus.COMPLETED),
                taskRepo.countByStatus(TaskStatus.IN_PROGRESS),
                taskRepo.countByStatus(TaskStatus.PARTIAL),
                taskRepo.countByStatus(TaskStatus.DELAYED),
                taskRepo.countByStatus(TaskStatus.NOT_STARTED)));

        d.setDepartmentDistribution(toPoints(taskRepo.countGroupByDepartment(), 20));
        d.setResponsibleDistribution(toPoints(taskRepo.countGroupByResponsible(), 10));
        d.setMeetingDistribution(toPoints(taskRepo.countGroupByMeeting(), 10));
        d.setProgressByDepartment(toProgress(taskRepo.progressByDepartment(), 20));
        d.setProgressByResponsible(toProgress(taskRepo.progressByResponsible(), 10));

        if (!d.getDepartmentDistribution().isEmpty()) d.setTopDepartment(d.getDepartmentDistribution().get(0).label());
        if (!d.getResponsibleDistribution().isEmpty()) d.setTopResponsible(d.getResponsibleDistribution().get(0).label());

        meetingRepo.findAll().stream().map(m -> m.getUpdatedAt()).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).ifPresent(d::setLastUpdate);
        fillLastImport(d);
        return d;
    }

    // ================= مؤشرات مقيّدة بنطاق المستخدم =================
    private DashboardData buildScoped(List<Task> tasks) {
        DashboardData d = new DashboardData();
        d.setTotalTasks(tasks.size());
        d.setTotalMeetings(tasks.stream()
                .map(t -> t.getMeeting() == null ? null : t.getMeeting().getId())
                .filter(Objects::nonNull).distinct().count());

        d.setCompletedTasks(countStatus(tasks, TaskStatus.COMPLETED));
        d.setInProgressTasks(countStatus(tasks, TaskStatus.IN_PROGRESS));
        d.setPartialTasks(countStatus(tasks, TaskStatus.PARTIAL));
        d.setDelayedTasks(countStatus(tasks, TaskStatus.DELAYED));
        d.setNotStartedTasks(countStatus(tasks, TaskStatus.NOT_STARTED));
        d.setEscalatedTasks(tasks.stream().filter(Task::isEscalated).count());
        d.setSharedTasks(tasks.stream().filter(Task::isShared).count());

        d.setOverallProgress((int) Math.round(tasks.stream()
                .mapToInt(t -> t.getProgressPercentage() == null ? 0 : t.getProgressPercentage())
                .average().orElse(0)));

        d.setStatusDistribution(statusPoints(
                countStatus(tasks, TaskStatus.COMPLETED),
                countStatus(tasks, TaskStatus.IN_PROGRESS),
                countStatus(tasks, TaskStatus.PARTIAL),
                countStatus(tasks, TaskStatus.DELAYED),
                countStatus(tasks, TaskStatus.NOT_STARTED)));

        d.setDepartmentDistribution(countPoints(tasks,
                t -> t.getDepartment() == null ? "غير محدد" : t.getDepartment().getName(), 20));
        d.setResponsibleDistribution(countPoints(tasks,
                t -> t.getMainResponsibleName() == null ? "غير محدد" : t.getMainResponsibleName(), 10));
        d.setMeetingDistribution(countPoints(tasks,
                t -> t.getMeeting() == null ? "غير محدد" : t.getMeeting().getTitle(), 10));
        d.setProgressByDepartment(progressPoints(tasks,
                t -> t.getDepartment() == null ? "غير محدد" : t.getDepartment().getName(), 20));
        d.setProgressByResponsible(progressPoints(tasks,
                t -> t.getMainResponsibleName() == null ? "غير محدد" : t.getMainResponsibleName(), 10));

        if (!d.getDepartmentDistribution().isEmpty()) d.setTopDepartment(d.getDepartmentDistribution().get(0).label());
        if (!d.getResponsibleDistribution().isEmpty()) d.setTopResponsible(d.getResponsibleDistribution().get(0).label());

        tasks.stream().map(Task::getUpdatedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).ifPresent(d::setLastUpdate);
        fillLastImport(d);
        return d;
    }

    // ================= أدوات مساعدة =================
    private long countStatus(List<Task> tasks, TaskStatus st) {
        return tasks.stream().filter(t -> t.getStatus() == st).count();
    }

    private List<ChartPoint> statusPoints(long done, long prog, long partial, long delayed, long notStarted) {
        List<ChartPoint> l = new ArrayList<>();
        l.add(new ChartPoint(TaskStatus.COMPLETED.getArabic(), done, "#22744a"));
        l.add(new ChartPoint(TaskStatus.IN_PROGRESS.getArabic(), prog, "#2980b9"));
        l.add(new ChartPoint(TaskStatus.PARTIAL.getArabic(), partial, "#c9a227"));
        l.add(new ChartPoint(TaskStatus.DELAYED.getArabic(), delayed, "#c0392b"));
        l.add(new ChartPoint(TaskStatus.NOT_STARTED.getArabic(), notStarted, "#8a92a6"));
        return l;
    }

    private List<ChartPoint> countPoints(List<Task> tasks, java.util.function.Function<Task, String> key, int limit) {
        Map<String, Long> m = tasks.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new ChartPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<ChartPoint> progressPoints(List<Task> tasks, java.util.function.Function<Task, String> key, int limit) {
        Map<String, List<Task>> g = tasks.stream().collect(Collectors.groupingBy(key));
        return g.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<Task>> e) -> e.getValue().size()).reversed())
                .limit(limit)
                .map(e -> new ChartPoint(e.getKey(), Math.round(e.getValue().stream()
                        .mapToInt(t -> t.getProgressPercentage() == null ? 0 : t.getProgressPercentage())
                        .average().orElse(0))))
                .collect(Collectors.toList());
    }

    private void fillLastImport(DashboardData d) {
        ExcelImportLog lastImport = importRepo.findFirstByImportStatusOrderByApprovedAtDesc(ImportStatus.APPROVED);
        if (lastImport != null) {
            d.setLastImport(lastImport.getApprovedAt() != null ? lastImport.getApprovedAt() : lastImport.getImportDate());
        }
    }

    private List<ChartPoint> toPoints(List<Object[]> rows, int limit) {
        List<ChartPoint> list = new ArrayList<>();
        int i = 0;
        for (Object[] r : rows) {
            if (i++ >= limit) break;
            String label = r[0] == null ? "غير محدد" : r[0].toString();
            list.add(new ChartPoint(label, ((Number) r[1]).doubleValue()));
        }
        return list;
    }

    private List<ChartPoint> toProgress(List<Object[]> rows, int limit) {
        List<ChartPoint> list = new ArrayList<>();
        int i = 0;
        for (Object[] r : rows) {
            if (i++ >= limit) break;
            String label = r[0] == null ? "غير محدد" : r[0].toString();
            double avg = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
            list.add(new ChartPoint(label, Math.round(avg)));
        }
        return list;
    }
}
