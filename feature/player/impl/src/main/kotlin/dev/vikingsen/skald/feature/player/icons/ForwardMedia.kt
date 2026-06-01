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
public val forward_media: ImageVector
  get() {
    if (_forward_media != null) {
      return _forward_media!!
    }
    _forward_media =
      ImageVector.Builder(
          name = "forward_media",
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
            moveTo(12f, 22f)
            quadTo(10.13f, 22f, 8.49f, 21.29f)
            reflectiveQuadTo(5.64f, 19.36f)
            reflectiveQuadTo(3.71f, 16.51f)
            reflectiveQuadTo(3f, 13f)
            reflectiveQuadTo(3.71f, 9.49f)
            reflectiveQuadTo(5.64f, 6.64f)
            reflectiveQuadTo(8.49f, 4.71f)
            reflectiveQuadTo(12f, 4f)
            horizontalLineToRelative(0.15f)
            lineTo(11.3f, 3.15f)
            quadTo(11.03f, 2.88f, 11.03f, 2.46f)
            reflectiveQuadTo(11.3f, 1.75f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.31f)
            quadToRelative(0.41f, -0.01f, 0.71f, 0.29f)
            lineTo(15.3f, 4.3f)
            quadTo(15.58f, 4.57f, 15.58f, 5f)
            reflectiveQuadTo(15.3f, 5.7f)
            lineTo(12.73f, 8.27f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.29f)
            reflectiveQuadTo(11.3f, 8.25f)
            quadTo(11.03f, 7.95f, 11.03f, 7.54f)
            quadToRelative(0f, -0.41f, 0.28f, -0.69f)
            lineTo(12.15f, 6f)
            horizontalLineTo(12f)
            quadTo(9.08f, 6f, 7.04f, 8.04f)
            reflectiveQuadTo(5f, 13f)
            reflectiveQuadToRelative(2.04f, 4.96f)
            reflectiveQuadTo(12f, 20f)
            quadToRelative(2.65f, 0f, 4.63f, -1.73f)
            reflectiveQuadToRelative(2.32f, -4.35f)
            quadTo(19f, 13.52f, 19.3f, 13.26f)
            reflectiveQuadTo(20f, 13f)
            reflectiveQuadToRelative(0.7f, 0.25f)
            reflectiveQuadToRelative(0.25f, 0.63f)
            quadToRelative(-0.35f, 3.48f, -2.9f, 5.8f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _forward_media!!
  }

private var _forward_media: ImageVector? = null
