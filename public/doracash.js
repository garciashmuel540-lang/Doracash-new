'use strict';
const $ = id => document.getElementById(id);
const key = 'doracash.web.movimientos.v1';
const money = cents => 'C$ ' + (cents / 100).toLocaleString('es-NI', {minimumFractionDigits:2,maximumFractionDigits:2});
const today = () => new Date(Date.now()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,10);
let editing = null;
function entries(){
  const rows = JSON.parse(localStorage.getItem(key) || '[]');
  if (!Array.isArray(rows) || !rows.every(r=>r && Number.isSafeInteger(r.cents) && r.cents>0 && ['income','expense'].includes(r.kind) && typeof r.description==='string' && typeof r.day==='string' && typeof r.id==='string')) throw Error('Datos ilegibles');
  return rows;
}
function reset(){
  editing=null; $('entry').reset(); $('day').value=today(); $('cancel').hidden=true; $('form-title').textContent='Nuevo movimiento';
}
function node(tag, value, cls){
  const el=document.createElement(tag); el.textContent=value; if(cls) el.className=cls; return el;
}
function render(){
  let rows;
  try { rows=entries(); } catch { $('status').textContent='No se pudieron leer tus movimientos. Revisá el almacenamiento del navegador.';return; }
  const total=kind=>rows.filter(r=>r.kind===kind).reduce((s,r)=>s+r.cents,0);
  const income=total('income'),expense=total('expense');
  $('income').textContent=money(income); $('expense').textContent=money(expense); $('balance').textContent=money(income-expense);
  const history=$('history'); history.replaceChildren(); $('download').hidden=!rows.length;
  if(!rows.length) history.append(node('p','Todavía no hay movimientos.','hint'));
  for(const row of [...rows].reverse().sort((a,b)=>b.day.localeCompare(a.day))){
    const item=node('div','','row');
    item.append(node('strong',row.description,'detail'),node('strong',(row.kind==='expense'?'−':'+')+money(row.cents),row.kind),node('span',row.day.split('-').reverse().join('/'),'date'));
    const actions=node('div','','actions'),edit=node('button','Editar','small'),del=node('button','Borrar','small');
    edit.onclick=()=>{ editing=row.id; $('kind').value=row.kind; $('description').value=row.description; $('day').value=row.day; $('amount').value=(row.cents/100).toFixed(2); $('cancel').hidden=false; $('form-title').textContent='Editar movimiento'; $('amount').focus(); };
    del.onclick=()=>{ if(confirm('¿Borrar este movimiento?')) change(list=>list.filter(r=>r.id!==row.id),'Movimiento borrado.'); };
    actions.append(edit,del);item.append(actions);history.append(item);
  }
}
function change(update,message){
  try{const rows=update(entries());localStorage.setItem(key,JSON.stringify(rows));reset();render();$('status').textContent=message;}
  catch{ $('status').textContent='No se pudo guardar el cambio. Revisá el almacenamiento y volvé a intentar.'; }
}
$('entry').onsubmit=e=>{
  e.preventDefault();
  const description=$('description').value.trim(),cents=Math.round(Number($('amount').value)*100);
  if(!description || !Number.isSafeInteger(cents) || cents<=0){$('status').textContent='Ingresá una descripción y un monto válido.';return;}
  const row={id:editing||crypto.randomUUID(),kind:$('kind').value,description,cents,day:$('day').value};
  change(rows=>{if(!editing)return [...rows,row];if(!rows.some(r=>r.id===editing))throw Error('Movimiento eliminado');return rows.map(r=>r.id===editing?row:r);},'Movimiento guardado.');
};
$('download').onclick=()=>{
  const href=URL.createObjectURL(new Blob([JSON.stringify(entries(),null,2)],{type:'application/json'}));
  const link=document.createElement('a');link.href=href;link.download='doracash-'+today()+'.json';link.click();setTimeout(()=>URL.revokeObjectURL(href),1000);
};
$('cancel').onclick=reset;addEventListener('storage',render);reset();render();
