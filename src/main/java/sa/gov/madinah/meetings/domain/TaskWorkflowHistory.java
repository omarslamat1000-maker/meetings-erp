package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sa.gov.madinah.meetings.domain.enums.WorkflowStatus;

import java.time.LocalDateTime;

/** سجل تغيّر حالة سير العمل للمهمة. */
@Entity
@Table(name = "task_workflow_history")
@Getter
@Setter
public class TaskWorkflowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private WorkflowStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private WorkflowStatus newStatus;

    @Column(name = "changed_by", length = 150)
    private String changedBy;

    @Column(name = "change_reason", length = 1000)
    private String changeReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
