package com.lucas.islandlauncher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var islandClock: TextView
    private val apps = mutableListOf<AppItem>()

    private val dockPackages = listOf(
        "com.android.chrome",
        "com.whatsapp",
        "com.google.android.youtube",
        "com.android.settings"
    )

    data class AppItem(val label: String, val pkg: String, val icon: Drawable)

    private val clockTick = object : Runnable {
        override fun run() {
            if (::islandClock.isInitialized) {
                islandClock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            islandClock = findViewById(R.id.islandClock)
            loadApps()
            setupGrid()
            setupDock()
        } catch (e: Throwable) {
            val tv = TextView(this)
            tv.setBackgroundColor(0xFF000000.toInt())
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.setPadding(40, 120, 40, 40)
            tv.text = "ERRO:\n" + e.stackTraceToString()
            setContentView(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockTick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
    }

    override fun onBackPressed() {
        // launcher não fecha ao apertar voltar
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        apps.clear()
        list.filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            .forEach {
                apps.add(
                    AppItem(
                        it.loadLabel(packageManager).toString(),
                        it.activityInfo.packageName,
                        it.loadIcon(packageManager)
                    )
                )
            }
    }

    private fun setupGrid() {
        val grid = findViewById<RecyclerView>(R.id.appGrid)
        grid.layoutManager = GridLayoutManager(this, 4)
        grid.adapter = AppAdapter(apps) { launch(it.pkg) }
    }

    private fun setupDock() {
        val dock = findViewById<LinearLayout>(R.id.dock)
        val size = (52 * resources.displayMetrics.density).toInt()
        dockPackages.forEach { pkg ->
            val item = apps.find { it.pkg == pkg } ?: return@forEach
            val iv = ImageView(this).apply {
                setImageDrawable(item.icon)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(10, 0, 10, 0)
                }
                setOnClickListener { launch(pkg) }
                setOnTouchListener { v, e ->
                    val s = if (e.action == MotionEvent.ACTION_DOWN) 1.25f else 1f
                    v.animate().scaleX(s).scaleY(s).setDuration(120).start()
                    false
                }
            }
            dock.addView(iv)
        }
    }

    private fun launch(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    inner class AppAdapter(
        private val items: List<AppItem>,
        private val onClick: (AppItem) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {

        inner class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val root = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 16, 8, 16)
            }
            root.addView(ImageView(parent.context))
            root.addView(TextView(parent.context).apply {
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 11f
                gravity = Gravity.CENTER
            })
            return VH(root)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            (holder.root.getChildAt(0) as ImageView).apply {
                setImageDrawable(item.icon)
                layoutParams = LinearLayout.LayoutParams(128, 128)
            }
            (holder.root.getChildAt(1) as TextView).text = item.label
            holder.root.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
