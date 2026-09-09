package com.sanjivanai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjivanai.app.core.*

@Composable
fun App(state: AppState) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = AppColors.bg,
        surface = AppColors.panel,
        primary = AppColors.accent,
        secondary = AppColors.accent2
    )) {
        val user = state.currentUser
        if (user == null) {
            LoginScreen(state)
        } else {
            MainShell(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(state: AppState) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(AppColors.bg), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.padding(20.dp).widthIn(max = 400.dp)) {
            Column(Modifier.padding(26.dp)) {
                Text("SanjivanAI", color = AppColors.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("A Personal AI Nurse for Every Family", color = AppColors.muted, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(err, color = AppColors.danger, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val e = state.login(email.trim(), password)
                    if (e != null) err = e else err = ""
                }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.accent),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Login", color = AppColors.bg, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
                    shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Demo accounts (password: demo123)", color = AppColors.txt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("asharma@demo.in  (home patient)", color = AppColors.muted, fontSize = 12.sp)
                        Text("rprakash@demo.in  (home patient)", color = AppColors.muted, fontSize = 12.sp)
                        Text("wardnurse@demo.in  (Virtual Ward)", color = AppColors.muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Region: ${DemoData.REGION}", color = AppColors.warn, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.accent,
    unfocusedBorderColor = AppColors.line,
    cursorColor = AppColors.accent,
    focusedTextColor = AppColors.txt,
    unfocusedTextColor = AppColors.txt,
    focusedLabelColor = AppColors.accent,
    unfocusedLabelColor = AppColors.muted
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(state: AppState) {
    var tab by remember { mutableStateOf("Dashboard") }
    val tabs = listOf("Dashboard", "Medicines", "Alerts", "Ward", "Camera")
    Scaffold(
        containerColor = AppColors.bg,
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("SanjivanAI", color = AppColors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${state.currentUser?.role ?: ""}", color = AppColors.muted, fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    TextButton(onClick = { state.logout() }) {
                        Text("Logout", color = AppColors.accent2)
                    }
                }
                Surface(color = AppColors.panel2) {
                    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 8.dp)) {
                        tabs.forEach { t ->
                            val active = tab == t
                            TextButton(onClick = { tab = t }) {
                                Text(t, color = if (active) AppColors.bg else AppColors.muted,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    modifier = if (active) Modifier.background(AppColors.accent, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                    else Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                "Dashboard" -> Dashboard(state)
                "Medicines" -> Medicines(state)
                "Alerts" -> Alerts(state)
                "Ward" -> Ward(state)
                "Camera" -> Camera(state)
            }
        }
    }
}

@Composable
private fun Section(text: String, color: Color = AppColors.accent) {
    Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "danger" -> AppColors.dangerPanel to AppColors.danger
        "caution" -> AppColors.cautionPanel to AppColors.warn
        else -> AppColors.okPanel to AppColors.ok
    }
    Text(status.uppercase(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(bg, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
}

@Composable
fun Dashboard(state: AppState) {
    val tick = state.tick
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Live Vitals Dashboard", color = AppColors.txt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Region: ${DemoData.REGION}", color = AppColors.warn, fontSize = 12.sp)
            Text("SIMULATED vital data • REAL monitoring logic", color = AppColors.muted, fontSize = 11.sp)
        }
        if (state.patients.isEmpty()) {
            item { Text("No patients registered for this account.", color = AppColors.muted) }
        }
        items(state.patients) { p ->
            val r = state.reportOf(p)
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, color = AppColors.txt, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        val bp = r["bp"] as String
                        StatusBadge(bp)
                    }
                    Text("${p.age} yrs • ${p.sex} • ${p.condition} • ${p.location}", color = AppColors.muted, fontSize = 12.sp)
                    if (p.ward != null) Text("${p.ward}", color = AppColors.accent2, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalCard("HR", (r["hr"] as Map<*, *>)["value"].toString(),
                            (r["hr"] as Map<*, *>)["status"] as String) { Modifier.weight(1f) }
                        VitalCard("SpO2", (r["spo2"] as Map<*, *>)["value"].toString() + "%",
                            (r["spo2"] as Map<*, *>)["status"] as String) { Modifier.weight(1f) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalCard("BP", "${(r["sbp"] as Map<*, *>)["value"]} / ${(r["dbp"] as Map<*, *>)["value"]}", r["bp"] as String) { Modifier.weight(1f) }
                        VitalCard("Temp", (r["temp"] as Map<*, *>)["value"].toString() + "°C",
                            (r["temp"] as Map<*, *>)["status"] as String) { Modifier.weight(1f) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalCard("Glucose", (r["glucose"] as Map<*, *>)["value"].toString(),
                            (r["glucose"] as Map<*, *>)["status"] as String) { Modifier.weight(1f) }
                        val danger = RulesEngine.dangerLabels(r).joinToString(", ")
                        Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
                            shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(10.dp)) {
                                Text("SYSTEM", color = AppColors.muted, fontSize = 10.sp)
                                Text(if (danger.isEmpty()) "All stable" else "DANGER: $danger",
                                    color = if (danger.isEmpty()) AppColors.ok else AppColors.danger,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item { Text("Updated: $tick", color = AppColors.muted, fontSize = 10.sp) }
    }
}

@Composable
private fun VitalCard(label: String, value: String, status: String, weight: @Composable () -> Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
        shape = RoundedCornerShape(10.dp), modifier = weight()) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = AppColors.muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                StatusBadge(status)
            }
            Text(value, color = AppColors.txt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Medicines(state: AppState) {
    var showAdd by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Section("Medicines Schedule")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAdd = true }) { Text("+ Add", color = AppColors.accent) }
            }
            Text("Offline saved on this device", color = AppColors.muted, fontSize = 11.sp)
        }
        items(state.meds()) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, color = AppColors.txt, fontWeight = FontWeight.Bold)
                        Text("  ${m.dose}", color = AppColors.muted)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { state.removeMed(m.id) }) { Text("Delete", color = AppColors.danger) }
                    }
                    Text("${m.frequency} • Times: ${m.times.joinToString(", ")}", color = AppColors.accent2, fontSize = 12.sp)
                    Text(m.notes, color = AppColors.muted, fontSize = 12.sp)
                }
            }
        }
        if (showAdd) {
            item { AddMedDialog(state) { showAdd = false } }
        }
    }
}

