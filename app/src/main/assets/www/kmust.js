import {validateCourse,validateData,conflict} from './core.js';

// Observed on 昆工「我的课表」的时间轴。导入前仍允许用户核对。
export const kmustTimes=['08:00-08:45','08:50-09:35','09:50-10:35','10:40-11:25','11:30-12:15','13:30-14:15','14:20-15:05','15:10-15:55','16:10-16:55','17:00-17:45','17:50-18:35','19:30-20:15','20:20-21:05'];
export function chineseNumber(s){
  if(/^\d+$/.test(s))return Number(s);
  const digits={一:1,二:2,三:3,四:4,五:5,六:6,七:7,八:8,九:9};
  if(s==='十')return 10;
  if(/^十[一二三]$/.test(s))return 10+digits[s[1]];
  if(digits[s])return digits[s];
  throw Error('无法识别节次：'+s);
}
export function parseWeeks(raw){
  let text=raw.replace(/\s/g,'').replace(/[，、]/g,',').replace(/[～—–-]/g,'~');
  const odd=/单/.test(text),even=/双/.test(text);
  if(odd&&even)throw Error('周次同时包含单周与双周');
  text=text.replace(/[周单双()（）]/g,'');
  if(!text)throw Error('周次为空');
  const weeks=new Set();
  for(const token of text.split(',')){
    const m=token.match(/^(\d{1,2})(?:~(\d{1,2}))?$/);
    if(!m)throw Error('无法识别周次：'+raw);
    const a=Number(m[1]),b=Number(m[2]||m[1]);
    if(a<1||b>30||a>b)throw Error('周次应在 1–30 之间且起止有序');
    for(let w=a;w<=b;w++)if((!odd||w%2===1)&&(!even||w%2===0))weeks.add(w);
  }
  if(!weeks.size)throw Error('没有符合单双周的课程');
  const sorted=[...weeks].sort((a,b)=>a-b),ranges=[];
  for(const w of sorted){const last=ranges.at(-1);if(last&&w===last.to+1)last.to=w;else ranges.push({from:w,to:w})}
  return ranges;
}
// 仅解析「全部课程」表格的名称和排课文本，不读取学号、成绩或登录信息。
export function parseKmustRows(rows){
  if(!Array.isArray(rows)||!rows.length||rows.length>200)throw Error('请在学校「我的课表 → 全部课程」页面提取数据');
  const result=[];
  for(const [i,row]of rows.entries()){
    if(!row||typeof row.name!=='string'||typeof row.schedule!=='string'||row.schedule.length>30000)throw Error('课程数据格式不正确');
    const name=row.name.trim(),lines=row.schedule.replace(/\u00a0/g,' ').split(/[;；\n]+/).map(s=>s.trim()).filter(Boolean);
    if(!lines.length)throw Error(`「${name}」尚无排课，无法导入；请先核对学校页面`);
    for(const line of lines){
      const m=line.match(/^(.+?周(?:[（(][单双]周?[）)])?)\s+(?:周|星期)([一二三四五六日天])\s+第?([\d一二三四五六七八九十]+)节(?:\s*[~～—–-]\s*第?([\d一二三四五六七八九十]+)节)?\s+(.+)$/);
      if(!m)throw Error(`无法完整解析「${name}」的排课：${line.slice(0,100)}`);
      const parts=m[5].trim().replace(/^\S+校区\s+/,'').match(/^(.*?)\s+(\S+)$/);
      if(!parts)throw Error(`「${name}」的教室/教师格式未识别，请保留原网页中的空格`);
      const day='一二三四五六日'.indexOf(m[2].replace('天','日'))+1;
      for(const range of parseWeeks(m[1])){
        const c={name,room:parts[1].trim(),teacher:parts[2],day,start:chineseNumber(m[3]),end:chineseNumber(m[4]||m[3]),...range,color:i%5,parity:'all'};
        validateCourse(c);
        // 同一时段的共同授课老师合并，避免把同一课误报为冲突。
        const same=result.find(x=>['name','room','day','start','end','from','to'].every(k=>x[k]===c[k]));
        if(same){same.teacher=[...new Set([...same.teacher.split('、'),c.teacher])].join('、');validateCourse(same)}else result.push(c);
      }
    }
  }
  for(let i=0;i<result.length;i++)for(let j=i+1;j<result.length;j++)if(conflict(result[i],result[j]))throw Error(`「${result[i].name}」与「${result[j].name}」存在排课冲突，请先核对学校课表`);
  if(result.length>500)throw Error('排课条目超过 500 条，暂不支持');
  for(const c of result)c.groupId='kmust-'+c.name;
  return result;
}
export function rowsFromText(text){
  // 支持全选复制「全部课程」页面；元信息中课程代码是名称结束边界。
  const lines=text.replace(/\r/g,'').split('\n').map(s=>s.trim()).filter(Boolean),rows=[];
  for(let i=0;i<lines.length-1;i++){
    if(!/^\d{6,}\b/.test(lines[i+1])||!lines[i+1].includes('类'))continue;
    const name=lines[i],schedules=[];
    let j=i+2;
    for(;j<lines.length;j++){
      if(lines[j].startsWith('备注'))break;
      if(/周\s+(?:周|星期)[一二三四五六日天]\s+第/.test(lines[j]))schedules.push(lines[j]);
    }
    rows.push({name,schedule:schedules.join('\n')});i=j;
  }
  if(!rows.length)throw Error('未识别到「全部课程」文本。请使用提取文件，或完整复制该页面文本');
  return rows;
}
export function readKmustSource(text){
  if(typeof text!=='string'||text.length>1000000)throw Error('文件或文本超过 1 MB');
  if(text.trim().startsWith('{')){
    let obj;try{obj=JSON.parse(text)}catch{throw Error('JSON 文件格式不正确')}
    if(obj.format!=='qingke-kmust'||obj.version!==1)throw Error('请使用昆工提取文件；普通轻课备份请用底部「导入」');
    return {term:typeof obj.term==='string'?obj.term:'',monday:typeof obj.monday==='string'?obj.monday:'',times:obj.times||kmustTimes,courses:parseKmustRows(obj.rows)};
  }
  return {term:text.match(/\b20\d{2}-20\d{2}-[12]\b/)?.[0]||'',monday:'',times:kmustTimes,courses:parseKmustRows(rowsFromText(text))};
}
export function buildKmustImport(source,{monday,term,weeks}){
  const settings={monday,term,weeks:Number(weeks),times:source.times};
  return validateData({version:1,settings,courses:source.courses.map((c,i)=>({...c,id:`kmust-${i}`}))});
}

