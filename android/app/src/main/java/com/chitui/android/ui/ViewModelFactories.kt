package com.chitui.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chitui.android.data.ChitUIRepository
import com.chitui.android.data.PreferencesManager

class LoginViewModelFactory(
    private val repository: ChitUIRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class PrintersViewModelFactory(
    private val repository: ChitUIRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrintersViewModel::class.java)) {
            return PrintersViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class PrinterDetailViewModelFactory(
    private val repository: ChitUIRepository,
    private val printerId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrinterDetailViewModel::class.java)) {
            return PrinterDetailViewModel(repository, printerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
