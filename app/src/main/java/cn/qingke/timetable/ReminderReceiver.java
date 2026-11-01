package cn.qingke.timetable;
import android.content.*;
public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){if(ReminderScheduler.ACTION.equals(intent.getAction()))ReminderScheduler.fire(c,intent.getIntExtra("revision",-1));}
}
