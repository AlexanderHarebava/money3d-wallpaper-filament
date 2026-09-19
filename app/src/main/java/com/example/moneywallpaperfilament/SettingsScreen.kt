package com.example.moneywallpaperfilament

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private const val MIN_BILLS = 50f
private const val MAX_BILLS = 500f
private const val BILL_STEP = 10f


@Composable
fun SettingsDialog(
    settings: WallpaperSettings,
    onSettingsChange: (WallpaperSettings) -> Unit,
    onBillCountCommit: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))


                SectionTitle(stringResource(R.string.section_sun))
                SwitchRow(stringResource(R.string.setting_show_sun), settings.sunVisible) {
                    onSettingsChange(settings.copy(sunVisible = it))
                }
                SliderRow(
                    label = stringResource(R.string.setting_intensity),
                    value = settings.sunIntensity,
                    range = 0f..20f,
                    enabled = settings.sunVisible,
                    valueLabel = { "%.1f".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(sunIntensity = it)) }
                )
                SliderRow(
                    label = stringResource(R.string.setting_disk_size),
                    value = settings.sunRadiusDeg,
                    range = 0.5f..6f,
                    enabled = settings.sunVisible,
                    valueLabel = { "%.1f°".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(sunRadiusDeg = it)) }
                )
                SliderRow(
                    label = stringResource(R.string.setting_glow),
                    value = settings.sunGlow,
                    range = 0f..2f,
                    enabled = settings.sunVisible,
                    valueLabel = { "%.2f".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(sunGlow = it)) }
                )
                SectionDivider()


                SectionTitle(stringResource(R.string.section_bills))
                SliderRow(
                    label = stringResource(R.string.setting_count),
                    value = settings.billCount.toFloat(),
                    range = MIN_BILLS..MAX_BILLS,
                    steps = ((MAX_BILLS - MIN_BILLS) / BILL_STEP).toInt() - 1,
                    valueLabel = { it.roundToInt().toString() },
                    onValueChange = {
                        onSettingsChange(settings.copy(billCount = snapBillCount(it)))
                    },
                    onValueChangeFinished = onBillCountCommit
                )
                SliderRow(
                    label = stringResource(R.string.setting_fall_speed),
                    value = settings.fallSpeed,
                    range = 0.2f..3f,
                    valueLabel = { "%.2f×".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(fallSpeed = it)) }
                )
                SectionDivider()


                SectionTitle(stringResource(R.string.section_motion))
                SliderRow(
                    label = stringResource(R.string.setting_sway_intensity),
                    value = settings.swayIntensity,
                    range = 0f..2f,
                    valueLabel = { "%.2f×".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(swayIntensity = it)) }
                )
                SliderRow(
                    label = stringResource(R.string.setting_spin_intensity),
                    value = settings.spinIntensity,
                    range = 0f..2f,
                    valueLabel = { "%.2f×".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(spinIntensity = it)) }
                )
                SliderRow(
                    label = stringResource(R.string.setting_bend_deformation),
                    value = settings.bendIntensity,
                    range = 0f..2f,
                    valueLabel = { "%.2f×".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(bendIntensity = it)) }
                )
                SectionDivider()


                SectionTitle(stringResource(R.string.section_parallax))
                SwitchRow(
                    stringResource(R.string.setting_parallax_gyro),
                    settings.parallaxEnabled
                ) {
                    onSettingsChange(settings.copy(parallaxEnabled = it))
                }
                SliderRow(
                    label = stringResource(R.string.setting_sensitivity),
                    value = settings.parallaxSensitivity,
                    range = 0f..2f,
                    enabled = settings.parallaxEnabled,
                    valueLabel = { "%.2f×".format(it) },
                    onValueChange = { onSettingsChange(settings.copy(parallaxSensitivity = it)) }
                )
                SectionDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_reset_defaults))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_close))
                    }
                }

                Spacer(Modifier.height(16.dp))

                val authorLink = buildAnnotatedString {
                    append("Author: ")
                    withLink(LinkAnnotation.Url(url = "https://github.com/AlexanderHarebava")) {
                        append("link")
                    }
                }
                Text(
                    text = authorLink,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}


private fun snapBillCount(value: Float): Int =
    ((value / BILL_STEP).roundToInt() * BILL_STEP.toInt())
        .coerceIn(MIN_BILLS.toInt(), MAX_BILLS.toInt())

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String = { "%.2f".format(it) },
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel(value),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            steps = steps,
            enabled = enabled
        )
    }
}