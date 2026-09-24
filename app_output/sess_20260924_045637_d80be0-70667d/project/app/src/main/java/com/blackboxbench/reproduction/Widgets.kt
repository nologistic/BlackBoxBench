package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// menu host: menus are rendered by the root so they can float over any screen
// ---------------------------------------------------------------------------

data class MenuItemSpec(
    val text: String,
    val destructive: Boolean = false,
    val checked: Boolean? = null,
    val submenu: List<MenuItemSpec>? = null,
    val onClick: (() -> Unit)? = null
)

data class MenuSpec(val items: List<MenuItemSpec>, val anchor: Offset)

class MenuController {
    var current by mutableStateOf<MenuSpec?>(null)
    var submenu by mutableStateOf<Pair<MenuItemSpec, Offset>?>(null)

    fun show(items: List<MenuItemSpec>, anchor: Offset) {
        submenu = null
        current = MenuSpec(items, anchor)
    }

    fun dismiss() {
        current = null
        submenu = null
    }
}

val LocalMenu = staticCompositionLocalOf { MenuController() }

@Composable
fun MenuHost(controller: MenuController) {
    val spec = controller.current ?: return
    val colors = LocalViColors.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controller.dismiss() }
        )
        val itemHeight = 48.dp
        val menuWidth = 244.dp
        val anchorX = with(density) { spec.anchor.x.toDp() }
        val anchorY = with(density) { spec.anchor.y.toDp() }
        val menuHeight = itemHeight * spec.items.size
        val left = (anchorX - menuWidth + 48.dp)
            .coerceIn(8.dp, (maxWidth - menuWidth - 8.dp).coerceAtLeast(8.dp))
        val top = if (anchorY + menuHeight + 16.dp > maxHeight) {
            (maxHeight - menuHeight - 16.dp).coerceAtLeast(8.dp)
        } else {
            anchorY
        }
        Column(
            modifier = Modifier
                .offset(x = left, y = top)
                .width(menuWidth)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.card)
        ) {
            spec.items.forEach { item ->
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                if (item.submenu != null) {
                                    controller.submenu = item to Offset(spec.anchor.x, spec.anchor.y)
                                } else {
                                    controller.dismiss()
                                    item.onClick?.invoke()
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.text,
                            color = if (item.destructive) colors.accent else colors.text,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        when {
                            item.checked == true -> Ic("check", 18.dp, colors.icon)
                            item.submenu != null -> Ic("chevron_down", 18.dp, colors.icon)
                        }
                    }
                }
            }
        }
        controller.submenu?.let { (parent, _) ->
            val subItems = parent.submenu ?: emptyList()
            val parentIndex = spec.items.indexOf(parent).coerceAtLeast(0)
            val subHeight = itemHeight * subItems.size
            val subTop = (anchorY + itemHeight * (parentIndex + 1))
                .let { if (it + subHeight + 16.dp > maxHeight) (maxHeight - subHeight - 16.dp).coerceAtLeast(8.dp) else it }
            val subLeft = (anchorX - 48.dp)
                .coerceIn(8.dp, (maxWidth - menuWidth - 8.dp).coerceAtLeast(8.dp))
            Column(
                modifier = Modifier
                    .offset(x = subLeft, y = subTop)
                    .width(menuWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.card)
            ) {
                subItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                controller.dismiss()
                                item.onClick?.invoke()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.text, color = colors.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (item.checked == true) Ic("check", 18.dp, colors.accent)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// generic pieces
// ---------------------------------------------------------------------------

@Composable
fun ViDivider(color: Color? = null) {
    val colors = LocalViColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color ?: colors.divider)
    )
}

@Composable
fun ViTopBar(
    title: String,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onOverflow: (() -> Unit)? = null,
    extra: @Composable RowScopeShim.() -> Unit = {}
) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.appBar)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButtonNoRipple("back", colors.appBarText, onBack)
        } else if (onMenu != null) {
            IconButtonNoRipple("menu", colors.appBarText, onMenu)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            color = colors.appBarText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        extra(RowScopeShim)
        if (onSearch != null) IconButtonNoRipple("search", colors.appBarText, onSearch)
        if (onOverflow != null) IconButtonNoRipple("more", colors.appBarText, onOverflow)
    }
}

object RowScopeShim

@Composable
fun IconButtonNoRipple(kind: String, tint: Color, onClick: () -> Unit, size: Dp = 26.dp) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Ic(kind, size, tint)
    }
}

