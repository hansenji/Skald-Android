package dev.vikingsen.absclientapp.feature.player.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val speed_2x: ImageVector
  get() {
    if (_speed_2x != null) {
      return _speed_2x!!
    }
    _speed_2x =
      ImageVector.Builder(
          name = "speed_2x",
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
            moveTo(10f, 17f)
            horizontalLineTo(7f)
            quadTo(6.18f, 17f, 5.59f, 16.41f)
            reflectiveQuadTo(5f, 15f)
            verticalLineTo(13f)
            quadTo(5f, 12.18f, 5.59f, 11.59f)
            reflectiveQuadTo(7f, 11f)
            horizontalLineTo(9f)
            verticalLineTo(9f)
            horizontalLineTo(6f)
            quadTo(5.58f, 9f, 5.29f, 8.71f)
            reflectiveQuadTo(5f, 8f)
            quadTo(5f, 7.57f, 5.29f, 7.29f)
            reflectiveQuadTo(6f, 7f)
            horizontalLineTo(9f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(11f, 9f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(9f, 13f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 16f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 17f)
            close()
            moveToRelative(6f, -3.33f)
            lineToRelative(-1.75f, 2.9f)
            quadToRelative(-0.13f, 0.2f, -0.31f, 0.31f)
            reflectiveQuadTo(13.53f, 17f)
            quadToRelative(-0.5f, 0f, -0.76f, -0.44f)
            reflectiveQuadTo(12.78f, 15.7f)
            lineTo(15f, 12f)
            lineTo(12.78f, 8.3f)
            quadTo(12.5f, 7.88f, 12.76f, 7.44f)
            reflectiveQuadTo(13.53f, 7f)
            quadToRelative(0.22f, 0f, 0.41f, 0.11f)
            quadToRelative(0.19f, 0.11f, 0.31f, 0.31f)
            lineTo(16f, 10.33f)
            lineToRelative(1.75f, -2.9f)
            quadToRelative(0.13f, -0.2f, 0.31f, -0.31f)
            reflectiveQuadTo(18.48f, 7f)
            quadToRelative(0.5f, 0f, 0.76f, 0.44f)
            reflectiveQuadTo(19.23f, 8.3f)
            lineTo(17f, 12f)
            lineToRelative(2.23f, 3.7f)
            quadToRelative(0.27f, 0.43f, 0.01f, 0.86f)
            reflectiveQuadTo(18.48f, 17f)
            quadToRelative(-0.23f, 0f, -0.41f, -0.11f)
            reflectiveQuadTo(17.75f, 16.58f)
            lineTo(16f, 13.68f)
            close()
          }
        }
        .build()
    return _speed_2x!!
  }

private var _speed_2x: ImageVector? = null
