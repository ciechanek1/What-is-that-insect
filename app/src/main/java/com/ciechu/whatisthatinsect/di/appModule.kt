package com.ciechu.whatisthatinsect.di

import com.ciechu.whatisthatinsect.ImageDetectorViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { ImageDetectorViewModel() }
}