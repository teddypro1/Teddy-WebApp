package com.teddy.bpositive.data
import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
object InboxRepository {
 private val client=OkHttpClient()
 private val handler=Handler(Looper.getMainLooper())
 private val media="application/json; charset=utf-8".toMediaType()
 private const val url="${SupabaseConfig.BASE_URL}/rest/v1/user_notifications"
 data class Item(val id:String,val title:String,val body:String,val createdAt:String,val readAt:String?){val isRead get()=!readAt.isNullOrBlank()}
 data class Result(val success:Boolean,val items:List<Item> = emptyList(),val message:String)
 fun load(session:SupabaseSessionManager.Session,done:(Result)->Unit){
  val uid=session.userId
  if(uid.isNullOrBlank()){done(Result(false,message="Account could not be verified."));return}
  Thread{
   val req=Request.Builder().url("$url?select=id,title,body,created_at,read_at&user_id=eq.$uid&order=created_at.desc&limit=100")
    .addHeader("apikey",SupabaseConfig.API_KEY).addHeader("Authorization","Bearer ${session.accessToken}").get().build()
   val result=try{client.newCall(req).execute().use{res->
    val raw=res.body?.string().orEmpty()
    if(!res.isSuccessful) Result(false,message="Could not load inbox. HTTP ${res.code}.")
    else {val arr=JSONArray(raw);val list=buildList{for(i in 0 until arr.length()){val o=arr.optJSONObject(i)?:continue
     add(Item(o.optString("id"),o.optString("title"),o.optString("body"),o.optString("created_at"),o.optString("read_at").takeIf{it.isNotBlank()&&it!="null"}))}}
     Result(true,list,"Inbox updated.")}
   }}catch(e:Exception){Result(false,message="Inbox error: ${e.message ?: "Unknown"}")}
   handler.post{done(result)}
  }.start()
 }
 fun markRead(session:SupabaseSessionManager.Session,id:String,done:(Boolean,String)->Unit){
  val uid=session.userId
  if(uid.isNullOrBlank()){done(false,"Account could not be verified.");return}
  Thread{
   val payload=JSONObject().put("read_at",java.time.Instant.now().toString())
   val req=Request.Builder().url("$url?id=eq.$id&user_id=eq.$uid")
    .addHeader("apikey",SupabaseConfig.API_KEY).addHeader("Authorization","Bearer ${session.accessToken}")
    .addHeader("Content-Type","application/json").addHeader("Prefer","return=representation")
    .patch(payload.toString().toRequestBody(media)).build()
   val result=try{client.newCall(req).execute().use{if(it.isSuccessful) true to "Marked as read." else false to "Update failed. HTTP ${it.code}."}}
    catch(e:Exception){false to "Update error: ${e.message ?: "Unknown"}"}
   handler.post{done(result.first,result.second)}
  }.start()
 }
}