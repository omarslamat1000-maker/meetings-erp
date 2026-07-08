package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sa.gov.madinah.meetings.domain.enums.ImportStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** سجل عملية استيراد ملف Excel. */
@Entity
@Table(name = "excel_import_logs")
@Getter
@Setter
public class ExcelImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "imported_by", length = 150)
    private String importedBy;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "success_rows")
    private int successRows;

    @Column(name = "failed_rows")
    private int failedRows;

    @Column(name = "duplicate_rows")
    private int duplicateRows;

    @Column(name = "new_rows")
    private int newRows;

    @Column(name = "updated_rows")
    private int updatedRows;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 30)
    private ImportStatus importStatus = ImportStatus.PENDING;

    @CreationTimestamp
    @Column(name = "import_date", updatable = false)
    private LocalDateTime importDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(length = 2000)
    private String notes;

    /** بيانات المعاينة (JSON) قبل الاعتماد. */
    @Lob
    @Column(name = "preview_payload")
    private String previewPayload;

    @OneToMany(mappedBy = "importLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExcelImportError> errors = new ArrayList<>();
}
