package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top Navigation and City Header faithfully replicating the reference design:
 * - Minimalist quad-grid-plus icon on the left (opens city drawer & weather showcase)
 * - Minimalist hexagon-dot icon on the right (opens settings, units, refresh)
 * - Centered City name with GPS indicator
 * - Formatted date subtitle ("Friday, January 3, 2024" style)
 */
@Composable
fun WeatherTopHeader(
    location: WeatherLocation,
    onOpenMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current date formatted as "Friday, January 3, 2024"
    val dateText = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        sdf.format(Date())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left Action Icon: Quad Grid with Plus (Drawer & Search)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp)
                ) { onOpenMenu() },
            contentAlignment = Alignment.Center
        ) {
            QuadGridPlusIcon(modifier = Modifier.size(24.dp))
        }

        // Center City & Date
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = location.name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                if (location.isGpsLocation) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS Location",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dateText,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp
            )
        }

        // Right Action Icon: Hexagon with Dot (Settings & Units)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 22.dp)
                ) { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            HexagonDotIcon(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Pixel-accurate Quad Grid Plus icon from the reference screenshot
 */
@Composable
fun QuadGridPlusIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val squareSize = w * 0.32f
        val gap = w * 0.16f
        val strokeWidth = 1.6f
        val cornerRadius = CornerRadius(2.5f, 2.5f)

        // Top-left square
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(squareSize, squareSize),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // Bottom-left square
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, squareSize + gap),
            size = Size(squareSize, squareSize),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // Bottom-right square
        drawRoundRect(
            color = color,
            topLeft = Offset(squareSize + gap, squareSize + gap),
            size = Size(squareSize, squareSize),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // Top-right tiny Plus symbol
        val plusCenterX = squareSize + gap + (squareSize / 2f)
        val plusCenterY = squareSize / 2f
        val plusArm = squareSize * 0.42f

        // Horizontal bar
        drawLine(
            color = color,
            start = Offset(plusCenterX - plusArm, plusCenterY),
            end = Offset(plusCenterX + plusArm, plusCenterY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Vertical bar
        drawLine(
            color = color,
            start = Offset(plusCenterX, plusCenterY - plusArm),
            end = Offset(plusCenterX, plusCenterY + plusArm),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Pixel-accurate Hexagon with Dot icon from the reference screenshot
 */
@Composable
fun HexagonDotIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f
        val radius = (w / 2f) * 0.88f
        val strokeWidth = 1.6f

        // Regular hexagon path
        val path = Path()
        for (i in 0 until 6) {
            val angleRad = (i * 60f + 30f) * PI / 180f
            val px = centerX + cos(angleRad).toFloat() * radius
            val py = centerY + sin(angleRad).toFloat() * radius
            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Center dot
        drawCircle(
            color = color,
            radius = strokeWidth * 1.2f,
            center = Offset(centerX, centerY)
        )
    }
}
