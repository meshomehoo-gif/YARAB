package com.miumiu.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.net.Uri
import java.util.Calendar

val FLAGS: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
const val TAL_ID = 900100

object Channels {
    fun adhanUri(c: Context): Uri? {
        val id = c.resources.getIdentifier("adhan", "raw", c.packageName)
        return if (id != 0) Uri.parse("android.resource://" + c.packageName + "/" + id) else null
    }

    fun prayerId(c: Context): String = if (adhanUri(c) != null) "prayer_adhan" else "prayer"

    fun ensure(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val alarmAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val med = NotificationChannel("med", "تنبيه الدوا والعلاج", NotificationManager.IMPORTANCE_HIGH)
        med.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAttr)
        med.enableVibration(true)
        med.vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
        med.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(med)

        nm.createNotificationChannel(NotificationChannel("routine", "الروتين اليومي", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel("talent", "تذكير المواهب", NotificationManager.IMPORTANCE_DEFAULT))

        val prayer = NotificationChannel(prayerId(c), "مواعيد الصلاة", NotificationManager.IMPORTANCE_HIGH)
        prayer.setSound(adhanUri(c) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), alarmAttr)
        prayer.enableVibration(true)
        prayer.vibrationPattern = longArrayOf(0, 300, 200, 300)
        prayer.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(prayer)
    }
}

fun emojiBmp(e: String): Bitmap {
    val b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val cv = Canvas(b)
    cv.drawColor(0xFFFFD6EA.toInt())
    val paint = Paint()
    paint.textSize = 86f
    paint.textAlign = Paint.Align.CENTER
    cv.drawText(e, 64f, 92f, paint)
    return b
}

/** A synthesized cute meow, so no audio file is needed. */
fun meow() {
    Thread {
        try {
            val sr = 22050
            val n = sr / 2
            val buf = ShortArray(n)
            var ph = 0.0
            for (i in 0 until n) {
                val t = i / n.toDouble()
                val f = 500 + 450 * Math.sin(Math.PI * t) * (1 - 0.3 * t)
                ph += 2 * Math.PI * f / sr
                buf[i] = ((Math.sin(ph) + 0.4 * Math.sin(2 * ph)) * Math.sin(Math.PI * t) * 9000).toInt().toShort()
            }
            val at = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(n * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            at.write(buf, 0, n)
            at.play()
            Thread.sleep(600)
            at.release()
        } catch (e: Exception) {
            // audio is a bonus; ignore failures
        }
    }.start()
}

object Sched {
    private fun showPi(c: Context): PendingIntent =
        PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    fun nextTime(h: Int, m: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** One PendingIntent per reminder (and per tag): the data URI keeps them distinct. */
    fun remPi(c: Context, r: Rem, tag: String, snooze: Boolean): PendingIntent {
        val i = Intent(c, RemReceiver::class.java)
            .setData(Uri.parse("miumiu://rem/" + tag + "/" + r.id))
            .putExtra("id", r.id)
            .putExtra("t", r.title)
            .putExtra("k", r.kind)
            .putExtra("snz", snooze)
        return PendingIntent.getBroadcast(c, 0, i, FLAGS)
    }

    fun set(c: Context, r: Rem) {
        val am = c.getSystemService(AlarmManager::class.java)
        val at = nextTime(r.h, r.m)
        if (r.kind == "routine") {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, remPi(c, r, "d", false))
        } else {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showPi(c)), remPi(c, r, "d", false))
        }
    }

    fun cancel(c: Context, r: Rem) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(remPi(c, r, "d", false))
        am.cancel(remPi(c, r, "s", true))
    }

    fun snooze(c: Context, id: Int, title: String, kind: String) {
        val am = c.getSystemService(AlarmManager::class.java)
        val r = Rem(id, title, 0, 0, kind)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 600_000, showPi(c)), remPi(c, r, "s", true))
    }

    fun setAll(c: Context) {
        Store.rems(c).forEach { set(c, it) }
    }
}

class RemReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", 0)
        val title = i.getStringExtra("t") ?: "الدوا"
        val kind = i.getStringExtra("k") ?: "med"
        val nm = c.getSystemService(NotificationManager::class.java)
        when (i.getStringExtra("act")) {
            "taken" -> {
                nm.cancel(id)
                Actions.complete(c, id, false)
            }
            "snooze" -> {
                nm.cancel(id)
                Sched.snooze(c, id, title, kind)
            }
            else -> {
                fire(c, nm, id, title, kind)
                if (!i.getBooleanExtra("snz", false)) {
                    Store.rems(c).find { it.id == id }?.let { Sched.set(c, it) }
                }
            }
        }
    }

    private fun fire(c: Context, nm: NotificationManager, id: Int, title: String, kind: String) {
        Channels.ensure(c)
        val loud = kind != "routine"

        fun action(a: String, label: String): Notification.Action {
            val i = Intent(c, RemReceiver::class.java)
                .setData(Uri.parse("miumiu://rem/" + a + "/" + id))
                .putExtra("id", id)
                .putExtra("t", title)
                .putExtra("k", kind)
                .putExtra("act", a)
            return Notification.Action.Builder(0, label, PendingIntent.getBroadcast(c, 0, i, FLAGS)).build()
        }

        val ring = PendingIntent.getActivity(
            c, 0,
            Intent(c, RingActivity::class.java)
                .setData(Uri.parse("miumiu://ring/" + id))
                .putExtra("id", id)
                .putExtra("t", title)
                .putExtra("k", kind)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            FLAGS
        )

        val msg = N(CHEERS.random())
        val head = when (kind) {
            "cat" -> "🐾 ميعاد علاج قطتك: $title"
            "routine" -> "🌿 وقت روتينك: $title"
            else -> "💊 ميعاد $title يا ${nick()}!"
        }
        val bigEmoji = when (kind) {
            "cat" -> "🐱"
            "routine" -> "🌿"
            else -> listOf("🐱", "🐶").random()
        }

        val b = Notification.Builder(c, if (loud) "med" else "routine")
            .setSmallIcon(R.drawable.ic_stat_miu)
            .setLargeIcon(emojiBmp(bigEmoji))
            .setContentTitle(head)
            .setContentText(msg)
            .setStyle(Notification.BigTextStyle().bigText(msg + "\n\n" + TIPS.random().second))
            .setContentIntent(ring)
            .setAutoCancel(true)
            .addAction(action("taken", if (kind == "routine") "عملتها ✓" else "خدتو ✓"))
            .addAction(action("snooze", "بعد 10 دقايق"))
        if (loud) {
            b.setCategory(Notification.CATEGORY_ALARM)
            b.setFullScreenIntent(ring, true)
            b.setOngoing(true)
            b.setTimeoutAfter(180_000)
        }
        val n = b.build()
        if (loud) n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify(id, n)
    }
}

/** Reminds her about her talents every few hours, between 9:00 and 22:00. */
object TalentSched {
    private fun pi(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 0,
            Intent(c, TalentReceiver::class.java).setData(Uri.parse("miumiu://talent/tick")),
            FLAGS
        )

    fun nextTime(hours: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, hours)
        val h = cal.get(Calendar.HOUR_OF_DAY)
        if (h < 9) {
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        } else if (h >= 22) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
        cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    fun schedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (!Store.getBool(c, "tal_on", true) || Store.talents(c).isEmpty()) {
            am.cancel(pi(c))
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime(Store.getInt(c, "tal_hrs", 4)), pi(c))
    }
}

class TalentReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (i.getStringExtra("act") == "practiced") {
            nm.cancel(TAL_ID)
            Actions.practice(c)
            return
        }
        val list = Store.talents(c)
        if (list.isNotEmpty()) {
            Channels.ensure(c)
            val t = list.random()
            val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val done = PendingIntent.getBroadcast(
                c, 0,
                Intent(c, TalentReceiver::class.java)
                    .setData(Uri.parse("miumiu://talent/practiced"))
                    .putExtra("act", "practiced"),
                FLAGS
            )
            val msg = N(T_LINES.random()).replace("{t}", t.name)
            val n = Notification.Builder(c, "talent")
                .setSmallIcon(R.drawable.ic_stat_miu)
                .setLargeIcon(emojiBmp(t.emoji))
                .setContentTitle("🎨 موهبتك مستنياكي يا ${nick()}!")
                .setContentText(msg)
                .setStyle(Notification.BigTextStyle().bigText(msg))
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(Notification.Action.Builder(0, "اشتغلت عليها ✓", done).build())
                .build()
            nm.notify(TAL_ID, n)
        }
        TalentSched.schedule(c)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Sched.setAll(c)
        TalentSched.schedule(c)
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
