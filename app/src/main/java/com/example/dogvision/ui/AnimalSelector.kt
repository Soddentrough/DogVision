package com.example.dogvision.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dogvision.model.AnimalRegistry
import com.example.dogvision.model.AnimalVisionProfile

@Composable
fun AnimalSelector(
    selectedAnimal: AnimalVisionProfile,
    onAnimalSelected: (AnimalVisionProfile) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimalRegistry.ALL_ANIMALS.forEach { animal ->
            val isSelected = animal.id == selectedAnimal.id
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) animal.primaryColor.copy(alpha = 0.9f)
                else Color.White.copy(alpha = 0.08f),
                label = "animal_btn_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.Black else Color.White,
                label = "animal_btn_text"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) animal.primaryColor else Color.White.copy(alpha = 0.2f),
                label = "animal_btn_border"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAnimalSelected(animal)
                    }
                    .padding(
                        horizontal = if (compact) 12.dp else 16.dp,
                        vertical = if (compact) 6.dp else 10.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(animal.nameRes),
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
                    )
                    if (!compact) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color.Black.copy(alpha = 0.2f)
                                    else animal.primaryColor.copy(alpha = 0.3f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = animal.visualAcuity,
                                color = if (isSelected) Color.Black else animal.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
