package org.example.flipweather

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*

class ConfigActivity : Activity() {
    private var locs = mutableListOf<Loc>()
    private lateinit var saved: LinearLayout
    private lateinit var results: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID)
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        locs = Store.locations(this).toMutableList()

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        root.addView(TextView(this).apply { text = "Background opacity" })
        root.addView(SeekBar(this).apply {
            max = 100; progress = Store.alpha(this@ConfigActivity) * 100 / 255
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    Store.setAlpha(this@ConfigActivity, p * 255 / 100); WeatherWidget.render(this@ConfigActivity)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        root.addView(TextView(this).apply { text = "\nLocations (first one is home)" })
        saved = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(saved)

        val q = EditText(this).apply { hint = "Search city"; setSingleLine() }
        root.addView(q)
        root.addView(Button(this).apply {
            text = "Search"
            setOnClickListener {
                val term = q.text.toString().trim()
                if (term.isEmpty()) return@setOnClickListener
                Thread {
                    val r = try { Weather.search(term) } catch (e: Exception) { emptyList() }
                    runOnUiThread { showResults(r) }
                }.start()
            }
        })
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(results)

        root.addView(Button(this).apply {
            text = "Done"
            setOnClickListener {
                save()
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                finish()
            }
        })
        setContentView(ScrollView(this).apply { addView(root, ViewGroup.LayoutParams(-1, -2)) })
        refreshSaved()
    }

    private fun save() {
        Store.setLocations(this, locs)
        WeatherWidget.render(this)
        RefreshWorker.schedule(this); RefreshWorker.now(this)
    }

    private fun showResults(r: List<Pair<String, Loc>>) {
        results.removeAllViews()
        if (r.isEmpty()) results.addView(TextView(this).apply { text = "No results (or offline)" })
        r.forEach { (label, loc) ->
            results.addView(Button(this).apply {
                text = "+ $label"; isAllCaps = false
                setOnClickListener { locs.add(loc); results.removeAllViews(); save(); refreshSaved() }
            })
        }
    }

    private fun refreshSaved() {
        saved.removeAllViews()
        locs.forEachIndexed { i, l ->
            saved.addView(LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(TextView(this@ConfigActivity).apply { text = l.name }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(Button(this@ConfigActivity).apply {
                    text = "✕"
                    setOnClickListener { locs.removeAt(i); save(); refreshSaved() }
                })
            })
        }
    }
}
