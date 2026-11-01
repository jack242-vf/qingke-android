import{test}from'node:test';import assert from'node:assert/strict';
import{buildNativeSnapshot}from'../app/src/main/assets/www/native-sync.js';
import{defaultTimes}from'../app/src/main/assets/www/core.js';
const profile={id:'default',name:'我的课表'},data={version:2,settings:{term:'秋季',monday:'2026-08-31',weeks:20,times:defaultTimes},courses:[{id:'a',name:'高数',room:'A201',teacher:'老师',day:1,start:1,end:2,from:1,to:5,parity:'all',color:0,weeks:[1,3,5],groupId:'group',note:'不应同步到后台副本的课程备注'}]};
test('同步保留不连续周次、课程与作息',()=>{const n=buildNativeSnapshot(data,profile);assert.deepEqual(n.data.courses[0].weeks,[1,3,5]);assert.equal(n.data.settings.times[0],'08:00-08:45');assert.equal(n.profileId,'default')});
test('后台副本不包含备注、外观和分组信息',()=>{const row=buildNativeSnapshot(data,profile).data.courses[0];assert.equal(row.note,undefined);assert.equal(row.color,undefined);assert.equal(row.groupId,undefined)});
test('切换为空课表时仍发送空列表以取消旧提醒',()=>{assert.deepEqual(buildNativeSnapshot({...data,courses:[]},{id:'new',name:'新课表'}).data.courses,[])});
test('原始课表不可被后台副本修改',()=>{const n=buildNativeSnapshot(data,profile);n.data.courses[0].weeks.push(4);n.data.settings.times[0]='changed';assert.deepEqual(data.courses[0].weeks,[1,3,5]);assert.equal(data.settings.times[0],'08:00-08:45')});
