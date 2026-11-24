package com.oorep.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oorep.android.data.model.RemedyGrade
import com.oorep.android.ui.theme.*

@Composable
fun RemedyChip(
    remedy: RemedyGrade,
    onClick: () -> Unit = {}
) {
    val (badgeBg, badgeText, fontStyle, fontWeight) = when (remedy.weight) {
        4 -> Quadruple(Grade4Color, Color.White, FontStyle.Normal, FontWeight.ExtraBold)
        3 -> Quadruple(Grade3Color, Color.White, FontStyle.Normal, FontWeight.Bold)
        2 -> Quadruple(Grade2Color, Color.White, FontStyle.Italic, FontWeight.SemiBold)
        else -> Quadruple(Grade1Color.copy(alpha = 0.2f), Grade1Color, FontStyle.Normal, FontWeight.Normal)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .padding(end = 6.dp, bottom = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = remedy.nameAbbrev,
                fontSize = 13.sp,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
            ) {
                Text(
                    text = remedy.weight.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
