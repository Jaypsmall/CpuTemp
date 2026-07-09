package com.jaylizapp.cputemp

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.abs

data class Sensor(val name: String, val path: String)

class ThermalOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val handler = Handler(Looper.getMainLooper())

    private val sensorResults = mutableStateMapOf<String, String>()
    private var cpuMode by mutableStateOf("NORMAL") 
    private var statusColor by mutableStateOf(Color(0xFF00E5FF))
    private var isRunning = true

    private val sensors = mutableStateListOf(
        Sensor("🔥 CPU", "/sys/class/thermal/thermal_zone3/temp"),
        Sensor("🎮 GPU", "/sys/class/thermal/thermal_zone4/temp"),
        Sensor("🔋 BAT", "/sys/class/thermal/thermal_zone1/temp")
    )
    private val governorPath = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"

    // Lifecycle boilerplate
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        initForeground()
        showOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        startThermalLoop()
    }

    private fun startThermalLoop() {
        Thread {
            var process: java.lang.Process? = null
            var os: DataOutputStream? = null
            var reader: BufferedReader? = null

            try {
                // Abrimos la sesión de ROOT una sola vez para evitar el spam de notificaciones
                process = Runtime.getRuntime().exec("su")
                os = DataOutputStream(process.outputStream)
                reader = BufferedReader(InputStreamReader(process.inputStream))

                while (isRunning) {
                    sensors.forEach { os.writeBytes("cat ${it.path} 2>/dev/null || echo NA\n") }
                    os.writeBytes("cat $governorPath 2>/dev/null || echo unknown\n")
                    os.writeBytes("echo \"END_BATCH\"\n")
                    os.flush()

                    val lines = mutableListOf<String>()
                    while (true) {
                        val line = reader.readLine()?.trim() ?: "END_BATCH"
                        if (line == "END_BATCH") break
                        lines.add(line)
                    }

                    if (lines.isNotEmpty()) {
                        handler.post {
                            var maxT = 0f
                            sensors.forEachIndexed { index, sensor ->
                                if (index < lines.size) {
                                    val raw = lines[index]
                                    val formatted = formatRaw(raw)
                                    sensorResults[sensor.name] = formatted
                                    
                                    val v = raw.toFloatOrNull() ?: 0f
                                    val temp = if (abs(v) > 200) v / 1000f else v
                                    if (temp > maxT) maxT = temp
                                }
                            }
                            
                            if (lines.size > sensors.size) {
                                cpuMode = lines.last().uppercase()
                            }
                            
                            statusColor = when {
                                maxT < 42 -> Color(0xFF00E5FF)
                                maxT < 60 -> Color(0xFFFFFF00)
                                else -> Color(0xFFFF5252)
                            }
                        }
                    }
                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    sensors.forEach { sensorResults[it.name] = readTempDirect(it.path) ?: "NA" }
                    cpuMode = readTextDirect(governorPath)?.uppercase() ?: "ROOT?"
                }
            } finally {
                try {
                    os?.writeBytes("exit\n")
                    os?.flush()
                    process?.destroy()
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun formatRaw(raw: String): String {
        if (raw == "NA" || raw.isEmpty()) return "NA"
        val v = raw.toFloatOrNull() ?: return "NA"
        val temp = if (abs(v) > 200) v / 1000f else v
        return "%.1f°C".format(Locale.getDefault(), temp)
    }

    private fun readTempDirect(path: String): String? {
        val raw = readTextDirect(path) ?: return null
        return formatRaw(raw)
    }

    private fun readTextDirect(path: String): String? {
        return try {
            File(path).takeIf { it.exists() && it.canRead() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100; y = 100
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ThermalOverlayService)
            setViewTreeViewModelStoreOwner(this@ThermalOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ThermalOverlayService)
            setContent { HUDContent() }
        }
        
        windowManager.addView(composeView, params)
        setupTouch(params)
    }

    @Composable
    private fun HUDContent() {
        Column(
            modifier = Modifier
                .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                .border(2.dp, statusColor, RoundedCornerShape(12.dp))
                .padding(10.dp).width(130.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔥 THERMAL HUD", color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("━━━━━━━━━━━━", color = statusColor.copy(alpha = 0.3f), fontSize = 8.sp)
            
            Text("MODE: $cpuMode", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            sensors.forEach { sensor ->
                StatRow(sensor.name, sensorResults[sensor.name] ?: "...")
            }
            
            Text("━━━━━━━━━━━━", color = statusColor.copy(alpha = 0.3f), fontSize = 8.sp)
        }
    }

    @Composable
    private fun StatRow(label: String, value: String) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    private fun setupTouch(params: WindowManager.LayoutParams) {
        composeView?.setOnTouchListener(object : View.OnTouchListener {
            private var ix = 0; private var iy = 0; private var itx = 0f; private var ity = 0f
            private var moved = false
            private var clicks = 0; private var lastTime = 0L

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ix = params.x; iy = params.y; itx = e.rawX; ity = e.rawY; moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - itx; val dy = e.rawY - ity
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            moved = true
                            params.x = ix + dx.toInt(); params.y = iy + dy.toInt()
                            windowManager.updateViewLayout(composeView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            val now = System.currentTimeMillis()
                            clicks = if (now - lastTime < 400) clicks + 1 else 1
                            lastTime = now
                            when (clicks) {
                                2 -> {
                                    val intent = Intent(this@ThermalOverlayService, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    startActivity(intent)
                                }
                                3 -> { stopSelf(); android.os.Process.killProcess(android.os.Process.myPid()) }
                            }
                        } else {
                            v.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    fun cambiarModoCpu(modo: String) {
        val gov = when(modo) {
            "cool" -> "powersave"
            "boost" -> "performance"
            else -> "schedutil"
        }
        Thread {
            try {
                val p = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(p.outputStream)
                os.writeBytes("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo $gov > \$cpu; done\n")
                os.writeBytes("exit\n")
                os.flush()
                p.waitFor()
                handler.post { Toast.makeText(this, "Perfil $modo ($gov) Activado", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                handler.post { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun initForeground() {
        val chan = NotificationChannel("thermal", "Thermal Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)

        val notification = Notification.Builder(this, "thermal")
            .setContentTitle("HUD Térmico Activo")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var instance: ThermalOverlayService? = null
    }
}
