package com.example.geobeacon.ui

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.R
import com.example.geobeacon.data.BluetoothConnectionManager
import com.example.geobeacon.data.DialogData
import com.example.geobeacon.data.db.EditorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class ConfigurationViewModel(private val repository: EditorRepository, private val bluetoothManager: BluetoothConnectionManager) : ViewModel() {
    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()
    private val _writeFailureResource = MutableStateFlow<Int?>(null)
    val writeFailureResource: StateFlow<Int?> = _writeFailureResource.asStateFlow()
    private val _transferring = MutableStateFlow(false)
    val transferring: StateFlow<Boolean> = _transferring.asStateFlow()

    val ready = bluetoothManager.ready
    val dialogs = repository.dialogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf())

    init {
        viewModelScope.launch {
            if (!bluetoothManager.ready.value) {
                bluetoothManager.startScan()
            }

            bluetoothManager.deviceName.collect {
                _deviceName.value = it
            }
        }
    }

    fun transferConfig(name: String, password: String, repeatPassword: String, dialog: DialogData?) {
        _transferring.value = true

        viewModelScope.launch {
            if (name != deviceName.value) {
                if (bluetoothManager.setConfigurationCharacteristic(
                    bluetoothManager.CONFIG_SET_NAME_UUID,
                    name.toByteArray()
                ) != 0) {
                    _writeFailureResource.value = R.string.name_write_failure
                    return@launch
                }
                bluetoothManager.readName()
            }

            if (password != "") {
                if (bluetoothManager.setConfigurationCharacteristic(
                    bluetoothManager.CONFIG_SET_PASSWORD_UUID,
                    password.toByteArray()
                ) != 0) {
                    _writeFailureResource.value = R.string.password_write_failure
                    return@launch
                }
            }

            if (dialog != null) {
                if (bluetoothManager.setConfigurationCharacteristic(
                    bluetoothManager.CONFIG_NOTIFICATION_UUID,
                    ByteArray(4)
                ) != 0) {
                    _writeFailureResource.value = R.string.dialog_write_failure
                    return@launch
                }
            }
        }.invokeOnCompletion {
            _transferring.value = false
        }
    }

    companion object {
        fun Factory(repository: EditorRepository, bluetoothManager: BluetoothConnectionManager): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ConfigurationViewModel(repository, bluetoothManager)
            }
        }
    }
}