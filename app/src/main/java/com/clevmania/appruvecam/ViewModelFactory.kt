package com.clevmania.appruvecam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */

class ViewModelFactory(private val dataSource: DocumentDataSource)
    : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainActivityViewModel(dataSource) as T
    }
}