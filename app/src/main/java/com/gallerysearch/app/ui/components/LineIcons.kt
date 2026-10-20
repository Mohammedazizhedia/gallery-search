package com.gallerysearch.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} $y H${x + w - r} A$r $r 0 0 1 ${x + w} ${y + r} V${y + h - r} " +
        "A$r $r 0 0 1 ${x + w - r} ${y + h} H${x + r} A$r $r 0 0 1 $x ${y + h - r} " +
        "V${y + r} A$r $r 0 0 1 ${x + r} $y Z"

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} $cy A$r $r 0 1 0 ${cx + r} $cy A$r $r 0 1 0 ${cx - r} $cy Z"

/** Line-art icons on a 64x64 grid. Each one draws what the category contains. */
enum class LineIcon(val paths: List<String>) {
    Car(listOf(
        "M10 40l4-12q1-3 4-3h28q3 0 4 3l4 12", "M20 25l4-7h16l4 7",
        rect(8f, 40f, 48f, 9f, 4f), circle(18f, 49f, 5f), circle(46f, 49f, 5f), "M14 44h4M46 44h4",
    )),
    Face(listOf(
        circle(32f, 26f, 8f), "M17 52q0-14 15-14t15 14", "M8 18v-8h8M48 10h8v8M56 46v8h-8M16 54H8v-8",
    )),
    Receipt(listOf(
        "M16 8h32v48l-5-4-5 4-6-4-6 4-5-4-5 4z", "M24 18h16M24 26h16M24 34h9",
    )),
    Scenery(listOf(
        rect(8f, 12f, 48f, 40f, 7f), "M8 44l14-14 10 10 8-8 16 16", circle(44f, 25f, 4f),
    )),
    Video(listOf(
        rect(8f, 12f, 48f, 34f, 7f), "M27 23v12l11-6z", "M12 54h40", circle(30f, 54f, 2.5f),
    )),
    Dup(listOf(
        rect(8f, 10f, 34f, 34f, 6f), rect(22f, 22f, 34f, 34f, 6f), "M26 50l9-9 7 7 5-5 5 5",
    )),
    Broken(listOf(
        rect(8f, 10f, 48f, 44f, 7f), "M36 10l-7 15 9 6-8 12 4 11", "M14 44l8-8",
    )),
    Trash(listOf(
        "M10 18h44", "M24 18v-6h16v6", "M16 18l3 38h26l3-38", "M28 28v18M36 28v18",
    )),
    Blur(listOf(
        rect(8f, 10f, 48f, 44f, 7f),
        "M16 26q5-5 10 0t10 0t10 0", "M16 36q5-5 10 0t10 0t10 0", "M16 46q5-5 10 0t10 0t10 0",
    )),
}

@Composable
fun LineIconView(icon: LineIcon, tint: Color, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val parsed = remember(icon) { icon.paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 64f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            parsed.forEach {
                drawPath(it, tint, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
