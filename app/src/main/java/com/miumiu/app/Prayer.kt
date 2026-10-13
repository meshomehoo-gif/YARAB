package com.miumiu.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Prayer times come from the Aladhan API (api.aladhan.com), calculated with method 5,
 * the Egyptian General Authority of Survey. Times are cached for a few days so the alarms
 * keep working offline, and every prayer alarm refreshes the cache for the days ahead.
 */
object Prayer {
    val KEYS = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val AR = mapOf(
        "Fajr" to "الفجر", "Sunrise" to "الشروق", "Dhuhr" to "الظهر",
        "Asr" to "العصر", "Maghrib" to "المغرب", "Isha" to "العشاء"
    )
    private const val CAIRO_LAT = 30.0444
    private const val CAIRO_LNG = 31.2357
    private const val METHOD = 5

    data class Next(val name: String, val h: Int, val m: Int, val left: Int)

    private fun dateStr(cal: Calendar): String = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time)
    private fun dayCal(offset: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        return cal
    }

    fun enabled(c: Context, key: String): Boolean = Store.getBool(c, "pr_$key", true)
    fun setEnabled(c: Context, key: String, on: Boolean) = Store.putBool(c, "pr_$key", on)

    fun lat(c: Context): Double = Store.getStr(c, "lat")?.toDoubleOrNull() ?: CAIRO_LAT
    fun lng(c: Context): Double = Store.getStr(c, "lng")?.toDoubleOrNull() ?: CAIRO_LNG
    fun hasCustomLocation(c: Context): Boolean = Store.getStr(c, "lat") != null

    private fun cache(c: Context): JSONObject =
        try { JSONObject(Store.getStr(c, "pcache") ?: "{}") } catch (e: Exception) { JSONObject() }

    /** Cached timings for a day, or null if that day has not been downloaded yet. */
    fun timings(c: Context, cal: Calendar): Map<String, String>? {
        val o = cache(c).optJSONObject(dateStr(cal)) ?: return null
        val out = HashMap<String, String>()
        (KEYS + "Sunrise").forEach { k ->
            val v = o.optString(k)
            if (v.length >= 5) out[k] = v
        }
        return if (out.size == 6) out else null
    }

    fun lastUpdated(c: Context): String? = Store.getStr(c, "pupdated")

    private fun fetch(c: Context, cal: Calendar): Boolean {
        return try {
            val url = URL(
                "https://api.aladhan.com/v1/timings/" + dateStr(cal) +
                    "?latitude=" + lat(c) + "&longitude=" + lng(c) +
                    "&method=" + METHOD + "&timezonestring=" + TimeZone.getDefault().id
            )
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = 6000
            con.readTimeout = 6000
            val body = con.inputStream.bufferedReader().use { it.readText() }
            val t = JSONObject(body).getJSONObject("data").getJSONObject("timings")
            val day = JSONObject()
            (KEYS + "Sunrise").forEach { k -> day.put(k, t.getString(k).trim().take(5)) }
            val all = cache(c)
            all.put(dateStr(cal), day)
            val keep = (-1..3).map { dateStr(dayCal(it)) }.toSet()
            all.keys().asSequence().toList().filter { it !in keep }.forEach { all.remove(it) }
            Store.putStr(c, "pcache", all.toString())
            Store.putStr(c, "pupdated", SimpleDateFormat("d/M HH:mm", Locale.US).format(Calendar.getInstance().time))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun millis(base: Calendar, hm: String): Long? {
        val p = parseHm(hm) ?: return null
        val cal = base.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, p.first)
        cal.set(Calendar.MINUTE, p.second)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun pi(c: Context, key: String, tag: String, act: String?): PendingIntent {
        val i = Intent(c, PrayerReceiver::class.java)
            .setData(Uri.parse("miumiu://prayer/" + tag + "/" + key))
            .putExtra("key", key)
        if (act != null) i.putExtra("act", act)
        return PendingIntent.getBroadcast(c, 0, i, FLAGS)
    }

    private fun showPi(c: Context): PendingIntent =
        PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    /** Sets one alarm-clock alarm for every enabled prayer in the next three days that is still ahead. */
    fun schedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        for (off in 0..2) {
            val cal = dayCal(off)
            val t = timings(c, cal) ?: continue
            for (k in KEYS) {
                val hm = t[k] ?: continue
                val at = millis(cal, hm) ?: continue
                val p = pi(c, k, dateStr(cal), null)
                if (at > now + 30_000 && enabled(c, k)) {
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showPi(c)), p)
                } else {
                    am.cancel(p)
                }
            }
        }
    }

    private fun retryLater(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 30 * 60_000,
            pi(c, "retry", "retry", "refresh")
        )
    }

    /** Downloads any missing day (today, tomorrow, the day after), then schedules. Call off the main thread. */
    fun refreshBlocking(c: Context) {
        var missing = false
        for (off in 0..2) {
            val cal = dayCal(off)
            if (timings(c, cal) == null && !fetch(c, cal)) missing = true
        }
        schedule(c)
        if (missing) retryLater(c)
    }

    fun refresh(c: Context) {
        Thread { refreshBlocking(c) }.start()
    }

    /** The next enabled prayer today (or the first one again if today's are over). */
    fun next(c: Context, nowM: Int): Next? {
        val t = timings(c, Calendar.getInstance()) ?: return null
        val list = KEYS.filter { enabled(c, it) }.mapNotNull { k ->
            parseHm(t[k])?.let { Triple(k, it.first, it.second) }
        }
        if (list.isEmpty()) return null
        val x = list.firstOrNull { it.second * 60 + it.third > nowM } ?: list.first()
        return Next(AR[x.first] ?: x.first, x.second, x.third, (x.second * 60 + x.third - nowM + 1440) % 1440)
    }

    /** Uses the last known phone location. Returns false if there is none or permission is missing. */
    @SuppressLint("MissingPermission")
    fun updateLocation(c: Context): Boolean {
        val ok = c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) return false
        val lm = c.getSystemService(LocationManager::class.java)
        val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (e: Exception) { null } }
            .maxByOrNull { it.time } ?: return false
        Store.putStr(c, "lat", loc.latitude.toString())
        Store.putStr(c, "lng", loc.longitude.toString())
        Store.putStr(c, "pcache", "{}")
        return true
    }

    fun notify(c: Context, key: String) {
        Channels.ensure(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        val name = AR[key] ?: key
        val n = Notification.Builder(c, Channels.prayerId(c))
            .setSmallIcon(R.drawable.ic_stat_miu)
            .setLargeIcon(emojiBmp("🕌"))
            .setContentTitle("🕌 حان الآن موعد صلاة $name")
            .setContentText(N(P_LINES.random()))
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(showPi(c))
            .setAutoCancel(true)
            .setTimeoutAfter(10 * 60_000)
            .addAction(Notification.Action.Builder(0, "تمام 🤍", pi(c, key, "ok", "ok")).build())
            .build()
        nm.notify(900 + KEYS.indexOf(key), n)
    }
}

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val key = i.getStringExtra("key") ?: ""
        val nm = c.getSystemService(NotificationManager::class.java)
        when (i.getStringExtra("act")) {
            "ok" -> nm.cancel(900 + Prayer.KEYS.indexOf(key))
            "test" -> Prayer.notify(c, if (key in Prayer.KEYS) key else "Dhuhr")
            else -> {
                if (i.getStringExtra("act") != "refresh") Prayer.notify(c, key)
                val pending = goAsync()
                Thread {
                    try {
                        Prayer.refreshBlocking(c)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }
}
