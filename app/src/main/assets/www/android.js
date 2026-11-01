import{initAgenda}from './agenda.js';
import{initWeekNavigation}from './navigation.js';
import{initAppearance}from './appearance.js';
import{initNativeTools}from './native-sync.js';
import{getProfiles,createProfile,renameProfile,activeProfile}from './profile-store.js';
const $=s=>document.querySelector(s);
const send=cmd=>window.prompt('__qingke_native__',JSON.stringify(cmd));
const message=text=>{const e=$('#toast');e.textContent=text;e.style.display='block';setTimeout(()=>e.style.display='none',4000)};
function currentKey(){const id=localStorage.getItem('qingke-active-profile')||'default';return id==='default'?'qingke-v1':'qingke-profile-'+id}
function profiles(){return getProfiles(localStorage)}
function saveProfiles(list){localStorage.setItem('qingke-profiles',JSON.stringify(list))}
const bar=document.createElement('section');bar.className='android-tools';
bar.innerHTML='<label>当前课表<select id="profileSelect" aria-label="当前课表"></select></label><button id="newProfile" aria-label="新建课表">＋</button><button id="renameProfile" aria-label="重命名当前课表">改名</button><button id="deleteProfile" aria-label="删除当前课表">删除</button><button id="themeBtn" aria-label="切换主题">◐</button>';
$('header').after(bar);
let list=profiles(),active=localStorage.getItem('qingke-active-profile')||'default';
for(const p of list){const option=document.createElement('option');option.value=p.id;option.textContent=p.name;$('#profileSelect').append(option)}
$('#profileSelect').value=active;
$('#profileSelect').onchange=e=>{try{localStorage.setItem('qingke-active-profile',e.target.value);location.reload()}catch{message('无法切换，存储不可用')}};
$('#newProfile').onclick=()=>{if(list.length>=10){message('最多保存 10 份课表');return}const name=prompt('新课表名称（最多 20 字）','新学期');if(!name?.trim())return;if(name.trim().length>20){message('课表名称最多 20 字');return}try{createProfile(localStorage,name);location.reload()}catch{message('创建失败，请检查设备空间')}};
$('#deleteProfile').onclick=()=>{if(list.length===1){message('至少保留一份课表');return}if(!confirm('删除当前课表？请先导出备份。'))return;try{saveProfiles(list.filter(p=>p.id!==active));localStorage.removeItem(currentKey());localStorage.setItem('qingke-active-profile',list.find(p=>p.id!==active).id);location.reload()}catch{message('删除失败')}};
$('#renameProfile').onclick=()=>{const name=prompt('修改课表名称',activeProfile(localStorage).name);if(name===null)return;try{renameProfile(localStorage,active,name);location.reload()}catch(err){message(err.message)}};
const themeKey='qingke-theme-'+activeProfile(localStorage).id;
function applyTheme(){document.documentElement.dataset.theme=localStorage.getItem(themeKey)||localStorage.getItem('qingke-theme')||'light'}
applyTheme();$('#themeBtn').onclick=()=>{try{localStorage.setItem(themeKey,document.documentElement.dataset.theme==='dark'?'light':'dark');applyTheme()}catch{message('主题保存失败')}};
// Capture before the normal browser handlers: these actions use Android UI.
document.addEventListener('click',e=>{
  if(e.target.closest('#exportBtn')){e.preventDefault();e.stopImmediatePropagation();try{const raw=localStorage.getItem(currentKey());if(!raw){message('请先添加或导入课程');return}send({action:'exportBackup',data:JSON.parse(raw)})}catch{message('导出失败，请重试')}}
  if(e.target.closest('#installBtn')){e.preventDefault();e.stopImmediatePropagation();message('轻课 Android 体验版 0.4.1 · 数据保存在本机')}
  if(e.target.closest('#schoolBtn')){e.preventDefault();e.stopImmediatePropagation();send({action:'openSchool'})}
},true);
$('#installBtn').textContent='关于轻课';
let snapshot=null;window.addEventListener('qingke-render',e=>snapshot=e.detail.data);const agenda=initAgenda(()=>snapshot);initWeekNavigation(()=>snapshot);initNativeTools({getData:()=>snapshot,getProfile:()=>activeProfile(localStorage),send,toast:message});initAppearance();window.dispatchEvent(new Event('qingke-request-render'));$('#todayBtn').addEventListener('click',()=>agenda.showToday());
const schoolHelp=document.createElement('button');schoolHelp.textContent='文件 / 文本导入';schoolHelp.className='alternate-import';schoolHelp.onclick=()=>{$('#schoolDialog').classList.remove('native-result');$('#schoolDialog').showModal()};$('.school-entry').after(schoolHelp);
for(const el of [$('#schoolDialog > p'),$('#schoolDialog .import-steps'),$('#schoolDialog details'),$('#schoolFile').closest('label'),$('#schoolText').closest('label'),$('#parseSchool')])el?.classList.add('import-source');
window.addEventListener('qingke-school-import',e=>{
  $('#schoolDialog').classList.add('native-result');$('#schoolDialog').showModal();$('#schoolText').value=JSON.stringify(e.detail);$('#schoolText').dispatchEvent(new Event('input',{bubbles:true}));$('#parseSchool').click();
  $('#schoolPreview').scrollIntoView({block:'start'});message('提取成功，请核对学期和开学日期后确认导入');
});
