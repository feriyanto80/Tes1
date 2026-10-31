package id.pbs.iptv

import org.junit.Assert.*
import org.junit.Test

class M3uTest {
    @Test fun parseLiveChannels() {
        val text="#EXTM3U\n#EXTINF:-1 tvg-id=\"one\" group-title=\"Berita\",News HD\nhttps://example.com/one.m3u8\n#EXTINF:-1 group-title=\"Olahraga\",Sport\nhttp://example.com/two.ts\n"
        val channels=IptvLoader.parseM3u(text,"playlist")
        assertEquals(2,channels.size)
        assertEquals("News HD",channels[0].name)
        assertEquals("Berita",channels[0].group)
        assertEquals("one",channels[0].tvgId)
        assertEquals("Sport",channels[1].name)
    }
}
