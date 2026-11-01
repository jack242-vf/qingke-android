import{validateData}from './core.js';
export function buildNativeSnapshot(data,profile){
 validateData(data);
 return {profileId:profile.id,profileName:profile.name,data:{version:2,settings:{term:data.settings.term,monday:data.settings.monday,weeks:data.settings.weeks,times:[...data.settings.times]},courses:data.courses.map(c=>{const row={id:c.id,name:c.name,room:c.room,teacher:c.teacher,day:c.day,start:c.start,end:c.end,from:c.from,to:c.to,parity:c.parity};if(c.weeks)row.weeks=[...c.weeks];return row})}};
}
export function initNativeTools({getData,getProfile,send,toast}){
 const available=navigator.userAgent.includes('QingkeAndroid/');let last='';
 function sync(){
  if(!available)return false;const data=getData();if(!data)return false;
  try{const snapshot=buildNativeSnapshot(data,getProfile()),serialized=JSON.stringify(snapshot);if(serialized===last)return true;const response=JSON.parse(send({action:'syncSchedule',snapshot})||'null');if(!response?.ok)throw Error();last=serialized;return true}
  catch{last='';toast('课表已保存在应用中，但提醒和桌面同步失败，请重新打开应用重试');return false}
 }
 window.addEventListener('qingke-render',sync);
 const button=document.createElement('button');button.id='nativeToolsBtn';button.textContent='提醒与桌面';button.disabled=!available;button.title=available?'设置上课提醒和桌面小组件':'请在安卓 APK 中使用此功能';
 document.querySelector('footer > div').append(button);
 button.onclick=()=>{if(sync())send({action:'openNativeTools'})};
 sync();
}
