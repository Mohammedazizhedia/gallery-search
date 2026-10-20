package com.gallerysearch.app.ui.theme

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class Tint(val bg: Color, val fg: Color)
data class Tints(val accent: Tint, val success: Tint, val warning: Tint, val pro: Tint, val danger: Tint)

private val lightTints = Tints(
    accent = Tint(Color(0xFFE6F1FB), Color(0xFF185FA5)),
    success = Tint(Color(0xFFEAF3DE), Color(0xFF3B6D11)),
    warning = Tint(Color(0xFFFAEEDA), Color(0xFF854F0B)),
    pro = Tint(Color(0xFFEEEDFE), Color(0xFF534AB7)),
    danger = Tint(Color(0xFFFCEBEB), Color(0xFFA32D2D)),
)
private val darkTints = Tints(
    accent = Tint(Color(0xFF0C447C), Color(0xFFB5D4F4)),
    success = Tint(Color(0xFF27500A), Color(0xFFC0DD97)),
    warning = Tint(Color(0xFF633806), Color(0xFFFAC775)),
    pro = Tint(Color(0xFF3C3489), Color(0xFFCECBF6)),
    danger = Tint(Color(0xFF791F1F), Color(0xFFF7C1C1)),
)

val LocalTints = staticCompositionLocalOf { lightTints }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val family = remember {
        runCatching { FontFamily(Typeface.createFromAsset(ctx.assets, "fonts/cairo.ttf")) }
            .getOrDefault(FontFamily.Default)
    }
    val typography = remember(family) {
        Typography(
            headlineMedium = TextStyle(fontFamily = family, fontSize = 22.sp, fontWeight = FontWeight.Medium),
            titleLarge = TextStyle(fontFamily = family, fontSize = 22.sp, fontWeight = FontWeight.Medium),
            titleMedium = TextStyle(fontFamily = family, fontSize = 15.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontFamily = family, fontSize = 15.sp),
            bodyMedium = TextStyle(fontFamily = family, fontSize = 14.sp),
            bodySmall = TextStyle(fontFamily = family, fontSize = 12.sp),
            labelMedium = TextStyle(fontFamily = family, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
    val scheme = if (dark) {
        darkColorScheme(primary = Color(0xFFB5D4F4), surfaceVariant = Color(0xFF2C2C2A))
    } else {
        lightColorScheme(primary = Color(0xFF185FA5), surfaceVariant = Color(0xFFF1EFE8))
    }
    CompositionLocalProvider(LocalTints provides if (dark) darkTints else lightTints) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
