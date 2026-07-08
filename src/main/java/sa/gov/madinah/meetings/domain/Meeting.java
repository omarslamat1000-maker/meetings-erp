package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** اجتماع (محضر اجتماع مرتبط بجهة وتاريخ). */
@Entity
@Table(name = "meetings")
@Getter
@Setter
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String title;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    /** التاريخ كنص كما ورد في الملف (قد يكون هجري/غير قياسي). */
    @Column(name = "meeting_date_text", length = 100)
    private String meetingDateText;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "minutes_reference", length = 1000)
    private String minutesReference;

    @Column(name = "task_count")
    private Integer taskCount = 0;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
