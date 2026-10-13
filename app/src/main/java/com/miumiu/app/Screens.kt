package com.miumiu.app

import android.app.TimePickerDialog
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------- shared pieces ----------

@Composable
fun TopBar(ui: Ui) {
    val p = LocalPal.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Logo(38.dp)
        Text(
            "MIU MIU",
            modifier = Modifier.weight(1f),
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Pink, Lilac, Sky)),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        val streak = if (ui.tick >= 0) Stats.streak(ui.c) else 0
        Box(Modifier.clip(RoundedCornerShape(50)).background(p.tb).padding(horizontal = 12.dp, vertical = 4.dp)) {
            T("🔥 $streak أيام", 14, FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ItemRow(
    emoji: String, title: String, sub: String, time: String, tint: Color, done: Boolean,
    onCheck: (() -> Unit)?, onDelete: (() -> Unit)?
) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(p.surface).border(1.dp, p.line, shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(tint), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 24.sp)
        }
        Column(Modifier.weight(1f)) {
            T(title, 16, FontWeight.Bold, if (done) p.soft else p.ink)
            T(sub, 13, c = p.soft)
        }
        T(time, 14, FontWeight.Bold, p.soft)
        if (onCheck != null) CheckDot(done, onCheck) else Spacer(Modifier.width(38.dp))
        if (onDelete != null) {
            Text("🗑", modifier = Modifier.clickable { onDelete() }.padding(4.dp), fontSize = 18.sp)
        } else {
            Spacer(Modifier.width(30.dp))
        }
    }
}

@Composable
fun SlotRow(ui: Ui, s: Slot) {
    val p = LocalPal.current
    val tint = when (s.kind) { "cat" -> p.tl; "routine" -> p.tm; "prayer" -> p.ts; else -> p.tp }
    val emoji = when (s.kind) { "cat" -> "🐾"; "routine" -> "🌿"; "prayer" -> "🕌"; else -> "💊" }
    val label = when (s.kind) { "cat" -> "علاج القطة"; "routine" -> "روتين"; "prayer" -> "صلاة"; else -> "دوا" }
    val rem = s.rem
    ItemRow(
        emoji, s.title, label, fmt(s.h, s.m), tint, ui.isDone(s),
        if (rem != null) ({ ui.complete(rem) }) else null,
        if (rem != null) ({ ui.del(rem) }) else null
    )
}

@Composable
fun Empty(text: String) {
    val p = LocalPal.current
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth().clip(shape).border(2.dp, p.line, shape).padding(22.dp), contentAlignment = Alignment.Center) {
        T(text, 15, c = p.soft, a = TextAlign.Center)
    }
}

/** Name field + time button + add button. */
@Composable
fun AddForm(hint: String, h0: Int, m0: Int, onAdd: (String, Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPal.current
    var name by remember { mutableStateOf("") }
    var h by remember { mutableIntStateOf(h0) }
    var m by remember { mutableIntStateOf(m0) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.weight(1f),
            placeholder = { T(hint, 14, c = p.soft) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = p.ink, unfocusedTextColor = p.ink,
                focusedBorderColor = Pink, unfocusedBorderColor = p.line, cursorColor = Pink
            )
        )
        Pill(fmt(h, m), false) {
            TimePickerDialog(ctx, { _, hh, mm -> h = hh; m = mm }, h, m, false).show()
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Pink).clickable {
                if (name.isNotBlank()) {
                    onAdd(name.trim(), h, m)
                    name = ""
                }
            },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
    }
}

// ---------- home ----------

