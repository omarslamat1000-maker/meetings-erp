package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import sa.gov.madinah.meetings.domain.enums.ParticipantStatus;

import java.time.LocalDateTime;

/** جهة/مسؤول مشارك في مهمة مشتركة. */
@Entity
@Table(name = "task_participants")
@Getter
@Setter
public class TaskParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    /** اسم المشارك كنص (لأن بعض المشاركين قد لا يملكون حسابًا). */
    @Column(name = "participant_name", length = 255)
    private String participantName;

    @Column(name = "responsibility_description", length = 1000)
    private String responsibilityDescription;

    @Column(name = "action_required", length = 1000)
    private String actionRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_status", nullable = false, length = 30)
    private ParticipantStatus participantStatus = ParticipantStatus.NOT_STARTED;

    @Column(name = "participant_progress")
    private Integer participantProgress = 0;

    @Column(name = "participant_feedback", length = 2000)
    private String participantFeedback;

    @Column(nullable = false)
    private boolean approved = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
