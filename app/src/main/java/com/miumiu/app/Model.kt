package com.miumiu.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** kind is one of: med, cat, routine */
data class Rem(val id: Int, val title: String, val h: Int, val m: Int, val kind: String)

data class Talent(val id: Int, val emoji: String, val name: String)

/** key names the counter that is compared with goal. */
data class Ach(val id: String, val emoji: String, val title: String, val desc: String, val key: String, val goal: Int)

val ACHS = listOf(
    Ach("first", "🌱", "أول خطوة", "خلّصتي أول مهمة", "done", 1),
    Ach("ten", "⭐", "نجمة مواظبة", "10 مهام اتعملت", "done", 10),
    Ach("fifty", "👑", "ملكة الالتزام", "50 مهمة اتعملت", "done", 50),
    Ach("s3", "🔥", "3 أيام ورا بعض", "نشاط 3 أيام متتالية", "best", 3),
    Ach("s7", "🌈", "أسبوع كامل", "7 أيام متتالية", "best", 7),
    Ach("s30", "💎", "شهر من الجمال", "30 يوم متتالي", "best", 30),
    Ach("t1", "🎨", "صاحبة موهبة", "ضيفتي أول موهبة", "talents", 1),
    Ach("t3", "🎭", "متعددة المواهب", "3 مواهب", "talents", 3),
    Ach("p5", "🎤", "بتتدربي صح", "اشتغلتي على مواهبك 5 مرات", "practice", 5),
    Ach("c3", "🐱", "ماما القطة", "3 مرات اهتميتي بقطتك", "cat", 3),
    Ach("sea", "🌊", "قد البحر وسمكاته", "جاوبتي سؤال البحر", "sea", 1)
)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("miumiu", Context.MODE_PRIVATE)

    fun dayKey(offset: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)
    }

    fun getStr(c: Context, k: String): String? = sp(c).getString(k, null)
    fun putStr(c: Context, k: String, v: String) { sp(c).edit().putString(k, v).apply() }
    fun getInt(c: Context, k: String, def: Int = 0): Int = sp(c).getInt(k, def)
    fun putInt(c: Context, k: String, v: Int) { sp(c).edit().putInt(k, v).apply() }
    fun getBool(c: Context, k: String, def: Boolean = false): Boolean = sp(c).getBoolean(k, def)
    fun putBool(c: Context, k: String, v: Boolean) { sp(c).edit().putBoolean(k, v).apply() }

    fun nextId(c: Context): Int {
        val n = getInt(c, "nid", 1000)
        putInt(c, "nid", n + 1)
        return n
    }

    fun rems(c: Context): List<Rem> {
        return try {
            val a = JSONArray(getStr(c, "rems") ?: "[]")
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Rem(o.getInt("id"), o.getString("t"), o.getInt("h"), o.getInt("m"), o.getString("k"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRems(c: Context, l: List<Rem>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("t", it.title).put("h", it.h).put("m", it.m).put("k", it.kind)) }
        putStr(c, "rems", a.toString())
    }

    fun talents(c: Context): List<Talent> {
        return try {
            val a = JSONArray(getStr(c, "talents") ?: "[]")
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Talent(o.getInt("id"), o.getString("e"), o.getString("n"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTalents(c: Context, l: List<Talent>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("e", it.emoji).put("n", it.name)) }
        putStr(c, "talents", a.toString())
    }

    /** Ids of reminders completed today. The list resets by itself when the date changes. */
    fun doneIds(c: Context): MutableSet<Int> {
        val p = (getStr(c, "done") ?: "").split("|")
        if (p.size != 2 || p[0] != dayKey()) return mutableSetOf()
        return p[1].split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    fun saveDone(c: Context, s: Set<Int>) {
        putStr(c, "done", dayKey() + "|" + s.joinToString(","))
    }
}

object Stats {
    fun get(c: Context, k: String): Int = Store.getInt(c, "st_$k")

    fun progress(c: Context, a: Ach): Int =
        if (a.key == "talents") Store.talents(c).size else get(c, a.key)

    fun unlocked(c: Context): Set<String> =
        ACHS.filter { progress(c, it) >= it.goal }.map { it.id }.toSet()

    /** Current streak counts only if she was active today or yesterday. */
    fun streak(c: Context): Int {
        val last = Store.getStr(c, "st_last")
        return if (last == Store.dayKey() || last == Store.dayKey(-1)) get(c, "streak") else 0
    }

    /** Adds to the named counters, optionally marks today as active, and returns newly unlocked achievements. */
    fun bump(c: Context, keys: List<String> = emptyList(), day: Boolean = false): List<Ach> {
        val before = unlocked(c)
        keys.forEach { Store.putInt(c, "st_$it", get(c, it) + 1) }
        if (day) {
            val today = Store.dayKey()
            val last = Store.getStr(c, "st_last")
            if (last != today) {
                val streak = if (last == Store.dayKey(-1)) get(c, "streak") + 1 else 1
                Store.putInt(c, "st_streak", streak)
                Store.putStr(c, "st_last", today)
                if (streak > get(c, "best")) Store.putInt(c, "st_best", streak)
            }
        }
        val after = unlocked(c)
        return ACHS.filter { it.id in after && it.id !in before }
    }
}

object Actions {
    /** Marks a reminder done today. With toggle=true a second call undoes it; notification buttons pass false. */
    fun complete(c: Context, id: Int, toggle: Boolean = true): List<Ach> {
        val r = Store.rems(c).find { it.id == id } ?: return emptyList()
        val s = Store.doneIds(c)
        if (id in s) {
            if (toggle) {
                s.remove(id)
                Store.saveDone(c, s)
            }
            return emptyList()
        }
        s.add(id)
        Store.saveDone(c, s)
        return Stats.bump(c, if (r.kind == "cat") listOf("done", "cat") else listOf("done"), true)
    }

    fun practice(c: Context): List<Ach> = Stats.bump(c, listOf("practice"), true)
}
