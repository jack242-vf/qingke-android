import{test}from'node:test';import assert from'node:assert/strict';
import{dayCourses,courseState}from'../app/src/main/assets/www/agenda.js';
import{getProfiles,createProfile,renameProfile,activeProfile,profileKey}from'../app/src/main/assets/www/profile-store.js';
import{defaultTimes}from'../app/src/main/assets/www/core.js';
function storage(){const map=new Map();return {map,getItem:k=>map.get(k)??null,setItem:(k,v)=>map.set(k,v),removeItem:k=>map.delete(k)}}
const settings={monday:'2026-08-31',times:defaultTimes};
const course={id:'a',name:'课程A',day:1,start:1,end:2,from:1,to:20,parity:'odd'};
test('每日课程按所选周次、星期筛选并排序',()=>{const data={courses:[{...course,id:'b',start:5,end:6},course,{...course,day:2}]};assert.deepEqual(dayCourses(data,1,1).map(c=>c.id),['a','b']);assert.equal(dayCourses(data,2,1).length,0);assert.equal(dayCourses(data,21,1).length,0)});
test('课程进行状态以实际日期和作息计算',()=>{assert.equal(courseState(course,settings,1,1,new Date(2026,7,31,7,59)),'未开始');assert.equal(courseState(course,settings,1,1,new Date(2026,7,31,8,0)),'进行中');assert.equal(courseState(course,settings,1,1,new Date(2026,7,31,9,40)),'已结束');assert.equal(courseState(course,settings,2,1,new Date(2026,7,31,10,0)),'未开始')});
test('新建导入保留当前课表且切换到新课表',()=>{const s=storage();s.setItem('qingke-v1','old timetable');createProfile(s,'秋季',{version:1,courses:[]},'new');assert.equal(s.getItem('qingke-v1'),'old timetable');assert.equal(activeProfile(s).id,'new');assert.equal(JSON.parse(s.getItem(profileKey('new'))).version,1);renameProfile(s,'new','新学期');assert.equal(activeProfile(s).name,'新学期')});
test('存储失败时撤销新建，保留原课表与选中状态',()=>{const s=storage();s.setItem('qingke-v1','old timetable');let failed=false;const set=s.setItem;s.setItem=(k,v)=>{if(k==='qingke-active-profile'&&!failed){failed=true;throw Error('quota')}return set(k,v)};assert.throws(()=>createProfile(s,'失败',{courses:[]},'bad'));assert.equal(s.getItem('qingke-v1'),'old timetable');assert.equal(s.getItem(profileKey('bad')),null);assert.equal(activeProfile(s).id,'default')});
test('无效名称、重复标识与超过十份课表被拒绝',()=>{const s=storage();assert.throws(()=>createProfile(s,'',null,'x'));assert.throws(()=>createProfile(s,'x',null,'default'));for(let i=1;i<10;i++)createProfile(s,'课表'+i,null,'p'+i);assert.equal(getProfiles(s).length,10);assert.throws(()=>createProfile(s,'超出',null,'p10'));assert.throws(()=>renameProfile(s,'p1',' '.repeat(3)))});
