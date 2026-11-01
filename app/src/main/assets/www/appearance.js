import{activeProfile}from './profile-store.js';
export function initAppearance(){
 const $=s=>document.querySelector(s),key='qingke-appearance-'+activeProfile(localStorage).id;
 const defaults={font:'normal',height:53,weekend:true};
 let settings=defaults;try{settings={...defaults,...JSON.parse(localStorage.getItem(key)||'{}')}}catch{}
 const dialog=document.createElement('dialog');dialog.id='appearanceDialog';dialog.innerHTML='<form id="appearanceForm"><div class="dialoghead"><h2>课表外观</h2><button type="button" class="close" aria-label="关闭外观设置">×</button></div><p class="muted">此设置只应用于当前课表。</p><label>文字大小<select name="font"><option value="normal">标准</option><option value="large">大字</option></select></label><label>周视图每节课高度<input name="height" type="range" min="44" max="80" step="1"></label><label class="consent"><input name="weekend" type="checkbox">周视图显示周六、周日</label><p class="muted">关闭周末显示后，周末课程仍可在每日列表中查看。</p><button class="primary wide" type="submit">保存外观</button></form>';
 document.body.append(dialog);dialog.querySelector('.close').onclick=()=>dialog.close();
 const trigger=document.createElement('button');trigger.textContent='课表外观';trigger.className='appearance-trigger';$('footer > div').append(trigger);
 const apply=()=>{document.documentElement.dataset.font=settings.font;document.documentElement.dataset.weekend=String(settings.weekend);document.documentElement.style.setProperty('--period-height',settings.height+'px');window.dispatchEvent(new Event('qingke-request-render'))};
 trigger.onclick=()=>{const f=$('#appearanceForm');f.elements.font.value=settings.font;f.elements.height.value=settings.height;f.elements.weekend.checked=settings.weekend;dialog.showModal()};
 $('#appearanceForm').onsubmit=e=>{e.preventDefault();const f=e.target;const next={font:f.elements.font.value,height:Number(f.elements.height.value),weekend:f.elements.weekend.checked};try{localStorage.setItem(key,JSON.stringify(next));settings=next;apply();dialog.close()}catch{alert('外观保存失败，请检查设备空间')}};
 apply();
}
