package com.teddy.bpositive.ui.screens
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme
import com.teddy.bpositive.data.InboxRepository
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun NotificationInboxScreen(items:List<InboxRepository.Item>,loading:Boolean,status:String,padding:PaddingValues,onRefresh:()->Unit,onRead:(InboxRepository.Item)->Unit){
 val unread=items.count{!it.isRead}
 Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   Column{Text("Notification Inbox",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("$unread unread · ${items.size} notifications",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)}
   TextButton(onClick=onRefresh,enabled=!loading){Text("Refresh")}
  }
  if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)
  if(loading&&items.isEmpty())Box(Modifier.fillMaxWidth().padding(36.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}
  else if(items.isEmpty())Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Text("🔔",fontSize=34.sp);Text("You're all caught up!",fontWeight=FontWeight.SemiBold);Text("Admin notifications will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
  else items.forEach{item->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(item.isRead)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer)){
   Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(item.title,Modifier.weight(1f),fontWeight=FontWeight.Bold,fontSize=17.sp);if(!item.isRead)Text("NEW",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,fontSize=11.sp)}
    Text(item.body,color=MaterialTheme.colorScheme.onSurface)
    Text(item.createdAt.replace("T"," ").take(16),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)
    if(!item.isRead)TextButton(onClick={onRead(item)}){Text("Mark as read")}else Text("Read",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }}
  if(loading&&items.isNotEmpty())LinearProgressIndicator(Modifier.fillMaxWidth())
  Spacer(Modifier.height(24.dp))
 }
}

@Preview(showBackground = true, name = "Notification Inbox")
@Composable
private fun NotificationInboxScreenPreview() {
    TeddyAppTheme(darkMode = false) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notification Inbox", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("1 unread · 2 notifications", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                TextButton(onClick = {}) { Text("Refresh") }
            }
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Welcome to Teddy WebApp", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("NEW", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text("A sample notification shown in the inbox preview.")
                    Text("21 Nov 2026 08:30", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    TextButton(onClick = {}) { Text("Mark as read") }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Workspace update", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("A previously read notification.")
                    Text("20 Nov 2026 14:00", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("Read", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
