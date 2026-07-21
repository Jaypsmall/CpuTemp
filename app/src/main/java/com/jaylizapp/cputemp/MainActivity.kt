@file:Suppress("DEPRECATION")

package com.jaylizapp.cputemp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.jaylizapp.cputemp.ui.theme.CpuTempTheme
import com.jaylizapp.cputemp.ui.theme.ShinySilver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            var hasOverlay by remember { mutableStateOf(false) }
            var liveBatteryTemp by remember { mutableStateOf("-- °C") }
            var currentGovernor by remember { mutableStateOf("unknown") }
            
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            // Usamos stringResource directamente para que Compose reaccione al cambio de configuración
            val settingsText = stringResource(R.string.nav_settings)
            val languageText = stringResource(R.string.nav_language)
            val hardwareInfoText = stringResource(R.string.nav_performance)

            LaunchedEffect(Unit) {
                checkRoot() // Solicitar root al iniciar
                while(true) {
                    hasOverlay = Settings.canDrawOverlays(this@MainActivity)
                    val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    liveBatteryTemp = "${temp / 10.0} °C"
                    currentGovernor = readCurrentGovernor()
                    delay(1000.milliseconds)
                }
            }

            // Sombra para el título (Solo modo claro para profundidad, modo oscuro limpio)
            val titleShadow = if (isDarkMode) null else Shadow(
                color = Color.Black.copy(alpha = 0.3f),
                offset = Offset(4f, 4f),
                blurRadius = 10f
            )

            // Título estilizado: CpuTemp usando colores del sistema
            val cpuTitle = buildAnnotatedString {
                val capsStyle = SpanStyle(
                    color = if (isDarkMode) Color(0xFF8B0000) else Color(0xFF0066FF),
                    fontWeight = FontWeight.ExtraBold,
                    shadow = titleShadow
                )
                val lowerStyle = SpanStyle(
                    color = if (isDarkMode) Color.White else Color(0xFF333333),
                    fontWeight = FontWeight.ExtraBold,
                    shadow = titleShadow
                )

                withStyle(style = capsStyle) { append("C") }
                withStyle(style = lowerStyle) { append("p") }
                withStyle(style = lowerStyle) { append("u") }
                withStyle(style = capsStyle) { append("T") }
                withStyle(style = lowerStyle) { append("e") }
                withStyle(style = lowerStyle) { append("m") }
                withStyle(style = lowerStyle) { append("p") }
                
                // Termómetro con color adaptativo y sombra
                withStyle(style = SpanStyle(
                    color = if (isDarkMode) Color.Unspecified else Color(0xFF0066FF),
                    shadow = titleShadow
                )) { 
                    append(" 🌡️") 
                }
            }

            CpuTempTheme(darkTheme = isDarkMode) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = if (isDarkMode) Color(0xFF1A1A1A) else ShinySilver,
                            modifier = Modifier.width(300.dp),
                            drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 1. EL TÍTULO
                                Text(
                                    text = cpuTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(vertical = 32.dp)
                                )

                                // 2. LÍNEA SEPARADORA
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 32.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )

                                // 3. BOTONES
                                DrawerButton(
                                    text = settingsText,
                                    icon = Icons.Default.Settings,
                                    isDarkMode = isDarkMode,
                                    primaryColor = MaterialTheme.colorScheme.primary
                                ) {
                                    scope.launch { drawerState.close() }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                DrawerButton(
                                    text = languageText,
                                    icon = Icons.Default.Language,
                                    isDarkMode = isDarkMode,
                                    primaryColor = MaterialTheme.colorScheme.primary
                                ) {
                                    scope.launch { 
                                        drawerState.close()
                                        updateLocale(if (Locale.getDefault().language == "es") "en" else "es")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                DrawerButton(
                                    text = hardwareInfoText,
                                    icon = Icons.Default.Info,
                                    isDarkMode = isDarkMode,
                                    primaryColor = MaterialTheme.colorScheme.primary
                                ) {
                                    scope.launch { drawerState.close() }
                                }

                                // 4. ESPACIADOR FLEXIBLE
                                Spacer(modifier = Modifier.weight(1f))

                                // 5. CRÉDITOS
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "CpuTemp v1.2.0-PRO",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.Gray else Color.DarkGray
                                    )
                                    Text(
                                        text = "Created by JAYLIZ with ❤️",
                                        fontSize = 9.sp,
                                        color = if (isDarkMode) Color.Gray.copy(0.7f) else Color.DarkGray.copy(0.7f)
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { isDarkMode = !isDarkMode }) {
                                        Icon(
                                            imageVector = Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme",
                                            tint = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Unspecified,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Título Principal Estilizado
                            Text(
                                text = cpuTitle,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 44.sp,
                                    lineHeight = 48.sp
                                ),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Header Status
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(R.string.engine_status),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (isDarkMode) stringResource(R.string.mode_demoniac) else stringResource(R.string.mode_professional),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Monitor Batería
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.battery_temp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            liveBatteryTemp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF008800),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // SECCIÓN DE PERFILES
                            Text(
                                stringResource(R.string.hardware_profiles),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileButton(
                                    stringResource(R.string.profile_cool),
                                    "powersave",
                                    if (isDarkMode) Color(0xFF00E5FF) else Color(0xFF00D4FF),
                                    currentGovernor == "powersave",
                                    Modifier.weight(1f)
                                ) {
                                    ThermalOverlayService.instance?.cambiarModoCpu("cool") ?: applyGovernor("powersave")
                                }
                                ProfileButton(
                                    stringResource(R.string.profile_normal),
                                    detectNormalGovernor(),
                                    if (isDarkMode) Color(0xFF76FF03) else MaterialTheme.colorScheme.primary,
                                    currentGovernor == detectNormalGovernor(),
                                    Modifier.weight(1f)
                                ) {
                                    ThermalOverlayService.instance?.cambiarModoCpu("normal")
                                        ?: applyGovernor(detectNormalGovernor())
                                }
                                ProfileButton(
                                    stringResource(R.string.profile_boost),
                                    "performance",
                                    if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF0044BB),
                                    currentGovernor == "performance",
                                    Modifier.weight(1f)
                                ) {
                                    ThermalOverlayService.instance?.cambiarModoCpu("boost") ?: applyGovernor("performance")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Permisos
                            PermissionRow(stringResource(R.string.perm_overlay), hasOverlay) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:$packageName".toUri()
                                )
                                startActivity(intent)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // BOTÓN MAESTRO
                            Button(
                                onClick = {
                                    if (!hasOverlay) {
                                        Toast.makeText(this@MainActivity, getString(R.string.msg_overlay_required), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val intent = Intent(this@MainActivity, ThermalOverlayService::class.java)
                                        startForegroundService(intent)
                                        finish()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.btn_start_overlay),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    stopService(Intent(this@MainActivity, ThermalOverlayService::class.java))
                                    Toast.makeText(this@MainActivity, getString(R.string.msg_service_stopped), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(R.string.btn_stop_service),
                                    color = if (isDarkMode) Color.Red.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DrawerButton(text: String, icon: ImageVector, isDarkMode: Boolean, primaryColor: Color, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkMode) Color(0xFF151B23) else Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color(0xFF0D0D0D)
                )
            }
        }
    }

    @Composable
    fun ProfileButton(label: String, governor: String, color: Color, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = modifier.height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) color else MaterialTheme.colorScheme.surface,
                contentColor = if (isActive) Color.Black else color
            ),
            border = if (!isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(governor, color = if (isActive) Color.Black.copy(alpha = 0.7f) else color.copy(alpha = 0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun PermissionRow(label: String, active: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (active) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF008800), modifier = Modifier.size(20.dp))
            else Text(stringResource(R.string.btn_grant), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 10.sp)
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
                runOnUiThread { 
                    Toast.makeText(this, getString(R.string.msg_profile_active, governor), Toast.LENGTH_SHORT).show() 
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Root Error: ${e.message}", Toast.LENGTH_SHORT).show() }
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
                if (p.waitFor() == 0) runOnUiThread { Toast.makeText(this, getString(R.string.msg_root_ok), Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {}
        }.start()
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale(getSharedPreferences("prefs", MODE_PRIVATE).getString("lang", "en") ?: "en")
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private fun updateLocale(languageCode: String) {
        getSharedPreferences("prefs", MODE_PRIVATE).edit { putString("lang", languageCode) }
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        
        // Esta es la forma más limpia de aplicar el cambio de idioma
        val intent = intent
        finish()
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
