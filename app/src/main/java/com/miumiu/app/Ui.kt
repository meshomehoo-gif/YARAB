package com.miumiu.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/** One line in the sorted schedule: a reminder (rem != null) or a prayer time (key != null). */
data class Slot(val kind: String, val title: String, val h: Int, val m: Int, val rem: Rem?, val key: String?)

class Ui(val c: Context) {
    val rems = mutableStateListOf<Rem>().apply { addAll(Store.rems(c)) }
    val talents = mutableStateListOf<Talent>().apply { addAll(Store.talents(c)) }
    var done by mutableStateOf(Store.doneIds(c).toSet())
    var tick by mutableIntStateOf(0)
    var nowM by mutableIntStateOf(minutesNow())
    var pet by mutableStateOf(Store.getStr(c, "pet") ?: "cat")
    var sort by mutableStateOf("time")
    var filter by mutableStateOf("all")
    var say by mutableStateOf(greeting())
    var newAch by mutableStateOf<List<Ach>>(emptyList())

    fun minutesNow(): Int {
        val k = Calendar.getInstance()
        return k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE)
    }

    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val g = if (h < 12) "صباح الفل" else if (h < 18) "نهارك سعيد" else "مساء الخير"
        return g + " يا " + nick() + " 🌸 " + N(CHEERS.random())
    }

    fun toast(s: String) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    fun refresh() {
        done = Store.doneIds(c).toSet()
        nowM = minutesNow()
        tick++
    }

    fun isDone(s: Slot): Boolean {
        val r = s.rem ?: return false
        return r.id in done
    }

    /** Reminders plus today's enabled prayer times, as one list. */
    fun slots(): List<Slot> {
        val l = ArrayList<Slot>()
        rems.forEach { l.add(Slot(it.kind, it.title, it.h, it.m, it, null)) }
        val t = Prayer.timings(c, Calendar.getInstance())
        if (t != null) {
            Prayer.KEYS.forEach { k ->
                if (Prayer.enabled(c, k)) {
                    parseHm(t[k])?.let { hm -> l.add(Slot("prayer", "صلاة " + (Prayer.AR[k] ?: k), hm.first, hm.second, null, k)) }
                }
            }
        }
        return l
    }

    fun sorted(l: List<Slot>, mode: String): List<Slot> =
        if (mode == "next") l.sortedBy { (it.h * 60 + it.m - nowM + 1440) % 1440 } else l.sortedBy { it.h * 60 + it.m }

    fun add(title: String, h: Int, m: Int, kind: String) {
        val r = Rem(Store.nextId(c), title, h, m, kind)
        rems.add(r)
        Store.saveRems(c, rems.toList())
        Sched.set(c, r)
        refresh()
    }

    fun del(r: Rem) {
        Sched.cancel(c, r)
        rems.remove(r)
        Store.saveRems(c, rems.toList())
        refresh()
    }

    fun complete(r: Rem) {
        val a = Actions.complete(c, r.id)
        if (a.isNotEmpty()) newAch = a
        refresh()
    }

    fun addTalent(emoji: String, name: String) {
        talents.add(Talent(Store.nextId(c), emoji, name))
        Store.saveTalents(c, talents.toList())
        TalentSched.schedule(c)
        val a = Stats.bump(c)
        if (a.isNotEmpty()) newAch = a
        refresh()
    }

    fun delTalent(t: Talent) {
        talents.remove(t)
        Store.saveTalents(c, talents.toList())
        TalentSched.schedule(c)
        refresh()
    }

    fun practice(t: Talent) {
        val a = Actions.practice(c)
        if (a.isNotEmpty()) newAch = a
        toast("برافو يا " + nick() + "، ربنا يزيدك " + t.name + " 🎨")
        refresh()
    }

    fun answeredSea() {
        val a = if (Stats.get(c, "sea") == 0) Stats.bump(c, listOf("sea")) else emptyList()
        if (a.isNotEmpty()) newAch = a
        refresh()
    }

    fun refreshPrayer() {
        Thread {
            Prayer.refreshBlocking(c)
            Handler(Looper.getMainLooper()).post {
                tick++
                toast("اتحدّثت مواعيد الصلاة")
            }
        }.start()
    }

    /** Sends a real test notification through the same receivers the alarms use. */
    fun test(kind: String) {
        val i: Intent = when (kind) {
            "talent" -> Intent(c, TalentReceiver::class.java).setData(Uri.parse("miumiu://talent/test"))
            "prayer" -> Intent(c, PrayerReceiver::class.java).setData(Uri.parse("miumiu://prayer/test/Dhuhr"))
                .putExtra("key", "Dhuhr").putExtra("act", "test")
            else -> {
                val n = sorted(slots().filter { it.kind != "prayer" }, "next").firstOrNull()
                Intent(c, RemReceiver::class.java).setData(Uri.parse("miumiu://rem/test/777"))
                    .putExtra("id", 777).putExtra("t", n?.title ?: "دوا تجريبي").putExtra("k", n?.kind ?: "med")
            }
        }
        c.sendBroadcast(i)
    }
}
