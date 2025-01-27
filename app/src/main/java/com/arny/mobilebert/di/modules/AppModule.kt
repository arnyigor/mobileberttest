package com.arny.mobilebert.di.modules

import android.content.Context
import com.arny.mobilebert.MobileBertApp
import dagger.Binds
import dagger.Module

@Module
internal abstract class AppModule {
    @Binds
    abstract fun provideContext(application: MobileBertApp): Context
}