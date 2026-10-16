package com.stark.jarvislocal

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

open class JarvisApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        graph = createGraph()
    }

    protected open fun createGraph(): AppGraph = AppGraph(this)
}
