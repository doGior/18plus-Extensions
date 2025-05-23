package com.Xtapes

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class InternetchicksProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Xtapes())
        registerExtractorAPI(Stream())
        registerExtractorAPI(VID())
        registerExtractorAPI(XtapesExtractor())
    }
}