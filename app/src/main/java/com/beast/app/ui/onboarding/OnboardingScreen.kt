package com.beast.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateDpAsState
import com.beast.app.R

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val pageCount = 3

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with skip aligned to end
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            val skipDescription = stringResource(R.string.cd_skip_onboarding)
            TextButton(
                onClick = { onFinish() },
                modifier = Modifier.semantics { contentDescription = skipDescription }
            ) {
                Text("Пропустить")
            }
        }

        // Content area
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val title = when (page) {
                0 -> "Добро пожаловать в BeastApp"
                1 -> "Следите за прогрессом и личными рекордами"
                else -> "Планируйте тренировки и достигайте целей"
            }
            val subtitle = when (page) {
                0 -> "Body Beast — программа для увеличения силы и массы"
                1 -> "Записывайте подходы, вес и повторы для точного учёта"
                else -> "Выберите программу и начните прямо сейчас"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, fontSize = 22.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // Dots indicator (clickable)
        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until pageCount) {
                val isSelected = page == i
                val currentSlideDescription = stringResource(R.string.cd_onboarding_current_slide, i + 1)
                val goToSlideDescription = stringResource(R.string.cd_onboarding_go_to_slide, i + 1)
                val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                val dotSize = if (isSelected) 12.dp else 8.dp
                // animated size
                val animatedSize by animateDpAsState(targetValue = dotSize)

                // make a larger hit target while keeping the visual dot small
                Box(
                    modifier = Modifier
                        .size(36.dp) // touch target
                        .clickable { page = i }
                        .semantics {
                            contentDescription = if (isSelected) {
                                currentSlideDescription
                            } else {
                                goToSlideDescription
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(animatedSize)
                            .background(color, CircleShape)
                    )
                }
            }
        }

        // Navigation buttons (bottom)
        Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val nextDescription = stringResource(R.string.cd_onboarding_next)
            val startDescription = stringResource(R.string.cd_onboarding_start)
            Button(
                onClick = {
                    if (page + 1 < pageCount) page++
                    else onFinish()
                },
                modifier = Modifier.semantics {
                    contentDescription = if (page + 1 < pageCount) nextDescription else startDescription
                }
            ) {
                Text(if (page + 1 < pageCount) "Далее" else "Начать")
            }
        }
    }
}
