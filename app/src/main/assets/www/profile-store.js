export const defaultProfiles=[{id:'default',name:'我的课表'}];
export function getProfiles(storage){
  const raw=storage.getItem('qingke-profiles');if(!raw)return defaultProfiles.map(x=>({...x}));
  const list=JSON.parse(raw);
  if(!Array.isArray(list)||!list.length||list.length>10||list.some(p=>!p||typeof p.id!=='string'||!p.id||typeof p.name!=='string'||!p.name.trim()||p.name.length>20)||new Set(list.map(p=>p.id)).size!==list.length)throw Error('课表列表损坏，请先保留备份');
  return list;
}
export function profileKey(id){return id==='default'?'qingke-v1':'qingke-profile-'+id}
export function activeProfile(storage){const list=getProfiles(storage),id=storage.getItem('qingke-active-profile')||'default';return list.find(p=>p.id===id)||list[0]}
export function createProfile(storage,name,data=null,id=crypto.randomUUID()){
  name=name.trim();const list=getProfiles(storage);
  if(!name||name.length>20)throw Error('课表名称应为 1–20 字');if(list.length>=10)throw Error('最多保存 10 份课表');if(list.some(p=>p.id===id))throw Error('课表标识重复');
  const oldList=storage.getItem('qingke-profiles'),oldActive=storage.getItem('qingke-active-profile');
  try{
    if(data)storage.setItem(profileKey(id),JSON.stringify(data));
    storage.setItem('qingke-profiles',JSON.stringify([...list,{id,name}]));storage.setItem('qingke-active-profile',id);
  }catch(error){
    try{if(oldList===null)storage.removeItem('qingke-profiles');else storage.setItem('qingke-profiles',oldList);if(oldActive===null)storage.removeItem('qingke-active-profile');else storage.setItem('qingke-active-profile',oldActive);storage.removeItem(profileKey(id))}catch{}
    throw Error('创建失败，请检查设备空间；原课表未被替换');
  }
  return id;
}
export function renameProfile(storage,id,name){name=name.trim();if(!name||name.length>20)throw Error('课表名称应为 1–20 字');const list=getProfiles(storage);if(!list.some(p=>p.id===id))throw Error('课表不存在');storage.setItem('qingke-profiles',JSON.stringify(list.map(p=>p.id===id?{...p,name}:p)))}
