package com.arny.mobilebert

import com.arny.mobilebert.di.DaggerAppComponent
import dagger.android.DaggerApplication

class MobileBertApp : DaggerApplication() {
    init {
        System.loadLibrary("tensorflowlite_jni")
        // Загрузите другие необходимые библиотеки
    }
    private val applicationInjector = DaggerAppComponent.builder()
        .application(this)
        .build()

    override fun applicationInjector() = applicationInjector
}