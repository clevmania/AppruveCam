package com.clevmania.appruvecam

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.RequestBody

/**
 * @author by Lawrence on 5/9/20.
 * for AppruveCam
 */

class MainActivityViewModel(private val documentDataSource: DocumentDataSource): ViewModel() {
    private val _progress = MutableLiveData<UIEvent<Boolean>>()
    val progress: LiveData<UIEvent<Boolean>> = _progress

    private val _error = MutableLiveData<UIEvent<String>>()
    val error: LiveData<UIEvent<String>> = _error

    private val _uploadedDocument = MutableLiveData<UIEvent<String>>()
    val uploadedDocument: LiveData<UIEvent<String>> = _uploadedDocument


    fun uploadDocument(request : RequestBody){
        viewModelScope.launch {
            try {
                _progress.value = UIEvent(true)
                val result = documentDataSource.uploadDocument(request)
                result.let {
                    _uploadedDocument.value = UIEvent(it)
                }

            }catch (e : Exception){
                Log.d("uploadDocError",e.message)
                _error.value = UIEvent(e.localizedMessage)
            }finally {
                _progress.value = UIEvent(false)
            }
        }
    }
}