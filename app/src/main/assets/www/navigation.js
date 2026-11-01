import{weekAt,dateOf}from './core.js';
export function initWeekNavigation(getData){
 const $=s=>document.querySelector(s);let current=1;
 const dialog=document.createElement('dialog');dialog.id='weekPicker';dialog.innerHTML='<div class="dialoghead"><h2>快速选择周次</h2><button class="close" aria-label="关闭周次选择">×</button></div><strong id="pickedWeek"></strong><p id="pickedDates" class="muted"></p><input id="weekRange" aria-label="选择周次" type="range" min="1" step="1"><div id="weekChips" class="week-chips"></div><div class="actions"><button id="pickerToday">回到本周</button><button id="applyWeek" class="primary">前往选中周</button></div>';
 document.body.append(dialog);dialog.querySelector('.close').onclick=()=>dialog.close();
 const badge=document.createElement('button');badge.className='week-status';$('.subline').after(badge);badge.onclick=()=>$('#todayBtn').click();
 const setWeek=w=>window.dispatchEvent(new CustomEvent('qingke-set-week',{detail:w}));
 function preview(){const w=Number($('#weekRange').value),s=getData().settings,date=dateOf(s.monday);date.setDate(date.getDate()+(w-1)*7);$('#pickedWeek').textContent=`第 ${w} 周`;$('#pickedDates').textContent=`从 ${date.getMonth()+1}月${date.getDate()}日开始`;for(const b of $('#weekChips').children)b.setAttribute('aria-pressed',String(Number(b.textContent)===w))}
 $('#weekRange').oninput=preview;
 $('#weekBtn').onclick=()=>{const data=getData();if(!data)return;$('#weekRange').max=data.settings.weeks;$('#weekRange').value=current;$('#weekChips').replaceChildren();for(let w=1;w<=data.settings.weeks;w++){const b=document.createElement('button');b.textContent=w;b.onclick=()=>{$('#weekRange').value=w;preview()};$('#weekChips').append(b)}preview();dialog.showModal()};
 $('#applyWeek').onclick=()=>{setWeek(Number($('#weekRange').value));dialog.close()};$('#pickerToday').onclick=()=>{$('#todayBtn').click();dialog.close()};
 window.addEventListener('qingke-render',e=>{current=e.detail.week;const actual=weekAt(e.detail.data.settings.monday);badge.textContent=current===actual?'本周课表 · 左右滑动切换周次':'正在查看非本周 · 点此回到本周';badge.dataset.current=String(current===actual)});
 for(const area of [$('.schedule'),$('.agenda-panel')]){
  let start=null;area.addEventListener('touchstart',e=>{if(e.touches.length!==1||e.target.closest('input,textarea,select')){start=null;return}start={x:e.touches[0].clientX,y:e.touches[0].clientY}},{passive:true});
  area.addEventListener('touchend',e=>{if(!start||!e.changedTouches.length)return;const x=e.changedTouches[0].clientX-start.x,y=e.changedTouches[0].clientY-start.y;start=null;if(Math.abs(x)<75||Math.abs(x)<Math.abs(y)*1.8)return;e.preventDefault();const next=current+(x<0?1:-1);if(next>=1&&next<=getData().settings.weeks)setWeek(next)},{passive:false});
  area.addEventListener('touchcancel',()=>start=null,{passive:true});
 }
}
