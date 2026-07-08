package sa.gov.madinah.meetings.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import sa.gov.madinah.meetings.domain.ExcelImportError;
import sa.gov.madinah.meetings.domain.ExcelImportLog;
import sa.gov.madinah.meetings.domain.Task;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.service.DashboardService;
import sa.gov.madinah.meetings.service.DepartmentService;
import sa.gov.madinah.meetings.service.ExcelImportService;
import sa.gov.madinah.meetings.service.TaskService;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final TaskService taskService;
    private final DashboardService dashboardService;
    private final ExcelImportService importService;
    private final DepartmentService departmentService;

    public ReportController(TaskService taskService, DashboardService dashboardService,
                            ExcelImportService importService, DepartmentService departmentService) {
        this.taskService = taskService;
        this.dashboardService = dashboardService;
        this.importService = importService;
        this.departmentService = departmentService;
    }

    /** صفحة تقرير مختصر قابل للطباعة (PDF عبر طباعة المتصفح) — تدعم الفلاتر لكل الحسابات. */
    @GetMapping
    public String report(@ModelAttribute TaskFilter filter, Model model) {
        var tasks = taskService.list(filter);
        // المؤشرات تعكس نتيجة الفلترة
        model.addAttribute("d", dashboardService.summarize(tasks));
        model.addAttribute("tasks", tasks);
        model.addAttribute("filter", filter);
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("meetings", taskService.visibleMeetings());
        model.addAttribute("active", "reports");
        return "reports/index";
    }

    /** جزء التقرير (المؤشرات + جدول المهام) للتحديث التفاعلي عبر AJAX. */
    @GetMapping("/fragment")
    public String fragment(@ModelAttribute TaskFilter filter, Model model) {
        var tasks = taskService.list(filter);
        model.addAttribute("d", dashboardService.summarize(tasks));
        model.addAttribute("tasks", tasks);
        model.addAttribute("filter", filter);
        return "reports/index :: reportBody";
    }

    /** تصدير المهام إلى CSV (يفتح في Excel) مع مراعاة الفلاتر والصلاحيات. */
    @GetMapping("/export/tasks.csv")
    public void exportTasks(@ModelAttribute TaskFilter filter, HttpServletResponse resp) throws IOException {
        List<Task> tasks = taskService.list(filter);
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"tasks-export.csv\"");
        Writer w = new OutputStreamWriter(resp.getOutputStream(), StandardCharsets.UTF_8);
        w.write('﻿'); // BOM ليعرض Excel العربية بشكل صحيح
        w.write("م,المهمة المسندة,جهة الاجتماع,المسؤول,الحالة,سير العمل,نسبة الإنجاز,حالة التصعيد,محضر الاجتماع,تاريخ الاستحقاق,الإفادة\n");
        for (Task t : tasks) {
            w.write(csv(t.getTaskNumber() == null ? "" : t.getTaskNumber().toString()));
            w.write("," + csv(t.getTitle()));
            w.write("," + csv(t.getDepartment() == null ? "" : t.getDepartment().getName()));
            w.write("," + csv(t.getMainResponsibleName()));
            w.write("," + csv(t.getStatus().getArabic()));
            w.write("," + csv(t.getWorkflowStatus().getArabic()));
            w.write("," + csv(t.getProgressPercentage() + "%"));
            w.write("," + csv(t.getEscalationStatus().getArabic()));
            w.write("," + csv(t.getMeeting() == null ? "" : t.getMeeting().getTitle()));
            w.write("," + csv(t.getDueDate() == null ? "" : t.getDueDate().toString()));
            w.write("," + csv(t.getFeedback()));
            w.write("\n");
        }
        w.flush();
    }

    /** تنزيل ملف أخطاء الاستيراد بصيغة CSV. */
    @GetMapping("/import/{id}/errors.csv")
    public void exportErrors(@org.springframework.web.bind.annotation.PathVariable Long id,
                             HttpServletResponse resp) throws IOException {
        ExcelImportLog log = importService.get(id);
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"import-errors-" + id + ".csv\"");
        Writer w = new OutputStreamWriter(resp.getOutputStream(), StandardCharsets.UTF_8);
        w.write('﻿');
        w.write("رقم الصف,الحقل,رسالة الخطأ,القيمة\n");
        for (ExcelImportError e : log.getErrors()) {
            w.write(csv(String.valueOf(e.getRowNumber())));
            w.write("," + csv(e.getFieldName()));
            w.write("," + csv(e.getErrorMessage()));
            w.write("," + csv(e.getRawValue()));
            w.write("\n");
        }
        w.flush();
    }

    private String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ");
        return "\"" + s + "\"";
    }
}
