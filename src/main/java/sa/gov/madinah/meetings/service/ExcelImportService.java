package sa.gov.madinah.meetings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sa.gov.madinah.meetings.domain.*;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.ImportStatus;
import sa.gov.madinah.meetings.domain.enums.TaskStatus;
import sa.gov.madinah.meetings.dto.ImportPreviewResult;
import sa.gov.madinah.meetings.dto.ImportRowPreview;
import sa.gov.madinah.meetings.repo.ExcelImportLogRepository;
import sa.gov.madinah.meetings.repo.TaskRepository;
import sa.gov.madinah.meetings.security.CurrentUser;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * خدمة استيراد ملفات Excel باستخدام Apache POI.
 * تدعم: قراءة الأعمدة العربية، التنظيف، منع التكرار، المعاينة قبل الاعتماد،
 * والاعتماد النهائي (إنشاء/تحديث) بموافقة مدير النظام.
 */
@Service
public class ExcelImportService {

    private final TaskRepository taskRepo;
    private final ExcelImportLogRepository importLogRepo;
    private final DepartmentService departmentService;
    private final MeetingService meetingService;
    private final UserService userService;
    private final StatusNormalizer normalizer;
    private final AuditService audit;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public ExcelImportService(TaskRepository taskRepo, ExcelImportLogRepository importLogRepo,
                              DepartmentService departmentService, MeetingService meetingService,
                              UserService userService, StatusNormalizer normalizer,
                              AuditService audit, CurrentUser currentUser, ObjectMapper objectMapper) {
        this.taskRepo = taskRepo;
        this.importLogRepo = importLogRepo;
        this.departmentService = departmentService;
        this.meetingService = meetingService;
        this.userService = userService;
        this.normalizer = normalizer;
        this.audit = audit;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    // فهارس الأعمدة المكتشفة
    private static final class ColMap {
        int feedback = -1, status = -1, escalation = -1, date = -1, responsible = -1,
            taskTitle = -1, number = -1, meeting = -1, taskCount = -1, meetingDate = -1, department = -1;
    }

    /** قراءة الملف وبناء المعاينة (دون حفظ نهائي)، ثم تخزين سجل استيراد PENDING. */
    @Transactional
    public ExcelImportLog previewAndStage(MultipartFile file) throws Exception {
        ImportPreviewResult result = new ImportPreviewResult();
        result.setFileName(file.getOriginalFilename());

        // مفاتيح المهام الموجودة مسبقًا لمنع التكرار
        Set<String> existingKeys = buildExistingKeys();
        Set<String> seenInFile = new HashSet<>();

        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("الملف لا يحتوي على أوراق عمل");

            ColMap cols = detectColumns(sheet.getRow(sheet.getFirstRowNum()));
            if (cols.taskTitle < 0 && cols.feedback < 0) {
                throw new IllegalArgumentException("تعذّر التعرف على أعمدة الملف. تأكد من مطابقة الترويسة العربية.");
            }

            int first = sheet.getFirstRowNum() + 1;
            int last = sheet.getLastRowNum();
            for (int r = first; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                ImportRowPreview p = new ImportRowPreview();
                p.setRowNumber(r + 1);
                p.setFeedback(cell(row, cols.feedback));
                p.setRawStatus(cell(row, cols.status));
                p.setEscalation(cell(row, cols.escalation));
                p.setResponsible(cell(row, cols.responsible));
                p.setTaskTitle(cell(row, cols.taskTitle));
                p.setMeetingTitle(cell(row, cols.meeting));
                p.setMeetingDate(cell(row, cols.meetingDate));
                p.setDepartment(cell(row, cols.department));
                p.setTaskNumber(parseInt(cell(row, cols.number)));

                TaskStatus st = normalizer.normalizeStatus(p.getRawStatus());
                p.setStatus(st.getArabic());
                p.setShared(p.getResponsible() != null && p.getResponsible().contains("+"));

                // صف بلا مهمة => خطأ/تجاهل
                if (p.getTaskTitle() == null || p.getTaskTitle().isBlank()) {
                    p.setOutcome("خطأ");
                    p.setNote("لا يوجد نص للمهمة المسندة");
                    result.setErrorRows(result.getErrorRows() + 1);
                    result.getRows().add(p);
                    continue;
                }

                String key = dedupKey(p.getMeetingTitle(), p.getMeetingDate(), p.getTaskTitle(),
                        p.getResponsible(), p.getDepartment());
                if (existingKeys.contains(key) || seenInFile.contains(key)) {
                    p.setOutcome("مكرر");
                    p.setNote("موجود مسبقًا - سيتم تخطيه أو تحديثه حسب اختيار مدير النظام");
                    result.setDuplicateRows(result.getDuplicateRows() + 1);
                } else {
                    p.setOutcome("جديد");
                    result.setNewRows(result.getNewRows() + 1);
                }
                seenInFile.add(key);
                result.getRows().add(p);
            }
        }
        result.setTotalRows(result.getRows().size());

        // حفظ سجل استيراد PENDING مع المعاينة
        ExcelImportLog log = new ExcelImportLog();
        log.setFileName(result.getFileName());
        log.setImportedBy(currentUser.username());
        log.setTotalRows(result.getTotalRows());
        log.setNewRows(result.getNewRows());
        log.setDuplicateRows(result.getDuplicateRows());
        log.setFailedRows(result.getErrorRows());
        log.setSuccessRows(result.getNewRows() + result.getDuplicateRows());
        log.setImportStatus(ImportStatus.PENDING);
        log.setPreviewPayload(objectMapper.writeValueAsString(result));

        // تسجيل أخطاء الصفوف
        for (ImportRowPreview p : result.getRows()) {
            if ("خطأ".equals(p.getOutcome())) {
                ExcelImportError err = new ExcelImportError();
                err.setImportLog(log);
                err.setRowNumber(p.getRowNumber());
                err.setFieldName("المهمة المسندة");
                err.setErrorMessage(p.getNote());
                err.setRawValue(p.getFeedback());
                log.getErrors().add(err);
            }
        }

        ExcelImportLog saved = importLogRepo.save(log);
        audit.log("استيراد Excel (معاينة)", "ExcelImportLog", saved.getId(), null,
                result.getFileName() + " - صفوف: " + result.getTotalRows());
        return saved;
    }

