package com.miumiu.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var ui: Ui

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val locPerm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) {
            val ok = Prayer.updateLocation(this)
            ui.toast(if (ok) "اتحدد موقعك 📍" else "مقدرتش أعرف موقعك، افتحي خرائط جوجل مرة وجرّبي تاني")
            ui.refreshPrayer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Channels.ensure(this)
        ui = Ui(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= 34 &&
            !getSystemService(NotificationManager::class.java).canUseFullScreenIntent() &&
            !Store.getBool(this, "fsi_asked", false)
        ) {
            Store.putBool(this, "fsi_asked", true)
            startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        }

        Sched.setAll(this)
        TalentSched.schedule(this)
        Prayer.refresh(this)

        setContent {
            Root(ui) {
                locPerm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::ui.isInitialized) ui.refresh()
    }
}

@Composable
fun Root(ui: Ui, askLoc: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val pal = if (dark) DarkPal else LightPal
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalPal provides pal) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Pink) else lightColorScheme(primary = Pink)) {
            App(ui, askLoc)
        }
    }
}

@Composable
fun App(ui: Ui, askLoc: () -> Unit) {
    val p = LocalPal.current
    var tab by remember { mutableStateOf("home") }
    var sub by remember { mutableStateOf("") }
    var sea by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            ui.refresh()
        }
    }
    BackHandler(enabled = sub.isNotEmpty()) { sub = "" }

    if (sea) SeaDialog(ui) { sea = false }
    if (ui.newAch.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { ui.newAch = emptyList() },
            confirmButton = { TextButton({ ui.newAch = emptyList() }) { Text("يا سلام 🎉") } },
            title = { Text("🏆 إنجاز جديد!") },
            text = { Text(ui.newAch.joinToString("\n") { it.emoji + " " + it.title }) }
        )
    }

    val tabs = listOf(
        Triple("home", "🏠", "الرئيسية"), Triple("sched", "📅", "مواعيدي"), Triple("routine", "🌿", "روتيني"),
        Triple("talents", "🎨", "مواهبي"), Triple("more", "✨", "المزيد")
    )
    val shown = if (sub.isNotEmpty()) "more" else tab

    Scaffold(
        containerColor = p.bg1,
        bottomBar = {
            NavigationBar(containerColor = p.surface) {
                tabs.forEach { (k, e, l) ->
                    NavigationBarItem(
                        selected = shown == k,
                        onClick = { tab = k; sub = "" },
                        icon = { Text(e, fontSize = 22.sp) },
                        label = { Text(l, fontSize = 11.sp, color = p.soft) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = p.tp)
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(Brush.verticalGradient(listOf(p.bg1, p.bg2)))) {
            key(tab, sub) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TopBar(ui)
                    when (if (sub.isNotEmpty()) sub else tab) {
                        "home" -> HomeScreen(ui, go = { tab = "more"; sub = it }, openSea = { sea = true })
                        "sched" -> SchedScreen(ui)
                        "routine" -> RoutineScreen(ui)
                        "talents" -> TalentsScreen(ui)
                        "more" -> MoreScreen { sub = it }
                        "prayer" -> PrayerScreen(ui, askLoc) { sub = "" }
                        "ach" -> AchScreen(ui) { sub = "" }
                        "cat" -> CatScreen(ui) { sub = "" }
                        "tips" -> TipsScreen { sub = "" }
                    }
                }
            }
        }
    }
}
