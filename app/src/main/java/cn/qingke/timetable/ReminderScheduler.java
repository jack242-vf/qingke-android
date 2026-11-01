package cn.qingke.timetable;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class ReminderScheduler {
    static final String ACTION="cn.qingke.timetable.COURSE_REMINDER",CHANNEL="course-reminders";
    static void createChannel(Context c){NotificationChannel channel=new NotificationChannel(CHANNEL,"上课提醒",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("按所选课表和提前时间提醒上课");c.getSystemService(NotificationManager.class).createNotificationChannel(channel);}
    static boolean canPost(Context c){return (Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&c.getSystemService(NotificationManager.class).areNotificationsEnabled()&&channelAllowed(c);}
    private static boolean channelAllowed(Context c){NotificationChannel channel=c.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);return channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    static boolean exact(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    private static PendingIntent pending(Context c,int revision){return PendingIntent.getBroadcast(c,401,new Intent(c,ReminderReceiver.class).setAction(ACTION).putExtra("revision",revision),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static Instant rebuild(Context c){
        AlarmManager alarms=c.getSystemService(AlarmManager.class);PendingIntent pi=pending(c,ScheduleRepository.revision(c));alarms.cancel(pi);
        if(!ScheduleRepository.enabled(c)||!canPost(c))return null;
        ScheduleEngine.Model model=ScheduleRepository.load(c);if(model==null)return null;
        Instant next=ScheduleEngine.nextAlarm(ScheduleEngine.allEvents(model,ZoneId.systemDefault()),Instant.now(),ScheduleRepository.lead(c),ScheduleRepository.delivered(c));if(next==null)return null;
        try{if(exact(c))alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toEpochMilli(),pi);else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toEpochMilli(),pi);}
        catch(SecurityException e){try{alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toEpochMilli(),pi);}catch(RuntimeException ignored){return null;}}
        return next;
    }
    static void fire(Context c,int revision){
        if(revision!=ScheduleRepository.revision(c)||!ScheduleRepository.enabled(c)||!canPost(c)){rebuild(c);return;}
        ScheduleEngine.Model model=ScheduleRepository.load(c);if(model==null){rebuild(c);return;}
        List<ScheduleEngine.Event> due=ScheduleEngine.due(ScheduleEngine.allEvents(model,ZoneId.systemDefault()),Instant.now(),ScheduleRepository.lead(c),ScheduleRepository.delivered(c));
        if(!due.isEmpty()){
            Notification.InboxStyle style=new Notification.InboxStyle();for(ScheduleEngine.Event e:due)style.addLine(time(e)+" · "+e.course.name+" · "+e.course.room);
            ScheduleEngine.Event first=due.get(0);String title=due.size()==1?first.course.name:"接下来有 "+due.size()+" 门课";
            String body=time(first)+" · "+(first.course.room.isEmpty()?"教室待定":first.course.room);
            try{c.getSystemService(NotificationManager.class).notify(400,notification(c,title,body).setStyle(style).build());ScheduleRepository.mark(c,due);}catch(SecurityException ignored){}
        }
        rebuild(c);ScheduleWidget.updateAll(c);
    }
    private static String time(ScheduleEngine.Event e){return e.start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))+"–"+e.end.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));}
    private static Notification.Builder notification(Context c,String title,String body){
        PendingIntent open=PendingIntent.getActivity(c,400,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification publicVersion=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("轻课 · 上课提醒").setContentText("打开应用查看课程安排").build();
        return new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publicVersion).setCategory(Notification.CATEGORY_REMINDER);
    }
    static boolean test(Context c){createChannel(c);if(!canPost(c))return false;try{c.getSystemService(NotificationManager.class).notify(402,notification(c,"轻课测试通知","通知已送达。正式提醒将使用当前课表。").build());return true;}catch(SecurityException e){return false;}}
}
