package com.example.dogvision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.pow

data class ConeData(val peak: Float, val sigma: Float, val color: Color)

@Composable
fun WavelengthComparison(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Human Column
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Human (Trichromat)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            EyeDrawing(isDog = false, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            SingleSensitivityGraph(
                cones = listOf(
                    ConeData(440f, 20f, Color(0xFF3F51B5)), // S Cone (Blue)
                    ConeData(535f, 30f, Color(0xFF4CAF50)), // M Cone (Green)
                    ConeData(565f, 35f, Color(0xFFF44336))  // L Cone (Red)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Human Spectrum Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Red, Color.Green, Color.Blue)),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Visible Spectrum (RGB)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        // Dog Column
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Dog (Dichromat)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            EyeDrawing(isDog = true, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            SingleSensitivityGraph(
                cones = listOf(
                    ConeData(435f, 20f, Color(0xFF00BCD4)),  // S Cone (Blue)
                    ConeData(555f, 35f, Color(0xFFFFEB3B))   // L Cone (Yellow/Green)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Dog Spectrum Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF3F51B5), Color(0xFFFFD700))),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Visible Spectrum (B/Y)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SingleSensitivityGraph(cones: List<ConeData>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF161616), RoundedCornerShape(12.dp))
            .padding(8.dp)
            .drawWithCache {
                val width = size.width
                val height = size.height
                
                val xPos = { lambda: Float -> (lambda - 400f) / 300f * width }
                val yPos = { sensitivity: Float -> height - (sensitivity * height * 0.85f) }

                // Precompute and cache the paths
                val cachedPaths = cones.map { cone ->
                    createPath(cone.peak, cone.sigma, xPos, yPos) to cone.color
                }

                onDrawBehind {
                    // Draw horizontal baseline
                    drawLine(Color.DarkGray.copy(alpha = 0.4f), Offset(0f, height), Offset(width, height), strokeWidth = 1f)

                    // Draw reference grid lines
                    drawLine(Color.DarkGray.copy(alpha = 0.2f), Offset(0f, height * 0.33f), Offset(width, height * 0.33f), strokeWidth = 1f)
                    drawLine(Color.DarkGray.copy(alpha = 0.2f), Offset(0f, height * 0.66f), Offset(width, height * 0.66f), strokeWidth = 1f)

                    // Draw cached curves
                    cachedPaths.forEach { (path, color) ->
                        drawPath(path, color, style = Stroke(width = 3f))
                    }
                }
            }
    ) {}
}

private fun createPath(peak: Float, sigma: Float, xPos: (Float) -> Float, yPos: (Float) -> Float): Path {
    val path = Path()
    for (lambda in 400..700) {
        val s = exp(-0.5f * ((lambda.toFloat() - peak) / sigma).pow(2))
        if (lambda == 400) path.moveTo(xPos(lambda.toFloat()), yPos(s))
        else path.lineTo(xPos(lambda.toFloat()), yPos(s))
    }
    return path
}
