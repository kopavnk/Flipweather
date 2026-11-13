package org.example.flipweather

import android.content.Context

data class Loc(val name: String, val lat: Double, val lon: Double)
data class Wx(
    val temp: Double, val code: Int, val day: Boolean,
    val tMax: Double, val tMin: Double, val wind: Double, val gust: Double, val worstCode: Int
)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("flipweather", Context.MODE_PRIVATE)

    fun locations(c: Context): List<Loc> =
        p(c).getString("locs", "")!!.lines().mapNotNull {
            val s = it.split("|")
            if (s.size != 3) return@mapNotNull null
            Loc(s[0], s[1].toDoubleOrNull() ?: return@mapNotNull null, s[2].toDoubleOrNull() ?: return@mapNotNull null)
        }

    fun setLocations(c: Context, l: List<Loc>) {
        val s = l.joinToString("\n") { "${it.name.replace("|", " ").replace("\n", " ")}|${it.lat}|${it.lon}" }
        p(c).edit().putString("locs", s).putInt("idx", 0).apply()
    }

    fun alpha(c: Context) = p(c).getInt("alpha", 200)           // 0..255
    fun setAlpha(c: Context, a: Int) = p(c).edit().putInt("alpha", a.coerceIn(0, 255)).apply()
    fun index(c: Context) = p(c).getInt("idx", 0)
    fun setIndex(c: Context, i: Int) = p(c).edit().putInt("idx", i).apply()

    private fun key(l: Loc) = "wx_${l.lat}_${l.lon}"
    fun putWx(c: Context, l: Loc, w: Wx) = p(c).edit().putString(
        key(l), listOf(w.temp, w.code, if (w.day) 1 else 0, w.tMax, w.tMin, w.wind, w.gust, w.worstCode).joinToString(";")
    ).apply()

    fun wx(c: Context, l: Loc): Wx? {
        val s = p(c).getString(key(l), null)?.split(";") ?: return null
        if (s.size != 8) return null
        return try {
            Wx(s[0].toDouble(), s[1].toInt(), s[2] == "1", s[3].toDouble(), s[4].toDouble(),
                s[5].toDouble(), s[6].toDouble(), s[7].toInt())
        } catch (e: NumberFormatException) { null }
    }
}
