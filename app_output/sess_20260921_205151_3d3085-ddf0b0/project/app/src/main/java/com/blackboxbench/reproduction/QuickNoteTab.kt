package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickNoteTab() {
    var text by remember { mutableStateOf(Store.read("QuickNote.md")) }

    Column(Modifier.fillMaxSize().background(EditorBg)) {
        EditorTopBar(onSave = { Store.write("QuickNote.md", text) })
        BasicTextField(
            value = text,
            onValueChange = {
                val formatted = autoFormatAppend(text, it)
                text = formatted
                Store.write("QuickNote.md", formatted)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF222222)),
        )
        EditorFormatBar(text = text, onChange = {
            text = it
            Store.write("QuickNote.md", it)
        })
    }
}