@Composable
fun HomeScreen(ui: Ui, go: (String) -> Unit, openSea: () -> Unit) {
    val p = LocalPal.current
    val tick = ui.tick
    val c = ui.c
    val nextSlot = ui.sorted(ui.slots().filter { it.kind != "prayer" && !ui.isDone(it) }, "next").firstOrNull()
    val all = ui.rems.size
    val doneCount = ui.rems.count { it.id in ui.done }
    val pr = Prayer.next(c, ui.nowM)
    val tip = TIPS[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) % TIPS.size]
    val ink = Plum

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFC2DE), Color(0xFFCBB8FF), Color(0xFFBFE3FF))))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.clickable { meow() }) { Pet(ui.pet == "cat", 112.dp) }
            T(ui.say, 16, FontWeight.Bold, ink, mod = Modifier.weight(1f))
        }
        Chips {
            Pill("🐱 قطة", ui.pet == "cat") {
                ui.pet = "cat"; Store.putStr(c, "pet", "cat"); ui.say = "ميياو! أنا بونبون 🐱"; meow()
            }
            Pill("🐶 كلب", ui.pet == "dog") {
                ui.pet = "dog"; Store.putStr(c, "pet", "dog"); ui.say = "هاو هاو! أنا كوكو 🐶"
            }
            Pill("دلّعيني 💗", false) { ui.say = N(DALAA.random()); meow() }
        }
    }

    Card2 {
        T("الجاي في يومك", 14, c = p.soft)
        T(nextSlot?.title ?: "خلّصتي كل مهامك النهاردة 🎉", 23, FontWeight.ExtraBold, Pink)
        if (nextSlot != null) T(fmt(nextSlot.h, nextSlot.m), 14, c = p.soft)
        val rem = nextSlot?.rem
        if (rem != null) Big("خلّصتها يا بطلة ✨") { ui.complete(rem) }
        Bar(if (all == 0) 0f else doneCount.toFloat() / all)
        T("خلّصتي $doneCount من $all النهاردة", 13, c = p.soft)
    }

    Card2(p.ts) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T("الصلاة الجاية", 14, c = p.soft)
                T(if (pr != null) pr.name + " • " + fmt(pr.h, pr.m) else "افتحي مواعيد الصلاة وحدّثيها", 19, FontWeight.ExtraBold)
                if (pr != null) T("بعد " + leftText(pr.left), 14, c = p.soft)
            }
            Pill("المواعيد", false) { go("prayer") }
        }
    }

    Card2(p.tl) {
        T("سؤال مهم", 14, c = p.soft, a = TextAlign.Center, mod = Modifier.fillMaxWidth())
        T("عارفة إنتي شاطرة قد إيه؟", 20, FontWeight.ExtraBold, a = TextAlign.Center, mod = Modifier.fillMaxWidth())
        Big("قد إيه؟ 🤔") { openSea() }
    }

    Card2(p.tb) {
        T("نصيحة النهاردة", 14, c = p.soft)
        T(tip.first, 17, FontWeight.ExtraBold)
        T(tip.second, 15)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatBox(Modifier.weight(1f), Stats.get(c, "done").toString(), "مهمة", p.tp)
        StatBox(Modifier.weight(1f), Stats.streak(c).toString(), "أيام متتالية", p.tb)
        StatBox(Modifier.weight(1f), Stats.unlocked(c).size.toString(), "إنجاز", p.tl)
    }

    Big("جرّبي المنبه دلوقتي", Sky) { ui.test("med") }
    if (tick < 0) T("", 1)
}

@Composable
fun StatBox(mod: Modifier, value: String, label: String, tint: Color) {
    Column(
        mod.clip(RoundedCornerShape(20.dp)).background(tint).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        T(value, 26, FontWeight.Bold)
        T(label, 13, c = LocalPal.current.soft)
    }
}

fun leftText(min: Int): String =
    if (min < 60) "$min دقيقة" else (min / 60).toString() + " ساعة" + (if (min % 60 != 0) " و" + (min % 60) + " دقيقة" else "")

// ---------- schedule (sorted) ----------

@Composable
fun SchedScreen(ui: Ui) {
    val tick = ui.tick
    var kind by remember { mutableStateOf("med") }
    val filters = listOf("all" to "الكل", "med" to "💊 دوا", "cat" to "🐾 القطة", "routine" to "🌿 روتين", "prayer" to "🕌 صلاة")
    T("مواعيدي", 22, FontWeight.ExtraBold)
    T("كل مواعيدك مرتبة في مكان واحد", 14, c = LocalPal.current.soft)
    Chips { filters.forEach { (k, l) -> Pill(l, ui.filter == k) { ui.filter = k } } }
    Chips {
        Pill("بالوقت", ui.sort == "time") { ui.sort = "time" }
        Pill("الجاي أولاً", ui.sort == "next") { ui.sort = "next" }
    }
    val l = ui.sorted(ui.slots().filter { ui.filter == "all" || it.kind == ui.filter }, ui.sort)
    if (l.isEmpty()) Empty("مفيش مواعيد هنا لسه. ضيفي أول ميعاد من تحت 🌸")
    l.forEach { SlotRow(ui, it) }
    Card2 {
        T("ميعاد جديد", 16, FontWeight.ExtraBold)
        Chips {
            Pill("💊 دوا", kind == "med") { kind = "med" }
            Pill("🐱 علاج القطة", kind == "cat") { kind = "cat" }
        }
        AddForm("اسم الدوا أو العلاج", 12, 0) { n, h, m -> ui.add(n, h, m, kind) }
    }
    if (tick < 0) T("", 1)
}

