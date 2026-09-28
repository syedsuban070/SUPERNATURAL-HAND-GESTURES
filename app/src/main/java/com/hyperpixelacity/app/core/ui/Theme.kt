package com.hyperpixelacity.app.core.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val palette = darkColorScheme(
    primary=Color(0xFFBDD4C7), onPrimary=Color(0xFF182C23),
    background=Color(0xFF151719), onBackground=Color(0xFFF5F3EF),
    surface=Color(0xFF202326), onSurface=Color(0xFFF5F3EF),
    surfaceVariant=Color(0xFF303538), onSurfaceVariant=Color(0xFFBAC1BD),
    outline=Color(0xFF737C77), secondary=Color(0xFFD4CABB)
)
@Composable fun HyperTheme(content:@Composable ()->Unit) { MaterialTheme(colorScheme=palette,content=content) }
