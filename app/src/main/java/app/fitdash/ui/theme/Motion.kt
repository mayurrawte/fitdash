package app.fitdash.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object Motion {
    const val DRAW = 1400
    const val RISE = 600
    const val COUNT = 1600
    const val RING = 1200
    const val STAGGER = 120
    const val COLOUR = 1800
    const val PAGE = 450
    val ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
}
