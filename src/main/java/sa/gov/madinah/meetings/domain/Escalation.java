package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sa.gov.madinah.meetings.domain.enums.EscalationStatus;
import sa.gov.madinah.meetings.domain.enums.Priority;

import java.time.LocalDateTime;

/** إجراء تصعيد مرتبط بمهمة. */
@Entity
@Table(name = "escalations")
@Getter
@Setter
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 30)
    private Priority escalationLevel = Priority.NORMAL;

    @Column(name = "escalation_reason", length = 2000)
    private String escalationReason;

    @Column(name = "escalation_action", length = 2000)
    private String escalationAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_status", nullable = false, length = 30)
    private EscalationStatus escalationStatus = EscalationStatus.OPEN;

    @Column(name = "escalated_by", length = 150)
    private String escalatedBy;

    @Column(name = "resolved_by", length = 150)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
