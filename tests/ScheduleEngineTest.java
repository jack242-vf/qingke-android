import cn.qingke.timetable.ScheduleEngine;
import java.time.*;
import java.util.*;

public class ScheduleEngineTest {
    static int assertions;
    static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    static void check(boolean b){assertions++;if(!b)throw new AssertionError("Assertion "+assertions);}
    static Instant at(int day,int hour,int minute){return ZonedDateTime.of(2026,8,day,hour,minute,0,0,ZONE).toInstant();}
    static ScheduleEngine.Course course(String id,String parity,Set<Integer> weeks){return new ScheduleEngine.Course(id,"示例课","A101","示例老师",1,1,2,1,3,parity,weeks);}
    static ScheduleEngine.Model model(String profile,ScheduleEngine.Course... courses){return new ScheduleEngine.Model(profile,"示例课表","秋季",LocalDate.of(2026,8,31),3,Arrays.asList(LocalTime.of(8,0),LocalTime.of(8,50),LocalTime.of(9,50)),Arrays.asList(LocalTime.of(8,45),LocalTime.of(9,35),LocalTime.of(10,35)),Arrays.asList(courses));}
    public static void main(String[] args){
        ScheduleEngine.Model m=model("one",course("a","all",null));
        check(ScheduleEngine.weekAt(m,LocalDate.of(2026,8,30))==0);
        check(ScheduleEngine.weekAt(m,LocalDate.of(2026,9,6))==1);
        check(ScheduleEngine.weekAt(m,LocalDate.of(2026,9,7))==2);
        check(ScheduleEngine.onDate(m,LocalDate.of(2026,8,30),ZONE).isEmpty());
        check(ScheduleEngine.onDate(m,LocalDate.of(2026,8,31),ZONE).size()==1);
        check(ScheduleEngine.onDate(m,LocalDate.of(2026,9,6),ZONE).isEmpty());
        check(ScheduleEngine.onDate(m,LocalDate.of(2026,9,21),ZONE).isEmpty());
        ScheduleEngine.Model odd=model("one",course("a","odd",null));
        check(ScheduleEngine.onDate(odd,LocalDate.of(2026,9,7),ZONE).isEmpty());
        check(ScheduleEngine.onDate(odd,LocalDate.of(2026,9,14),ZONE).size()==1);
        ScheduleEngine.Model selected=model("one",course("a","all",new HashSet<>(Arrays.asList(1,3))));
        check(ScheduleEngine.allEvents(selected,ZONE).size()==2);
        check(ScheduleEngine.onDate(selected,LocalDate.of(2026,9,7),ZONE).isEmpty());
        List<ScheduleEngine.Event> events=ScheduleEngine.allEvents(m,ZONE);Set<String> delivered=new HashSet<>();
        check(events.size()==3);
        check(ScheduleEngine.nextAlarm(events,at(31,7,0),10,delivered).equals(at(31,7,50)));
        check(ScheduleEngine.nextAlarm(events,at(31,7,0),30,delivered).equals(at(31,7,30)));
        check(ScheduleEngine.due(events,at(31,7,49),10,delivered).isEmpty());
        check(ScheduleEngine.due(events,at(31,7,50),10,delivered).size()==1);
        check(ScheduleEngine.due(events,at(31,8,5),10,delivered).size()==1);
        check(ScheduleEngine.due(events,at(31,8,30),10,delivered).isEmpty());
        check(ScheduleEngine.due(events,at(31,9,35),10,delivered).isEmpty());
        check(ScheduleEngine.nextAlarm(events,at(31,8,5),10,delivered).equals(at(31,8,5).plusSeconds(1)));
        delivered.add(events.get(0).key);
        check(ScheduleEngine.due(events,at(31,8,5),10,delivered).isEmpty());
        check(ScheduleEngine.nextAlarm(events,at(31,8,5),10,delivered).equals(events.get(1).start.toInstant().minusSeconds(600)));
        check(!events.get(0).key.equals(ScheduleEngine.allEvents(model("two",course("a","all",null)),ZONE).get(0).key));
        check(!events.get(0).key.equals(events.get(1).key));
        check(ScheduleEngine.due(ScheduleEngine.allEvents(model("one",course("a","all",null),course("b","all",null)),ZONE),at(31,7,50),10,new HashSet<>()).size()==2);
        check(ScheduleEngine.nextAlarm(events,ZonedDateTime.of(2026,10,1,10,0,0,0,ZONE).toInstant(),10,delivered)==null);
        check(ScheduleEngine.nextAlarm(Collections.emptyList(),Instant.now(),10,delivered)==null);
        System.out.println(assertions+" schedule/reminder assertions passed");
    }
}
