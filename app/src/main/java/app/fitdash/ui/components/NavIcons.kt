package app.fitdash.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun lineIcon(name: String, vararg strokes: String, fills: List<String> = emptyList()): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        strokes.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        fills.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.White)) }
    }.build()

object NavIcons {
    val today = lineIcon(
        "today",
        "M12 5a7 7 0 1 0 0 14a7 7 0 1 0 0 -14z",
        fills = listOf("M12 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0 -5z"),
    )
    val sleep = lineIcon("sleep", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val activity = lineIcon("activity", "M3 12h4l3-7 4 14 3-7h4")
    val trends = lineIcon("trends", "M4 19V5M4 19h16M8 15l4-4 3 3 5-6")
}
