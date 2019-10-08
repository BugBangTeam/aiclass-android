package com.acadsoc.aiclass

import android.app.Application
import com.acadsoc.aiclass.core.Constants
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptor
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst
import timber.log.Timber
import java.io.File

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化WebView缓存
        val builder = WebViewCacheInterceptor.Builder(this)
        builder.setCachePath(File(this.externalCacheDir.toString(), "CacheWebViewCache"))
        WebViewCacheInterceptorInst.getInstance().init(builder)

        // 初始化日志
        Timber.plant(Timber.DebugTree())

        // 注册讯飞
        SpeechUtility.createUtility(this, "${SpeechConstant.APPID}=${Constants.XFYUN_APP_KEY}")
    }

}
