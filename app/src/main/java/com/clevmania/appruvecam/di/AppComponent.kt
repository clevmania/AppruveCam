package com.clevmania.appruvecam.di

import android.app.Application
import com.clevmania.appruvecam.app.AppruveCamApp
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */
@Singleton
@Component(modules = [AndroidInjectionModule::class,AppModule::class,MainActivityModule::class])
interface AppComponent {
    @Component.Builder
    interface Builder{
        @BindsInstance
        fun application(application: Application): Builder
        fun appModule(appModule: AppModule): Builder
        fun build(): AppComponent
    }

    fun inject(application : AppruveCamApp)
}