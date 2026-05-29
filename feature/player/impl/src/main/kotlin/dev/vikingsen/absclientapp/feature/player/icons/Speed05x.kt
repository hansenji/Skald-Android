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
public val speed_0_5x: ImageVector
  get() {
    if (_speed_0_5x != null) {
      return _speed_0_5x!!
    }
    _speed_0_5x =
      ImageVector.Builder(
          name = "speed_0_5x",
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
            moveTo(10f, 17f)
            horizontalLineTo(7f)
            quadTo(6.58f, 17f, 6.29f, 16.71f)
            quadTo(6f, 16.43f, 6f, 16f)
            reflectiveQuadTo(6.29f, 15.29f)
            reflectiveQuadTo(7f, 15f)
            horizontalLineToRelative(3f)
            verticalLineTo(13f)
            horizontalLineTo(7f)
            quadTo(6.58f, 13f, 6.29f, 12.71f)
            quadTo(6f, 12.43f, 6f, 12f)
            verticalLineTo(8f)
            quadTo(6f, 7.57f, 6.29f, 7.29f)
            reflectiveQuadTo(7f, 7f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(12f, 8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(11f, 9f)
            horizontalLineTo(8f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(2f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(12f, 12.18f, 12f, 13f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(10f, 17f)
            close()
            moveTo(3.29f, 16.71f)
            quadTo(3f, 16.43f, 3f, 16f)
            reflectiveQuadTo(3.29f, 15.29f)
            reflectiveQuadTo(4f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 16f)
            reflectiveQuadTo(4.71f, 16.71f)
            reflectiveQuadTo(4f, 17f)
            reflectiveQuadTo(3.29f, 16.71f)
            close()
          }
        }
        .build()
    return _speed_0_5x!!
  }

private var _speed_0_5x: ImageVector? = null
