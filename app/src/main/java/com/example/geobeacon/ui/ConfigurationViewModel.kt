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
import java.nio.ByteBuffer
import java.nio.ByteOrder

@SuppressLint("MissingPermission")
class ConfigurationViewModel(private val repository: EditorRepository, private val bluetoothManager: BluetoothConnectionManager) : ViewModel() {
    val deviceName = bluetoothManager.deviceName
    private val _writeFailureResource = MutableStateFlow<Int?>(null)
    val writeFailureResource: StateFlow<Int?> = _writeFailureResource.asStateFlow()
    private val _transferring = MutableStateFlow(false)
    val transferring: StateFlow<Boolean> = _transferring.asStateFlow()

    private val _nameFieldContent = MutableStateFlow("")
    val nameFieldContent: StateFlow<String> = _nameFieldContent.asStateFlow()
    private val _nameFieldError = MutableStateFlow(false)
    val nameFieldError: StateFlow<Boolean> = _nameFieldError.asStateFlow()
    private val _passwordFieldContent = MutableStateFlow("")
    val passwordFieldContent: StateFlow<String> = _passwordFieldContent.asStateFlow()
    private val _passwordFieldError = MutableStateFlow(false)
    val passwordFieldError: StateFlow<Boolean> = _passwordFieldError.asStateFlow()
    private val _passwordRepeatFieldContent = MutableStateFlow("")
    val passwordRepeatFieldContent: StateFlow<String> = _passwordRepeatFieldContent.asStateFlow()
    private val _passwordRepeatFieldError = MutableStateFlow(false)
    val passwordRepeatFieldError: StateFlow<Boolean> = _passwordRepeatFieldError.asStateFlow()
    private val _selectedDialog = MutableStateFlow<DialogData?>(null)
    val selectedDialog: StateFlow<DialogData?> = _selectedDialog.asStateFlow()

    val dialogs = repository.dialogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf())

    init {
        viewModelScope.launch {
            if (!bluetoothManager.ready.value) {
                bluetoothManager.startScan()
            }
        }
    }

    fun setNameFieldContent(content: String) {
        _nameFieldContent.value = content
        _nameFieldError.value = content.toByteArray().size > bluetoothManager.MAX_NAME_SIZE
    }

    fun setPasswordFieldContent(content: String) {
        _passwordFieldContent.value = content
        _passwordFieldError.value = content.toByteArray().size > bluetoothManager.MAX_PASSWORD_SIZE
        if (_passwordRepeatFieldContent.value != "") {
            _passwordRepeatFieldError.value = content != _passwordRepeatFieldContent.value
        }
    }

    fun setPasswordRepeatFieldContent(content: String) {
        _passwordRepeatFieldContent.value = content
        _passwordRepeatFieldError.value = content != _passwordFieldContent.value
    }

    fun selectDialog(dialog: DialogData?) {
        _selectedDialog.value = dialog
    }

    fun transferConfig() {
        _transferring.value = true

        viewModelScope.launch {
            if (_nameFieldContent.value != "") {
                if (bluetoothManager.setConfigurationCharacteristic(
                    bluetoothManager.CONFIG_SET_NAME_UUID,
                    _nameFieldContent.value.toByteArray()
                ) != 0) {
                    _writeFailureResource.value = R.string.name_write_failure
                    return@launch
                }
                bluetoothManager.readName()
                _nameFieldContent.value = ""
            }

            if (_passwordFieldContent.value != "") {
                if (bluetoothManager.setConfigurationCharacteristic(
                    bluetoothManager.CONFIG_SET_PASSWORD_UUID,
                    _passwordFieldContent.value.toByteArray()
                ) != 0) {
                    _writeFailureResource.value = R.string.password_write_failure
                    return@launch
                }
                _passwordFieldContent.value = ""
                _passwordRepeatFieldContent.value = ""
            }

            if (_selectedDialog.value != null) {
                val fullDialog = repository.getDialogWithStates(_selectedDialog.value!!.id)
                if (fullDialog == null) {
                    _writeFailureResource.value = R.string.dialog_write_failure
                    return@launch
                }
                val dialogBytes = fullDialog.serialize()
                val notificationBuffer = ByteBuffer.allocate(4)
                notificationBuffer.order(ByteOrder.LITTLE_ENDIAN)

                var dialogBytesSize: UShort = dialogBytes.size.toUShort()

                var totalPackets: UShort = (dialogBytesSize / bluetoothManager.DIALOG_PACKET_SIZE.toUShort()).toUShort()
                if (dialogBytesSize.mod(bluetoothManager.DIALOG_PACKET_SIZE.toUInt()) > 0U) {
                    totalPackets++
                }
                dialogBytesSize = (dialogBytesSize + totalPackets).toUShort()
                notificationBuffer.putShort(dialogBytesSize.toShort())
                notificationBuffer.putShort(totalPackets.toShort())

                if (bluetoothManager.setConfigurationCharacteristic(
                    bluetoothManager.CONFIG_NOTIFICATION_UUID,
                    notificationBuffer.array()
                ) != 0) {
                    _writeFailureResource.value = R.string.dialog_write_failure
                    return@launch
                }

                if (bluetoothManager.transferDialog(dialogBytes, totalPackets.toInt()) != 0) {
                    _writeFailureResource.value = R.string.dialog_write_failure
                    return@launch
                }
                _selectedDialog.value = null
            }
        }.invokeOnCompletion {
            _transferring.value = false
        }
    }

    fun clearWriteFailure() {
        _writeFailureResource.value = null
    }

    fun authorize(password: String) {
        bluetoothManager.authorize(password)
    }

    fun reconnect() {
        bluetoothManager.disconnect()
        bluetoothManager.startScan()
    }

    companion object {
        fun Factory(repository: EditorRepository, bluetoothManager: BluetoothConnectionManager): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ConfigurationViewModel(repository, bluetoothManager)
            }
        }
    }
}