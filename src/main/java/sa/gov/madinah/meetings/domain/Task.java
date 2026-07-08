package sa.gov.madinah.meetings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import sa.gov.madinah.meetings.domain.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** المهمة المسندة الناتجة عن اجتماع. */
@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    /** رقم المهمة (العمود "م" في ملف Excel). */
    @Column(name = "task_number")
    private Integer taskNumber;

    @Column(nullable = false, length = 2000)
    private String title;

    /** اسم المسؤول الرئيسي كنص (كما ورد في الملف). */
    @Column(name = "main_responsible_name", length = 500)
    private String mainResponsibleName;

    /** تاريخ الاجتماع كما ورد في صف الملف (يُستخدم لمنع التكرار بدقة). */
    @Column(name = "source_meeting_date", length = 100)
    private String sourceMeetingDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false, length = 30)
    private WorkflowStatus workflowStatus = WorkflowStatus.CREATED;

    @Column(length = 4000)
    private String feedback;

    /** نص حالة التصعيد كما ورد في الملف. */
    @Column(name = "escalation_status_text", length = 500)
    private String escalationStatusText;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_status", nullable = false, length = 30)
    private EscalationStatus escalationStatus = EscalationStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", length = 30)
    private Priority escalationLevel = Priority.NORMAL;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Priority priority = Priority.NORMAL;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "is_shared", nullable = false)
    private boolean shared = false;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TaskParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TaskComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TaskWorkflowHistory> workflowHistory = new ArrayList<>();

    @Transient
    public boolean isEscalated() {
        return escalationStatus != null && escalationStatus != EscalationStatus.NONE
                && escalationStatus != EscalationStatus.CLOSED;
    }

    @Transient
    public boolean isDelayed() {
        return status == TaskStatus.DELAYED
                || (dueDate != null && status != TaskStatus.COMPLETED
                    && dueDate.isBefore(LocalDate.now()));
    }
}
