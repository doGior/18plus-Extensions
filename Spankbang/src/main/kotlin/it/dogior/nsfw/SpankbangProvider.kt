package it.dogior.nsfw.coxju

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class SpankbangProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Spankbang())
    }
}