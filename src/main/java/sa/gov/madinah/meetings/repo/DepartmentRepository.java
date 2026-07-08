package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.gov.madinah.meetings.domain.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    List<Department> findByActiveTrueOrderByNameAsc();
    List<Department> findAllByOrderByNameAsc();
}
