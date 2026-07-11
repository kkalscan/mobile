package ru.kkalscan.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import ru.kkalscan.app.components.KkalPrimaryButton
import ru.kkalscan.app.theme.KkalScanColors
import ru.kkalscan.app.theme.KkalScanDimens
import ru.kkalscan.domain.activity.EnergyProfile

@Composable
fun EnergyProfileCard(
    profile: EnergyProfile,
    dailyBmrKcal: Int,
    saved: Boolean,
    onSave: (EnergyProfile) -> Unit,
    onClearSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var weightText by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toInt().toString()) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toInt().toString()) }
    var ageText by remember(profile.ageYears) { mutableStateOf(profile.ageYears.toString()) }

    LaunchedEffect(saved) {
        if (saved) {
            kotlinx.coroutines.delay(2000)
            onClearSaved()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("energy-profile-card"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(KkalScanDimens.cardRadius),
        color = KkalScanColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, KkalScanColors.Outline.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Расход калорий", style = MaterialTheme.typography.titleMedium)
            Text(
                "Для расчёта базового метаболизма (BMR) и ходьбы. Формула Миффлина — Сан Жеора.",
                style = MaterialTheme.typography.bodySmall,
                color = KkalScanColors.OnSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Вес, кг") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("profile-weight"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Рост, см") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("profile-height"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Возраст") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("profile-age"),
                    singleLine = true,
                )
            }
            Text(
                "BMR ≈ $dailyBmrKcal ккал/день (в покое)",
                style = MaterialTheme.typography.bodyMedium,
                color = KkalScanColors.OnSurfaceVariant,
            )
            KkalPrimaryButton(
                text = if (saved) "Сохранено" else "Сохранить",
                onClick = {
                    val weight = weightText.toDoubleOrNull() ?: return@KkalPrimaryButton
                    val height = heightText.toDoubleOrNull() ?: return@KkalPrimaryButton
                    val age = ageText.toIntOrNull() ?: return@KkalPrimaryButton
                    onSave(EnergyProfile(weightKg = weight, heightCm = height, ageYears = age))
                },
                enabled = !saved,
                modifier = Modifier.fillMaxWidth().testTag("profile-save"),
            )
        }
    }
}