// Self-contained so the bookmarklet uses this exact implementation, no external script/CDN.
export function extractKmustPage(){
  if(location.hostname!=='jwctsp.kust.edu.cn'||!location.pathname.startsWith('/integration/for-std/course-table')){alert('请先打开昆工教务系统「我的课表 → 全部课程」页面');return}
  const table=document.querySelector('#lessons');
  if(!table){alert('请点击「全部课程」，加载完课程列表后再运行本书签');return}
  const rows=[...table.querySelectorAll('tr.lessonInfo')].map(row=>({name:row.querySelector('.courseInfo .showSchedules')?.textContent.trim()||'',schedule:row.cells[2]?.innerText.trim()||''}));
  if(!rows.length||rows.some(r=>!r.name)){alert('未找到完整课程列表，学校页面可能已改变，请先不要导入');return}
  const term=document.body.innerText.match(/\b20\d{2}-20\d{2}-[12]\b/)?.[0]||'';
  const output={format:'qingke-kmust',version:1,term,rows};
  const url=URL.createObjectURL(new Blob([JSON.stringify(output,null,2)],{type:'application/json'}));
  const a=document.createElement('a');a.href=url;a.download='昆工课表-'+(term||'当前学期')+'.json';a.click();
  setTimeout(()=>URL.revokeObjectURL(url),10000);
}
export function kmustBookmarklet(){return 'javascript:('+encodeURIComponent(extractKmustPage.toString())+')();void(0)'}
