package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherConditionCategory
import com.example.data.model.WeatherSafetyAdvice

/**
 * Centered Safety Reminder card faithfully matching the reference design:
 * - Cyan/blue info icon + "Safety Reminder" header
 * - Contextual 2-line safety advisory matching the active weather condition
 */
@Composable
fun WeatherSafetyCard(
    category: WeatherConditionCategory,
    isDay: Boolean = true,
    customAdvice: String? = null,
    modifier: Modifier = Modifier
) {
    val advice = customAdvice ?: WeatherSafetyAdvice.getReminder(category, isDay)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "ⓘ Safety Reminder"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Safety Reminder",
                tint = Color(0xFF67E8F9),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Safety Reminder",
                color = Color(0xFF67E8F9),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Two-line safety advice
        Text(
            text = advice,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal
        )
    }
}
