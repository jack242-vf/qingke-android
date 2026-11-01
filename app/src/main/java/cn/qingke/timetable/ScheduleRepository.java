package cn.qingke.timetable;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.time.*;
import java.util.*;

final class ScheduleRepository {
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("qingke-native",Context.MODE_PRIVATE);}
    static ScheduleEngine.Model parse(JSONObject obj)throws Exception{
        String id=text(obj,"profileId",120,false),name=text(obj,"profileName",20,false);
        JSONObject data=obj.getJSONObject("data"),settings=data.getJSONObject("settings");
        int version=data.getInt("version");if(version!=1&&version!=2)throw new Exception("Unsupported version");
        String term=text(settings,"term",40,false);LocalDate monday=LocalDate.parse(settings.getString("monday"));
        int weeks=settings.getInt("weeks");if(monday.getDayOfWeek()!=DayOfWeek.MONDAY||weeks<1||weeks>30)throw new Exception("Invalid semester");
        JSONArray times=settings.getJSONArray("times");if(times.length()<1||times.length()>13)throw new Exception("Invalid periods");
        List<LocalTime> starts=new ArrayList<>(),ends=new ArrayList<>();LocalTime previous=null;
        for(int i=0;i<times.length();i++){String[] parts=times.getString(i).split("-");if(parts.length!=2)throw new Exception();LocalTime a=LocalTime.parse(parts[0]),b=LocalTime.parse(parts[1]);if(!a.isBefore(b)||previous!=null&&a.isBefore(previous))throw new Exception("Invalid time");starts.add(a);ends.add(b);previous=b;}
        JSONArray rows=data.getJSONArray("courses");if(rows.length()>500)throw new Exception("Too many courses");List<ScheduleEngine.Course> courses=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);String cid=text(row,"id",120,false),cn=text(row,"name",40,false),room=text(row,"room",40,true),teacher=text(row,"teacher",40,true),parity=row.getString("parity");
            int day=row.getInt("day"),start=row.getInt("start"),end=row.getInt("end"),from=row.getInt("from"),to=row.getInt("to");
            if(!ids.add(cid)||day<1||day>7||start<1||end<start||end>times.length()||from<1||to<from||to>weeks||!Arrays.asList("all","odd","even").contains(parity))throw new Exception("Invalid course");
            Set<Integer> selected=null;if(row.has("weeks")){JSONArray ws=row.getJSONArray("weeks");if(ws.length()==0||ws.length()>30)throw new Exception();selected=new HashSet<>();for(int j=0;j<ws.length();j++){int w=ws.getInt(j);if(w<from||w>to||!selected.add(w))throw new Exception("Invalid weeks");}}
            courses.add(new ScheduleEngine.Course(cid,cn,room,teacher,day,start,end,from,to,parity,selected));
        }
        return new ScheduleEngine.Model(id,name,term,monday,weeks,starts,ends,courses);
    }
    private static String text(JSONObject obj,String key,int max,boolean empty)throws Exception{String s=obj.getString(key);if(s.length()>max||!empty&&s.trim().isEmpty())throw new Exception("Invalid "+key);return s;}
    static ScheduleEngine.Model load(Context c){try{String raw=prefs(c).getString("snapshot",null);return raw==null?null:parse(new JSONObject(raw));}catch(Exception e){return null;}}
    static void save(Context c,JSONObject source)throws Exception{
        parse(source);SharedPreferences p=prefs(c);
        if(!p.edit().putString("snapshot",source.toString()).putInt("revision",p.getInt("revision",0)+1).commit())throw new Exception("Storage unavailable");
    }
    static boolean enabled(Context c){return prefs(c).getBoolean("reminders",false);}
    static void invalidate(Context c){SharedPreferences p=prefs(c);p.edit().remove("snapshot").putInt("revision",p.getInt("revision",0)+1).apply();}
    static int lead(Context c){return prefs(c).getInt("lead",10);}
    static int revision(Context c){return prefs(c).getInt("revision",0);}
    static void configure(Context c,boolean enabled,int lead){if(!Arrays.asList(5,10,15,30).contains(lead))throw new IllegalArgumentException();SharedPreferences p=prefs(c);p.edit().putBoolean("reminders",enabled).putInt("lead",lead).putInt("revision",p.getInt("revision",0)+1).apply();}
    static Set<String> delivered(Context c){return new HashSet<>(prefs(c).getStringSet("delivered",Collections.emptySet()));}
    static void mark(Context c,List<ScheduleEngine.Event> events){Set<String> keys=delivered(c);String oldest=LocalDate.now().minusDays(2).toString();keys.removeIf(k->k.length()<10||k.substring(0,10).compareTo(oldest)<0);for(ScheduleEngine.Event e:events)keys.add(e.key);prefs(c).edit().putStringSet("delivered",keys).apply();}
}
