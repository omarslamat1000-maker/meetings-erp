package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import sa.gov.madinah.meetings.domain.User;
import sa.gov.madinah.meetings.domain.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAllByOrderByFullNameAsc();
    List<User> findByRole(Role role);
    List<User> findByActiveTrueOrderByFullNameAsc();
}
