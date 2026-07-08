package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** خطأ في صف من صفوف عملية استيراد Excel. */
@Entity
@Table(name = "excel_import_errors")
@Getter
@Setter
public class ExcelImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "import_log_id", nullable = false)
    private ExcelImportLog importLog;

    @Column(name = "row_number")
    private int rowNumber;

    @Column(name = "field_name", length = 150)
    private String fieldName;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "raw_value", length = 2000)
    private String rawValue;
}
