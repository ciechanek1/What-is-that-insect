package com.ciechu.whatisthatinsect

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { ImageDetectorViewModel() }
}