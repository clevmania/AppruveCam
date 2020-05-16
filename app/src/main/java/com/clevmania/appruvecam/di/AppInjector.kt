package com.clevmania.appruvecam.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.clevmania.appruvecam.app.AppruveCamApp
import dagger.android.AndroidInjection
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */
class AppInjector {
    companion object{
        fun init(app: AppruveCamApp){
            DaggerAppComponent.builder()
                .application(app)
                .appModule(AppModule(app))
                .build().also { app.appComponent = it }
                .inject(app)

            app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbackAdapter(){
                override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                    super.onActivityCreated(activity, bundle)
                    handleActivity(activity)
                }
            })
        }

        fun handleActivity(activity: Activity) {
            if (activity is HasAndroidInjector) {
                AndroidInjection.inject(activity)
            }

            (activity as? FragmentActivity)?.supportFragmentManager?.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {

                    override fun onFragmentPreAttached(
                        fm: FragmentManager,
                        f: Fragment,
                        context: Context
                    ) {
                        super.onFragmentPreAttached(fm, f, context)

                        if (f is Injectable) {
                            AndroidSupportInjection.inject(f)
                        }
                    }
                }, true
            )

        }
    }
}


open class ActivityLifecycleCallbackAdapter : Application.ActivityLifecycleCallbacks {

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {

    }

    override fun onActivityResumed(activity: Activity) {

    }

}