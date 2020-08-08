package com.ciechu.whatisthatinsect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ImageDetectorViewModel: ViewModel() {

    val objectLabel = MutableLiveData<String?>()
    var isAnalysing = false
}