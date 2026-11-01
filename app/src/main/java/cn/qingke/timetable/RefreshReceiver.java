package cn.qingke.timetable;
import android.content.*;
public class RefreshReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){ReminderScheduler.createChannel(c);ReminderScheduler.rebuild(c);ScheduleWidget.updateAll(c);}
}
