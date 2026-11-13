package org.example.flipweather

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.view.View
import android.widget.RemoteViews
import kotlin.math.roundToInt

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        render(c); RefreshWorker.schedule(c); RefreshWorker.now(c)
    }

    override fun onReceive(c: Context, i: Intent) {
        super.onReceive(c, i)
        when (i.action) {
            ACT_NEXT -> step(c, 1)
            ACT_PREV -> step(c, -1)
            ACT_HOME -> { Store.setIndex(c, 0); render(c) }
            Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_LOW, Intent.ACTION_BATTERY_OKAY -> render(c)
        }
    }

    companion object {
        private const val ACT_NEXT = "org.example.flipweather.NEXT"
        private const val ACT_PREV = "org.example.flipweather.PREV"
        private const val ACT_HOME = "org.example.flipweather.HOME"
        private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        private fun bc(c: Context, action: String) = PendingIntent.getBroadcast(
            c, 0, Intent(c, WeatherWidget::class.java).setAction(action), FLAGS)

        private fun act(c: Context, i: Intent) =
            PendingIntent.getActivity(c, 0, i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS)

        /** Flip to the next/previous location; fall back to home after 10 s (inexact alarm, may drift slightly). */
        private fun step(c: Context, d: Int) {
            val n = Store.locations(c).size
            if (n == 0) return
            Store.setIndex(c, ((Store.index(c) + d) % n + n) % n)
            render(c)
            val am = c.getSystemService(AlarmManager::class.java)
            val home = bc(c, ACT_HOME)
            if (Store.index(c) != 0) am.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + 10_000, home)
            else am.cancel(home)
        }

        /** Launch the default clock app's own entry point (AlarmClock intents only open specific tabs). */
        private fun clockIntent(c: Context): Intent {
            val alarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val pkg = c.packageManager.resolveActivity(alarms, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            val launch = if (pkg != null && pkg != "android") c.packageManager.getLaunchIntentForPackage(pkg) else null
            return launch ?: alarms
        }

        fun render(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, WeatherWidget::class.java))
            if (ids.isNotEmpty()) mgr.updateAppWidget(ids, build(c))
        }

        private fun build(c: Context): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget)
            v.setInt(R.id.bg, "setImageAlpha", Store.alpha(c))

            // 1) clock -> main screen of the default clock app
            v.setOnClickPendingIntent(R.id.clock_tile, act(c, clockIntent(c)))

            // 3) battery -> system battery usage
            v.setOnClickPendingIntent(R.id.battery_tile, act(c, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)))
            c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { b ->
                val scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val lvl = b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale
                val chg = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                v.setProgressBar(R.id.bat_bar, 100, lvl, false)
                v.setTextViewText(R.id.bat_pct, (if (chg) "⚡" else "") + lvl)
            }

            // 2) weather
            val locs = Store.locations(c)
            if (locs.isEmpty()) {
                v.setTextViewText(R.id.name, "Add a location")
                v.setTextViewText(R.id.icon, "➕")
                v.setTextViewText(R.id.temps, ""); v.setTextViewText(R.id.wind, "")
                v.setViewVisibility(R.id.warn, View.GONE)
                v.setViewVisibility(R.id.prev, View.INVISIBLE); v.setViewVisibility(R.id.next, View.INVISIBLE)
                v.setOnClickPendingIntent(R.id.wx_row, act(c, Intent(c, ConfigActivity::class.java)))
                return v
            }
            val l = locs[Store.index(c).coerceIn(0, locs.lastIndex)]
            val w = Store.wx(c, l)
            val flip = if (locs.size > 1) View.VISIBLE else View.INVISIBLE
            v.setViewVisibility(R.id.prev, flip); v.setViewVisibility(R.id.next, flip)
            v.setOnClickPendingIntent(R.id.prev, bc(c, ACT_PREV))
            v.setOnClickPendingIntent(R.id.next, bc(c, ACT_NEXT))
            v.setTextViewText(R.id.name, l.name)

            // Tap -> accuweather.com search for this place (opens the AccuWeather app if it handles the link)
            val open = act(c, Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.accuweather.com/en/search-locations?query=" + Uri.encode(l.name))))
            v.setOnClickPendingIntent(R.id.wx_row, open)
            v.setOnClickPendingIntent(R.id.name, open)

            if (w == null) {
                v.setTextViewText(R.id.icon, "…"); v.setTextViewText(R.id.temps, ""); v.setTextViewText(R.id.wind, "")
                v.setViewVisibility(R.id.warn, View.GONE)
            } else {
                v.setTextViewText(R.id.icon, Weather.emoji(w.code, w.day))
                v.setTextViewText(R.id.temps, "${w.tMax.roundToInt()}/${w.tMin.roundToInt()}")
                v.setTextViewText(R.id.wind, "${w.wind.roundToInt()} km/h")
                v.setViewVisibility(R.id.warn, if (Weather.severe(w)) View.VISIBLE else View.GONE)
            }
            return v
        }
    }
}
