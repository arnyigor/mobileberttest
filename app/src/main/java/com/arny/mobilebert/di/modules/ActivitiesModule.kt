package com.arny.mobilebert.di.modules

import com.arny.mobilebert.di.HomeFragmentModule
import com.arny.mobilebert.di.scopes.ActivityScope
import com.arny.mobilebert.ui.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivitiesModule {
    @ActivityScope
    @ContributesAndroidInjector(
        modules = [
            HomeFragmentModule::class,
        ]
    )
    abstract fun bindMainActivity(): MainActivity
}
