package sa.gov.madinah.meetings.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** نتيجة معاينة استيراد Excel قبل الاعتماد. */
@Getter
@Setter
@NoArgsConstructor
public class ImportPreviewResult {
    private String fileName;
    private int totalRows;
    private int newRows;
    private int duplicateRows;
    private int errorRows;
    private List<ImportRowPreview> rows = new ArrayList<>();
}
