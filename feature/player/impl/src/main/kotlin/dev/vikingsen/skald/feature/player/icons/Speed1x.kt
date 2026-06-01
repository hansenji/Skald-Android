package dev.vikingsen.skald.feature.player.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val speed_1x: ImageVector
  get() {
    if (_speed_1x != null) {
      return _speed_1x!!
    }
    _speed_1x =
      ImageVector.Builder(
          name = "speed_1x",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6f, 9f)
            horizontalLineTo(5f)
            quadTo(4.58f, 9f, 4.29f, 8.71f)
            reflectiveQuadTo(4f, 8f)
            quadTo(4f, 7.57f, 4.29f, 7.29f)
            reflectiveQuadTo(5f, 7f)
            horizontalLineTo(7f)
            quadTo(7.43f, 7f, 7.71f, 7.29f)
            reflectiveQuadTo(8f, 8f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(7f, 17f)
            quadTo(6.58f, 17f, 6.29f, 16.71f)
            quadTo(6f, 16.43f, 6f, 16f)
            verticalLineTo(9f)
            close()
            moveToRelative(8.65f, 4.65f)
            lineToRelative(-1.72f, 2.88f)
            quadToRelative(-0.13f, 0.23f, -0.34f, 0.35f)
            reflectiveQuadTo(12.1f, 17f)
            quadToRelative(-0.58f, 0f, -0.86f, -0.5f)
            reflectiveQuadToRelative(0.01f, -1f)
            lineTo(13.5f, 11.7f)
            lineTo(11.58f, 8.52f)
            quadTo(11.28f, 8.02f, 11.56f, 7.51f)
            reflectiveQuadTo(12.43f, 7f)
            quadToRelative(0.28f, 0f, 0.51f, 0.14f)
            reflectiveQuadTo(13.3f, 7.5f)
            lineToRelative(1.35f, 2.25f)
            lineToRelative(1.4f, -2.28f)
            quadTo(16.18f, 7.25f, 16.4f, 7.13f)
            reflectiveQuadTo(16.9f, 7f)
            quadToRelative(0.57f, 0f, 0.86f, 0.5f)
            reflectiveQuadToRelative(-0.01f, 1f)
            lineToRelative(-1.9f, 3.2f)
            lineToRelative(2.25f, 3.78f)
            quadToRelative(0.3f, 0.5f, 0.01f, 1.01f)
            reflectiveQuadTo(17.23f, 17f)
            quadToRelative(-0.28f, 0f, -0.51f, -0.14f)
            reflectiveQuadTo(16.35f, 16.5f)
            lineToRelative(-1.7f, -2.85f)
            close()
          }
        }
        .build()
    return _speed_1x!!
  }

private var _speed_1x: ImageVector? = null
