package com.jaylizapp.cputemp

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import java.io.DataOutputStream
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var hasOverlay by remember { mutableStateOf(false) }
            var liveBatteryTemp by remember { mutableStateOf("-- °C") }
            var currentGovernor by remember { mutableStateOf("unknown") }

            // Monitor de estado en tiempo real
            LaunchedEffect(Unit) {
                while(true) {
                    hasOverlay = Settings.canDrawOverlays(this@MainActivity)

                    // Batería
                    val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    liveBatteryTemp = "${temp / 10.0} °C"

                    // Gobernador actual
                    currentGovernor = readCurrentGovernor()

                    delay(1000.milliseconds)
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Pro
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF00E5FF))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("HUD TÉRMICO ENGINE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Gobernador: $currentGovernor", color = Color(0xFF00E5FF), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    // Monitor Batería
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF76FF03))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TEMPERATURA BATERÍA", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(liveBatteryTemp, color = Color(0xFF76FF03), fontSize = 32.sp, fontWeight = FontWeight.Black)
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF76FF03), modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    // SECCIÓN DE PERFILES
                    Text("CONTROL DE GOBERNADOR (ROOT)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileButton("COOL", "powersave", Color(0xFF00E5FF), currentGovernor == "powersave", Modifier.weight(1f)) {
                            ThermalOverlayService.instance?.cambiarModoCpu("cool") ?: applyGovernor("powersave")
                        }
                        ProfileButton("NORMAL", detectNormalGovernor(), Color(0xFF76FF03), currentGovernor == detectNormalGovernor(), Modifier.weight(1f)) {
                            ThermalOverlayService.instance?.cambiarModoCpu("normal") ?: applyGovernor(detectNormalGovernor())
                        }
                        ProfileButton("BOOST", "performance", Color(0xFFFF5252), currentGovernor == "performance", Modifier.weight(1f)) {
                            ThermalOverlayService.instance?.cambiarModoCpu("boost") ?: applyGovernor("performance")
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Configuración
                    Text("AJUSTES DE SISTEMA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionRow("Permiso de Ventana Flotante", hasOverlay) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:$packageName".toUri())
                        startActivity(intent)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { checkRoot() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SOLICITAR ACCESO ROOT", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(20.dp))

                    // BOTÓN MAESTRO
                    Button(
                        onClick = {
                            if (!hasOverlay) {
                                Toast.makeText(this@MainActivity, "Falta permiso de superposición", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(this@MainActivity, ThermalOverlayService::class.java)
                                startForegroundService(intent)
                                finish()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ACTIVAR HUD REAL-TIME", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            stopService(Intent(this@MainActivity, ThermalOverlayService::class.java))
                            Toast.makeText(this@MainActivity, "HUD DETENIDO", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("DETENER HUD", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun ProfileButton(label: String, governor: String, color: Color, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = modifier.height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isActive) color else Color(0xFF1A1A1A)),
            border = if (!isActive) BorderStroke(1.dp, color) else null,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = if (isActive) Color.Black else color, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(governor, color = if (isActive) Color(0x99000000) else color.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun PermissionRow(label: String, active: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
            if (active) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E5FF))
            else Text("PENDIENTE", color = Color(0xFFFF5252), fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }

    private fun applyGovernor(governor: String) {
        Thread {
            try {
                val p = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(p.outputStream)
                os.writeBytes("for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo $governor > \$cpu; done\n")
                os.writeBytes("exit\n")
                os.flush()
                p.waitFor()
                runOnUiThread { Toast.makeText(this, "Perfil $governor Activado ⚡", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Error Root: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun readCurrentGovernor(): String {
        val path = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
        return try {
            val f = File(path)
            if (f.exists()) f.readText().trim() else "NA"
        } catch (e: Exception) { "NA" }
    }

    private fun detectNormalGovernor(): String {
        return try {
            val g = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors").readText()
            when {
                g.contains("schedutil") -> "schedutil"
                g.contains("interactive") -> "interactive"
                else -> "ondemand"
            }
        } catch (e: Exception) { "schedutil" }
    }

    private fun checkRoot() {
        Thread {
            try {
                val p = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(p.outputStream)
                os.writeBytes("id\nexit\n")
                os.flush()
                if (p.waitFor() == 0) runOnUiThread { Toast.makeText(this, "Acceso Magisk OK ✅", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {}
        }.start()
    }
}
