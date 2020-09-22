package com.ciechu.whatisthatinsect.di

import com.ciechu.whatisthatinsect.viewmodels.ImageDetectorViewModel
import com.ciechu.whatisthatinsect.adapters.InsectAdapter
import com.ciechu.whatisthatinsect.viewmodels.ExpViewModel
import com.ciechu.whatisthatinsect.viewmodels.InsectViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { ImageDetectorViewModel() }
    viewModel { InsectViewModel(get()) }
    viewModel { ExpViewModel(get()) }
    factory { InsectAdapter(get(), get()) }

}