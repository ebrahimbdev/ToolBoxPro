package com.toolbox.pro.fileshare.server

internal val panelHtml = """
<!DOCTYPE html>
<html lang="en" dir="ltr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>ToolBox Pro · File Share</title>
<style>
:root{
--bg0:#0B0F1E;--bg1:#16213E;
--card:rgba(255,255,255,.055);--line:rgba(255,255,255,.10);
--txt:#ECECF1;--muted:#8B90A6;
--acc1:#6C63FF;--acc2:#9C27B0;
--ok:#3DDC97;--err:#FF6B6B;
--r:16px;
}
*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
html{font-size:16px}
body{
font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Vazirmatn","Noto Naskh Arabic",Tahoma,sans-serif;
color:var(--txt);
background:radial-gradient(1100px 520px at 8% -12%,rgba(108,99,255,.26),transparent 60%),
radial-gradient(900px 480px at 112% 8%,rgba(156,39,176,.18),transparent 55%),
linear-gradient(165deg,var(--bg0),var(--bg1));
min-height:100vh;min-height:100dvh;
padding:env(safe-area-inset-top) env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left);
}
.wrap{max-width:860px;margin:0 auto;padding:20px 14px 44px}
header{display:flex;align-items:center;gap:12px;margin-bottom:16px}
.logo{width:46px;height:46px;flex:0 0 46px;border-radius:15px;background:linear-gradient(135deg,var(--acc1),var(--acc2));display:grid;place-items:center;box-shadow:0 10px 26px rgba(108,99,255,.38)}
.logo svg{width:24px;height:24px}
.ttl{min-width:0;flex:1}
h1{font-size:1.22rem;letter-spacing:.2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.sub{color:var(--muted);font-size:.78rem;margin-top:2px}
.pill{display:flex;align-items:center;gap:7px;padding:7px 12px;border-radius:999px;background:var(--card);border:1px solid var(--line);font-size:.76rem;color:var(--muted);white-space:nowrap}
.dot{width:8px;height:8px;flex:0 0 8px;border-radius:50%;background:var(--err)}
.dot.on{background:var(--ok);animation:pulse 2.2s infinite}
@keyframes pulse{0%{box-shadow:0 0 0 0 rgba(61,220,151,.45)}70%{box-shadow:0 0 0 7px rgba(61,220,151,0)}100%{box-shadow:0 0 0 0 rgba(61,220,151,0)}}
.card{background:var(--card);border:1px solid var(--line);border-radius:var(--r);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);padding:16px;margin-bottom:14px}
.card h2{font-size:.95rem;margin-bottom:12px;display:flex;align-items:center;gap:9px}
.card h2 svg{width:18px;height:18px;color:var(--acc1)}
.count{margin-inline-start:auto;font-size:.72rem;color:#A9A4FF;background:rgba(108,99,255,.16);border:1px solid rgba(108,99,255,.4);padding:3px 10px;border-radius:999px}
.off{display:none;background:rgba(255,107,107,.12);border:1px solid rgba(255,107,107,.45);color:#FFB4B4;border-radius:12px;padding:11px 14px;font-size:.82rem;margin-bottom:14px}
.off.show{display:block}
.drop{border:1.5px dashed rgba(255,255,255,.24);border-radius:14px;padding:26px 14px;text-align:center;cursor:pointer;transition:border-color .2s,background .2s;background:rgba(255,255,255,.02)}
.drop:hover,.drop.over{border-color:var(--acc1);background:rgba(108,99,255,.09)}
.drop svg{width:36px;height:36px;color:#A9A4FF;margin-bottom:8px}
.drop b{display:block;font-size:.92rem;margin-bottom:5px}
.drop span{font-size:.75rem;color:var(--muted);line-height:1.5}
.prog{display:none;flex-direction:column;gap:10px;margin-top:12px}
.prog.show{display:flex}
.prow{background:rgba(0,0,0,.28);border:1px solid var(--line);border-radius:12px;padding:10px 12px}
.prow .pt{display:flex;justify-content:space-between;gap:10px;font-size:.8rem;margin-bottom:7px}
.prow .pname{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0}
.pct{color:#A9A4FF;flex:0 0 auto}
.bar{height:6px;border-radius:99px;background:rgba(255,255,255,.10);overflow:hidden}
.bar i{display:block;height:100%;width:0%;border-radius:99px;background:linear-gradient(90deg,var(--acc1),var(--acc2));transition:width .18s}
.files{display:flex;flex-direction:column;gap:10px}
.file{display:flex;align-items:center;gap:12px;background:rgba(255,255,255,.03);border:1px solid var(--line);border-radius:14px;padding:11px 12px;transition:background .18s,transform .18s}
.file:hover{background:rgba(255,255,255,.075);transform:translateY(-1px)}
.ficon{width:42px;height:42px;flex:0 0 42px;border-radius:12px;display:grid;place-items:center;font-size:.64rem;font-weight:800;letter-spacing:.4px;color:#fff;text-transform:uppercase;text-shadow:0 1px 2px rgba(0,0,0,.25)}
.g-img{background:linear-gradient(135deg,#F57C00,#FFB74D)}
.g-pdf{background:linear-gradient(135deg,#E53935,#FF7043)}
.g-zip{background:linear-gradient(135deg,#00838F,#4DD0E1)}
.g-aud{background:linear-gradient(135deg,#8E24AA,#CE93D8)}
.g-vid{background:linear-gradient(135deg,#D81B60,#F48FB1)}
.g-doc{background:linear-gradient(135deg,#1565C0,#64B5F6)}
.g-xls{background:linear-gradient(135deg,#2E7D32,#81C784)}
.g-ppt{background:linear-gradient(135deg,#EF6C00,#FFB74D)}
.g-apk{background:linear-gradient(135deg,#00C853,#69F0AE)}
.g-code{background:linear-gradient(135deg,#5C6BC0,#9FA8DA)}
.g-txt{background:linear-gradient(135deg,#455A64,#90A4AE)}
.g-def{background:linear-gradient(135deg,#546E7A,#B0BEC5)}
.fmain{flex:1;min-width:0}
.fname{font-size:.89rem;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.fmeta{font-size:.73rem;color:var(--muted);display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin-top:4px}
.tag{font-size:.65rem;padding:2px 8px;border-radius:99px;border:1px solid var(--line);color:var(--muted);white-space:nowrap}
.tag.up{color:var(--ok);border-color:rgba(61,220,151,.45);background:rgba(61,220,151,.10)}
.dl{display:inline-flex;align-items:center;justify-content:center;gap:6px;min-width:44px;min-height:44px;padding:0 14px;border-radius:12px;text-decoration:none;font-size:.82rem;font-weight:600;color:#fff;background:linear-gradient(135deg,var(--acc1),var(--acc2));transition:filter .18s,transform .18s,box-shadow .18s;white-space:nowrap}
.dl:hover{filter:brightness(1.13);box-shadow:0 6px 18px rgba(108,99,255,.4)}
.dl:active{transform:scale(.96)}
.empty{text-align:center;padding:26px 8px;color:var(--muted);font-size:.85rem}
.empty svg{width:42px;height:42px;opacity:.45;margin-bottom:8px;color:#A9A4FF}
.foot{text-align:center;color:var(--muted);font-size:.72rem;margin-top:12px}
footer{text-align:center;color:var(--muted);font-size:.72rem;margin-top:4px;line-height:1.7}
.toast{position:fixed;left:50%;bottom:calc(20px + env(safe-area-inset-bottom));transform:translate(-50%,90px);background:#1D2440;border:1px solid var(--line);color:var(--txt);padding:11px 18px;border-radius:12px;font-size:.85rem;opacity:0;pointer-events:none;transition:transform .3s,opacity .3s;z-index:60;box-shadow:0 12px 32px rgba(0,0,0,.55);max-width:90vw;text-align:center}
.toast.show{transform:translate(-50%,0);opacity:1}
.toast.err{border-color:rgba(255,107,107,.6);color:#FFC9C9}
:focus-visible{outline:2px solid var(--acc1);outline-offset:2px}
@media(min-width:640px){.wrap{padding:30px 24px 52px}.card{padding:20px}.drop{padding:34px 18px}}
@media(max-width:430px){.dl span{display:none}.dl{padding:0;width:44px}.fname{font-size:.85rem}}
@media(prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}}
</style>
</head>
<body>
<div class="wrap">
<header>
<div class="logo"><svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 13.5 6.8 4"/><path d="m15.4 6.5-6.8 4"/></svg></div>
<div class="ttl">
<h1>ToolBox Pro</h1>
<div class="sub" data-i18n="subtitle">Local file sharing</div>
</div>
<div class="pill"><i class="dot" id="dot"></i><span id="statusTxt" data-i18n="connecting">Connecting…</span></div>
</header>

<div class="off" id="off" data-i18n="offline">Connection lost — retrying…</div>

<section class="card">
<h2><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 18a4.5 4.5 0 0 1-.4-9A6 6 0 0 1 18 9.6a3.8 3.8 0 0 1-.5 8.4"/><path d="M12 21v-8"/><path d="m9 16 3-3 3 3"/></svg><span data-i18n="uploadTitle">Upload to phone</span></h2>
<div class="drop" id="drop" role="button" tabindex="0" aria-label="Upload files">
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 16V6"/><path d="m8.5 9.5 3.5-3.5 3.5 3.5"/><path d="M4.5 15a4 4 0 0 1 .6-7.9A6.2 6.2 0 0 1 17 7.7a3.9 3.9 0 0 1 2.4 7.2"/></svg>
<b data-i18n="dropHere">Drop files here or tap to select</b>
<span data-i18n="dropHint">Saved on the host phone and visible to everyone</span>
</div>
<input type="file" id="picker" multiple hidden>
<div class="prog" id="prog"></div>
</section>

<section class="card">
<h2><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg><span data-i18n="filesTitle">Shared files</span><span class="count" id="count">0</span></h2>
<div class="files" id="list"></div>
<div class="empty" id="empty">
<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8"/><path d="M2 5h20v3H2z"/><path d="M10 12h4"/></svg>
<div data-i18n="noFiles">No files shared yet</div>
</div>
<div class="foot" data-i18n="autoRefresh">List refreshes automatically</div>
</section>

<footer data-i18n="footer">ToolBox Pro · Share on your local network</footer>
</div>

<div class="toast" id="toast"></div>

<script>
var I18N={
en:{subtitle:"Local file sharing",connecting:"Connecting…",offline:"Connection lost — retrying…",uploadTitle:"Upload to phone",dropHere:"Drop files here or tap to select",dropHint:"Saved on the host phone and visible to everyone",filesTitle:"Shared files",noFiles:"No files shared yet",autoRefresh:"List refreshes automatically",footer:"ToolBox Pro · Share on your local network",filesWord:"file(s)",download:"Download",hostTag:"From host",uploadedTag:"Uploaded",justNow:"just now",minAgo:"min ago",hourAgo:"hr ago",upDone:"Uploaded:",upFail:"Upload failed"},
fa:{subtitle:"اشتراک فایل محلی",connecting:"در حال اتصال…",offline:"اتصال قطع شد — در حال تلاش مجدد…",uploadTitle:"آپلود روی گوشی",dropHere:"فایل‌ها را اینجا رها کنید یا بزنید تا انتخاب شود",dropHint:"روی گوشی میزبان ذخیره و برای همه نمایش داده می‌شود",filesTitle:"فایل‌های اشتراکی",noFiles:"هنوز فایلی اشتراک‌گذاری نشده است",autoRefresh:"فهرست به‌صورت خودکار تازه می‌شود",footer:"ToolBox Pro · اشتراک‌گذاری در شبکه محلی",filesWord:"فایل",download:"دانلود",hostTag:"از میزبان",uploadedTag:"آپلودشده",justNow:"همین حالا",minAgo:"دقیقه پیش",hourAgo:"ساعت پیش",upDone:"آپلود شد:",upFail:"آپلود ناموفق بود"},
ar:{subtitle:"مشاركة الملفات المحلية",connecting:"جارٍ الاتصال…",offline:"انقطع الاتصال — جارٍ إعادة المحاولة…",uploadTitle:"الرفع إلى الهاتف",dropHere:"أسقط الملفات هنا أو انقر للاختيار",dropHint:"تُحفظ على هاتف المضيف وتظهر للجميع",filesTitle:"الملفات المشتركة",noFiles:"لا توجد ملفات مشتركة بعد",autoRefresh:"تتحدث القائمة تلقائيًا",footer:"ToolBox Pro · مشاركة في الشبكة المحلية",filesWord:"ملف",download:"تنزيل",hostTag:"من المضيف",uploadedTag:"مرفوع",justNow:"الآن",minAgo:"دقيقة مضت",hourAgo:"ساعة مضت",upDone:"تم الرفع:",upFail:"فشل الرفع"},
tr:{subtitle:"Yerel dosya paylaşımı",connecting:"Bağlanıyor…",offline:"Bağlantı kesildi — yeniden deneniyor…",uploadTitle:"Telefona yükle",dropHere:"Dosyaları buraya bırakın veya seçmek için dokunun",dropHint:"Ana telefona kaydedilir ve herkese görünür",filesTitle:"Paylaşılan dosyalar",noFiles:"Henüz dosya paylaşılmadı",autoRefresh:"Liste otomatik yenilenir",footer:"ToolBox Pro · Yerel ağda paylaş",filesWord:"dosya",download:"İndir",hostTag:"Ana makineden",uploadedTag:"Yüklenen",justNow:"az önce",minAgo:"dk önce",hourAgo:"sa önce",upDone:"Yüklendi:",upFail:"Yükleme başarısız"}
};
var lang="en";var T=I18N.en;
(function(){
var n=(navigator.language||"en").toLowerCase();
if(n.indexOf("fa")===0){lang="fa";}else if(n.indexOf("ar")===0){lang="ar";}else if(n.indexOf("tr")===0){lang="tr";}
T=I18N[lang];
var d=document.documentElement;
d.lang=lang;
if(lang==="fa"||lang==="ar"){d.dir="rtl";}
document.title="ToolBox Pro · "+T.subtitle;
var els=document.querySelectorAll("[data-i18n]");
for(var i=0;i<els.length;i++){var k=els[i].getAttribute("data-i18n");if(T[k]){els[i].textContent=T[k];}}
})();

function esc(s){return String(s).replace(/[&<>"']/g,function(c){return{"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c];});}
function fmtSize(b){if(b<1024)return b+" B";if(b<1048576)return (b/1024).toFixed(1)+" KB";if(b<1073741824)return (b/1048576).toFixed(1)+" MB";return (b/1073741824).toFixed(2)+" GB";}
function fmtTime(ts){if(!ts)return "";var d=Math.floor((Date.now()-ts)/1000);if(d<60)return T.justNow;if(d<3600)return Math.floor(d/60)+" "+T.minAgo;if(d<86400)return Math.floor(d/3600)+" "+T.hourAgo;var x=new Date(ts);return x.getDate()+"/"+(x.getMonth()+1)+"/"+x.getFullYear();}
function typeInfo(name){
var ext="";var i=name.lastIndexOf(".");
if(i>0&&i<name.length-1){ext=name.slice(i+1).toLowerCase();}
var groups={img:["png","jpg","jpeg","gif","webp","svg","bmp","heic","avif"],zip:["zip","rar","7z","tar","gz","bz2","xz"],aud:["mp3","wav","ogg","m4a","flac","aac"],vid:["mp4","mkv","mov","avi","webm","m4v"],doc:["doc","docx","odt","rtf"],xls:["xls","xlsx","csv","ods"],ppt:["ppt","pptx","odp"],code:["js","ts","jsx","tsx","py","java","kt","kts","c","cpp","h","cs","go","rs","php","html","css","json","xml","yml","yaml","sh","sql","md"]};
var labels={img:"IMG",zip:"ZIP",aud:"AUD",vid:"VID",doc:"DOC",xls:"XLS",ppt:"PPT",code:"CODE"};
if(ext==="pdf"){return{t:"PDF",g:"g-pdf"};}
if(ext==="apk"){return{t:"APK",g:"g-apk"};}
if(ext==="txt"||ext==="log"){return{t:"TXT",g:"g-txt"};}
for(var k in groups){if(groups[k].indexOf(ext)>=0){return{t:labels[k],g:"g-"+k};}}
return{t:ext?ext.slice(0,4).toUpperCase():"FILE",g:"g-def"};
}

var lastJson="";var fails=0;
function failConn(){
fails++;
if(fails>=2){
document.getElementById("off").className="off show";
document.getElementById("dot").className="dot";
}
}
function loadFiles(){
var x=new XMLHttpRequest();
x.open("GET","/api/files?t="+Date.now(),true);
x.timeout=6000;
x.onload=function(){
if(x.status===200){
fails=0;
document.getElementById("off").className="off";
document.getElementById("dot").className="dot on";
if(x.responseText===lastJson){return;}
lastJson=x.responseText;
try{render(JSON.parse(x.responseText));}catch(e){failConn();}
}else{failConn();}
};
x.onerror=failConn;x.ontimeout=failConn;
x.send();
}
function render(f){
var list=document.getElementById("list");
var empty=document.getElementById("empty");
document.getElementById("count").textContent=f.length;
document.getElementById("statusTxt").textContent=f.length+" "+T.filesWord;
if(!f.length){list.innerHTML="";empty.style.display="block";return;}
empty.style.display="none";
var dlSvg='<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v11"/><path d="m7.5 11 4.5 4.5 4.5-4.5"/><path d="M5 20h14"/></svg>';
var html="";
for(var i=0;i<f.length;i++){
var x=f[i];
var ti=typeInfo(x.name||"");
var time=fmtTime(x.uploadedAt);
var meta=fmtSize(x.size||0)+(time?" · "+time:"");
var tag=x.uploaded?'<span class="tag up">'+esc(T.uploadedTag)+'</span>':'<span class="tag">'+esc(T.hostTag)+'</span>';
html+='<div class="file">'+
'<div class="ficon '+ti.g+'">'+esc(ti.t)+'</div>'+
'<div class="fmain"><div class="fname" dir="auto">'+esc(x.name||"")+'</div>'+
'<div class="fmeta"><span>'+esc(meta)+'</span>'+tag+'</div></div>'+
'<a class="dl" href="/download/'+encodeURIComponent(x.id)+'?t='+Date.now()+'" download aria-label="'+esc(T.download)+'">'+dlSvg+'<span>'+esc(T.download)+'</span></a>'+
'</div>';
}
list.innerHTML=html;
}

var toastTimer=null;
function toast(msg,isErr){
var el=document.getElementById("toast");
el.textContent=msg;
el.className="toast show"+(isErr?" err":"");
if(toastTimer){clearTimeout(toastTimer);}
toastTimer=setTimeout(function(){el.className="toast"+(isErr?" err":"");},2800);
}

var drop=document.getElementById("drop");
var picker=document.getElementById("picker");
var prog=document.getElementById("prog");
drop.addEventListener("click",function(){picker.click();});
drop.addEventListener("keydown",function(e){if(e.key==="Enter"||e.key===" "){e.preventDefault();picker.click();}});
picker.addEventListener("change",function(){queue(picker.files);picker.value="";});
function hi(on){if(on){drop.classList.add("over");}else{drop.classList.remove("over");}}
["dragenter","dragover"].forEach(function(ev){drop.addEventListener(ev,function(e){e.preventDefault();e.stopPropagation();hi(true);});});
["dragleave","drop"].forEach(function(ev){drop.addEventListener(ev,function(e){e.preventDefault();e.stopPropagation();hi(false);});});
drop.addEventListener("drop",function(e){if(e.dataTransfer&&e.dataTransfer.files&&e.dataTransfer.files.length){queue(e.dataTransfer.files);}});

var pending=[];var working=false;
function queue(files){
for(var i=0;i<files.length;i++){pending.push(files[i]);}
if(!working){next();}
}
function next(){
if(!pending.length){working=false;return;}
working=true;
uploadOne(pending.shift(),function(){next();});
}
function uploadOne(file,done){
prog.className="prog show";
var row=document.createElement("div");
row.className="prow";
row.innerHTML='<div class="pt"><span class="pname" dir="auto"></span><span class="pct">0%</span></div><div class="bar"><i></i></div>';
row.querySelector(".pname").textContent=file.name;
prog.appendChild(row);
var bar=row.querySelector("i");
var pct=row.querySelector(".pct");
function finish(){row.remove();if(!prog.children.length){prog.className="prog";}}
var x=new XMLHttpRequest();
x.open("POST","/upload?t="+Date.now(),true);
x.upload.onprogress=function(e){if(e.lengthComputable){var p=Math.round(e.loaded*100/e.total);bar.style.width=p+"%";pct.textContent=p+"%";}};
x.onload=function(){
if(x.status===200){
bar.style.width="100%";pct.textContent="100%";
toast(T.upDone+" "+file.name,false);
lastJson="";
loadFiles();
}else{
toast(T.upFail+" ("+(x.statusText||x.status)+")",true);
}
finish();done();
};
x.onerror=function(){toast(T.upFail,true);finish();done();};
var fd=new FormData();
fd.append("file",file,file.name);
x.send(fd);
}

loadFiles();
setInterval(loadFiles,2000);
</script>
</body>
</html>
""".trimIndent()
