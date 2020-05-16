package com.clevmania.appruvecam.app

import android.app.Application
import android.content.Context
import com.clevmania.appruvecam.di.AppComponent
import com.clevmania.appruvecam.di.AppInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */

class AppruveCamApp : Application(), HasAndroidInjector{
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        AppInjector.init(this)
    }

    companion object {
        @JvmStatic
        fun coreComponent(context: Context) =
            (context.applicationContext as AppruveCamApp).appComponent
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}