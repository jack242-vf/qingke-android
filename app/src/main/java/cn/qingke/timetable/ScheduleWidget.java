package cn.qingke.timetable;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleWidget extends AppWidgetProvider {
    private static final String REFRESH="cn.qingke.timetable.REFRESH_WIDGET";
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){updateAll(c);ReminderScheduler.rebuild(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,android.os.Bundle options){updateAll(c);}
    @Override public void onReceive(Context c,Intent i){if(REFRESH.equals(i.getAction())){updateAll(c);ReminderScheduler.rebuild(c);}else super.onReceive(c,i);}
    static void updateAll(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,ScheduleWidget.class));
        if(ids.length==0)return;ScheduleEngine.Model model=ScheduleRepository.load(c);ZonedDateTime now=ZonedDateTime.now();
        for(int id:ids){
            RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.widget_schedule);
            view.setTextViewText(R.id.widget_title,model==null?"轻课 · 今日课表":model.profileName);
            int week=model==null?0:ScheduleEngine.weekAt(model,now.toLocalDate());
            String semester=model==null?"":week<1?" · 未开学":week>model.weeks?" · 学期已结束":" · 第 "+week+" 周";
            view.setTextViewText(R.id.widget_date,now.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA))+semester);
            view.removeAllViews(R.id.widget_courses);
            List<ScheduleEngine.Event> today=model==null?Collections.emptyList():ScheduleEngine.onDate(model,now.toLocalDate(),now.getZone());
            List<ScheduleEngine.Event> remaining=new ArrayList<>();for(ScheduleEngine.Event event:today)if(event.end.toInstant().isAfter(now.toInstant()))remaining.add(event);
            int height=manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,220);
            int limit=height<210?1:height<300?2:height<370?3:4;
            if(remaining.isEmpty()){
                RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.widget_course);
                row.setTextViewText(R.id.widget_course_time,model==null?"先导入":"今日");
                row.setTextViewText(R.id.widget_course_name,model==null?"打开轻课，添加你的课表":today.isEmpty()?"今天没有课程安排":"今天的课程已结束");
                row.setTextViewText(R.id.widget_course_room,"点击打开应用");view.addView(R.id.widget_courses,row);
            }else for(int n=0;n<Math.min(limit,remaining.size());n++){
                ScheduleEngine.Event event=remaining.get(n);RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.widget_course);
                row.setTextViewText(R.id.widget_course_time,event.start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                row.setTextViewText(R.id.widget_course_name,event.course.name);
                row.setTextViewText(R.id.widget_course_room,(event.start.toInstant().isAfter(now.toInstant())?"":"进行中 · ")+(event.course.room.isEmpty()?"教室待定":event.course.room));view.addView(R.id.widget_courses,row);
            }
            view.setTextViewText(R.id.widget_footer,remaining.size()>limit?"还有 "+(remaining.size()-limit)+" 次课 · 点击查看全部":"课表保存在本机 · 点击查看详情");
            PendingIntent open=PendingIntent.getActivity(c,501,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent refresh=PendingIntent.getBroadcast(c,502,new Intent(c,ScheduleWidget.class).setAction(REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            view.setOnClickPendingIntent(R.id.widget_root,open);view.setOnClickPendingIntent(R.id.widget_refresh,refresh);manager.updateAppWidget(id,view);
        }
    }
}
