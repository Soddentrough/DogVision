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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dogvision.R
import com.example.dogvision.model.AnimalVisionProfile
import com.example.dogvision.model.ConeData
import kotlin.math.exp
import kotlin.math.pow

@Composable
fun WavelengthComparison(
    animal: AnimalVisionProfile,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Human Column
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.human_trichromat),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            EyeDrawing(animal = null, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(8.dp))
            SingleSensitivityGraph(
                cones = listOf(
                    ConeData(440f, 20f, Color(0xFF3F51B5), "S (440nm)"),
                    ConeData(535f, 30f, Color(0xFF4CAF50), "M (535nm)"),
                    ConeData(565f, 35f, Color(0xFFF44336), "L (565nm)")
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Human Spectrum Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.visible_spectrum_rgb),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.Gray
            )
        }

        // Animal Column
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${stringResource(animal.nameRes)} (${animal.visualAcuity})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = animal.primaryColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            EyeDrawing(animal = animal, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(8.dp))
            SingleSensitivityGraph(
                cones = animal.cones,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Animal Spectrum Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(
                        Brush.horizontalGradient(animal.spectrumBarColors),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.visible_spectrum_animal),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SingleSensitivityGraph(cones: List<ConeData>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF161616), RoundedCornerShape(12.dp))
            .padding(6.dp)
            .drawWithCache {
                val width = size.width
                val height = size.height

                // Wavelength range: 320nm to 720nm to accommodate UV and near-IR
                val minLambda = 320f
                val maxLambda = 720f
                val range = maxLambda - minLambda

                val xPos = { lambda: Float -> ((lambda - minLambda) / range) * width }
                val yPos = { sensitivity: Float -> height - (sensitivity * height * 0.85f) }

                // Precompute and cache the paths
                val cachedPaths = cones.map { cone ->
                    createPath(cone.peakWavelengthNm, cone.sigma, minLambda.toInt(), maxLambda.toInt(), xPos, yPos) to cone.color
                }

                onDrawBehind {
                    // Draw horizontal baseline
                    drawLine(Color.DarkGray.copy(alpha = 0.4f), Offset(0f, height), Offset(width, height), strokeWidth = 1f)

                    // Draw reference grid lines
                    drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(0f, height * 0.33f), Offset(width, height * 0.33f), strokeWidth = 1f)
                    drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(0f, height * 0.66f), Offset(width, height * 0.66f), strokeWidth = 1f)

                    // Draw 400nm (UV cut threshold) marker line
                    val uvCutX = xPos(400f)
                    drawLine(
                        Color.White.copy(alpha = 0.2f),
                        Offset(uvCutX, 0f),
                        Offset(uvCutX, height),
                        strokeWidth = 1f
                    )

                    // Draw cached curves
                    cachedPaths.forEach { (path, color) ->
                        drawPath(path, color, style = Stroke(width = 2.5f))
                    }
                }
            }
    ) {}
}

private fun createPath(
    peak: Float,
    sigma: Float,
    minLambda: Int,
    maxLambda: Int,
    xPos: (Float) -> Float,
    yPos: (Float) -> Float
): Path {
    val path = Path()
    var isFirst = true
    for (lambda in minLambda..maxLambda step 2) {
        val s = exp(-0.5f * ((lambda.toFloat() - peak) / sigma).pow(2))
        if (isFirst) {
            path.moveTo(xPos(lambda.toFloat()), yPos(s))
            isFirst = false
        } else {
            path.lineTo(xPos(lambda.toFloat()), yPos(s))
        }
    }
    return path
}