    public ImportPreviewResult readPreview(ExcelImportLog log) {
        try {
            return objectMapper.readValue(log.getPreviewPayload(), ImportPreviewResult.class);
        } catch (Exception e) {
            return new ImportPreviewResult();
        }
    }

    /** اعتماد الاستيراد: إنشاء المهام الجديدة وتحديث المكررة (إن اختير ذلك). */
    @Transactional
    public ExcelImportLog approve(Long importLogId, boolean updateExisting) {
        ExcelImportLog log = importLogRepo.findById(importLogId).orElseThrow();
        if (log.getImportStatus() == ImportStatus.APPROVED) {
            return log; // مُعتمد مسبقًا
        }
        ImportPreviewResult preview = readPreview(log);
        int created = 0, updated = 0;

        for (ImportRowPreview p : preview.getRows()) {
            if ("خطأ".equals(p.getOutcome())) continue;

            Department dept = departmentService.findOrCreate(p.getDepartment());
            Meeting meeting = meetingService.findOrCreate(p.getMeetingTitle(), dept, p.getMeetingDate());

            String key = dedupKey(p.getMeetingTitle(), p.getMeetingDate(), p.getTaskTitle(),
                    p.getResponsible(), p.getDepartment());
            Task existing = findExistingByKey(key);

            if (existing != null) {
                if (updateExisting) {
                    applyRow(existing, p, dept, meeting);
                    taskRepo.save(existing);
                    updated++;
                }
                continue;
            }

            Task t = new Task();
            applyRow(t, p, dept, meeting);
            t.setCreatedBy(currentUser.username());
            taskRepo.save(t);
            created++;
        }

        // تحديث عدد المهام لكل اجتماع
        log.setImportStatus(ImportStatus.APPROVED);
        log.setApprovedAt(LocalDateTime.now());
        log.setApprovedBy(currentUser.username());
        log.setNewRows(created);
        log.setUpdatedRows(updated);
        log.setNotes("تم الاعتماد: " + created + " جديدة، " + updated + " محدثة");
        ExcelImportLog saved = importLogRepo.save(log);

        audit.log("اعتماد استيراد Excel", "ExcelImportLog", saved.getId(), null,
                "جديدة: " + created + " / محدثة: " + updated);
        return saved;
    }

    @Transactional
    public void reject(Long importLogId, String reason) {
        ExcelImportLog log = importLogRepo.findById(importLogId).orElseThrow();
        log.setImportStatus(ImportStatus.REJECTED);
        log.setNotes(reason);
        importLogRepo.save(log);
        audit.log("رفض استيراد Excel", "ExcelImportLog", importLogId, null, reason);
    }

