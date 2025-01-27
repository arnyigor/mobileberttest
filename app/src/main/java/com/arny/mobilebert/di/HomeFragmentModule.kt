package com.arny.mobilebert.di

import com.arny.mobilebert.di.scopes.FragmentScope
import com.arny.mobilebert.ui.home.HomeFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface HomeFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeFragmentInjector(): HomeFragment
}
