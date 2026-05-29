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
public val speed_1_7x: ImageVector
  get() {
    if (_speed_1_7x != null) {
      return _speed_1_7x!!
    }
    _speed_1_7x =
      ImageVector.Builder(
          name = "speed_1_7x",
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
            moveTo(3.29f, 16.71f)
            quadTo(3f, 16.43f, 3f, 16f)
            verticalLineTo(9f)
            horizontalLineTo(2f)
            quadTo(1.58f, 9f, 1.29f, 8.71f)
            reflectiveQuadTo(1f, 8f)
            quadTo(1f, 7.57f, 1.29f, 7.29f)
            reflectiveQuadTo(2f, 7f)
            horizontalLineTo(4f)
            quadTo(4.43f, 7f, 4.71f, 7.29f)
            reflectiveQuadTo(5f, 8f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(4f, 17f)
            reflectiveQuadTo(3.29f, 16.71f)
            close()
            moveTo(19f, 13.68f)
            lineToRelative(-1.75f, 2.9f)
            quadToRelative(-0.13f, 0.2f, -0.31f, 0.31f)
            reflectiveQuadTo(16.53f, 17f)
            quadToRelative(-0.5f, 0f, -0.76f, -0.44f)
            reflectiveQuadTo(15.78f, 15.7f)
            lineTo(18f, 12f)
            lineTo(15.78f, 8.3f)
            quadTo(15.5f, 7.88f, 15.76f, 7.44f)
            reflectiveQuadTo(16.53f, 7f)
            quadToRelative(0.22f, 0f, 0.41f, 0.11f)
            quadToRelative(0.19f, 0.11f, 0.31f, 0.31f)
            lineTo(19f, 10.33f)
            lineToRelative(1.75f, -2.9f)
            quadToRelative(0.13f, -0.2f, 0.31f, -0.31f)
            reflectiveQuadTo(21.48f, 7f)
            quadToRelative(0.5f, 0f, 0.76f, 0.44f)
            reflectiveQuadTo(22.23f, 8.3f)
            lineTo(20f, 12f)
            lineToRelative(2.23f, 3.7f)
            quadToRelative(0.27f, 0.43f, 0.01f, 0.86f)
            reflectiveQuadTo(21.48f, 17f)
            quadToRelative(-0.23f, 0f, -0.41f, -0.11f)
            reflectiveQuadTo(20.75f, 16.58f)
            lineTo(19f, 13.68f)
            close()
            moveTo(11.25f, 17f)
            quadToRelative(-0.47f, 0f, -0.77f, -0.38f)
            reflectiveQuadTo(10.3f, 15.8f)
            lineTo(12f, 9f)
            horizontalLineTo(9f)
            quadTo(8.58f, 9f, 8.29f, 8.71f)
            reflectiveQuadTo(8f, 8f)
            quadTo(8f, 7.57f, 8.29f, 7.29f)
            quadTo(8.58f, 7f, 9f, 7f)
            horizontalLineToRelative(3.25f)
            quadToRelative(0.73f, 0f, 1.24f, 0.54f)
            reflectiveQuadTo(14f, 8.8f)
            quadToRelative(0f, 0f, -0.05f, 0.45f)
            lineToRelative(-1.78f, 7.02f)
            quadTo(12.1f, 16.6f, 11.85f, 16.8f)
            reflectiveQuadTo(11.25f, 17f)
            close()
            moveTo(6.29f, 16.71f)
            quadTo(6f, 16.43f, 6f, 16f)
            reflectiveQuadTo(6.29f, 15.29f)
            reflectiveQuadTo(7f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(8f, 16f)
            reflectiveQuadTo(7.71f, 16.71f)
            reflectiveQuadTo(7f, 17f)
            quadTo(6.58f, 17f, 6.29f, 16.71f)
            close()
          }
        }
        .build()
    return _speed_1_7x!!
  }

private var _speed_1_7x: ImageVector? = null
