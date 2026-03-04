package com.veiltech.demo.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veiltech.demo.data.repository.VeilRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: VeilRepository) : ViewModel() {
    val status = MutableLiveData<String>()

    fun register(name: String, phone: String, passHash: String, onToken: (String, Long) -> Unit) = viewModelScope.launch {
        runCatching { repository.register(name, phone, passHash) }
            .onSuccess { onToken(it.token, it.userId); status.postValue("Registered") }
            .onFailure { status.postValue(it.message) }
    }

    fun login(phone: String, passHash: String, onToken: (String, Long) -> Unit) = viewModelScope.launch {
        runCatching { repository.login(phone, passHash) }
            .onSuccess { onToken(it.token, it.userId); status.postValue("Logged in") }
            .onFailure { status.postValue(it.message) }
    }
}
