package app.hopescrolling.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val iconCache = mutableMapOf<String, ImageBitmap?>()

private fun loadIcon(c: Context, pkg: String): ImageBitmap? = iconCache.getOrPut(pkg) {
    try {
        val d = c.packageManager.getApplicationIcon(pkg)
        val px = (48 * c.resources.displayMetrics.density).toInt()
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, px, px)
        d.draw(Canvas(b))
        b.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/** Icône de l'appli, ou un emoji si elle est introuvable. */
@Composable
fun AppIcon(pkg: String, size: Int = 44) {
    val c = LocalContext.current
    val img = remember(pkg) { loadIcon(c, pkg) }
    if (img != null) Image(img, contentDescription = null, modifier = Modifier.size(size.dp))
    else IconBadge(androidx.compose.material.icons.Icons.Rounded.Apps, size)
}
