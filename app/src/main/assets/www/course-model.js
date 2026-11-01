import{active,validateData}from './core.js';
export function courseWeeks(c){return Array.from({length:30},(_,i)=>i+1).filter(w=>active(c,w))}
export function weekText(c){const weeks=courseWeeks(c),ranges=[];for(const w of weeks){const r=ranges.at(-1);if(r&&r[1]+1===w)r[1]=w;else ranges.push([w,w])}return ranges.map(([a,b])=>a===b?String(a):a+'–'+b).join('、')+'周'}
export function normalizeSlot(c,weeks){
 const selected=[...new Set(weeks)].sort((a,b)=>a-b);
 if(!selected.length)throw Error('每个时段至少选择一周');
 return {...c,weeks:selected,from:selected[0],to:selected.at(-1),parity:'all'};
}
export function groupCourses(data,c){return c.groupId?data.courses.filter(x=>x.groupId===c.groupId):data.courses.filter(x=>x.id===c.id)}
export function replaceGroup(data,original,slots){
 if(!slots.length)throw Error('至少保留一个上课时段');
 const ids=new Set(original?groupCourses(data,original).map(c=>c.id):[]);
 return validateData({...data,version:2,courses:[...data.courses.filter(c=>!ids.has(c.id)),...slots]});
}
export function cancelWeek(data,id,week){
 const c=data.courses.find(c=>c.id===id);if(!c||!active(c,week))throw Error('这门课在所选周没有安排');
 const weeks=courseWeeks(c).filter(w=>w!==week);
 return validateData({...data,version:2,courses:data.courses.flatMap(x=>x.id!==id?[x]:weeks.length?[normalizeSlot(x,weeks)]:[])});
}
export function moveWeek(data,id,week,changes,newId){
 const c=data.courses.find(c=>c.id===id);if(!c)throw Error('课程不存在');
 const next=cancelWeek(data,id,week),groupId=c.groupId||'group-'+c.id;
 const moved=normalizeSlot({...c,...changes,id:newId,groupId},[week]);
 return validateData({...next,courses:[...next.courses.map(x=>x.id===id?{...x,groupId}:x),moved]});
}
