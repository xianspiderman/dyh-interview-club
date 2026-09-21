export type ApiResult<T>={code:number;message:string;data:T;traceId:string}
const TOKEN='club-token'
export async function api<T>(path:string,options:RequestInit={}):Promise<T>{
  const token=localStorage.getItem(TOKEN);const headers=new Headers(options.headers||{});headers.set('Content-Type','application/json');if(token)headers.set('satoken',token)
  const response=await fetch(path,{...options,headers});const result=await response.json() as ApiResult<T>;if(!response.ok||result.code>=400)throw new Error(`${result.message||'请求失败'}${result.traceId?`（追踪号：${result.traceId}）`:''}`);return result.data
}
export function saveToken(value:string){localStorage.setItem(TOKEN,value)}
export function clearToken(){localStorage.removeItem(TOKEN)}
export function hasToken(){return Boolean(localStorage.getItem(TOKEN))}
