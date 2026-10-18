package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.R
import com.salco.dragonfly.core.designsystem.theme.DfTheme

@Composable
fun DfSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = DfTheme.colors.accent,
            checkedThumbColor = DfTheme.colors.onAccent,
            uncheckedTrackColor = DfTheme.colors.surfaceMuted,
            uncheckedBorderColor = DfTheme.colors.border,
            uncheckedThumbColor = DfTheme.colors.textTertiary,
        ),
    )
}

/** A label (and optional description) with a switch on the right. */
@Composable
fun DfSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = DfTheme.spacing.m)) {
            DfText(label, tone = if (enabled) DfTone.Primary else DfTone.Tertiary)
            description?.let { DfText(it, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary) }
        }
        DfSwitch(checked, onCheckedChange, enabled)
    }
}

@Composable
fun DfCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = DfTheme.colors.accent, uncheckedColor = DfTheme.colors.textTertiary),
        )
        DfText(label, style = DfTheme.typography.bodySmall)
    }
}

/** A row of mutually exclusive options in a muted track, the selected one raised. */
@Composable
fun <T> DfSegmentedControl(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(DfTheme.spacing.radiusSmall)
    val inner = RoundedCornerShape(DfTheme.spacing.radiusSmall - 2.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(DfTheme.colors.surfaceMuted, outer)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(inner)
                    .let {
                        if (isSelected) it.background(DfTheme.colors.surface, inner).border(1.dp, DfTheme.colors.border, inner) else it
                    }
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                DfText(
                    optionLabel(option),
                    style = DfTheme.typography.label,
                    tone = if (isSelected) DfTone.Primary else DfTone.Secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A pill that can be on or off; [trailingDropdown] adds a small arrow for chips that open a menu. */
@Composable
fun DfChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingDropdown: Boolean = false,
    onRemove: (() -> Unit)? = null,
    removeDescription: String? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(34.dp)
            .clip(shape)
            .background(if (selected) DfTheme.colors.textPrimary else DfTheme.colors.surface, shape)
            .border(BorderStroke(1.dp, if (selected) DfTheme.colors.textPrimary else DfTheme.colors.border), shape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = if (trailingDropdown || onRemove != null) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tone = if (selected) DfTheme.colors.background else DfTheme.colors.textPrimary
        DfText(label, style = DfTheme.typography.label, color = tone, maxLines = 1)
        if (trailingDropdown) DfIcon(Icons.Filled.ArrowDropDown, null, tint = tone, modifier = Modifier.size(20.dp))
        if (onRemove != null) {
            DfIcon(
                Icons.Filled.Close,
                removeDescription,
                tint = tone,
                modifier = Modifier.padding(start = 4.dp).size(16.dp).clickable(onClick = onRemove),
            )
        }
    }
}

/** A chip that opens a menu of [options]; picking "all" (null) clears the choice. */
@Composable
fun <T> DfMenuChip(
    label: String,
    selected: Boolean,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        DfChip(label, selected, onClick = { expanded = true }, trailingDropdown = true)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = DfTheme.colors.surface) {
            DropdownMenuItem(
                text = { DfText(stringResource(R.string.core_designsystem_all), tone = DfTone.Secondary) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { DfText(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** A horizontally scrolling row of chips. */
@Composable
fun DfChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = DfTheme.spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.s),
    ) { content() }
}

/** A thin rounded progress bar; values above 1 are drawn full. */
@Composable
fun DfProgressBar(progress: Float, modifier: Modifier = Modifier, tone: DfTone = DfTone.Accent) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
        color = tone.color(),
        trackColor = DfTheme.colors.surfaceMuted,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** "‹ Ottobre 2026 ›" to move between periods. */
@Composable
fun DfPeriodSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousDescription: String,
    nextDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = DfTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
        DfIconButton(Icons.AutoMirrored.Filled.ArrowBack, previousDescription, onPrevious, tone = DfTone.Secondary)
        DfText(label, style = DfTheme.typography.subtitle, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        DfIconButton(Icons.AutoMirrored.Filled.ArrowForward, nextDescription, onNext, tone = DfTone.Secondary)
    }
}

/** A small label above a value, used in summary rows. */
@Composable
fun DfStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: DfTone = DfTone.Primary,
    alignEnd: Boolean = false,
) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        DfText(label, style = DfTheme.typography.caption, tone = DfTone.Secondary)
        DfText(value, style = DfTheme.typography.subtitle, tone = tone, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1)
    }
}

/** Underlined text tabs that scroll horizontally. */
@Composable
fun DfTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = DfTheme.spacing.screen - 8.dp),
        ) {
            titles.forEachIndexed { index, title ->
                Column(
                    Modifier
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DfText(
                        title,
                        style = DfTheme.typography.label,
                        tone = if (index == selected) DfTone.Primary else DfTone.Tertiary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(if (index == selected) DfTheme.colors.textPrimary else androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }
        DfDivider()
    }
}

/** Removable tag chips laid out on as many lines as needed. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun DfChipFlow(
    labels: List<String>,
    onRemove: (String) -> Unit,
    removeDescription: (String) -> String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(DfTheme.spacing.s),
    ) {
        labels.forEach { label ->
            DfChip(label, selected = false, onClick = { onRemove(label) }, onRemove = { onRemove(label) }, removeDescription = removeDescription(label))
        }
    }
}
