package org.example.flipweather

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Open-Meteo client: free, keyless, metric (°C, km/h). */
object Weather {
    private fun get(url: String): JSONObject {
        val cn = URL(url).openConnection() as HttpURLConnection
        cn.connectTimeout = 10_000; cn.readTimeout = 10_000
        try { return JSONObject(cn.inputStream.bufferedReader().readText()) } finally { cn.disconnect() }
    }

    fun fetch(l: Loc): Wx {
        val j = get(
            "https://api.open-meteo.com/v1/forecast?latitude=${l.lat}&longitude=${l.lon}" +
            "&current=temperature_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min,wind_gusts_10m_max,weather_code" +
            "&forecast_days=1&timezone=auto&wind_speed_unit=kmh&temperature_unit=celsius"
        )
        val cur = j.getJSONObject("current")
        val d = j.getJSONObject("daily")
        return Wx(
            cur.getDouble("temperature_2m"), cur.getInt("weather_code"), cur.getInt("is_day") == 1,
            d.getJSONArray("temperature_2m_max").getDouble(0), d.getJSONArray("temperature_2m_min").getDouble(0),
            cur.getDouble("wind_speed_10m"), d.getJSONArray("wind_gusts_10m_max").getDouble(0),
            d.getJSONArray("weather_code").getInt(0)
        )
    }

    /** Returns (label, location) pairs. */
    fun search(q: String): List<Pair<String, Loc>> {
        val j = get("https://geocoding-api.open-meteo.com/v1/search?count=6&language=en&name=" + URLEncoder.encode(q, "UTF-8"))
        val r = j.optJSONArray("results") ?: return emptyList()
        return (0 until r.length()).map {
            val o = r.getJSONObject(it)
            val label = listOf(o.optString("name"), o.optString("admin1"), o.optString("country_code"))
                .filter { s -> s.isNotBlank() }.joinToString(", ")
            label to Loc(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    // Open-Meteo has no official alert feed, so "severe" is derived from the forecast.
    fun severe(w: Wx) = w.worstCode in intArrayOf(65, 67, 75, 77, 82, 86, 95, 96, 99) ||
        w.gust >= 70 || w.tMax >= 35 || w.tMin <= -15

    fun emoji(code: Int, day: Boolean) = when (code) {
        0 -> if (day) "☀️" else "🌙"
        1, 2 -> if (day) "🌤️" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67 -> "🌧️"
        in 71..77 -> "🌨️"
        in 80..82 -> "🌧️"
        85, 86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "❔"
    }
}
