package com.example.geobeacon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geobeacon.R
import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.MessageData
import com.example.geobeacon.ui.theme.GeoBeaconTheme

@Composable
fun ChatMessage(message: MessageData, enableAnswer: Boolean, onAnswer: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (message.closedQuestion) message.question.split("\n")[0] else message.question,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (message.closedQuestion) {
            ChatAnswerButtons(message.answers, enableAnswer = enableAnswer && message.last, onAnswer)
        } else {
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
                        enabled = enableAnswer,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(end = 8.dp)
                    )
                    IconButton(
                        enabled = enableAnswer && textField.value.isNotEmpty(),
                        onClick = { onSubmit() },
                        modifier = Modifier.size(32.dp)
                    ){
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if(enableAnswer) Color.Blue else Color.Gray
                        )
                    }
                }
            } else if (message.answers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                AnswerStatus.ANSWER_UNANSWERED -> Icons.Default.Warning
            },
            tint = when (answer.status) {
                AnswerStatus.ANSWER_CORRECT -> Color.Green
                AnswerStatus.ANSWER_WRONG -> Color.Red
                AnswerStatus.ANSWER_PENDING -> Color.Blue
                AnswerStatus.ANSWER_UNANSWERED -> Color(255, 165, 0, 255)
            },
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Text(answer.text)
    }
}

@Composable
fun ChatAnswerButtons(answers: List<MessageAnswer>, enableAnswer: Boolean, onClick: (String) -> Unit) {
    Column {
        answers.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { answer ->
                    val enabled = enableAnswer && answer.status == AnswerStatus.ANSWER_UNANSWERED
                    Button(
                        enabled = enabled,
                        onClick = { onClick((answers.indexOf(answer) + 1).toString()) },
                        border = BorderStroke(
                            width = 2.dp,
                            color = when (answer.status) {
                                AnswerStatus.ANSWER_CORRECT -> Color.Green
                                AnswerStatus.ANSWER_WRONG -> Color.Red
                                AnswerStatus.ANSWER_PENDING -> Color.Blue
                                AnswerStatus.ANSWER_UNANSWERED -> Color.DarkGray
                            }
                        ),
                        shape = ShapeDefaults.Medium,
                        /*colors = ButtonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black,
                            disabledContentColor = if (answer.status == AnswerStatus.ANSWER_CORRECT) Color.Black else Color.DarkGray,
                            disabledContainerColor = if (answer.status == AnswerStatus.ANSWER_CORRECT) Color.LightGray else Color.Gray
                        ),*/
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 12.dp,
                            hoveredElevation = 12.dp,
                            disabledElevation = 0.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            //.padding(vertical = 4.dp)
                    ) {
                        Text(
                            answer.text,
                            style = MaterialTheme.typography.bodyLarge,
                            //color = if (enabled) Color.Black else Color.DarkGray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
private fun ChatMessagePreview() {
    var answer by remember {
        mutableStateOf("")
    }

    var anoStatus by remember {
        mutableStateOf(AnswerStatus.ANSWER_UNANSWERED)
    }
    var neStatus by remember {
        mutableStateOf(AnswerStatus.ANSWER_UNANSWERED)
    }
    var moznaStatus by remember {
        mutableStateOf(AnswerStatus.ANSWER_UNANSWERED)
    }

    when (answer) {
        "1" -> anoStatus = AnswerStatus.ANSWER_CORRECT
        "2" -> neStatus = AnswerStatus.ANSWER_WRONG
        "3" -> moznaStatus = AnswerStatus.ANSWER_PENDING
    }

    GeoBeaconTheme(darkTheme = false, dynamicColor = false) {
        ChatMessage(
            message = MessageData(
                question = "Je voda mokra?\n1) Ano\n2) ne\n3) Mozna",
                answers = listOf(
                    MessageAnswer(text = "Ano", status = anoStatus),
                    MessageAnswer("ne", neStatus),
                    MessageAnswer("Mozna", moznaStatus)
                ),
                closedQuestion = true,
                last = true
            ),
            enableAnswer = true,
            onAnswer = { answer = it }
        )
    }
}