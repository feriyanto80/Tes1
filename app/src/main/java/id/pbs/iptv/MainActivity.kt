
package id.pbs.iptv

import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID

private val Bg=Color(0xFF090F1B)
private val CardBg=Color(0xFF151F30)
private val Accent=Color(0xFF7B43F6)
private val Soft=Color(0xFF9AA8BF)
private val White=Color(0xFFF4F5FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        setContent { IPTVApp(this) }
    }
}

@Composable private fun IPTVApp(activity:MainActivity){
    val context=LocalContext.current
    val store=remember{IptvStore(context)}
    val scope=rememberCoroutineScope()
    val playlists=remember{mutableStateListOf<Playlist>().apply{addAll(store.playlists())}}
    var selected by remember { mutableStateOf(playlists.find{it.id==store.selected()} ?: playlists.firstOrNull()) }
    val channels=remember{mutableStateListOf<Channel>().apply{selected?.let{addAll(store.channels(it.id))}}}
    var page by remember{mutableStateOf("home")}
    var playing by remember{mutableStateOf<Channel?>(null)}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var search by remember{mutableStateOf("")}
    var selectedGroup by remember{mutableStateOf("Semua")}
    var favorites by remember{mutableStateOf(store.favorites())}
    var guideData by remember{mutableStateOf<List<IptvLoader.Programme>>(emptyList())}
    var guideBusy by remember{mutableStateOf(false)}
    var guideUrl by remember{mutableStateOf(store.guideUrl())}

    fun setPlaylist(p:Playlist, refresh:Boolean){
        selected=p;store.select(p.id);channels.clear();channels.addAll(store.channels(p.id))
        if(refresh && p.kind!="Local"){
            scope.launch{
                busy=true;message=""
                try {val result=IptvLoader.load(p);channels.clear();channels.addAll(result);store.saveChannels(p.id,result);if(result.isEmpty()) message="Playlist tidak berisi channel yang didukung."}
                catch(e:Exception){message="Tidak dapat memuat playlist: "+(e.message ?: "Periksa URL dan koneksi.")}
                finally{busy=false}
            }
        }
    }
    fun addPlaylist(p:Playlist, localText:String?=null) {
        playlists.add(p);store.savePlaylists(playlists)
        if(localText!=null) {
            val parsed=IptvLoader.parseM3u(localText,p.id)
            store.saveChannels(p.id,parsed)
            setPlaylist(p,false)
            message=if(parsed.isEmpty()) "File tidak berisi channel HTTP(S) yang valid." else "Berhasil memuat "+parsed.size+" channel."
        }else setPlaylist(p,true)
        page="home"
    }
    fun toggle(c:Channel){store.toggleFavorite(c.id);favorites=store.favorites()}
    val tabs=listOf("home" to "Beranda","tv" to "Live TV","film" to "Film","favorites" to "Favorit","settings" to "Pengaturan")
    val icons=listOf(Icons.Default.Home,Icons.Default.Tv,Icons.Default.Movie,Icons.Default.Favorite,Icons.Default.Settings)

    MaterialTheme(colorScheme=darkColorScheme(primary=Accent,background=Bg,surface=CardBg,onBackground=White,onSurface=White)) {
        Scaffold(
            containerColor=Bg,
            topBar={
                if(playing==null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=17.dp, vertical=15.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(page=="add"||page=="guide") IconButton(onClick={page="home"}){Icon(Icons.Default.ArrowBack,"Kembali",tint=White)}
                        Text(when(page){"add"->"Tambahkan Playlist";"guide"->"Program TV";"tv"->"Semua Channel";"film"->"Film & Series";"favorites"->"Favorit";"settings"->"Pengaturan";else->"IPTV Player"},fontWeight=FontWeight.Bold,fontSize=20.sp,modifier=Modifier.weight(1f))
                        if(page=="home"||page=="tv") {
                            IconButton(onClick={page="add"}){Icon(Icons.Default.Add,"Tambah playlist",tint=Accent)}
                        }
                        if(page=="home") IconButton(onClick={page="guide"}){Icon(Icons.Default.CalendarMonth,"EPG",tint=Soft)}
                    }
                }
            },
            bottomBar={
                if(playing==null && page in listOf("home","tv","film","favorites","settings")){
                    NavigationBar(containerColor=CardBg) {
                        tabs.forEachIndexed{ index,entry->
                            NavigationBarItem(selected=page==entry.first,onClick={page=entry.first;selectedGroup="Semua";search=""},icon={Icon(icons[index],entry.second)},label={Text(entry.second,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Accent,selectedTextColor=Accent,unselectedIconColor=Soft,unselectedTextColor=Soft))
                        }
                    }
                }
            }
        ){ padding ->
            Box(Modifier.fillMaxSize().padding(padding)){
                if(playing!=null) {
                    PlayerPanel(playing!!,onBack={playing=null},isFavorite=favorites.contains(playing!!.id),onFavorite={toggle(playing!!)})
                }else when(page){
                    "add"->AddScreen(onAdd=::addPlaylist,onCancel={page="home"})
                    "settings"->SettingsScreen(playlists,selected,guideUrl,onGuideUrl={guideUrl=it;store.setGuideUrl(it)},onSelect={p->setPlaylist(p,true);page="home"},onRemove={p->store.removePlaylist(p.id);playlists.remove(p);if(selected?.id==p.id){selected=playlists.firstOrNull();channels.clear();selected?.let { setPlaylist(it,false) }}},onAdd={page="add"})
                    "guide"->GuideScreen(guideData,guideBusy,guideUrl,onLoad={
                        scope.launch{
                            guideBusy=true;message=""
                            try {guideData=IptvLoader.guide(guideUrl)}catch(e:Exception){message="EPG gagal dimuat: "+(e.message?:"Kesalahan koneksi")}
                            finally{guideBusy=false}
                        }
                    },onSettings={page="settings"})
                    else -> {
                        val source=channels.filter{ c-> when(page){
                            "favorites"->favorites.contains(c.id)
                            "film"->listOf("film","movie","series","vod").any{c.group.contains(it,true)}
                            else->true
                        }}
                        Column(Modifier.fillMaxSize()){
                            if(page=="home") {
                                Box(Modifier.fillMaxWidth().padding(horizontal=17.dp).height(155.dp).background(Brush.linearGradient(listOf(Color(0xFF222B55),Color(0xFF16172B),Accent.copy(alpha=.5f))),RoundedCornerShape(20.dp)).padding(20.dp)) {
                                    Column(Modifier.align(Alignment.CenterStart)) {
                                        Text("LIVE TV • FILM • SERIES",color=Color(0xFFBBB0FF),fontWeight=FontWeight.Bold,fontSize=11.sp)
                                        Spacer(Modifier.height(7.dp));Text("Tontonan Favoritmu",fontSize=23.sp,fontWeight=FontWeight.Bold)
                                        Text("Kapan saja, di mana saja.",color=White.copy(alpha=.78f),fontSize=13.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text((selected?.name ?: "Belum ada playlist")+" • "+channels.size+" channel",color=Soft,fontSize=11.sp)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth().padding(horizontal=17.dp),verticalAlignment=Alignment.CenterVertically){
                                    Text("Kategori",fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                                    TextButton(onClick={page="tv"}){Text("Lihat Semua")}
                                }
                                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=17.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    listOf("Semua","Olahraga","Berita","Film","Anak-anak").forEach{cat->
                                        FilterChip(selected=selectedGroup==cat,onClick={selectedGroup=cat},label={Text(cat)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Accent,containerColor=CardBg,selectedLabelColor=White))
                                    }
                                }
                            }
                            OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth().padding(horizontal=17.dp, vertical=10.dp),placeholder={Text("Cari channel...",color=Soft)},singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(15.dp))
                            if(page=="tv"){
                                Row(Modifier.horizontalScroll(rememberScrollState()).padding(start=17.dp,bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                    (listOf("Semua")+source.map{it.group}.distinct().take(40)).forEach{g->
                                        FilterChip(selected=selectedGroup==g,onClick={selectedGroup=g},label={Text(g,maxLines=1)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Accent,containerColor=CardBg))
                                    }
                                }
                            }
                            val filtered=source.filter{ c->
                                (search.isBlank()||c.name.contains(search,true)||c.group.contains(search,true)) &&
                                (selectedGroup=="Semua"||c.group.contains(selectedGroup,true) ||
                                    (selectedGroup=="Olahraga"&&listOf("sport","bola").any{c.group.contains(it,true)}) ||
                                    (selectedGroup=="Anak-anak"&&listOf("kids","children").any{c.group.contains(it,true)}))
                            }
                            if(filtered.isEmpty()){
                                Column(Modifier.fillMaxSize().padding(25.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                                    Icon(Icons.Default.LiveTv,null,tint=Accent,modifier=Modifier.size(52.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(if(playlists.isEmpty()) "Tambahkan playlist untuk memulai" else if(busy) "Memuat channel..." else "Belum ada channel",fontWeight=FontWeight.Bold)
                                    Text(if(playlists.isEmpty()) "Masukkan URL M3U, file M3U, atau Xtream Codes." else "Periksa playlist, kategori, atau kata pencarian.",color=Soft)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick={page=if(playlists.isEmpty()) "add" else "settings"},colors=ButtonDefaults.buttonColors(containerColor=Accent)){Text(if(playlists.isEmpty()) "Tambahkan Playlist" else "Kelola Playlist")}
                                }
                            } else LazyColumn(contentPadding=PaddingValues(start=17.dp,end=17.dp,bottom=15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                                items(filtered,key={it.id}){ c->ChannelRow(c,favorites.contains(c.id),onPlay={playing=c},onFavorite={toggle(c)}) }
                            }
                        }
                    }
                }
                if(playing==null && (busy || message.isNotBlank())){
                    Surface(Modifier.align(Alignment.BottomCenter).padding(12.dp),color=CardBg,shape=RoundedCornerShape(12.dp),shadowElevation=5.dp) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                            if(busy){CircularProgressIndicator(modifier=Modifier.size(16.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp))}
                            Text(if(busy)"Sedang mengambil channel..." else message,fontSize=12.sp,modifier=Modifier.weight(1f,fill=false))
                            IconButton(onClick={message="";busy=false},modifier=Modifier.size(28.dp)){Icon(Icons.Default.Close,"Tutup",modifier=Modifier.size(17.dp))}
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ChannelRow(ch:Channel,liked:Boolean,onPlay:()->Unit,onFavorite:()->Unit){
    Row(Modifier.fillMaxWidth().background(CardBg,RoundedCornerShape(13.dp)).clickable(onClick=onPlay).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(48.dp).background(Color.White,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center) {
            if(ch.logo.isNotBlank()) AsyncImage(model=ch.logo,contentDescription=null,modifier=Modifier.fillMaxSize().padding(5.dp))
            else Icon(Icons.Default.Tv,null,tint=Accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){Text(ch.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=14.sp);Text(ch.group,fontSize=11.sp,color=Soft,maxLines=1)}
        IconButton(onClick=onFavorite){Icon(if(liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Favorit",tint=if(liked)Accent else Soft)}
        Icon(Icons.Default.PlayArrow,"Putar",tint=Accent)
    }
}

@Composable private fun AddScreen(onAdd:(Playlist,String?)->Unit,onCancel:()->Unit) {
    val context=LocalContext.current
    var type by remember{mutableStateOf("URL")}
    var name by remember{mutableStateOf("Playlist Utama")}
    var url by remember{mutableStateOf("")}
    var user by remember{mutableStateOf("")}
    var pass by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri->
        if(uri!=null) {
            try {
                val contents=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: ""
                if(!contents.contains("#EXTINF")) error="File bukan playlist M3U yang valid."
                else onAdd(Playlist(UUID.randomUUID().toString(),name.ifBlank{"Playlist Lokal"},"Local","local"),contents)
            }catch(e:Exception){error="Gagal membuka file: "+e.message}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=19.dp)){
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(125.dp).background(CardBg,RoundedCornerShape(20.dp)),contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally){ Icon(Icons.Default.PlaylistAdd,null,modifier=Modifier.size(44.dp),tint=Accent);Text("Tambahkan Playlist",fontWeight=FontWeight.Bold,fontSize=19.sp);Text("M3U / M3U8 atau Xtream Codes",color=Soft,fontSize=12.sp) }
        }
        Spacer(Modifier.height(17.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("URL","Xtream","File").forEach { t->
                FilterChip(selected=type==t,onClick={type=t;error=""},label={Text(t)},modifier=Modifier.weight(1f),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Accent,containerColor=CardBg))
            }
        }
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(name,{name=it},label={Text("Nama Playlist")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(8.dp))
        if(type!="File") OutlinedTextField(url,{url=it},label={Text(if(type=="Xtream") "Server URL" else "URL playlist M3U / M3U8")},placeholder={Text(if(type=="Xtream") "https://server.example.com:8080" else "https://server.example.com/playlist.m3u",fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),singleLine=true)
        if(type=="Xtream"){
            Spacer(Modifier.height(8.dp));OutlinedTextField(user,{user=it},label={Text("Username")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Spacer(Modifier.height(8.dp));OutlinedTextField(pass,{pass=it},label={Text("Password")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation())
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick={
            error=""
            if(type=="File") picker.launch(arrayOf("audio/x-mpegurl","application/x-mpegURL","text/*","*/*"))
            else if(!url.startsWith("http://")&&!url.startsWith("https://")) error="URL harus diawali http:// atau https://"
            else if(type=="Xtream"&&(user.isBlank()||pass.isBlank())) error="Masukkan username dan password."
            else onAdd(Playlist(UUID.randomUUID().toString(),name.ifBlank{"Playlist Utama"},if(type=="Xtream")"Xtream" else "URL",url.trim(),user.trim(),pass),null)
        },modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Accent),shape=RoundedCornerShape(12.dp)){
            Text(if(type=="File")"Pilih File M3U" else "Simpan Playlist",fontWeight=FontWeight.Bold)
        }
        if(error.isNotBlank()) {Spacer(Modifier.height(9.dp));Text(error,color=Color(0xFFFF8686),fontSize=12.sp)}
        Spacer(Modifier.height(14.dp))
        Text("Aplikasi ini hanya memutar playlist dari sumber yang Anda miliki izin aksesnya. Tidak menyertakan channel atau layanan berlangganan.",fontSize=12.sp,color=Soft)
    }
}
