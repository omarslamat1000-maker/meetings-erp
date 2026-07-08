package sa.gov.madinah.meetings.dto;

import lombok.Getter;
import lombok.Setter;
import sa.gov.madinah.meetings.domain.Meeting;
import sa.gov.madinah.meetings.domain.Task;

import java.util.ArrayList;
import java.util.List;

/** حزمة الإشعارات الخاصة بالمستخدم الحالي (ضمن نطاقه). */
@Getter
@Setter
public class NotificationData {
    private List<Task> recentTasks = new ArrayList<>();       // مهام مضافة حديثًا
    private List<Meeting> recentMeetings = new ArrayList<>();  // اجتماعات مضافة حديثًا
    private List<Task> delayedTasks = new ArrayList<>();       // مهام متأخرة
    private List<Task> escalatedTasks = new ArrayList<>();     // مهام مصعّدة
    private List<Task> sharedTasks = new ArrayList<>();        // مهام مشتركة

    public int getTotal() {
        return recentTasks.size() + recentMeetings.size()
                + delayedTasks.size() + escalatedTasks.size() + sharedTasks.size();
    }
}
