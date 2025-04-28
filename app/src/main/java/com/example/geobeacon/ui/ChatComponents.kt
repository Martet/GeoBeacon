package com.example.geobeacon.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.geobeacon.R
import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.MessageData

@Composable
fun ChatMessage(message: MessageData, onAnswer: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message.question,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        for (answer in message.answers) {
            ChatAnswer(answer)
        }
        if (message.last) {
            val textField = rememberSaveable {
                mutableStateOf("")
            }
            val focusManager = LocalFocusManager.current
            fun onSubmit() {
                onAnswer(textField.value)
                textField.value = ""
                focusManager.clearFocus()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            ) {
                OutlinedTextField(
                    value = textField.value,
                    onValueChange = { textField.value = it },
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.type_answer)) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(end = 8.dp)
                )
                IconButton(
                    onClick = { onSubmit() },
                    modifier = Modifier.size(32.dp)
                ){
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.Blue
                    )
                }
            }
        } else if (message.answers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChatAnswer(answer: MessageAnswer) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = when (answer.status) {
                AnswerStatus.ANSWER_CORRECT -> Icons.Default.Check
                AnswerStatus.ANSWER_WRONG -> Icons.Default.Close
                AnswerStatus.ANSWER_PENDING -> Icons.Default.Refresh
            },
            tint = when (answer.status) {
                AnswerStatus.ANSWER_CORRECT -> Color.Green
                AnswerStatus.ANSWER_WRONG -> Color.Red
                AnswerStatus.ANSWER_PENDING -> Color.Blue
            },
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Text(answer.text)
    }
}