@Composable
private fun AddMedDialog(state: AppState, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("daily") }
    var time by remember { mutableStateOf("08:00") }
    Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().border(1.dp, AppColors.line, RoundedCornerShape(14.dp))) {
        Column(Modifier.padding(14.dp)) {
            Section("New medicine", AppColors.accent2)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, colors = fieldColors())
            OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dose (e.g. 10 mg)") }, singleLine = true, colors = fieldColors())
            OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frequency") }, singleLine = true, colors = fieldColors())
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (HH:MM)") }, singleLine = true, colors = fieldColors())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val pid = state.patients.firstOrNull()?.id ?: return@Button
                    state.addMed(Medication("MED-${System.currentTimeMillis()%100000}", pid, name.ifBlank { "Med" },
                        dose, freq, listOf(time), "Added on device"))
                    onClose()
                }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.accent)) { Text("Save", color = AppColors.bg) }
                TextButton(onClick = onClose) { Text("Cancel", color = AppColors.muted) }
            }
        }
    }
}

@Composable
fun Alerts(state: AppState) {
    val alerts = state.alerts()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("Alerts & Emergency Calls")
            Text("REAL auto-trigger + escalation logic • SIMULATED placement", color = AppColors.muted, fontSize = 11.sp)
        }
        if (alerts.isEmpty()) {
            item { Text("No active alerts.", color = AppColors.muted) }
        }
        items(alerts) { a ->
            val call = state.callFor(a)
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    .border(1.dp, if (a.status == "danger") AppColors.danger else AppColors.warn, RoundedCornerShape(12.dp))) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.patientName, color = AppColors.txt, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        StatusBadge(a.status)
                    }
                    Text(a.message, color = AppColors.txt, fontSize = 12.sp)
                    Text("Metric: ${a.metric}", color = AppColors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Emergency call chain", color = AppColors.accent2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    call.steps.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("•", color = stepColor(s.status), fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(s.label, color = AppColors.txt, fontSize = 12.sp)
                                Text(s.number, color = AppColors.muted, fontSize = 10.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(s.status.uppercase(), color = stepColor(s.status), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun stepColor(status: String) = when (status) {
    "answered" -> AppColors.ok
    "skipped" -> AppColors.muted
    "dialing" -> AppColors.warn
    else -> AppColors.danger
}

@Composable
fun Ward(state: AppState) {
    val wardPatients = state.patients.filter { it.ward != null }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("Virtual Ward — GB Pant Hospital, Port Blair")
            Text("Nurse-station view • priority queue", color = AppColors.muted, fontSize = 11.sp)
        }
        if (wardPatients.isEmpty()) {
            item { Text("This account has no ward beds assigned.", color = AppColors.muted) }
        }
        items(wardPatients) { p ->
            val r = state.reportOf(p)
            val status = worstStatus(r)
            val (bar, barColor) = when (status) {
                "danger" -> "DANGER — act now!" to AppColors.danger
                "caution" -> "CAUTION — monitor" to AppColors.warn
                else -> "NORMAL — stable" to AppColors.ok
            }
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    .border(2.dp, barColor, RoundedCornerShape(12.dp))) {
                Column(Modifier.padding(14.dp)) {
                    Text(bar, color = barColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(p.name, color = AppColors.txt, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${p.ward} • ${p.condition}", color = AppColors.muted, fontSize = 12.sp)
                }
            }
        }
        item {
            Text("REGION: ${DemoData.REGION} • Reachable hospital: GB Pant Hospital", color = AppColors.warn, fontSize = 11.sp)
        }
    }
}

private fun worstStatus(r: Map<String, Any>): String {
    var w = "normal"
    for (m in listOf("hr", "spo2", "bp", "temp", "glucose")) {
        val s = r[m]
        val status = if (s is Map<*, *>) (s["status"] as? String) else s as? String
        if (status == "danger") return "danger"
        if (status == "caution") w = "caution"
    }
    return w
}

@Composable
fun Camera(state: AppState) {
    val zones = state.zones
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("Privacy-First Camera Zones")
            Text("No video recorded or stored • on-device AI only (SIMULATED)", color = AppColors.muted, fontSize = 11.sp)
        }
        zones.forEach { z ->
            item { CameraCard(state, z) }
        }
        item { Text("OFFLINE: this app works fully without internet.", color = AppColors.ok, fontSize = 11.sp) }
    }
}

@Composable
private fun CameraCard(state: AppState, zone: CameraZone) {
    val now = state.tick
    val frame = CameraZoneEngine.liveFrame(zone.id, if (now > 0) now else System.currentTimeMillis())
    Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(zone.name, color = AppColors.txt, fontWeight = FontWeight.Bold)
            Text(zone.room, color = AppColors.muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (frame.person) "Person present" else "No person",
                    color = if (frame.person) AppColors.ok else AppColors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text("Motion ${(frame.motion * 100).toInt()}%", color = AppColors.muted, fontSize = 12.sp)
                Spacer(Modifier.width(12.dp))
                Text("Lighting: ${frame.lighting}", color = AppColors.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(70.dp).background(Color(0xFF0A0F18), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PRIVACY-SAFE VIEW", color = AppColors.accent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("On-device metadata only — no video", color = AppColors.muted, fontSize = 9.sp)
                }
            }
        }
    }
}
