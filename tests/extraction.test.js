import{test}from'node:test';import assert from'node:assert/strict';import vm from'node:vm';import{readFileSync}from'node:fs';
import{readKmustSource}from'../app/src/main/assets/www/kmust.js';
const script=readFileSync(new URL('../app/src/main/assets/native-extract.js',import.meta.url),'utf8');
function run({host='jwctsp.kust.edu.cn',path='/integration/for-std/course-table/info/all-courses',table=null,storage=new Map(),body='2026-2027-1',button=null}={}){
 const document={body:{innerText:body},querySelector:s=>s==='#lessons'?table:s==='#totalCourses'?button:null,querySelectorAll:()=>[]};
 return vm.runInNewContext(script,{location:{hostname:host,pathname:path},document,sessionStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v)}});
}
function table(){return{querySelectorAll:()=>[{querySelector:()=>({textContent:'测试课程'}),cells:[null,null,{innerText:'4~11,17~19周 周二 第六节~第八节 呈贡校区 A101 示例老师'}]}]}}
test('仅从正确学校课表页面提取',()=>{assert.ok(run({host:'fake.test',table:table()}).error);assert.ok(run({path:'/integration/home',table:table()}).error)});
test('提取结果只有课程与学期，可被现有解析器读取',()=>{const result=run({table:table(),body:'用户信息（不导出） 2026-2027-1'});assert.deepEqual(Object.keys(result).sort(),['format','rows','term','version']);const source=readKmustSource(JSON.stringify(result));assert.equal(source.courses.length,2);assert.equal(source.courses[1].from,17)});
test('先打开全部课程，未加载页面不伪报成功',()=>{let clicked=false;const result=run({button:{click:()=>clicked=true}});assert.ok(clicked);assert.ok(result.error);assert.ok(run({table:{querySelectorAll:()=>[]}}).error)});
test('学期变更时不沿用旧学期日期',()=>{const storage=new Map([['qingke-term-name','2025-2026-2'],['qingke-term-monday','2026-03-02']]);const result=run({table:table(),storage});assert.equal(result.monday,undefined)});
test('同一学期提取起始日期',()=>{const storage=new Map([['qingke-term-name','2026-2027-1'],['qingke-term-monday','2026-08-24']]);const result=run({table:table(),storage});assert.equal(result.monday,'2026-08-24')});
test('自动等待时不会重复点击导航按钮',()=>{const storage=new Map();let clicks=0;const options={storage,button:{click:()=>clicks++}};assert.ok(run(options).pending);assert.ok(run(options).pending);assert.equal(clicks,1)});
