package sa.gov.madinah.meetings.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.service.DepartmentService;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("active", "departments");
        return "departments/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("department", new Department());
        model.addAttribute("allDepartments", departmentService.findAll());
        model.addAttribute("active", "departments");
        return "departments/form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("department", departmentService.findById(id).orElseThrow());
        model.addAttribute("allDepartments", departmentService.findAll());
        model.addAttribute("active", "departments");
        return "departments/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@ModelAttribute Department department,
                       @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        // ربط الجهة بالجهة الأعلى (التسلسل الإداري)، مع تفادي جعلها أبًا لنفسها
        if (parentId != null && !parentId.equals(department.getId())) {
            departmentService.findById(parentId).ifPresent(department::setParent);
        } else {
            department.setParent(null);
        }
        departmentService.save(department);
        ra.addFlashAttribute("toast", "تم حفظ الجهة");
        return "redirect:/departments";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        departmentService.toggleActive(id);
        ra.addFlashAttribute("toast", "تم تغيير حالة الجهة");
        return "redirect:/departments";
    }
}