// ---------- routine ----------

@Composable
fun RoutineScreen(ui: Ui) {
    val p = LocalPal.current
    val tick = ui.tick
    val l = ui.sorted(ui.slots().filter { it.kind == "routine" }, "time")
    val d = l.count { ui.isDone(it) }
    Card2(p.tm) {
        T("روتيني اليومي", 22, FontWeight.ExtraBold)
        T("خطوات صغيرة بتفرق، وهفكّرك بكل واحدة في ميعادها", 14, c = p.soft)
        Bar(if (l.isEmpty()) 0f else d.toFloat() / l.size)
        T("$d من ${l.size} خطوة النهاردة", 13, c = p.soft)
    }
    T("اقتراحات بضغطة واحدة", 14, c = p.soft)
    Chips { SUG.forEach { s -> Pill(s.first, false) { ui.add(s.first, s.second, s.third, "routine"); ui.toast("اتضافت الخطوة 🌿") } } }
    if (l.isEmpty()) Empty("ابدئي بخطوة صغيرة من الاقتراحات 🌿")
    l.forEach { SlotRow(ui, it) }
    Card2 {
        T("خطوة جديدة", 16, FontWeight.ExtraBold)
        AddForm("مثلاً: تمرين خفيف", 8, 0) { n, h, m -> ui.add(n, h, m, "routine") }
    }
    if (tick < 0) T("", 1)
}

// ---------- talents ----------

@Composable
fun TalentsScreen(ui: Ui) {
    val p = LocalPal.current
    val c = ui.c
    val tick = ui.tick
    var em by remember { mutableStateOf("🎨") }
    var name by remember { mutableStateOf("") }
    val on = Store.getBool(c, "tal_on", true)
    val hrs = Store.getInt(c, "tal_hrs", 4)

    Card2(p.tl) {
        T("مواهبي", 22, FontWeight.ExtraBold)
        T("كل موهبة بتضيفيها هتفضل تفكّرك بيها وتشجعك تكمّلي", 14, c = p.soft)
    }
    if (ui.talents.isEmpty()) Empty("ضيفي أول موهبة، حتى لو كانت صغيرة 🎨")
    ui.talents.toList().forEach { t ->
        Card2 {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(p.tl), contentAlignment = Alignment.Center) {
                    Text(t.emoji, fontSize = 24.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    T(t.name, 17, FontWeight.ExtraBold)
                    Pill("اشتغلت عليها ✓", false) { ui.practice(t) }
                }
                Text("🗑", modifier = Modifier.clickable { ui.delTalent(t) }.padding(6.dp), fontSize = 18.sp)
            }
        }
    }
    Card2 {
        T("موهبة جديدة", 16, FontWeight.ExtraBold)
        Chips { EMOJIS.forEach { e -> Pill(e, em == e) { em = e } } }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.weight(1f),
                placeholder = { T("مثلاً: الرسم، الكتابة، الغناء", 14, c = p.soft) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = p.ink, unfocusedTextColor = p.ink,
                    focusedBorderColor = Pink, unfocusedBorderColor = p.line, cursorColor = Pink
                )
            )
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Pink).clickable {
                    if (name.isNotBlank()) {
                        ui.addTalent(em, name.trim())
                        name = ""
                        ui.toast("موهبة جديدة! أنا فخورة بيكي يا " + nick() + " 🎨")
                    }
                },
                contentAlignment = Alignment.Center
            ) { Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
    Card2(p.tp) {
        T("تنبيهات المواهب", 16, FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("فكّريني بمواهبي", 15, mod = Modifier.weight(1f))
            Switch(checked = on, onCheckedChange = {
                Store.putBool(c, "tal_on", it); TalentSched.schedule(c); ui.tick++
            })
        }
        T("كل كام ساعة؟", 14, c = p.soft)
        Chips {
            listOf(2, 4, 6, 12).forEach { h ->
                Pill("كل $h ساعات", hrs == h) { Store.putInt(c, "tal_hrs", h); TalentSched.schedule(c); ui.tick++ }
            }
        }
        T("من 9 الصبح لـ 10 بالليل بس، عشان مزعجكيش وقت النوم.", 13, c = p.soft)
        Big("جرّبي تنبيه موهبة") { if (ui.talents.isEmpty()) ui.toast("ضيفي موهبة الأول 🎨") else ui.test("talent") }
    }
    if (tick < 0) T("", 1)
}

