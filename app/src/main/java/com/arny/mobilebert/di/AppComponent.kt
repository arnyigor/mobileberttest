package com.arny.mobilebert.di

import android.app.Application
import com.arny.mobilebert.MobileBertApp
import com.arny.mobilebert.di.modules.AppModule
import com.arny.mobilebert.di.modules.DataModule
import com.arny.mobilebert.di.modules.UiModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        DataModule::class,
        UiModule::class,
        AppModule::class
    ]
)
interface AppComponent : AndroidInjector<MobileBertApp> {
    override fun inject(application: MobileBertApp)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: MobileBertApp): Builder

        fun build(): AppComponent
    }
}