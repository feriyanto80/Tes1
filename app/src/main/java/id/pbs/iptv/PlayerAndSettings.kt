
package id.pbs.iptv

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.text.SimpleDateFormat
import java.util.Locale

private val Background=Color(0xFF090F1B)
private val Surface=Color(0xFF151F30)
private val Purple=Color(0xFF7B43F6)
private val Secondary=Color(0xFF9AA8BF)

@Composable fun PlayerPanel(item:Channel,onBack:()->Unit,isFavorite:Boolean,onFavorite:()->Unit){
    val context=LocalContext.current
    val activity=context as? Activity
    var fullscreen by remember{mutableStateOf(false)}
    val player=remember(item.url){ExoPlayer.Builder(context).build().apply{
        setMediaItem(MediaItem.fromUri(item.url));playWhenReady=true;prepare()
    }}
    DisposableEffect(player){onDispose{activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;player.release()}}
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Kembali")}
            Text(item.name,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Bold)
            IconButton(onClick=onFavorite){Icon(if(isFavorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Favorit",tint=if(isFavorite)Purple else Secondary)}
            IconButton(onClick={
                fullscreen=!fullscreen
                activity?.requestedOrientation=if(fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }){Icon(Icons.Default.Fullscreen,"Layar penuh")}
        }
        AndroidView(factory={ ctx->PlayerView(ctx).apply{this.player=player;useController=true;setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)}},update={it.player=player},modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black))
        Column(Modifier.padding(18.dp)){
            Text(item.name,fontSize=21.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(5.dp));Text(item.group,color=Secondary)
            Spacer(Modifier.height(12.dp))
            Surface(color=Surface,shape=RoundedCornerShape(12.dp)){
                Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.LiveTv,null,tint=Purple)
                    Spacer(Modifier.width(9.dp))
                    Column{
                        Text("LIVE STREAM",fontWeight=FontWeight.Bold,color=Purple,fontSize=12.sp)
                        Text("Kontrol play, pause, posisi, dan volume tersedia pada pemutar.",color=Secondary,fontSize=12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick={player.stop();player.prepare();player.play()}){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(6.dp));Text("Coba Sambungkan Kembali")}
        }
    }
}

@Composable fun SettingsScreen(
    playlists:List<Playlist>,selected:Playlist?,guideUrl:String,
    onGuideUrl:(String)->Unit,onSelect:(Playlist)->Unit,onRemove:(Playlist)->Unit,onAdd:()->Unit
){
    var deleteItem by remember{mutableStateOf<Playlist?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp)){
        Text("Playlist",fontSize=17.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if(playlists.isEmpty()) Text("Belum ada playlist.",color=Secondary)
        playlists.forEach{p->
            Row(Modifier.fillMaxWidth().padding(bottom=8.dp).background(Surface,RoundedCornerShape(13.dp)).clickable{onSelect(p)}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Default.PlaylistPlay,null,tint=Purple)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)){
                    Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=1)
                    Text(p.kind+" • "+if(p.id==selected?.id)"Aktif" else "Ketuk untuk memuat",fontSize=11.sp,color=Secondary)
                }
                IconButton(onClick={onSelect(p)}){Icon(Icons.Default.Refresh,"Muat ulang",tint=Purple)}
                IconButton(onClick={deleteItem=p}){Icon(Icons.Default.Delete,"Hapus",tint=Color(0xFFFF8888))}
            }
        }
        Button(onClick=onAdd,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Purple)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Tambahkan Playlist")}
        Spacer(Modifier.height(26.dp))
        Text("Electronic Program Guide (EPG)",fontSize=17.sp,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value=guideUrl,onValueChange=onGuideUrl,modifier=Modifier.fillMaxWidth(),label={Text("URL XMLTV")},placeholder={Text("https://server.example.com/guide.xml")},singleLine=true)
        Spacer(Modifier.height(8.dp))
        Text("Masukkan URL XMLTV untuk menampilkan jadwal acara. Channel dan jadwal berasal dari penyedia playlist Anda.",color=Secondary,fontSize=12.sp)
        Spacer(Modifier.height(28.dp))
        Text("IPTV Player v1.0.0",fontWeight=FontWeight.Bold)
        Text("Android Native • Kotlin • Media3 ExoPlayer",color=Secondary,fontSize=12.sp)
        Spacer(Modifier.height(8.dp))
        Text("Gunakan hanya konten yang Anda berhak akses. Informasi playlist disimpan di perangkat ini.",color=Secondary,fontSize=12.sp)
    }
    deleteItem?.let{p->
        AlertDialog(onDismissRequest={deleteItem=null},title={Text("Hapus playlist?")},text={Text("Playlist "+p.name+" dan cache channel akan dihapus dari perangkat ini.")},confirmButton={TextButton(onClick={onRemove(p);deleteItem=null}){Text("Hapus",color=Color(0xFFFF8888))}},dismissButton={TextButton(onClick={deleteItem=null}){Text("Batal")}})
    }
}

@Composable fun GuideScreen(
    programmes:List<IptvLoader.Programme>,busy:Boolean,url:String,onLoad:()->Unit,onSettings:()->Unit
){
    var filter by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(horizontal=17.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("Jadwal TV",fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.weight(1f))
            TextButton(onClick=onSettings){Text("Atur EPG")}
            IconButton(onClick=onLoad,enabled=!busy&&url.isNotBlank()){Icon(Icons.Default.Refresh,"Perbarui EPG")}
        }
        if(url.isBlank()){
            Text("URL XMLTV belum diatur. Tambahkan melalui menu Pengaturan.",color=Secondary)
            Button(onClick=onSettings,colors=ButtonDefaults.buttonColors(containerColor=Purple)){Text("Atur EPG")}
            return@Column
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(programmes.isEmpty()){
            Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    Icon(Icons.Default.CalendarMonth,null,modifier=Modifier.size(44.dp),tint=Purple)
                    Text("Belum ada jadwal yang dimuat",color=Secondary)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick=onLoad,enabled=!busy){Text("Muat Program TV")}
                }
            }
        }else{
            OutlinedTextField(filter,{filter=it},placeholder={Text("Cari program / ID channel")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Spacer(Modifier.height(12.dp))
            val list=programmes.filter{filter.isBlank()||it.title.contains(filter,true)||it.channel.contains(filter,true)}
            LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=20.dp)){
                items(list){p->
                    Column(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(12.dp)).padding(12.dp)){
                        Text(p.title.ifBlank{"Tanpa judul"},fontWeight=FontWeight.SemiBold)
                        Text(p.channel,color=Secondary,fontSize=12.sp)
                        Text(p.start.take(12)+"  -  "+p.end.take(12),color=Purple,fontSize=12.sp)
                    }
                }
            }
        }
    }
}
