package it.dogior.nsfw

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ChatrubatePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Chatrubate())
    }
}