// ---------- more ----------

@Composable
fun MoreScreen(sub: (String) -> Unit) {
    val p = LocalPal.current
    T("المزيد", 22, FontWeight.ExtraBold)
    val tiles = listOf(
        Triple("🕌", "مواعيد الصلاة", "prayer"), Triple("🏆", "إنجازاتي", "ach"),
        Triple("🐱", "قطتي", "cat"), Triple("💡", "نصايح ورسايل", "tips")
    )
    val tints = listOf(p.ts, p.tb, p.tl, p.tm)
    tiles.chunked(2).forEachIndexed { r, pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEachIndexed { i, t ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(tints[r * 2 + i]).clickable { sub(t.third) }.padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(t.first, fontSize = 34.sp)
                    T(t.second, 16, FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun Back(onBack: () -> Unit) {
    Text("→ رجوع", modifier = Modifier.clickable { onBack() }.padding(vertical = 2.dp), color = LocalPal.current.soft, fontWeight = FontWeight.ExtraBold)
}

// ---------- prayer ----------

@Composable
fun PrayerScreen(ui: Ui, askLoc: () -> Unit, onBack: () -> Unit) {
    val p = LocalPal.current
    val c = ui.c
    val tick = ui.tick
    val t = Prayer.timings(c, java.util.Calendar.getInstance())
    val nx = Prayer.next(c, ui.nowM)
    Back(onBack)
    Card2(p.ts) {
        T("الصلاة الجاية", 14, c = p.soft)
        T(if (nx != null) nx.name + " • " + fmt(nx.h, nx.m) else "لسه مفيش مواعيد", 20, FontWeight.ExtraBold)
        if (nx != null) T("بعد " + leftText(nx.left), 14, c = p.soft)
    }
    if (t == null) {
        Empty("لسه ماحمّلتش مواعيد اليوم. اتأكدي إن النت شغال وادوسي تحديث 🔄")
    } else {
        val rows = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        rows.forEach { k ->
            val hm = parseHm(t[k])
            if (hm != null) {
                val isPrayer = k != "Sunrise"
                val shape = RoundedCornerShape(20.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(p.surface).border(1.dp, p.line, shape).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(p.ts), contentAlignment = Alignment.Center) {
                        Text(if (isPrayer) "🕌" else "🌅", fontSize = 24.sp)
                    }
                    T(Prayer.AR[k] ?: k, 17, FontWeight.Bold, mod = Modifier.weight(1f))
                    T(fmt(hm.first, hm.second), 15, FontWeight.Bold, p.soft)
                    if (isPrayer) {
                        Switch(checked = Prayer.enabled(c, k), onCheckedChange = {
                            Prayer.setEnabled(c, k, it); Prayer.schedule(c); ui.tick++
                        })
                    } else {
                        Spacer(Modifier.width(52.dp))
                    }
                }
            }
        }
    }
    Card2 {
        T("📍 الموقع", 14, c = p.soft)
        T(if (Prayer.hasCustomLocation(c)) "موقعك الحالي" else "القاهرة، مصر (افتراضي)", 16, FontWeight.Bold)
        val up = Prayer.lastUpdated(c)
        T(if (up != null) "آخر تحديث: $up" else "لسه ماتحدّثتش", 13, c = p.soft)
        Chips {
            Pill("📍 حدّدي موقعي", false) { askLoc() }
            Pill("🔄 تحديث", false) { ui.refreshPrayer() }
        }
        T("المصدر: Aladhan (api.aladhan.com) بطريقة الهيئة المصرية العامة للمساحة. المواعيد بتتحدّث لوحدها مع كل صلاة، وبتفضل شغالة حتى لو النت قطع كام يوم.", 13, c = p.soft)
        Big("جرّبي منبه الصلاة", Mint) { ui.test("prayer") }
    }
    if (tick < 0) T("", 1)
}

// ---------- achievements ----------

@Composable
fun AchScreen(ui: Ui, onBack: () -> Unit) {
    val p = LocalPal.current
    val c = ui.c
    val tick = ui.tick
    val u = Stats.unlocked(c)
    Back(onBack)
    Card2(p.tb) {
        T("إنجازاتي 🏆", 22, FontWeight.ExtraBold)
        T("فتحتي ${u.size} من ${ACHS.size} إنجاز", 14, c = p.soft)
    }
    ACHS.forEach { a ->
        val v = Stats.progress(c, a).coerceAtMost(a.goal)
        val ok = v >= a.goal
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(p.surface).border(1.dp, p.line, shape).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(if (ok) p.tb else p.line), contentAlignment = Alignment.Center) {
                Text(if (ok) a.emoji else "🔒", fontSize = 24.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T(a.title, 16, FontWeight.Bold)
                T(a.desc, 13, c = p.soft)
                Bar(v.toFloat() / a.goal)
            }
            T("$v/${a.goal}", 14, FontWeight.Bold, p.soft)
        }
    }
    if (tick < 0) T("", 1)
}

// ---------- cat ----------

@Composable
fun CatScreen(ui: Ui, onBack: () -> Unit) {
    val p = LocalPal.current
    val tick = ui.tick
    var say by remember { mutableStateOf("دوسي عليا وهقولك ميياو 💕") }
    Back(onBack)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFC2DE), Color(0xFFCBB8FF), Color(0xFFBFE3FF))))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.clickable {
                meow()
                say = listOf("ميياو 💕", "مياااو! عايزة سمك 🐟", "بررررر بررر 😻", "ميو ميو، بحبك 😽").random()
            }) { Pet(true, 112.dp) }
            T(say, 16, FontWeight.Bold, Plum, mod = Modifier.weight(1f))
        }
    }
    T("مواعيد علاج قطتك", 14, c = p.soft)
    val l = ui.sorted(ui.slots().filter { it.kind == "cat" }, "time")
    if (l.isEmpty()) Empty("ضيفي أول ميعاد لقطتك 🐾")
    l.forEach { SlotRow(ui, it) }
    Card2 {
        AddForm("مثلاً: قطرة العين", 10, 0) { n, h, m -> ui.add(n, h, m, "cat"); ui.toast("اتضاف ميعاد لقطتك 🐾"); meow() }
    }
    T("رعاية القطة", 14, c = p.soft)
    val tints = listOf(p.tp, p.ts, p.tl, p.tm, p.tb, p.tp)
    CAT_TIPS.forEachIndexed { i, t ->
        Card2(tints[i % tints.size]) {
            T(t.first, 17, FontWeight.ExtraBold)
            T(t.second, 15)
        }
    }
    if (tick < 0) T("", 1)
}

