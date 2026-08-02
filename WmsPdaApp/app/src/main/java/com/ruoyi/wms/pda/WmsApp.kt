package com.ruoyi.wms.pda

import android.app.Application

class WmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WmsApp private set
    }
}
