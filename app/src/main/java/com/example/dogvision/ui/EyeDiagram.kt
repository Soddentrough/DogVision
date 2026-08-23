package com.example.dogvision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dogvision.R
import com.example.dogvision.model.AnimalVisionProfile
import com.example.dogvision.model.PupilType

@Composable
fun EyeDiagram(
    animal: AnimalVisionProfile,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.eye_anatomy_comparison),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.human), style = MaterialTheme.typography.labelMedium, color = Color.White)
                EyeDrawing(animal = null, modifier = Modifier.size(100.dp))
                Text(text = stringResource(R.string.fovea_detail), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(animal.nameRes), style = MaterialTheme.typography.labelMedium, color = animal.primaryColor)
                EyeDrawing(animal = animal, modifier = Modifier.size(100.dp))
                val label = if (animal.hasTapetum) stringResource(R.string.tapetum_night_vision) else animal.pupilType.name
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EyeDrawing(animal: AnimalVisionProfile?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width * 0.4f

        if (animal?.pupilType == PupilType.COMPOUND_HEXAGONAL) {
            // Compound Eye (Bee): Ommatidia Facet Array
            drawCircle(Color.LightGray.copy(alpha = 0.5f), radius = radius, center = center, style = Stroke(width = 2f))
            val step = radius * 0.35f
            for (dx in -2..2) {
                for (dy in -2..2) {
                    val pos = Offset(center.x + dx * step, center.y + dy * step + (if (dx % 2 != 0) step * 0.5f else 0f))
                    if ((pos - center).getDistance() < radius * 0.85f) {
                        drawCircle(animal.primaryColor.copy(alpha = 0.7f), radius = step * 0.4f, center = pos, style = Stroke(width = 1.5f))
                    }
                }
            }
            return@Canvas
        }

        // Sclera
        drawCircle(Color.LightGray, radius = radius, center = center, style = Stroke(width = 3f))

        // Cornea
        drawArc(
            color = Color.Cyan.copy(alpha = 0.35f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = size * 0.8f,
            style = Stroke(width = 3f)
        )

        // Lens
        val lensWidth = radius * 0.28f
        val lensHeight = radius * 0.75f
        drawOval(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(center.x + radius * 0.1f, center.y - lensHeight / 2),
            size = Size(lensWidth, lensHeight),
            style = Stroke(width = 2f)
        )

        // Pupil / Iris representation
        val pupilColor = Color.Black
        when (animal?.pupilType) {
            PupilType.HORIZONTAL_SLIT -> {
                // Deer: Horizontal Slit Pupil
                drawOval(
                    color = animal.primaryColor,
                    topLeft = Offset(center.x + radius * 0.15f, center.y - radius * 0.12f),
                    size = Size(lensWidth * 1.4f, lensWidth * 0.5f)
                )
            }
            PupilType.VERTICAL_SLIT -> {
                // Cat / Snake: Vertical Slit Pupil
                drawOval(
                    color = animal.primaryColor,
                    topLeft = Offset(center.x + radius * 0.22f, center.y - lensHeight * 0.45f),
                    size = Size(lensWidth * 0.4f, lensHeight * 0.9f)
                )
            }
            PupilType.TUBULAR_DOUBLE_FOVEA -> {
                // Bird: Dual Foveal points
                drawCircle(Color.Magenta, radius = 4f, center = Offset(center.x - radius * 0.9f, center.y - radius * 0.3f))
                drawCircle(Color.Magenta, radius = 4f, center = Offset(center.x - radius * 0.75f, center.y + radius * 0.35f))
            }
            else -> {
                // Circular pupil / Human / Dog
                drawCircle(pupilColor, radius = radius * 0.18f, center = Offset(center.x + radius * 0.2f, center.y))
            }
        }

        // Retina Arc
        val retinaColor = if (animal != null) animal.primaryColor.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.4f)
        drawArc(
            color = retinaColor,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = size * 0.8f,
            style = Stroke(width = 6f)
        )

        if (animal?.hasTapetum == true) {
            // Tapetum Lucidum (Retroreflective choroid backing)
            val tapetumColor = if (animal.id == "deer") Color(0xFF00E5FF) else Color.Green.copy(alpha = 0.6f)
            drawArc(
                color = tapetumColor,
                startAngle = 135f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.06f, center.y - radius * 1.06f),
                size = size * 0.848f,
                style = Stroke(width = 2.5f)
            )
        } else if (animal == null) {
            // Human Fovea Centralis point
            drawCircle(Color.Red, radius = 4f, center = Offset(center.x - radius * 0.92f, center.y))
        }

        // Snake Pit Organ indicator
        if (animal?.id == "snake") {
            drawArc(
                color = Color(0xFFFF3D00),
                startAngle = -20f,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(center.x + radius * 0.6f, center.y - radius * 0.3f),
                size = Size(radius * 0.6f, radius * 0.6f),
                style = Stroke(width = 3f)
            )
        }
    }
}
