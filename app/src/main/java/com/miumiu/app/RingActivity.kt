package com.miumiu.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/** Full-screen alarm shown over the lock screen when a medicine or cat-care reminder rings. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("t") ?: "الدوا"
        val kind = intent.getStringExtra("k") ?: "med"
        val cat = kind == "cat" || Random.nextBoolean()
        val msg = N(CHEERS.random())
        val tip = TIPS.random().second

        fun send(a: String) {
            sendBroadcast(
                Intent(this, RemReceiver::class.java)
                    .setData(Uri.parse("miumiu://rem/" + a + "/" + id))
                    .putExtra("id", id)
                    .putExtra("t", title)
                    .putExtra("k", kind)
                    .putExtra("act", a)
            )
            finish()
        }

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.12f,
                        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                        label = "s"
                    )
                    Column(
                        Modifier.fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0xFFFF9CCB), Color(0xFF8ED3FF), Color(0xFF9CF0C8))))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
                    ) {
                        Box(Modifier.scale(pulse)) { Pet(cat, 210.dp) }
                        val head = if (kind == "cat") "🐾 علاج قطتك: $title" else "⏰ ميعاد $title"
                        Text(head, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                        Text(msg, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Plum, textAlign = TextAlign.Center)
                        Text("💡 $tip", fontSize = 14.sp, color = Plum, textAlign = TextAlign.Center)
                        androidx.compose.material3.Button({ send("taken") }, Modifier.fillMaxWidth().height(56.dp)) {
                            Text("خدتو ✓", fontSize = 20.sp)
                        }
                        OutlinedButton({ send("snooze") }, Modifier.fillMaxWidth()) {
                            Text("فكّريني بعد 10 دقايق", color = Plum)
                        }
                    }
                }
            }
        }
    }
}
