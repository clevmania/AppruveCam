package com.clevmania.appruvecam.di

import android.app.Activity
import com.clevmania.appruvecam.app.AppruveCamApp

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */

fun Activity.coreComponent() = AppruveCamApp.coreComponent(this)