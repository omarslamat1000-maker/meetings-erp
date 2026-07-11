package sa.gov.madinah.meetings.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sa.gov.madinah.meetings.domain.ExcelImportLog;
import sa.gov.madinah.meetings.service.ExcelImportService;

@Controller
@RequestMapping("/import")
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {

    private final ExcelImportService importService;

    public ImportController(ExcelImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("history", importService.history());
        model.addAttribute("active", "import");
        return "import/index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "الرجاء اختيار ملف Excel صالح");
            return "redirect:/import";
        }
        try {
            ExcelImportLog log = importService.previewAndStage(file);
            return "redirect:/import/" + log.getId() + "/preview";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "تعذّر قراءة الملف: " + e.getMessage());
            return "redirect:/import";
        }
    }

    @GetMapping("/{id}/preview")
    public String preview(@PathVariable Long id, Model model) {
        ExcelImportLog log = importService.get(id);
        model.addAttribute("log", log);
        model.addAttribute("preview", importService.readPreview(log));
        model.addAttribute("active", "import");
        return "import/preview";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "false") boolean updateExisting,
                          @RequestParam(defaultValue = "false") boolean deletePrevious,
                          RedirectAttributes ra) {
        importService.approve(id, updateExisting, deletePrevious);
        ra.addFlashAttribute("toast", deletePrevious
                ? "تم استبدال البيانات: حُذفت المهام السابقة وأُضيفت مهام الملف المرفوع فقط"
                : "تم اعتماد الاستيراد وإضافة البيانات إلى قاعدة البيانات");
        return "redirect:/import";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        importService.reject(id, reason == null ? "مرفوض" : reason);
        ra.addFlashAttribute("toast", "تم رفض عملية الاستيراد");
        return "redirect:/import";
    }
}
