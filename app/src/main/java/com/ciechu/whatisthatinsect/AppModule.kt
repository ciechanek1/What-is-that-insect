package com.ciechu.whatisthatinsect

import android.app.Activity
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    viewModel { ImageDetectorViewModel() }
}