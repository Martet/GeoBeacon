package com.example.geobeacon.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.BluetoothConnectionManager
import com.example.geobeacon.data.ConversationData
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.MessageData
import com.example.geobeacon.data.db.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@SuppressLint("MissingPermission")
class ChatViewModel(private val repository: ChatRepository, private val bluetoothManager: BluetoothConnectionManager) : ViewModel() {
    private val _conversation = MutableStateFlow<ConversationData>(ConversationData())
    val conversation: StateFlow<ConversationData> = _conversation.asStateFlow()
    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()
    private val _enableAnswer = MutableStateFlow(false)
    val enableAnswer: StateFlow<Boolean> = _enableAnswer.asStateFlow()

    private var deviceAddress: String? = null
    private var lastQuestion = MessageData()
    private var lastAnswerNum = -1
    private val semaphore = Semaphore(1)

    init {
        viewModelScope.launch {
            bluetoothManager.startScan()

            launch {
                bluetoothManager.deviceName.collect {
                    _deviceName.value = it
                    Log.d("GeoBeacon", "Device name: ${deviceName.value}")
                    if (it != null && deviceAddress != null) {
                        initConversation()
                    }
                }
            }

            launch {
                bluetoothManager.deviceAddress.collect {
                    deviceAddress = it
                    Log.d("GeoBeacon", "Device address: ${deviceAddress}")
                    if (it != null && deviceName.value != null) {
                        initConversation()
                    }
                }
            }

            launch {
                bluetoothManager.ready.collect {
                    _enableAnswer.value = it
                }
            }

            launch {
                for (message in bluetoothManager.messageChannel) {
                    val trimmedMessage = message.trim()
                    Log.d("GeoBeacon", "Message received: $trimmedMessage")

                    when (trimmedMessage) {
                        "Spatna odpoved. Zkuste to prosim znovu." -> updateLastAnswer(AnswerStatus.ANSWER_WRONG)
                        "Spravne!" -> updateLastAnswer(AnswerStatus.ANSWER_CORRECT)
                        "Jste na konci dialogu. Pro novy pruchod se odpojte a znovu pripojte." -> finishConversation()
                        else -> addMessage(trimmedMessage)
                    }
                }
            }
        }
    }

    fun initConversation() {
        viewModelScope.launch {
            semaphore.withPermit {
                Log.d("GeoBeacon", "Initializing conversation with ${deviceName.value}")
                loadMessages()
                if (lastQuestion.closedQuestion) {
                    for (answer in lastQuestion.answers) {
                        if (answer.status == AnswerStatus.ANSWER_PENDING) {
                            repository.updateAnswer(
                                answer.copy(status = AnswerStatus.ANSWER_UNANSWERED),
                                lastQuestion.id
                            )
                        }
                    }
                }
            }
        }
    }

    fun addMessage(message: String) {
        viewModelScope.launch {
            semaphore.withPermit {
                try {
                    if (_conversation.value.messages.lastOrNull()?.question != message) {
                        val splitQuestion = message.split("\n")
                        val isClosedQuestion = splitQuestion.count() > 1
                        val regex = Regex("\\d\\) ")
                        val messageData = MessageData(
                            question = message,
                            closedQuestion = isClosedQuestion,
                            answers = if (isClosedQuestion) {
                                splitQuestion.subList(1, splitQuestion.size).map { answer ->
                                    MessageAnswer(answer.split(regex)[1], AnswerStatus.ANSWER_UNANSWERED)
                                }
                            } else {
                                emptyList()
                            }
                        )
                        repository.insertMessage(messageData, deviceAddress!!)
                        if (messageData.isQuestion()) {
                            lastQuestion = messageData
                        }
                        loadMessages()
                    }
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to add message", e)
                }
            }
        }
    }

    fun addAnswer(answer: String) {
        viewModelScope.launch {
            try {
                bluetoothManager.writeCharacteristic(
                    bluetoothManager.WUART_SERVICE_UUID,
                    bluetoothManager.WUART_CHARACTERISTIC_UUID,
                    (answer + "\n").toByteArray()
                )
                if (lastQuestion.closedQuestion) {
                    updateLastAnswer(AnswerStatus.ANSWER_PENDING, answer.toInt() - 1)
                } else {
                    val newAnswer = MessageAnswer(answer, AnswerStatus.ANSWER_PENDING)
                    semaphore.withPermit {
                        repository.insertAnswer(newAnswer, lastQuestion.id)
                    }
                }
                loadMessages()
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to add answer", e)
            }
        }
    }

    fun updateLastAnswer(newState: AnswerStatus, answerNum: Int = -1) {
        viewModelScope.launch {
            semaphore.withPermit {
                try {
                    Log.d("GeoBeacon", "Updating last answer to $newState")
                    if (answerNum > -1) {
                        lastAnswerNum = answerNum
                    }
                    val toUpdate = if (lastQuestion.closedQuestion) {
                        lastQuestion.answers[lastAnswerNum]
                    } else {
                        lastQuestion.answers.last()
                    }
                    repository.updateAnswer(
                        toUpdate.copy(status = newState),
                        lastQuestion.id
                    )
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to update last answer", e)
                }
            }
        }
    }

    fun finishConversation() {
        viewModelScope.launch {
            semaphore.withPermit {
                try {
                    _conversation.value = _conversation.value.copy(finished = true)
                    repository.finishConversation(_conversation.value.id)
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to finish conversation", e)
                }
            }
        }
    }

    fun resetConversation() {
        viewModelScope.launch {
            bluetoothManager.disconnect()
            _conversation.value = ConversationData()
            _deviceName.value = null
            lastQuestion = MessageData()
            lastAnswerNum = -1
            bluetoothManager.startScan()
            Log.d("GeoBeacon", "Resetting conversation")
        }
    }

    private suspend fun loadMessages() {
        try {
            val loadedConversation = if (!_conversation.value.finished) {
                Log.d("GeoBeacon", "Loading messages for ${deviceName.value} with address $deviceAddress")
                repository.getLastConversation(deviceAddress!!, deviceName.value!!)
            } else {
                repository.getConversation(_conversation.value.id)
            }
            _conversation.value = loadedConversation
            Log.d("GeoBeacon", "Loaded ${loadedConversation.messages.size} messages")

            val _lastQuestion = loadedConversation.getLastQuestion()
            if (_lastQuestion != null) {
                lastQuestion = _lastQuestion
            }
        } catch (e: Exception) {
            Log.e("GeoBeacon", "Failed to load messages", e)
        }
    }

    companion object {
        fun Factory(repository: ChatRepository, bluetoothManager: BluetoothConnectionManager): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatViewModel(repository, bluetoothManager)
            }
        }
    }
}