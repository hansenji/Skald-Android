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
public val speed_0_7x: ImageVector
  get() {
    if (_speed_0_7x != null) {
      return _speed_0_7x!!
    }
    _speed_0_7x =
      ImageVector.Builder(
          name = "speed_0_7x",
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
            moveTo(17f, 13.68f)
            lineToRelative(-1.75f, 2.9f)
            quadToRelative(-0.13f, 0.2f, -0.31f, 0.31f)
            reflectiveQuadTo(14.53f, 17f)
            quadToRelative(-0.5f, 0f, -0.76f, -0.44f)
            reflectiveQuadTo(13.78f, 15.7f)
            lineTo(16f, 12f)
            lineTo(13.78f, 8.3f)
            quadTo(13.5f, 7.88f, 13.76f, 7.44f)
            reflectiveQuadTo(14.53f, 7f)
            quadToRelative(0.22f, 0f, 0.41f, 0.11f)
            quadToRelative(0.19f, 0.11f, 0.31f, 0.31f)
            lineTo(17f, 10.33f)
            lineToRelative(1.75f, -2.9f)
            quadToRelative(0.13f, -0.2f, 0.31f, -0.31f)
            reflectiveQuadTo(19.48f, 7f)
            quadToRelative(0.5f, 0f, 0.76f, 0.44f)
            reflectiveQuadTo(20.23f, 8.3f)
            lineTo(18f, 12f)
            lineToRelative(2.23f, 3.7f)
            quadToRelative(0.27f, 0.43f, 0.01f, 0.86f)
            reflectiveQuadTo(19.48f, 17f)
            quadToRelative(-0.23f, 0f, -0.41f, -0.11f)
            reflectiveQuadTo(18.75f, 16.58f)
            lineTo(17f, 13.68f)
            close()
            moveTo(9.25f, 17f)
            quadTo(8.78f, 17f, 8.48f, 16.63f)
            quadTo(8.18f, 16.25f, 8.3f, 15.8f)
            lineTo(10f, 9f)
            horizontalLineTo(7f)
            quadTo(6.58f, 9f, 6.29f, 8.71f)
            reflectiveQuadTo(6f, 8f)
            quadTo(6f, 7.57f, 6.29f, 7.29f)
            reflectiveQuadTo(7f, 7f)
            horizontalLineToRelative(3.25f)
            quadToRelative(0.73f, 0f, 1.24f, 0.54f)
            reflectiveQuadTo(12f, 8.8f)
            quadToRelative(0f, 0f, -0.05f, 0.45f)
            lineToRelative(-1.78f, 7.02f)
            quadTo(10.1f, 16.6f, 9.85f, 16.8f)
            reflectiveQuadTo(9.25f, 17f)
            close()
            moveTo(4.29f, 16.71f)
            quadTo(4f, 16.43f, 4f, 16f)
            reflectiveQuadTo(4.29f, 15.29f)
            reflectiveQuadTo(5f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(6f, 16f)
            reflectiveQuadTo(5.71f, 16.71f)
            reflectiveQuadTo(5f, 17f)
            quadTo(4.58f, 17f, 4.29f, 16.71f)
            close()
          }
        }
        .build()
    return _speed_0_7x!!
  }

private var _speed_0_7x: ImageVector? = null
