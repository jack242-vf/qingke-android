package cn.qingke.timetable;

import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class NativeTools {
    static final int NOTIFICATION_PERMISSION=410;
    private final Activity activity;private TextView status;private AlertDialog dialog;private boolean pendingTest;
    NativeTools(Activity activity){this.activity=activity;ReminderScheduler.createChannel(activity);}
    void show(){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int padding=(int)(20*activity.getResources().getDisplayMetrics().density);body.setPadding(padding,0,padding,padding/2);
        ScheduleEngine.Model model=ScheduleRepository.load(activity);
        TextView info=new TextView(activity);info.setText("当前课表："+(model==null?"尚未添加":model.profileName)+"\n提醒和桌面小组件会跟随当前选中的课表。");info.setTextSize(13);body.addView(info);
        Switch enabled=new Switch(activity);enabled.setText("开启上课提醒");enabled.setPadding(0,20,0,12);enabled.setChecked(ScheduleRepository.enabled(activity));body.addView(enabled);
        TextView label=new TextView(activity);label.setText("提前多久提醒");body.addView(label);
        Spinner lead=new Spinner(activity);String[] labels={"提前 5 分钟","提前 10 分钟","提前 15 分钟","提前 30 分钟"};int[] minutes={5,10,15,30};lead.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,labels));for(int i=0;i<minutes.length;i++)if(minutes[i]==ScheduleRepository.lead(activity))lead.setSelection(i);body.addView(lead);
        status=new TextView(activity);status.setTextSize(12);status.setPadding(0,16,0,12);body.addView(status);
        Button test=new Button(activity);test.setText("发送测试通知");test.setOnClickListener(v->{if(needsPermission()){pendingTest=true;requestPermission();}else if(ReminderScheduler.canPost(activity)){boolean sent=ReminderScheduler.test(activity);Toast.makeText(activity,sent?"已发送，请查看通知栏":"通知未发送，请检查通知设置",Toast.LENGTH_SHORT).show();}else openNotificationSettings();});body.addView(test);
        Button permission=new Button(activity);permission.setText("通知权限设置");permission.setOnClickListener(v->openNotificationSettings());body.addView(permission);
        if(Build.VERSION.SDK_INT>=31){Button exact=new Button(activity);exact.setText("允许准时提醒");exact.setOnClickListener(v->{try{activity.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+activity.getPackageName())));}catch(ActivityNotFoundException e){Toast.makeText(activity,"此系统不支持该设置，将使用标准提醒",Toast.LENGTH_LONG).show();}});body.addView(exact);}
        Button pin=new Button(activity);pin.setText("添加今日课表到桌面");pin.setOnClickListener(v->{if(dialog!=null)dialog.dismiss();AppWidgetManager manager=AppWidgetManager.getInstance(activity);if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(activity,ScheduleWidget.class),null,null);else Toast.makeText(activity,"请长按桌面空白处 → 小组件 → 轻课",Toast.LENGTH_LONG).show();});body.addView(pin);
        TextView help=new TextView(activity);help.setText("提醒默认关闭。未允许准时提醒时可能延迟；系统省电或强行停止应用也可能影响送达。桌面小组件只在你添加后显示课程。");help.setTextSize(11);help.setPadding(0,12,0,0);body.addView(help);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        dialog=new AlertDialog.Builder(activity).setTitle("提醒与桌面").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            ScheduleRepository.configure(activity,enabled.isChecked(),minutes[lead.getSelectedItemPosition()]);
            if(enabled.isChecked()&&needsPermission())requestPermission();else{ReminderScheduler.rebuild(activity);Toast.makeText(activity,"设置已保存",Toast.LENGTH_SHORT).show();}
        }).create();dialog.show();refresh();
    }
    private boolean needsPermission(){return Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED;}
    private void requestPermission(){if(Build.VERSION.SDK_INT>=33)activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_PERMISSION);}
    private void openNotificationSettings(){activity.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,activity.getPackageName()));}
    void permissionResult(){if(pendingTest&&ReminderScheduler.canPost(activity)){boolean sent=ReminderScheduler.test(activity);Toast.makeText(activity,sent?"已发送测试通知":"通知未发送，请检查通知设置",Toast.LENGTH_SHORT).show();}else if(!ReminderScheduler.canPost(activity)){Toast.makeText(activity,"未获通知权限，正式提醒暂不可用",Toast.LENGTH_LONG).show();}pendingTest=false;ReminderScheduler.rebuild(activity);refresh();}
    void resume(){ReminderScheduler.rebuild(activity);ScheduleWidget.updateAll(activity);refresh();}
    private void refresh(){
        if(status==null||dialog==null||!dialog.isShowing())return;
        String text=!ScheduleRepository.enabled(activity)?"正式提醒尚未开启":!ReminderScheduler.canPost(activity)?"通知权限或上课提醒通知渠道未开启，暂时无法提醒":"提醒已开启";
        text+="\n准时提醒："+(ReminderScheduler.exact(activity)?"已允许":"未允许，将使用标准提醒，可能延迟");
        ScheduleEngine.Model m=ScheduleRepository.load(activity);if(m!=null&&ScheduleRepository.enabled(activity)&&ReminderScheduler.canPost(activity)){Instant next=ScheduleEngine.nextAlarm(ScheduleEngine.allEvents(m,ZoneId.systemDefault()),Instant.now(),ScheduleRepository.lead(activity),ScheduleRepository.delivered(activity));text+=next==null?"\n暂无待提醒课程":"\n下次提醒："+next.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日 HH:mm",Locale.CHINA));}
        status.setText(text);
    }
}
