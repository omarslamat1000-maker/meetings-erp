package sa.gov.madinah.meetings.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import sa.gov.madinah.meetings.dto.DashboardData;
import sa.gov.madinah.meetings.dto.TaskFilter;
import sa.gov.madinah.meetings.service.DashboardService;
import sa.gov.madinah.meetings.service.DepartmentService;
import sa.gov.madinah.meetings.service.TaskService;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final TaskService taskService;
    private final DepartmentService departmentService;
    private final ObjectMapper objectMapper;

    public DashboardController(DashboardService dashboardService, TaskService taskService,
                              DepartmentService departmentService, ObjectMapper objectMapper) {
        this.dashboardService = dashboardService;
        this.taskService = taskService;
        this.departmentService = departmentService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute TaskFilter filter, Model model) throws Exception {
        fillModel(filter, model);
        model.addAttribute("filter", filter);
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("meetings", taskService.visibleMeetings());
        model.addAttribute("active", "dashboard");
        return "dashboard";
    }

    /** جزء لوحة التحكم (المؤشرات + الرسوم) للتحديث التفاعلي. */
    @GetMapping("/dashboard/fragment")
    public String fragment(@ModelAttribute TaskFilter filter, Model model) throws Exception {
        fillModel(filter, model);
        return "dashboard :: dashBody";
    }

    private void fillModel(TaskFilter filter, Model model) throws Exception {
        DashboardData d = dashboardService.summarize(taskService.list(filter));
        model.addAttribute("d", d);
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("status", d.getStatusDistribution());
        charts.put("dept", d.getDepartmentDistribution());
        charts.put("resp", d.getResponsibleDistribution());
        charts.put("meeting", d.getMeetingDistribution());
        charts.put("progDept", d.getProgressByDepartment());
        charts.put("progResp", d.getProgressByResponsible());
        model.addAttribute("chartData", objectMapper.writeValueAsString(charts));
    }
}
