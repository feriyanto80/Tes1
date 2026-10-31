
package id.pbs.iptv

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

data class Channel(val id:String,val name:String,val url:String,val group:String="Umum",val logo:String="",val tvgId:String="")
data class Playlist(val id:String,val name:String,val kind:String,val url:String,val username:String="",val password:String="")

class IptvStore(context:Context) {
    private val sp:SharedPreferences=context.getSharedPreferences("iptv_v1", Context.MODE_PRIVATE)
    fun playlists():List<Playlist> = try {
        val a=JSONArray(sp.getString("playlists","[]"))
        (0 until a.length()).map { i-> val o=a.getJSONObject(i); Playlist(o.optString("id"),o.optString("name"),o.optString("kind"),o.optString("url"),o.optString("username"),o.optString("password")) }
    }catch(_:Exception){ emptyList() }
    fun savePlaylists(items:List<Playlist>) {
        val a=JSONArray()
        items.forEach { p-> a.put(JSONObject().put("id",p.id).put("name",p.name).put("kind",p.kind).put("url",p.url).put("username",p.username).put("password",p.password)) }
        sp.edit().putString("playlists",a.toString()).apply()
    }
    fun channels(playlistId:String):List<Channel> = try {
        val a=JSONArray(sp.getString("channels_"+playlistId,"[]"))
        (0 until a.length()).map { i-> val o=a.getJSONObject(i); Channel(o.optString("id"),o.optString("name"),o.optString("url"),o.optString("group","Umum"),o.optString("logo"),o.optString("tvgId")) }
    }catch(_:Exception){ emptyList() }
    fun saveChannels(playlistId:String,items:List<Channel>) {
        val a=JSONArray()
        items.forEach { c->a.put(JSONObject().put("id",c.id).put("name",c.name).put("url",c.url).put("group",c.group).put("logo",c.logo).put("tvgId",c.tvgId)) }
        sp.edit().putString("channels_"+playlistId,a.toString()).apply()
    }
    fun removePlaylist(id:String) { savePlaylists(playlists().filterNot {it.id==id}); sp.edit().remove("channels_"+id).apply() }
    fun favorites():Set<String> = sp.getStringSet("favorites",emptySet())?.toSet() ?: emptySet()
    fun toggleFavorite(id:String) { val set=favorites().toMutableSet(); if(!set.add(id)) set.remove(id); sp.edit().putStringSet("favorites",set).apply() }
    fun guideUrl():String=sp.getString("guide","") ?: ""
    fun setGuideUrl(value:String) {sp.edit().putString("guide",value).apply()}
    fun selected():String=sp.getString("selected","") ?: ""
    fun select(id:String){sp.edit().putString("selected",id).apply()}
}

object IptvLoader {
    private fun attributes(line:String):Map<String,String> {
        val re=Regex("""([\w-]+)\s*=\s*"([^"]*)"""")
        return re.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
    }
    fun parseM3u(body:String,playlistId:String):List<Channel> {
        val items=mutableListOf<Channel>(); var title="";var group="Umum";var logo="";var tvgId=""
        for (raw in body.lineSequence()){
            val line=raw.trim()
            if (line.startsWith("#EXTINF",true)){
                val attrs=attributes(line)
                title=line.substringAfter(",",attrs["tvg-name"] ?: "Channel").trim().ifBlank {attrs["tvg-name"] ?: "Channel"}
                group=attrs["group-title"].orEmpty().ifBlank {"Umum"}; logo=attrs["tvg-logo"].orEmpty(); tvgId=attrs["tvg-id"].orEmpty()
            }else if(line.startsWith("#EXTGRP:",true)){group=line.substringAfter(":").trim()}
            else if(line.isNotBlank()&&!line.startsWith("#")){
                if(line.startsWith("http://")||line.startsWith("https://")){
                    val id=playlistId+":"+items.size
                    items.add(Channel(id,title.ifBlank {"Channel "+(items.size+1)},line,group,logo,tvgId))
                };title="";group="Umum";logo="";tvgId=""
            }
        }
        return items
    }
    private fun fetch(address:String):String {
        val c=URL(address).openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=30000;c.setRequestProperty("User-Agent","IPTVPlayer/1.0")
        try {
            if(c.responseCode !in 200..299) throw IllegalStateException("Server merespons HTTP "+c.responseCode)
            return c.inputStream.bufferedReader(Charsets.UTF_8).use {it.readText()}
        }finally{c.disconnect()}
    }
    suspend fun load(p:Playlist):List<Channel> = withContext(Dispatchers.IO){
        if(p.kind=="Xtream"){
            val host=p.url.trimEnd('/');val user=URLEncoder.encode(p.username,"UTF-8");val pass=URLEncoder.encode(p.password,"UTF-8")
            val url=host+"/player_api.php?username="+user+"&password="+pass+"&action=get_live_streams"
            val arr=JSONArray(fetch(url))
            (0 until arr.length()).mapNotNull { i->
                val o=arr.optJSONObject(i) ?: return@mapNotNull null
                val streamId=o.optString("stream_id")
                if(streamId.isBlank()) null else Channel(p.id+":"+streamId,o.optString("name","Channel"),host+"/live/"+user+"/"+pass+"/"+streamId+".m3u8",o.optString("category_name").ifBlank{"Live TV"},o.optString("stream_icon"),o.optString("epg_channel_id"))
            }
        }else parseM3u(fetch(p.url),p.id)
    }
    data class Programme(val channel:String,val title:String,val start:String,val end:String)
    suspend fun guide(url:String):List<Programme> = withContext(Dispatchers.IO){
        if(url.isBlank()) return@withContext emptyList()
        val c=URL(url).openConnection() as HttpURLConnection
        c.connectTimeout=15000;c.readTimeout=30000
        val result=mutableListOf<Programme>()
        try {
            c.inputStream.use { input->
                val xml=Xml.newPullParser();xml.setInput(input,null)
                var event=xml.eventType;var channel="";var start="";var end="";var title=""
                while(event!=XmlPullParser.END_DOCUMENT && result.size<2000) {
                    if(event==XmlPullParser.START_TAG){
                        if(xml.name=="programme") {channel=xml.getAttributeValue(null,"channel") ?: "";start=xml.getAttributeValue(null,"start") ?: "";end=xml.getAttributeValue(null,"stop") ?: "";title=""}
                        if(xml.name=="title"&&channel.isNotBlank()) title=xml.nextText()
                    } else if(event==XmlPullParser.END_TAG&&xml.name=="programme"){
                        if(channel.isNotBlank()) result.add(Programme(channel,title,start,end))
                        channel=""
                    }
                    event=xml.next()
                }
            }
        }finally {c.disconnect()}
        result
    }
}
