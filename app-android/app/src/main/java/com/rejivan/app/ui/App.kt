package com.rejivan.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rejivan.app.core.*
import com.rejivan.app.data.Repository

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
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("rejivan_prefs", Context.MODE_PRIVATE)
    var email by remember { mutableStateOf(prefs.getString("last_email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("last_pass", "") ?: "") }
    var rememberLogin by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }

    fun doLogin(em: String, pw: String) {
        val e = state.login(em.trim(), pw)
        if (e != null) { err = e; return }
        err = ""
        if (rememberLogin) prefs.edit()
            .putString("last_email", em.trim()).putString("last_pass", pw).apply()
    }

    Box(Modifier.fillMaxSize().background(AppColors.bg), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.padding(20.dp).widthIn(max = 400.dp)) {
            Column(Modifier.padding(26.dp)) {
                Text("ReJivan", color = AppColors.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("A Personal Nurse for Every Family", color = AppColors.muted, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true, isError = err.isNotEmpty(),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true, isError = err.isNotEmpty(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(err, color = AppColors.danger, fontSize = 13.sp, minLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberLogin, onCheckedChange = { rememberLogin = it })
                    Text("Remember login", color = AppColors.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(2.dp))
                Button(onClick = { doLogin(email, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.accent),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Login", color = AppColors.bg, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
                    shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Tap an account to log in instantly (password: demo123)", color = AppColors.txt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        DemoShortcut("asharma@demo.in", "home patient", AppColors.ok) { doLogin("asharma@demo.in", "demo123") }
                        DemoShortcut("rprakash@demo.in", "home patient", AppColors.ok) { doLogin("rprakash@demo.in", "demo123") }
                        DemoShortcut("wardnurse@demo.in", "Virtual Ward", AppColors.accent2) { doLogin("wardnurse@demo.in", "demo123") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Region: ${DemoData.REGION}", color = AppColors.warn, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DemoShortcut(mail: String, roleLabel: String, roleColor: Color, onTap: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onTap)
        .background(AppColors.panel)
        .padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(mail, color = AppColors.accent2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(roleLabel, color = roleColor, fontSize = 11.sp)
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
    val tabs = listOf("Dashboard", "Medicines", "Alerts", "Devices", "Ward", "Camera")
    Scaffold(
        containerColor = AppColors.bg,
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).background(AppColors.accent, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        Text("R", color = AppColors.bg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("ReJivan", color = AppColors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("A Personal Nurse for Every Family", color = AppColors.muted, fontSize = 10.sp)
                    }
                    val srcLabel = if (state.dataSource == Repository.Source.SERVER) "SERVER" else "OFFLINE"
                    val srcColor = if (state.dataSource == Repository.Source.SERVER) AppColors.ok else AppColors.warn
                    Spacer(Modifier.weight(1f))
                    Card(colors = CardDefaults.cardColors(containerColor = srcColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp)) {
                        Text(srcLabel, color = srcColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = AppColors.warn.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(20.dp)) {
                        Text("LIVE · SIMULATED", color = AppColors.warn, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.width(6.dp))
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
                "Devices" -> Devices(state)
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
            val srcNote = if (state.dataSource == Repository.Source.SERVER)
                "Data fetched from rejivan.vercel.app" else "OFFLINE: local engine"
            Text(srcNote, color = if (state.dataSource == Repository.Source.SERVER) AppColors.ok else AppColors.muted, fontSize = 11.sp)
            Text("SIMULATED vital data • REAL monitoring logic", color = AppColors.muted, fontSize = 11.sp)
        }
        item {
            val nNormal = state.patients.count { worstStatus(state.reportOf(it)) == "normal" }
            val nCaution = state.patients.count { worstStatus(state.reportOf(it)) == "caution" }
            val nDanger = state.patients.count { worstStatus(state.reportOf(it)) == "danger" }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Patients", state.patients.size, AppColors.accent2) { Modifier.weight(1f) }
                StatTile("Stable", nNormal, AppColors.ok) { Modifier.weight(1f) }
                StatTile("Caution", nCaution, AppColors.warn) { Modifier.weight(1f) }
                StatTile("Danger", nDanger, AppColors.danger) { Modifier.weight(1f) }
            }
        }
        if (state.patients.isEmpty()) {
            item { Text("No patients registered for this account.", color = AppColors.muted) }
        }
        items(state.patients) { p ->
            val r = state.reportOf(p)
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    val bp = r["bp"] as? String ?: "normal"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, color = AppColors.txt, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        StatusBadge(bp)
                    }
                    Text("${p.age} yrs • ${p.sex} • ${p.condition} • ${p.location}", color = AppColors.muted, fontSize = 12.sp)
                    if (p.ward != null) Text("${p.ward}", color = AppColors.accent2, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    val hrMap = r["hr"] as? Map<*, *> ?: mapOf("value" to "--", "status" to "normal")
                    val spo2Map = r["spo2"] as? Map<*, *> ?: mapOf("value" to "--", "status" to "normal")
                    val sbpMap = r["sbp"] as? Map<*, *> ?: mapOf("value" to "--", "status" to "normal")
                    val dbpMap = r["dbp"] as? Map<*, *> ?: mapOf("value" to "--", "status" to "normal")
                    val tempMap = r["temp"] as? Map<*, *> ?: mapOf("value" to "--", "status" to "normal")
                    val glucoseMap = r["glucose"] as? Map<*, *> ?: mapOf("value" to "--", "status" to "normal")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalCard("HR", hrMap["value"].toString(), hrMap["status"] as? String ?: "normal") { Modifier.weight(1f) }
                        VitalCard("SpO2", spo2Map["value"].toString() + "%", spo2Map["status"] as? String ?: "normal") { Modifier.weight(1f) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalCard("BP", "${sbpMap["value"]} / ${dbpMap["value"]}", bp) { Modifier.weight(1f) }
                        VitalCard("Temp", tempMap["value"].toString() + "\u00B0C", tempMap["status"] as? String ?: "normal") { Modifier.weight(1f) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VitalCard("Glucose", glucoseMap["value"].toString(), glucoseMap["status"] as? String ?: "normal") { Modifier.weight(1f) }
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
private fun StatTile(label: String, value: Int, color: Color, weight: @Composable () -> Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
        shape = RoundedCornerShape(12.dp), modifier = weight()) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 8.dp)) {
            Text(label.uppercase(), color = AppColors.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value.toString(), color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
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
            val medNote = if (state.dataSource == Repository.Source.SERVER)
                "Live from rejivan.vercel.app • changes sync two ways"
            else "Offline: saved on this device only"
            Text(medNote, color = if (state.dataSource == Repository.Source.SERVER) AppColors.ok else AppColors.muted, fontSize = 11.sp)
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
            Text("No video recorded or stored • on-device Prajñā only (SIMULATED)", color = AppColors.muted, fontSize = 11.sp)
        }
        zones.forEach { z ->
            item { CameraCard(state, z) }
        }
        item {
            val note = if (state.dataSource == Repository.Source.SERVER)
                "Live data from rejivan.vercel.app — falls back to on-device engine when offline."
            else "OFFLINE: local on-device engine (no internet needed)."
            Text(note, color = if (state.dataSource == Repository.Source.SERVER) AppColors.ok else AppColors.warn, fontSize = 11.sp)
        }
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

private fun metricLabel(m: String): String = when (m) {
    "hr" -> "HR"
    "spo2" -> "SpO2"
    "sbp" -> "SYS BP"
    "dbp" -> "DIA BP"
    "temp" -> "Temp"
    "glucose" -> "Glucose"
    else -> m
}

@Composable
fun Devices(state: AppState) {
    val groups = state.deviceGroups()
    val catalogue = state.deviceCatalogue()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Section("Medical Devices")
            val srcNote = if (state.dataSource == Repository.Source.SERVER)
                "Live from rejivan.vercel.app" else "OFFLINE: local catalogue (FDA/CDSCO approved)"
            Text(srcNote, color = if (state.dataSource == Repository.Source.SERVER) AppColors.ok else AppColors.muted, fontSize = 11.sp)
            Text("SIMULATED connectivity • REAL device profiles & approvals", color = AppColors.muted, fontSize = 11.sp)
        }
        if (groups.isNotEmpty()) {
            items(groups) { g ->
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${g.patientName} — Connected Devices", color = AppColors.txt, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val online = g.devices.count { it.connected }
                        Text("${g.devices.size} devices · $online online", color = AppColors.muted, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        g.devices.forEach { d -> DeviceRow(d) }
                        if (g.devices.isEmpty()) {
                            Text("No devices connected.", color = AppColors.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            item { Text("No devices for this account.", color = AppColors.muted) }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Section("Supported Device Catalogue", AppColors.accent2)
            Text("${catalogue.size} medical-grade devices (all FDA/CDSCO/CE approved)", color = AppColors.muted, fontSize = 11.sp)
        }
        items(catalogue) { c ->
            Card(colors = CardDefaults.cardColors(containerColor = AppColors.panel2),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, color = AppColors.txt, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        if (c.madeInIndia) {
                            Card(colors = CardDefaults.cardColors(containerColor = AppColors.ok.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(20.dp)) {
                                Text("IN", color = AppColors.ok, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${c.manufacturer} · ${c.approval}", color = AppColors.muted, fontSize = 11.sp)
                    if (c.priceINR.isNotBlank() && c.priceINR != "0") {
                        Text("\u20B9${c.priceINR.toIntOrNull()?.let { it } ?: c.priceINR}",
                            color = AppColors.accent2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (c.measures.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) {
                            c.measures.take(4).forEach { m ->
                                Text(metricLabel(m), color = AppColors.accent2, fontSize = 9.sp,
                                    modifier = Modifier.background(AppColors.line, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    if (c.description.isNotBlank()) {
                        Text(c.description, color = AppColors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
        item {
            Text("In production these connect via BLE/WiFi to real hardware — here connectivity is SIMULATED.",
                color = AppColors.warn, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DeviceRow(d: com.rejivan.app.network.Sync.ServerDevice) {
    val dot = if (d.connected) AppColors.ok else AppColors.danger
    val badgeTxt = if (d.connected) "CONNECTED" else "DISCONNECTED"
    val badgeColor = if (d.connected) AppColors.ok else AppColors.danger
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(dot, RoundedCornerShape(20.dp)))
            Spacer(Modifier.width(8.dp))
            Text(d.name, color = AppColors.txt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Card(colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp)) {
                Text(badgeTxt, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
            }
        }
        Text("${d.manufacturer} · ${d.approval}", color = AppColors.muted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
            Box(Modifier.width(60.dp).height(6.dp).background(AppColors.line, RoundedCornerShape(3.dp))) {
                Box(Modifier.width((d.battery.coerceIn(0, 100) * 0.60).dp).height(6.dp)
                    .background(if (d.battery > 20) AppColors.ok else AppColors.danger, RoundedCornerShape(3.dp)))
            }
            Spacer(Modifier.width(6.dp))
            Text("${d.battery}%", color = AppColors.muted, fontSize = 10.sp)
            Spacer(Modifier.width(10.dp))
            Text("${d.secondsSinceLastSeen}s ago", color = AppColors.muted, fontSize = 10.sp)
        }
    }
}
