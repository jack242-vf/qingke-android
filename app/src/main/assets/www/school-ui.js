import{readKmustSource,buildKmustImport,kmustBookmarklet}from './kmust.js';
import{createProfile}from './profile-store.js';
export function initSchoolImport({getData,saveData,onChange,toast}){
  const $=s=>document.querySelector(s);let source=null,signature='';
  const resetPreview=()=>{source=null;signature='';$('#schoolPreview').replaceChildren();$('#applySchool').disabled=true;$('#schoolError').textContent='';$('#schoolConsent').checked=false;if($('#importAsNew'))$('#importAsNew').checked=getData().courses.length>0};
  const open=()=>{resetPreview();$('#schoolDialog').showModal()};
  $('#schoolBtn').onclick=open;
  $('#schoolText').oninput=resetPreview;
  $('#schoolFile').onchange=async e=>{resetPreview();const file=e.target.files[0];if(!file)return;try{if(file.size>1000000)throw Error('文件超过 1 MB');$('#schoolText').value=await file.text();toast('文件已读取，请点击解析预览')}catch(err){$('#schoolError').textContent=err.message}finally{e.target.value=''}};
  const bookmark=kmustBookmarklet();$('#schoolBookmark').href=bookmark;$('#bookmarkCode').value=bookmark;
  $('#copyBookmark').onclick=async()=>{try{await navigator.clipboard.writeText(bookmark);toast('已复制书签代码')}catch{$('#bookmarkCode').focus();$('#bookmarkCode').select();toast('请长按或按 Ctrl+C 复制代码')}};
  $('#parseSchool').onclick=()=>{resetPreview();try{
    const text=$('#schoolText').value;source=readKmustSource(text);signature=text;
    $('#schoolTerm').value=source.term||getData().settings.term;
    $('#schoolMonday').value=source.monday||'';
    $('#schoolWeeks').value=Math.max(20,...source.courses.map(c=>c.to));
    const list=$('#schoolPreview');const summary=document.createElement('strong');summary.textContent=`识别 ${new Set(source.courses.map(c=>c.name)).size} 门课程 · ${source.courses.length} 个排课时段`;list.append(summary);
    const table=document.createElement('table');table.className='import-table';
    for(const c of source.courses){const tr=document.createElement('tr');for(const text of [c.name,`周${'一二三四五六日'[c.day-1]} ${c.start}–${c.end}节`,`${c.from===c.to?c.from:c.from+'–'+c.to}周`,c.room,c.teacher]){const td=document.createElement('td');td.textContent=text;tr.append(td)}table.append(tr)}list.append(table);$('#applySchool').disabled=false;
  }catch(err){source=null;$('#schoolError').textContent=err.message}};
  $('#applySchool').onclick=()=>{try{
    if(!source||signature!==$('#schoolText').value)throw Error('内容已改变，请重新解析预览');
    const incoming=buildKmustImport(source,{term:$('#schoolTerm').value.trim(),monday:$('#schoolMonday').value,weeks:$('#schoolWeeks').value});
    if(!$('#schoolConsent').checked)throw Error('请先勾选确认：已核对预览并同意替换当前课表');
    incoming.courses=incoming.courses.map(c=>({...c,id:crypto.randomUUID()}));
    if($('#importAsNew')?.checked){createProfile(localStorage,$('#schoolTerm').value.trim().slice(0,20),incoming);location.reload();return;}
    if(saveData(incoming)){$('#schoolDialog').close();onChange();toast('昆工课表已导入，可离线查看')}
  }catch(err){$('#schoolError').textContent=err.message}};
}
