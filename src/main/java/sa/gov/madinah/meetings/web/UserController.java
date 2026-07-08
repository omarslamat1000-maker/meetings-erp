package sa.gov.madinah.meetings.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.Department;
import sa.gov.madinah.meetings.domain.User;
import sa.gov.madinah.meetings.domain.enums.Role;
import sa.gov.madinah.meetings.service.DepartmentService;
import sa.gov.madinah.meetings.service.UserService;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final DepartmentService departmentService;

    public UserController(UserService userService, DepartmentService departmentService) {
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("active", "users");
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("active", "users");
        return "users/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id).orElseThrow());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("active", "users");
        return "users/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute User user,
                       @RequestParam(required = false) String rawPassword,
                       @RequestParam(required = false) Long departmentId,
                       RedirectAttributes ra) {
        if (user.getId() == null && userService.usernameExists(user.getUsername())) {
            ra.addFlashAttribute("error", "اسم المستخدم مستخدم مسبقًا");
            return "redirect:/users/new";
        }
        Department dept = departmentId == null ? null
                : departmentService.findById(departmentId).orElse(null);
        userService.save(user, rawPassword, dept);
        ra.addFlashAttribute("toast", "تم حفظ المستخدم");
        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleActive(id);
        ra.addFlashAttribute("toast", "تم تغيير حالة المستخدم");
        return "redirect:/users";
    }
}