@Composable
fun MenuDots(tint: Color, items: () -> List<MenuItemSpec>) {
    val menu = LocalMenu.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .onGloballyPositioned { pos = it.positionInRoot() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                menu.show(items(), pos)
            },
        contentAlignment = Alignment.Center
    ) {
        Ic("more", 22.dp, tint)
    }
}

@Composable
fun ViDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalViColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card)
                .padding(vertical = 8.dp)
        ) { content() }
    }
}

@Composable
fun DialogTitle(text: String) {
    val colors = LocalViColors.current
    Text(
        text = text,
        color = colors.text,
        fontSize = 22.sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 12.dp)
    )
}

@Composable
fun DialogText(text: String) {
    val colors = LocalViColors.current
    Text(
        text = text,
        color = colors.text,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun DialogActions(vararg buttons: Pair<String, () -> Unit>) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        buttons.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { action() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(label, color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun DialogRadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
                .padding(6.dp)
                .clip(CircleShape)
                .background(if (selected) colors.card else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (!selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.subtitle.copy(alpha = 0.4f))
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                )
            }
        }
        Spacer(Modifier.width(28.dp))
        Text(text, color = colors.text, fontSize = 17.sp)
    }
}

@Composable
fun DialogCheckRow(text: String, checked: Boolean, onClick: () -> Unit) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) colors.accent else Color.Transparent)
                .then(
                    if (checked) Modifier else Modifier.background(colors.divider)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Ic("check", 14.dp, Color.White)
        }
        Spacer(Modifier.width(28.dp))
        Text(text, color = colors.text, fontSize = 17.sp)
    }
}

@Composable
fun SectionHeader(text: String) {
    val colors = LocalViColors.current
    Text(
        text = text,
        color = colors.accent,
        fontSize = 15.sp,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = colors.subtitle, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accent.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun PlainRow(title: String, subtitle: String?, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = colors.subtitle, fontSize = 14.sp)
        }
        trailing?.invoke()
    }
}

@Composable
fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun ViSnackbarHost(message: String?, onDone: () -> Unit) {
    val colors = LocalViColors.current
    if (message == null) return
    LaunchedEffect(message) {
        delay(3000)
        onDone()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF323232))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// artwork placeholders
// ---------------------------------------------------------------------------

@Composable
fun VinylArt(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (size > 100.dp) 0.dp else 4.dp))
            .background(if (size > 100.dp) Color(0xFF1E1E1E) else Color(0xFF2B2B2B)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.86f)) {
            val r = this.size.minDimension / 2f
            drawCircle(Color(0xFF6E6E6E), radius = r, center = center)
            drawCircle(
                Color(0xFF8A8A8A), radius = r * 0.78f, center = center,
                style = Stroke(width = r * 0.08f)
            )
            drawCircle(
                Color(0xFF8A8A8A), radius = r * 0.58f, center = center,
                style = Stroke(width = r * 0.08f)
            )
            drawCircle(Color(0xFF3A3A3A), radius = r * 0.34f, center = center)
            drawCircle(Color(0xFFF50057), radius = r * 0.16f, center = center)
        }
    }
}

@Composable
fun PersonArt(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF2B2B2B)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.7f)) {
            val s = this.size.minDimension
            drawCircle(Color(0xFF8E8E8E), radius = s * 0.18f, center = Offset(center.x, center.y - s * 0.16f))
            drawArc(
                color = Color(0xFF8E8E8E),
                startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(center.x - s * 0.32f, center.y + s * 0.05f),
                size = Size(s * 0.64f, s * 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    playing: Boolean,
    showTrack: Boolean,
    onTap: () -> Unit,
    onMenu: () -> Unit,
    menuItems: () -> List<MenuItemSpec>,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected || isCurrent) colors.highlighted else Color.Transparent)
            .combinedClickable(onClick = onTap, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) VinylArt(46.dp) else VinylArt(46.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showTrack && song.track > 0) {
                    Text(
                        "${song.track}. ",
                        color = colors.subtitle,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = song.title,
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = songSubtitle(song),
                color = colors.subtitle,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MenuDots(colors.icon, menuItems)
    }
}

fun songSubtitle(song: Song): String = when {
    song.artist.isEmpty() -> song.album
    song.album.isEmpty() -> song.artist
    else -> "${song.artist} · ${song.album}"
}
