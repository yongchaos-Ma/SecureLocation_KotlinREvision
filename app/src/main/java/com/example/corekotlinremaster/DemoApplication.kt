package com.example.corekotlinremaster

import android.app.Application
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SDKInitializer.setAgreePrivacy(this,true)
        SDKInitializer.initialize(this)
        SDKInitializer.setCoordType(CoordType.BD09LL)
    }
}