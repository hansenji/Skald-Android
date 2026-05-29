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
public val speed_1_2x: ImageVector
  get() {
    if (_speed_1_2x != null) {
      return _speed_1_2x!!
    }
    _speed_1_2x =
      ImageVector.Builder(
          name = "speed_1_2x",
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
            moveTo(13.5f, 17f)
            horizontalLineTo(10f)
            quadTo(9.58f, 17f, 9.29f, 16.71f)
            quadTo(9f, 16.43f, 9f, 16f)
            verticalLineTo(13f)
            quadTo(9f, 12.18f, 9.59f, 11.59f)
            reflectiveQuadTo(11f, 11f)
            horizontalLineToRelative(1.5f)
            verticalLineTo(9f)
            horizontalLineTo(10f)
            quadTo(9.58f, 9f, 9.29f, 8.71f)
            reflectiveQuadTo(9f, 8f)
            quadTo(9f, 7.57f, 9.29f, 7.29f)
            quadTo(9.58f, 7f, 10f, 7f)
            horizontalLineToRelative(2.5f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(14.5f, 9f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(12.5f, 13f)
            horizontalLineTo(11f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(2.5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(14.5f, 16f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(13.5f, 17f)
            close()
            moveTo(3f, 9f)
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
            quadTo(3f, 16.43f, 3f, 16f)
            verticalLineTo(9f)
            close()
            moveToRelative(16.5f, 4.67f)
            lineToRelative(-1.75f, 2.9f)
            quadToRelative(-0.13f, 0.2f, -0.31f, 0.31f)
            reflectiveQuadTo(17.03f, 17f)
            quadToRelative(-0.5f, 0f, -0.76f, -0.44f)
            reflectiveQuadTo(16.28f, 15.7f)
            lineTo(18.5f, 12f)
            lineTo(16.28f, 8.3f)
            quadTo(16f, 7.88f, 16.26f, 7.44f)
            quadTo(16.53f, 7f, 17.03f, 7f)
            quadToRelative(0.22f, 0f, 0.41f, 0.11f)
            quadToRelative(0.19f, 0.11f, 0.31f, 0.31f)
            lineToRelative(1.75f, 2.9f)
            lineToRelative(1.75f, -2.9f)
            quadToRelative(0.13f, -0.2f, 0.31f, -0.31f)
            reflectiveQuadTo(21.98f, 7f)
            quadToRelative(0.5f, 0f, 0.76f, 0.44f)
            reflectiveQuadTo(22.73f, 8.3f)
            lineTo(20.5f, 12f)
            lineToRelative(2.23f, 3.7f)
            quadToRelative(0.27f, 0.43f, 0.01f, 0.86f)
            reflectiveQuadTo(21.98f, 17f)
            quadToRelative(-0.23f, 0f, -0.41f, -0.11f)
            reflectiveQuadTo(21.25f, 16.58f)
            lineTo(19.5f, 13.68f)
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
    return _speed_1_2x!!
  }

private var _speed_1_2x: ImageVector? = null
