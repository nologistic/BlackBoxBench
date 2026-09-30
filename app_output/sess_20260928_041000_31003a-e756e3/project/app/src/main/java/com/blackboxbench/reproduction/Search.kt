package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (Task) -> Unit
) {
    val flat = mutableListOf<Pair<Task, Task?>>()
    Repo.tasks.forEach { t ->
        flat.add(t to null)
        t.subtasks.forEach { s -> flat.add(s to t) }
    }
    val results = if (query.isBlank()) emptyList()
    else flat.filter { (t, _) -> t.title.contains(query, true) || t.notes.contains(query, true) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        androidx.activity.compose.BackHandler { onBack() }
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconBack, AppColors.OnSurface, 22.dp) }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = AppColors.OnSurface),
                cursorBrush = SolidColor(AppColors.Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("搜索", fontSize = 17.sp, color = AppColors.SecondaryText)
                    inner()
                }
            )
            Box(
                Modifier.size(48.dp).clickable { onQuery("") },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconClose, AppColors.OnSurface, 20.dp) }
        }
        SectionDivider()
        LazyColumn(Modifier.fillMaxWidth()) {
            results.forEach { (t, parent) ->
                item(key = t.id) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(t) }
                            .padding(start = if (parent == null) 20.dp else 48.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityCheckbox(t.priority, t.isCompleted, size = 20.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                t.title,
                                fontSize = 16.sp,
                                color = if (t.isCompleted) AppColors.SecondaryText else AppColors.OnSurface,
                                textDecoration = if (t.isCompleted) TextDecoration.LineThrough else null
                            )
                            if (parent != null) {
                                Text("子任务 · ${parent.title}", fontSize = 12.sp, color = AppColors.SecondaryText)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
