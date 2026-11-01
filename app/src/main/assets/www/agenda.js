import{active,dateOf}from './core.js';
import{activeProfile}from './profile-store.js';
export function dayCourses(data,week,day){return data.courses.filter(c=>c.day===day&&active(c,week)).sort((a,b)=>a.start-b.start||a.name.localeCompare(b.name,'zh-CN'))}
export function courseState(c,settings,week,day,now=new Date()){
  const date=dateOf(settings.monday);date.setDate(date.getDate()+(week-1)*7+day-1);
  const time=(period,end=false)=>{const d=new Date(date);const[h,m]=settings.times[period-1].split('-')[end?1:0].split(':').map(Number);d.setHours(h,m,0,0);return d};
  if(now>=time(c.end,true))return '已结束';if(now>=time(c.start))return '进行中';return '未开始';
}
export function initAgenda(getData){
 const $=s=>document.querySelector(s),names=['一','二','三','四','五','六','日'];
 let selectedDay=new Date().getDay()||7,currentWeek=Number($('#weekBtn').textContent.match(/\d+/)?.[0]||1),mode=localStorage.getItem('qingke-view-'+activeProfile(localStorage).id)||localStorage.getItem('qingke-view')||'day';
 const tabs=document.createElement('div');tabs.className='view-tabs';tabs.innerHTML='<button id="agendaTab" role="tab">每日课程</button><button id="weekTab" role="tab">整周课表</button>';tabs.setAttribute('role','tablist');tabs.setAttribute('aria-label','课表视图');$('.toolbar').before(tabs);
 const panel=document.createElement('section');panel.className='agenda-panel';panel.innerHTML='<div class="day-strip" aria-label="选择星期"></div><div id="agendaHeading" class="agenda-heading"></div><div id="agendaCards"></div>';
 $('.schedule').before(panel);
 for(let d=1;d<=7;d++){const b=document.createElement('button');b.textContent='周'+names[d-1];b.dataset.day=d;b.onclick=()=>{selectedDay=d;render()};$('.day-strip').append(b)}
 function setMode(m){mode=m;try{localStorage.setItem('qingke-view-'+activeProfile(localStorage).id,m)}catch{}render()}
 $('#agendaTab').onclick=()=>setMode('day');$('#weekTab').onclick=()=>setMode('week');
 function render(){
  $('.schedule').hidden=mode==='day';panel.hidden=mode!=='day';$('#agendaTab').setAttribute('aria-selected',String(mode==='day'));$('#weekTab').setAttribute('aria-selected',String(mode==='week'));
  const data=getData();if(!data)return;const date=dateOf(data.settings.monday);date.setDate(date.getDate()+(currentWeek-1)*7+selectedDay-1);
  for(const b of $('.day-strip').children)b.setAttribute('aria-pressed',String(Number(b.dataset.day)===selectedDay));
  const courses=dayCourses(data,currentWeek,selectedDay);$('#agendaHeading').textContent=`${date.getMonth()+1}月${date.getDate()}日 · ${courses.length} 次课`;
  const cards=$('#agendaCards');cards.replaceChildren();
  if(!courses.length){const empty=document.createElement('div');empty.className='agenda-empty';empty.textContent='这一天没有课，留一点时间给自己。';cards.append(empty)}
  for(const c of courses){
   const card=document.createElement('button');card.className='agenda-card';card.dataset.color=c.color;card.setAttribute('aria-label',c.name+'，'+c.room+'，点击查看或编辑');
   const time=document.createElement('div');time.className='agenda-time';const start=document.createElement('strong');start.textContent=data.settings.times[c.start-1].split('-')[0];const end=document.createElement('small');end.textContent=data.settings.times[c.end-1].split('-')[1];time.append(start,end);
   const details=document.createElement('div');details.className='agenda-details';const title=document.createElement('strong');title.textContent=c.name;const room=document.createElement('span');room.textContent='⌖ '+(c.room||'教室待定');const teacher=document.createElement('small');teacher.textContent=`${c.teacher||'教师待定'} · 第 ${c.start}–${c.end} 节`;details.append(title,room,teacher);
   const state=document.createElement('span');state.className='course-state';state.textContent=courseState(c,data.settings,currentWeek,selectedDay);card.append(time,details,state);
   card.onclick=()=>window.dispatchEvent(new CustomEvent('qingke-show-course',{detail:c.id}));cards.append(card);
  }
 }
 window.addEventListener('qingke-render',e=>{currentWeek=e.detail.week;render()});render();
 return {showToday(){selectedDay=new Date().getDay()||7;render()}};
}
