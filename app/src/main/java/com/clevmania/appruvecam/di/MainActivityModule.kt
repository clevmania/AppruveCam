package com.clevmania.appruvecam.di

import com.clevmania.appruvecam.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */

@Suppress("unused")
@Module
abstract class MainActivityModule {
    @ContributesAndroidInjector()
    abstract fun contributeMainActivity(): MainActivity
}