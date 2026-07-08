package sa.gov.madinah.meetings.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sa.gov.madinah.meetings.domain.Meeting;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByOrderByCreatedAtDesc();

    List<Meeting> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    Optional<Meeting> findByTitleAndDepartmentId(String title, Long departmentId);

    Optional<Meeting> findFirstByTitle(String title);

    @Query("select count(m) from Meeting m")
    long countMeetings();

    @Query("select m from Meeting m order by m.meetingDate desc nulls last, m.createdAt desc")
    List<Meeting> findAllOrderByDate();
}
