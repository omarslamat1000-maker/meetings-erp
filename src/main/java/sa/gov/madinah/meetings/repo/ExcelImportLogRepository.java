package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.gov.madinah.meetings.domain.ExcelImportLog;

import java.util.List;

public interface ExcelImportLogRepository extends JpaRepository<ExcelImportLog, Long> {
    List<ExcelImportLog> findAllByOrderByImportDateDesc();
    ExcelImportLog findFirstByImportStatusOrderByApprovedAtDesc(sa.gov.madinah.meetings.domain.enums.ImportStatus status);
}
