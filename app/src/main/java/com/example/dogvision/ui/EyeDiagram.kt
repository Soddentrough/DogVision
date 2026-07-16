package com.example.dogvision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun EyeDiagram(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Eye Anatomy Comparison", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Human", style = MaterialTheme.typography.labelMedium)
                EyeDrawing(isDog = false, modifier = Modifier.size(120.dp))
                Text("Fovea (High Detail)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Dog", style = MaterialTheme.typography.labelMedium)
                EyeDrawing(isDog = true, modifier = Modifier.size(120.dp))
                Text("Tapetum (Night Vision)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EyeDrawing(isDog: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width * 0.4f
        
        // Sclera
        drawCircle(Color.LightGray, radius = radius, center = center, style = Stroke(width = 4f))
        
        // Cornea
        drawArc(
            color = Color.Cyan.copy(alpha = 0.3f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = size * 0.8f,
            style = Stroke(width = 4f)
        )
        
        // Lens
        val lensWidth = radius * 0.3f
        val lensHeight = radius * 0.8f
        drawOval(
            color = Color.White,
            topLeft = Offset(center.x + radius * 0.1f, center.y - lensHeight / 2),
            size = androidx.compose.ui.geometry.Size(lensWidth, lensHeight),
            style = Stroke(width = 2f)
        )

        // Retina
        drawArc(
            color = if (isDog) Color.Yellow.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.3f),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = size * 0.8f,
            style = Stroke(width = 8f)
        )
        
        if (isDog) {
            // Tapetum Lucidum
            drawArc(
                color = Color.Green.copy(alpha = 0.5f),
                startAngle = 135f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.05f, center.y - radius * 1.05f),
                size = size * 0.84f,
                style = Stroke(width = 2f)
            )
        } else {
            // Fovea
            drawCircle(Color.Red, radius = 5f, center = Offset(center.x - radius, center.y))
        }
    }
}
