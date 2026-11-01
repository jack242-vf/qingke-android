(() => {
  if(location.hostname!=='jwctsp.kust.edu.cn'||!location.pathname.startsWith('/integration/for-std/course-table'))return {error:'请先打开学校「我的课表」。'};
  const table=document.querySelector('#lessons');
  if(!table){
    const start=[...document.querySelectorAll('body *')].filter(e=>e.children.length===0).map(e=>e.textContent.trim()).find(s=>/^20\d{2}-\d{2}-\d{2}$/.test(s));
    if(start)sessionStorage.setItem('qingke-term-monday',start);
    const semester=document.body.innerText.match(/\b20\d{2}-20\d{2}-[12]\b/)?.[0];
    if(semester)sessionStorage.setItem('qingke-term-name',semester);
    const units=[...(document.querySelector('#table-content .weekday')?.querySelectorAll('.blank-unit')||[])];
    if(units.length===13){const f=s=>s.padStart(4,'0').replace(/(\d{2})(\d{2})/,'$1:$2');sessionStorage.setItem('qingke-term-times',JSON.stringify(units.map(e=>f(e.getAttribute('start-time'))+'-'+f(e.getAttribute('end-time')))))}
    const btn=document.querySelector('#totalCourses');
    if(btn){const previous=Number(sessionStorage.getItem('qingke-list-opening')||0);if(Date.now()-previous>30000){sessionStorage.setItem('qingke-list-opening',String(Date.now()));btn.click()}return {pending:true,error:'正在打开全部课程并提取，请稍候…'}}
    return {pending:true,error:'正在等待课表页面加载…'};
  }
  const term=document.body.innerText.match(/\b20\d{2}-20\d{2}-[12]\b/)?.[0]||'';
  const rows=[...table.querySelectorAll('tr.lessonInfo')].map(r=>({name:r.querySelector('.courseInfo .showSchedules')?.textContent.trim()||'',schedule:r.cells[2]?.innerText.trim()||''}));
  if(!rows.length||rows.some(r=>!r.name))return {pending:true,error:'正在等待完整课程列表…'};
  const result={format:'qingke-kmust',version:1,term,rows};
  if(sessionStorage.getItem('qingke-term-name')===term){const monday=sessionStorage.getItem('qingke-term-monday');if(monday)result.monday=monday;try{const times=JSON.parse(sessionStorage.getItem('qingke-term-times'));if(Array.isArray(times)&&times.length===13)result.times=times}catch{}}
  return result;
})()
