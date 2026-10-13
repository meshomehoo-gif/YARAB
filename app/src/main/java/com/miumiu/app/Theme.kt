package com.miumiu.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Pink = Color(0xFFFF5FA2)
val Sky = Color(0xFF5BBDF5)
val Mint = Color(0xFF34C486)
val Lilac = Color(0xFFA98BFF)
val Plum = Color(0xFF3B2146)

/** Light and dark palettes. t* are the soft tints used behind cards (pink, sky, mint, lilac, butter). */
data class Pal(
    val bg1: Color, val bg2: Color, val surface: Color, val ink: Color, val soft: Color, val line: Color,
    val tp: Color, val ts: Color, val tm: Color, val tl: Color, val tb: Color
)

val LightPal = Pal(
    Color(0xFFFFF3F9), Color(0xFFE9F5FF), Color.White, Color(0xFF3B2146), Color(0xFF7C5C92), Color(0x1F3B2146),
    Color(0xFFFFE1EE), Color(0xFFDDF1FF), Color(0xFFD8F6E7), Color(0xFFECE4FF), Color(0xFFFFF0C2)
)
val DarkPal = Pal(
    Color(0xFF211430), Color(0xFF15213A), Color(0xFF33204A), Color(0xFFFDEBF4), Color(0xFFCDB0E0), Color(0x24FFFFFF),
    Color(0xFF4E2347), Color(0xFF1F3B5C), Color(0xFF1E4A44), Color(0xFF3F3068), Color(0xFF54431F)
)
val LocalPal = staticCompositionLocalOf { LightPal }

@Composable
fun T(
    s: String,
    size: Int = 15,
    w: FontWeight = FontWeight.Normal,
    c: Color = LocalPal.current.ink,
    a: TextAlign? = null,
    mod: Modifier = Modifier
) {
    Text(s, modifier = mod, color = c, fontSize = size.sp, fontWeight = w, textAlign = a, lineHeight = (size * 1.6).sp)
}

@Composable
fun Card2(tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tint ?: p.surface).border(1.dp, p.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun Big(label: String, color: Color = Pink, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape)
            .background(if (on) p.tp else p.surface)
            .border(2.dp, if (on) Pink else p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        T(label, 14, FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
fun CheckDot(done: Boolean, onClick: () -> Unit) {
    val p = LocalPal.current
    Box(
        Modifier.size(38.dp).clip(CircleShape)
            .background(if (done) Mint else Color.Transparent)
            .border(2.dp, if (done) Mint else p.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (done) Text("✓", color = Color(0xFF07261A), fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun Bar(frac: Float) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(50)
    val f = frac.coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(10.dp).clip(shape).background(p.line)) {
        if (f > 0f) Box(Modifier.fillMaxWidth(f).height(10.dp).clip(shape).background(Brush.horizontalGradient(listOf(Sky, Mint))))
    }
}

private fun DrawScope.tri(a: Offset, b: Offset, c: Offset, col: Color) {
    val path = Path()
    path.moveTo(a.x, a.y)
    path.lineTo(b.x, b.y)
    path.lineTo(c.x, c.y)
    path.close()
    drawPath(path, col)
}

/** The MIU MIU mark: a cat face with a flower on a pink-to-blue tile. Swap it for your own logo any time. */
@Composable
fun Logo(size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val k = w / 120f
        fun pt(x: Float, y: Float) = Offset(x * k, y * k)
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFFFF8FC7), Color(0xFF7FD1FF)), Offset.Zero, Offset(w, w)),
            cornerRadius = CornerRadius(w * 0.25f),
            size = Size(w, w)
        )
        val cream = Color(0xFFFFF5FA)
        tri(pt(26f, 62f), pt(32f, 26f), pt(56f, 44f), cream)
        tri(pt(94f, 62f), pt(88f, 26f), pt(64f, 44f), cream)
        tri(pt(34f, 54f), pt(37f, 36f), pt(49f, 46f), Color(0xFFFF9CCB))
        tri(pt(86f, 54f), pt(83f, 36f), pt(71f, 46f), Color(0xFFFF9CCB))
        drawOval(cream, pt(26f, 39f), Size(68f * k, 58f * k))
        drawCircle(Plum, 4.5f * k, pt(48f, 66f))
        drawCircle(Plum, 4.5f * k, pt(72f, 66f))
        drawCircle(Color.White, 1.6f * k, pt(49.6f, 64.4f))
        drawCircle(Color.White, 1.6f * k, pt(73.6f, 64.4f))
        drawOval(Pink, pt(56.6f, 71.4f), Size(6.8f * k, 5.2f * k))
        drawArc(Plum, 20f, 140f, false, pt(52f, 70f), Size(16f * k, 12f * k), style = Stroke(2.4f * k, cap = StrokeCap.Round))
        val gold = Color(0xFFFFD86B)
        listOf(Offset(90f, 21f), Offset(97f, 26f), Offset(94.5f, 34f), Offset(85.5f, 34f), Offset(83f, 26f))
            .forEach { drawCircle(gold, 5.5f * k, pt(it.x, it.y)) }
        drawCircle(Pink, 4f * k, pt(90f, 28f))
    }
}

/** The cat or dog buddy. */
@Composable
fun Pet(cat: Boolean, size: Dp = 130.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val ink = Plum
        for (s in listOf(-1f, 1f)) {
            if (cat) {
                val path = Path()
                path.moveTo(w * (0.5f + s * 0.38f), w * 0.5f)
                path.lineTo(w * (0.5f + s * 0.3f), w * 0.1f)
                path.lineTo(w * (0.5f + s * 0.08f), w * 0.3f)
                path.close()
                drawPath(path, Color(0xFFFFB0D2))
            } else {
                drawOval(Color(0xFF8EC9FF), Offset(w * (0.5f + s * 0.38f) - w * 0.12f, w * 0.3f), Size(w * 0.24f, w * 0.45f))
            }
        }
        drawOval(if (cat) Color(0xFFFFC9E0) else Color(0xFFCFE9FF), Offset(w * 0.08f, w * 0.25f), Size(w * 0.84f, w * 0.7f))
        for (x in listOf(0.36f, 0.64f)) {
            drawCircle(ink, w * 0.06f, Offset(w * x, w * 0.57f))
            drawCircle(Color.White, w * 0.02f, Offset(w * x + w * 0.02f, w * 0.55f))
        }
        for (x in listOf(0.17f, 0.7f)) drawOval(Color(0x80FF7EB6), Offset(w * x, w * 0.65f), Size(w * 0.13f, w * 0.07f))
        drawCircle(if (cat) Pink else ink, w * 0.035f, Offset(w * 0.5f, w * 0.64f))
        drawArc(ink, 20f, 140f, false, Offset(w * 0.42f, w * 0.62f), Size(w * 0.16f, w * 0.14f), style = Stroke(w * 0.025f, cap = StrokeCap.Round))
        for (s in listOf(-1f, 1f)) {
            val bow = Path()
            bow.moveTo(w * 0.76f, w * 0.2f)
            bow.lineTo(w * (0.76f + s * 0.12f), w * 0.12f)
            bow.lineTo(w * (0.76f + s * 0.12f), w * 0.28f)
            bow.close()
            drawPath(bow, Pink)
        }
        drawCircle(Color(0xFFFFD86B), w * 0.035f, Offset(w * 0.76f, w * 0.2f))
    }
}
