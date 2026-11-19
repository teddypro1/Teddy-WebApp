package com.teddy.bpositive.data
import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
object TopicRepository {
 private val client=OkHttpClient()
 private val handler=Handler(Looper.getMainLooper())
 private val media="application/json; charset=utf-8".toMediaType()
 private const val endpoint="/functions/v1/manage-notification-topics"
 data class Preferences(val allUsers:Boolean,val announcements:Boolean,val updates:Boolean)
 data class Result(val success:Boolean,val preferences:Preferences?=null,val message:String)
 fun load(session:SupabaseSessionManager.Session,done:(Result)->Unit)=call(session,JSONObject().put("action","preferences")){ok,data,msg->
  val p=data?.optJSONObject("preferences")
  done(if(ok&&p!=null) Result(true,Preferences(p.optBoolean("all_users",true),p.optBoolean("announcements",false),p.optBoolean("updates",false)),"Preferences loaded.") else Result(false,message=msg))
 }
 fun set(session:SupabaseSessionManager.Session,topic:String,enabled:Boolean,done:(Boolean,String)->Unit){
  require(topic in setOf("all_users","announcements","updates"))
  call(session,JSONObject().put("action",if(enabled)"subscribe" else "unsubscribe").put("topic",topic)){ok,_,msg->done(ok,msg)}
 }
 fun send(session:SupabaseSessionManager.Session,topic:String,title:String,body:String,done:(Boolean,String)->Unit){
  call(session,JSONObject().put("action","send_topic").put("topic",topic).put("title",title).put("body",body)){ok,data,msg->
   done(ok,if(ok)"Topic message accepted by FCM for '$topic'." else msg)
  }
 }
 private fun call(session:SupabaseSessionManager.Session,payload:JSONObject,done:(Boolean,JSONObject?,String)->Unit){
  Thread{
   val req=Request.Builder().url("${SupabaseConfig.BASE_URL}$endpoint")
    .addHeader("apikey",SupabaseConfig.API_KEY).addHeader("Authorization","Bearer ${session.accessToken}")
    .addHeader("Content-Type","application/json").post(payload.toString().toRequestBody(media)).build()
   val result=try{client.newCall(req).execute().use{res->val raw=res.body?.string().orEmpty();val data=runCatching{JSONObject(raw)}.getOrNull()
    Triple(res.isSuccessful&&data?.optBoolean("success",true)!=false,data,data?.optString("error")?.takeIf{it.isNotBlank()}?:"HTTP ${res.code}")}}
    catch(e:Exception){Triple(false,null,"Network error: ${e.message?: "Unknown"}")}
   handler.post{done(result.first,result.second,result.third)}
  }.start()
 }
}