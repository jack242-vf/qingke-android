package cn.qingke.timetable;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Calendar logic shared by reminders and widgets; no Android dependencies. */
public final class ScheduleEngine {
    public static final class Course {
        public final String id,name,room,teacher,parity;
        public final int day,start,end,from,to;
        public final Set<Integer> weeks;
        public Course(String id,String name,String room,String teacher,int day,int start,int end,int from,int to,String parity,Set<Integer> weeks){
            this.id=id;this.name=name;this.room=room;this.teacher=teacher;this.day=day;this.start=start;this.end=end;this.from=from;this.to=to;this.parity=parity;this.weeks=weeks==null?null:new HashSet<>(weeks);
        }
        public boolean active(int week){return week>=from&&week<=to&&(weeks!=null?weeks.contains(week):"all".equals(parity)||("odd".equals(parity)?week%2==1:week%2==0));}
    }
    public static final class Model {
        public final String profileId,profileName,term;
        public final LocalDate monday;
        public final int weeks;
        public final List<LocalTime> starts,ends;
        public final List<Course> courses;
        public Model(String id,String name,String term,LocalDate monday,int weeks,List<LocalTime> starts,List<LocalTime> ends,List<Course> courses){this.profileId=id;this.profileName=name;this.term=term;this.monday=monday;this.weeks=weeks;this.starts=starts;this.ends=ends;this.courses=courses;}
    }
    public static final class Event {
        public final Course course;
        public final LocalDate date;
        public final ZonedDateTime start,end;
        public final String key;
        public Event(Model m,Course c,LocalDate date,ZoneId zone){this.course=c;this.date=date;this.start=ZonedDateTime.of(date,m.starts.get(c.start-1),zone);this.end=ZonedDateTime.of(date,m.ends.get(c.end-1),zone);this.key=date+"|"+m.profileId+"|"+c.id+"|"+start.toLocalTime();}
    }
    public static int weekAt(Model model,LocalDate date){return (int)Math.floorDiv(ChronoUnit.DAYS.between(model.monday,date),7)+1;}
    public static List<Event> onDate(Model m,LocalDate date,ZoneId zone){
        List<Event> events=new ArrayList<>();int week=weekAt(m,date);
        if(week<1||week>m.weeks)return events;
        for(Course c:m.courses)if(c.day==date.getDayOfWeek().getValue()&&c.active(week))events.add(new Event(m,c,date,zone));
        events.sort(Comparator.comparing(e->e.start.toInstant()));return events;
    }
    public static List<Event> allEvents(Model m,ZoneId zone){
        List<Event> events=new ArrayList<>();
        for(int w=1;w<=m.weeks;w++)for(Course c:m.courses)if(c.active(w))events.add(new Event(m,c,m.monday.plusDays((w-1)*7L+c.day-1),zone));
        events.sort(Comparator.comparing(e->e.start.toInstant()));return events;
    }
    public static List<Event> due(List<Event> events,Instant now,int lead,Set<String> delivered){
        List<Event> result=new ArrayList<>();for(Event e:events){Instant fire=e.start.toInstant().minusSeconds(lead*60L);if(!delivered.contains(e.key)&&!fire.isAfter(now)&&!fire.isBefore(now.minusSeconds(1800))&&e.end.toInstant().isAfter(now))result.add(e);}return result;
    }
    public static Instant nextAlarm(List<Event> events,Instant now,int lead,Set<String> delivered){
        Instant next=null;for(Event e:events){if(delivered.contains(e.key)||!e.end.toInstant().isAfter(now))continue;Instant fire=e.start.toInstant().minusSeconds(lead*60L);if(fire.isBefore(now.minusSeconds(1800)))continue;if(!fire.isAfter(now))fire=now.plusSeconds(1);if(next==null||fire.isBefore(next))next=fire;}return next;
    }
    private ScheduleEngine(){}
}