// ---------- tips ----------

@Composable
fun TipsScreen(onBack: () -> Unit) {
    val p = LocalPal.current
    val tints = listOf(p.tp, p.ts, p.tm, p.tl, p.tb)
    Back(onBack)
    TIPS.forEachIndexed { i, t ->
        Card2(tints[i % tints.size]) {
            T(t.first, 17, FontWeight.ExtraBold)
            T(t.second, 15)
        }
    }
    CHEERS.forEachIndexed { i, s ->
        Card2(tints[(i + 2) % tints.size]) { T(N(s), 16, FontWeight.Bold) }
    }
}

// ---------- the sea question ----------

@Composable
fun SeaDialog(ui: Ui, onClose: () -> Unit) {
    val w = remember { SWEET.random() }
    val t = rememberInfiniteTransition(label = "sea")
    val x by t.animateFloat(1f, -0.3f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "x")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "y")
    androidx.compose.runtime.LaunchedEffect(Unit) { meow(); ui.answeredSea() }
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF8FE3FF), Color(0xFF2A9FE0), Color(0xFF0A4C94))))
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                listOf("🐠", "🐟", "🐡", "🐬", "🐙").forEachIndexed { i, f ->
                    Text(
                        f, fontSize = 34.sp,
                        modifier = Modifier.offset(maxWidth * (((x + 0.3f + i * 0.26f) % 1.3f) - 0.3f), maxHeight * (0.08f + i * 0.17f))
                    )
                }
                repeat(8) { i ->
                    Box(
                        Modifier.offset(maxWidth * (0.08f + i * 0.12f), maxHeight * (1f - (y + i * 0.125f) % 1f))
                            .size(12.dp).clip(CircleShape).background(Color(0x80FFFFFF))
                    )
                }
            }
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Pet(true, 150.dp)
                T("عارفة إنتي شاطرة قد إيه؟", 20, FontWeight.Bold, Color.White, TextAlign.Center)
                T("قد البحر وسمكاته! 🌊🐠", 32, FontWeight.Black, Color.White, TextAlign.Center)
                T(w, 16, FontWeight.Bold, Color.White, TextAlign.Center)
                Button(onClose) { Text("💙 حبيتك يا بحر") }
            }
        }
    }
}