    public List<ExcelImportLog> history() {
        return importLogRepo.findAllByOrderByImportDateDesc();
    }

    public ExcelImportLog get(Long id) {
        return importLogRepo.findById(id).orElseThrow();
    }

    // ---------------- أدوات مساعدة ----------------

    private void applyRow(Task t, ImportRowPreview p, Department dept, Meeting meeting) {
        t.setTitle(p.getTaskTitle());
        t.setFeedback(p.getFeedback());
        t.setMainResponsibleName(p.getResponsible());
        t.setDepartment(dept);
        t.setMeeting(meeting);
        t.setSourceMeetingDate(norm(p.getMeetingDate()));
        t.setTaskNumber(p.getTaskNumber());
        TaskStatus st = normalizer.normalizeStatus(p.getRawStatus());
        t.setStatus(st);
        t.setProgressPercentage(normalizer.progressFor(st));
        t.setWorkflowStatus(normalizer.deriveWorkflow(st));
        t.setEscalationStatusText(normalizer.clean(p.getEscalation()));
        EscalationStatus es = normalizer.normalizeEscalation(p.getEscalation());
        t.setEscalationStatus(es);
        t.setEscalationLevel(normalizer.deriveLevel(p.getEscalation()));
        t.setShared(p.isShared());
        // ربط المسؤول بحساب مستخدم إن وُجد
        if (!p.isShared() && p.getResponsible() != null) {
            userService.matchResponsibleByName(p.getResponsible()).ifPresent(t::setResponsibleUser);
        }
    }

    private Set<String> buildExistingKeys() {
        Set<String> keys = new HashSet<>();
        for (Task t : taskRepo.findAll()) {
            String mt = t.getMeeting() == null ? null : t.getMeeting().getTitle();
            String dn = t.getDepartment() == null ? null : t.getDepartment().getName();
            keys.add(dedupKey(mt, t.getSourceMeetingDate(), t.getTitle(), t.getMainResponsibleName(), dn));
        }
        return keys;
    }

    private Task findExistingByKey(String key) {
        for (Task t : taskRepo.findAll()) {
            String mt = t.getMeeting() == null ? null : t.getMeeting().getTitle();
            String dn = t.getDepartment() == null ? null : t.getDepartment().getName();
            if (dedupKey(mt, t.getSourceMeetingDate(), t.getTitle(), t.getMainResponsibleName(), dn).equals(key)) {
                return t;
            }
        }
        return null;
    }

    /** مفتاح منع التكرار: محضر + تاريخ الاجتماع + المهمة + المسؤول + الجهة. */
    private String dedupKey(String meeting, String date, String task, String responsible, String dept) {
        return norm(meeting) + "|" + norm(date) + "|" + norm(task) + "|" + norm(responsible) + "|" + norm(dept);
    }

    private String norm(String s) {
        String c = normalizer.clean(s);
        return c == null ? "" : c;
    }

    private ColMap detectColumns(Row header) {
        ColMap c = new ColMap();
        if (header == null) return c;
        for (int i = header.getFirstCellNum(); i < header.getLastCellNum(); i++) {
            String h = normalizer.clean(cell(header, i));
            if (h == null) continue;
            if (h.contains("تصعيد")) c.escalation = i;
            else if (h.contains("الإفادة") || h.contains("الافادة")) c.feedback = i;
            else if (h.equals("الحالة")) c.status = i;
            else if (h.contains("تاريخ الاجتماع")) c.meetingDate = i;
            else if (h.equals("التاريخ")) c.date = i;
            else if (h.contains("المسؤول") || h.contains("المسئول")) c.responsible = i;
            else if (h.contains("المهمة")) c.taskTitle = i;
            else if (h.contains("محضر")) c.meeting = i;
            else if (h.contains("عدد المهام")) c.taskCount = i;
            else if (h.contains("جهة")) c.department = i;
            else if (h.equals("م")) c.number = i;
        }
        return c;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            String v = cell(row, i);
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }

    private String cell(Row row, int idx) {
        if (idx < 0 || row == null) return null;
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String v = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> null;
        };
        return normalizer.clean(v);
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        try { return (int) Double.parseDouble(s.trim()); }
        catch (Exception e) { return null; }
    }
}
