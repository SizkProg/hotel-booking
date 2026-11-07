const {spawn}=require('child_process'),fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'..'),children=[],logs=[],results=[];
fs.mkdirSync(path.join(root,'.work'),{recursive:true});
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function wait(url,ok=200){for(let i=0;i<90;i++){try{const r=await fetch(url,{signal:AbortSignal.timeout(2000)});if(r.status===ok)return;}catch{}await sleep(1000);}throw Error('Не дождались '+url);}
function start(m){const file=fs.openSync(path.join(root,'.work',m+'-e2e.log'),'w');logs.push(file);children.push(spawn((process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME,'bin','java') : 'java'),['-Xmx192m','-jar',path.join(root,m,'target',m+'-1.0.0.jar')],{stdio:['ignore',file,file],windowsHide:true}));}
async function request(method,p,body,token,status=200){const res=await fetch('http://localhost:8080'+p,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(20000)});const text=await res.text();if(res.status!==status)throw Error(method+' '+p+': '+res.status+' '+text);results.push(method+' '+p+' → '+res.status);return text?JSON.parse(text):null;}
(async()=>{try{
start('discovery');await wait('http://localhost:8761/eureka/apps');start('hotel');start('booking');start('gateway');await wait('http://localhost:8081/api/hotels',401);await wait('http://localhost:8082/bookings',401);await wait('http://localhost:8080/api/hotels',401);
await sleep(35000);
await request('GET','/api/hotels',null,null,401);
const auth=await request('POST','/user/register',{username:'e2e-'+Date.now(),password:'Password123!'});const token=auth.token;
await request('GET','/api/hotels',null,token);
const startDate=new Date(Date.now()+86400000).toISOString().slice(0,10),endDate=new Date(Date.now()+3*86400000).toISOString().slice(0,10);
const rooms=await request('GET',`/api/rooms/recommend?startDate=${startDate}&endDate=${endDate}`,null,token);
if(rooms.length!==3||rooms[0].id!==1||rooms[1].id!==2)throw Error('Неверные рекомендации');
const body={requestId:crypto.randomUUID(),autoSelect:true,startDate,endDate};
const b=await request('POST','/booking',body,token);if(b.status!=='CONFIRMED'||b.roomId!==1)throw Error('Не подтверждено');
const replay=await request('POST','/booking',body,token);if(replay.id!==b.id)throw Error('Дубликат');
await request('POST','/booking',{requestId:crypto.randomUUID(),roomId:1,autoSelect:false,startDate,endDate},token,409);
await request('GET','/api/bookings',null,token);
await request('GET','/user',null,token,403);
await request('POST','/api/rooms/1/release',{},token,404);
const cancelled=await request('DELETE','/booking/'+b.id,null,token);if(cancelled.status!=='CANCELLED')throw Error('Не отменено');
const admin=await request('POST','/user/auth',{username:'admin',password:'Admin12345!'});
await request('GET','/api/rooms/statistics',null,admin.token);
console.log('E2E SUCCESS\n'+results.join('\n'));fs.writeFileSync(path.join(root,'docs','E2E-results.txt'),'E2E SUCCESS\n'+results.join('\n')+'\n');
}catch(e){console.error(e);process.exitCode=1;}finally{for(const c of children)c.kill();for(const f of logs)fs.closeSync(f);}})();


