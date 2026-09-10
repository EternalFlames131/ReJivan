package com.rejivan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Txt = Color(0xEE, 0xF1, 0xEF)
private val Dim = Color(0x8A, 0x9B, 0x9F)
private val Bg = Color(0x0C, 0x12, 0x18)
private val Surface = Color(0x14, 0x21, 0x2B)
private val Surface2 = Color(0x1C, 0x2B, 0x37)
private val Accent = Color(0x2F, 0xBF, 0x8F)
private val Warn = Color(0xF0, 0xB4, 0x5C)
private val Danger = Color(0xF0, 0x63, 0x6F)
private val Good = Color(0x2A, 0xC9, 0x8A)

class MainActivity : ComponentActivity() {

    private val repo by lazy { Repository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReJivanTheme {
                AppRoot(repo)
            }
        }
    }
}

@Composable
private fun ReJivanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Accent, secondary = Accent, background = Bg, surface = Surface,
            onPrimary = Color(0x07, 0x18, 0x14), onBackground = Txt, onSurface = Txt,
            onSurfaceVariant = Dim,
        ),
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Root: login gate + polling loop
// ---------------------------------------------------------------------------
@Composable
private fun AppRoot(repo: Repository) {
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(repo.store.isLoggedIn()) }
    var snapshot by remember { mutableStateOf<AppSnapshot?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    // 5-second heartbeat: server first, on-device engine as automatic fallback.
    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        while (true) {
            if (repo.isOnlineLogin()) { repo.syncMedications() }
            snapshot = repo.snapshot()
            delay(5000)
        }
    }

    if (!loggedIn) {
        LoginScreen(repo) { ok ->
            if (ok) {
                loggedIn = true
                scope.launch { snapshot = repo.snapshot() }
            }
        }
        return
    }

    val snap = snapshot
    if (snap == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading ReJivan…", color = Dim, fontSize = 16.sp)
        }
        return
    }

    val isOnline = snap.source == Source.ONLINE
    val selected = snap.patients.find { it.patient.id == selectedId }

    if (selected != null) {
        PatientDetail(
            snapshot = snap, ps = selected, isOnline = isOnline,
            getLastTaken = { repo.store.lastTaken(it) },
            onBack = { selectedId = null },
            onTake = { med ->
                scope.launch {
                    repo.takeMed(med)
                    if (repo.isOnlineLogin()) repo.syncMedications()
                    snapshot = repo.snapshot()
                }
            },
        )
    } else {
        HomeScreen(
            snapshot = snap, isOnline = isOnline, user = repo.store.userName,
            onSelect = { selectedId = it },
            onLogout = {
                repo.store.clearSession(); loggedIn = false
                snapshot = null; selectedId = null
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Login
// ---------------------------------------------------------------------------
@Composable
private fun LoginScreen(repo: Repository, onResult: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ReJivan", color = Accent, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("A Personal Nurse for Every Family", color = Dim, fontSize = 15.sp)
        Spacer(Modifier.height(36.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        err?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Danger, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !busy && email.isNotBlank() && password.isNotBlank(),
            onClick = {
                busy = true; err = null
                scope.launch {
                    val ok = repo.login(email.trim(), password)
                    busy = false
                    if (!ok) err = "Wrong email or password. Check the demo list below (or try again when online)."
                    else onResult(true)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text(if (busy) "Signing in…" else "Sign in", fontSize = 16.sp) }

        Spacer(Modifier.height(32.dp))
        Text("Demo accounts (password: demo123)", color = Dim, fontSize = 12.sp)
        Text("• asharma@demo.in — Anita Sharma (home, Port Blair)", color = Dim, fontSize = 12.sp)
        Text("• rprakash@demo.in — Ram Prakash (home, Little Andaman)", color = Dim, fontSize = 12.sp)
        Text("• wardnurse@demo.in — Ward Nurse (GB Pant Hospital virtual ward)", color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Text("Works even without internet — it switches to on-device mode automatically.", color = Accent, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// Home / dashboard
// ---------------------------------------------------------------------------
@Composable
private fun HomeScreen(snapshot: AppSnapshot, isOnline: Boolean, user: String, onSelect: (String) -> Unit, onLogout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Bg), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ReJivan", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(user, color = Dim, fontSize = 13.sp)
                    Text(fmtClock(System.currentTimeMillis()), color = Dim, fontSize = 12.sp)
                }
                Pill(if (isOnline) "● Connected — server view" else "● On-device mode", if (isOnline) Good else Warn)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (snapshot.alerts.isNotEmpty()) {
            item {
                SectionTitle("Recent alerts")
                snapshot.alerts.forEach { a ->
                    Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${a.patientName} — ${a.type.uppercase()}", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(a.message, color = Txt, fontSize = 12.sp, maxLines = 2)
                            }
                            Text(fmtTime(a.createdAt), color = Dim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item { SectionTitle("Monitored patients") }

        items(snapshot.patients) { ps ->
            PatientCard(ps, onSelect = { onSelect(ps.patient.id) })
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text("Prototype honesty: vitals & events are a deterministic simulation that mirrors the server. Real sensors will feed the same pipeline.", color = Dim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
}

@Composable
private fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = color, fontSize = 11.sp,
        modifier = modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(50)).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun PatientCard(ps: PatientSnapshot, onSelect: () -> Unit) {
    val p = ps.patient
    val r = ps.report
    val colors = r.metricStatus.map { (k, v) -> k to statusColor(v) }.toMap()
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onSelect),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Txt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${p.age} yrs · ${sexLabel(p.sex)} · ${p.condition.cap()} · ${p.location}", color = Dim, fontSize = 12.sp)
                }
                Pill(if (ps.reliability.validationPassed) "VALID" else "REJECTED", if (ps.reliability.validationPassed) Good else Danger)
                Spacer(Modifier.width(6.dp))
                Pill("${ps.reliability.confidence}% conf", Accent)
            }
            ps.episode?.let { ep ->
                Spacer(Modifier.height(8.dp))
                Text("⚠ ${ep.type.cap()} in progress", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("HR", ps.vitals.hr?.toString() ?: "–", colors["hr"], Modifier.weight(1f))
                Metric("SpO₂", ps.vitals.spo2?.let { "${it.round1()}%" } ?: "–", colors["spo2"], Modifier.weight(1f))
                Metric("BP", ps.vitals.sbp?.let { "$it/${ps.vitals.dbp}" } ?: "–", colors["bp"], Modifier.weight(1f))
                Metric("Temp", ps.vitals.temp?.let { "${it.round1()}°C" } ?: "–", colors["temp"], Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("Glu", ps.vitals.glucose?.toString() ?: "–", colors["glucose"], Modifier.weight(1f))
                Metric("Tier", ps.deviceTier.cap(), if (ps.deviceTier == "medical") Good else Warn, Modifier.weight(1f))
                Metric("Devices", ps.devices.count { it.connected }.toString(), Accent, Modifier.weight(1f))
                Metric("Aids", ps.reliability.degradationMissing.size.toString(), Dim, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).background(Surface2, RoundedCornerShape(50))) {
                Box(Modifier.fillMaxWidth(ps.reliability.confidence / 100f).height(6.dp).background(Accent, RoundedCornerShape(50)))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color?, weight: androidx.compose.ui.Modifier) {
    Column(Modifier.background(Surface2, RoundedCornerShape(10.dp)).padding(8.dp).then(weight)) {
        Text(label, color = Dim, fontSize = 10.sp)
        Text(value, color = color ?: Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Patient detail
// ---------------------------------------------------------------------------
@Composable
private fun PatientDetail(snapshot: AppSnapshot, ps: PatientSnapshot, isOnline: Boolean, getLastTaken: (String) -> String?, onBack: () -> Unit, onTake: (Med) -> Unit) {
    val p = ps.patient
    val r = ps.report
    val zone = snapshot.zones.find { it.patientId == p.id }
    val meds = snapshot.meds.filter { it.patientId == p.id }
    val alertsMe = snapshot.alerts.filter { it.patientId == p.id }
    val call = snapshot.calls.find { it.patientId == p.id }
    val esc = snapshot.escalations.filter { it.patientId == p.id }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Bg).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("‹ Back") }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, color = Txt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${p.age} yrs · ${p.condition.cap()} · ${p.location}${p.ward?.let { " · $it" } ?: ""}", color = Dim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(if (isOnline) "● Connected" else "● On-device", if (isOnline) Good else Warn)
            Pill("${ps.reliability.confidence}% confidence", Accent)
            Pill(if (ps.reliability.validationPassed) "VALIDATED" else "NOT VALIDATED", if (ps.reliability.validationPassed) Good else Danger)
        }
        Spacer(Modifier.height(16.dp))

        ps.episode?.let { ep ->
            SectionTitle("Episode")
            Card(colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.12f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${ep.type.cap()} — from ${fmtTime(ep.from)} until ${fmtTime(ep.until)}", color = Danger, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        SectionTitle("Live vitals (${fmtTime(System.currentTimeMillis())})")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric2("Heart rate", ps.vitals.hr?.let { "$it bpm" } ?: "—", r.metricStatus["hr"], Modifier.weight(1f))
            Metric2("SpO₂", ps.vitals.spo2?.let { "${it.round1()}%" } ?: "—", r.metricStatus["spo2"], Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric2("Blood pressure", ps.vitals.sbp?.let { "$it / ${ps.vitals.dbp} mmHg" } ?: "—", r.bpStatus, Modifier.weight(1f))
            Metric2("Temperature", ps.vitals.temp?.let { "${it.round1()}°C" } ?: "—", r.metricStatus["temp"], Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric2("Glucose", ps.vitals.glucose?.let { "$it mg/dL" } ?: "—", r.metricStatus["glucose"], Modifier.weight(1f))
            Metric2("Device tier", ps.deviceTier.cap(), if (ps.deviceTier == "medical") "normal" else "caution", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Reliability")
        Text("${ps.reliability.confidence}% confidence that readings reflect true state.",
            color = Txt, fontSize = 13.sp)
        if (ps.reliability.rejectedMetrics.isNotEmpty()) {
            Text("Automatic rejections: ${ps.reliability.rejectedMetrics.joinToString(", ")}", color = Warn, fontSize = 12.sp)
        }
        if (ps.reliability.degradationMissing.isNotEmpty()) {
            Text("Sensors offline / missing: ${ps.reliability.degradationMissing.joinToString(", ")}", color = Warn, fontSize = 12.sp)
        } else {
            Text("All vital sensors active.", color = Good, fontSize = 12.sp)
        }
        Text("Consecutive-check + rate-limit: alerts only after persistent (≥2) readings; max 5 per 5 min per patient.", color = Dim, fontSize = 11.sp)

        Spacer(Modifier.height(16.dp))
        SectionTitle("Medications")
        if (meds.isEmpty()) {
            Text("No medication schedule for this patient in the demo set.", color = Dim, fontSize = 13.sp)
        } else {
            meds.forEach { m ->
                val takenAt = getLastTaken(m.id)
                Row(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(12.dp).border(1.dp, Surface2, RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${m.dose} · ${m.frequency} · at ${m.times.joinToString(", ")}${m.notes.let { if (it.isNotEmpty()) " · $it" else "" }}", color = Dim, fontSize = 12.sp)
                        if (takenAt != null) Text("Last taken: $takenAt", color = Good, fontSize = 11.sp)
                    }
                    Button(onClick = { onTake(m) }) { Text("Take now") }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text("Two-way sync: doses taken here also appear on the website (and vice-versa) whenever connected.", color = Dim, fontSize = 11.sp)
        }

        zone?.let { z ->
            Spacer(Modifier.height(16.dp))
            SectionTitle("Camera zone (privacy-first)")
            val frame = Engine.liveFrame(z.id, System.currentTimeMillis())
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(z.name, color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(z.room, color = Dim, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    LiveRow("Motion", "${(frame.motion * 100).roundToIntPct()}%", if (frame.motion > 0.75) Warn else Good)
                    LiveRow("Person present", if (frame.person) "yes" else "no", Good)
                    LiveRow("Lighting", frame.lighting, Dim)
                    Spacer(Modifier.height(8.dp))
                    Text("Privacy-first: body-motion features only — no video is recorded or stored.", color = Dim, fontSize = 11.sp)
                    Text("Simulated feed for the demo; identical output on website & phone.", color = Dim, fontSize = 11.sp)
                }
            }
        }

        if (alertsMe.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle("Alerts for this patient")
            alertsMe.forEach { a ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${fmtTime(a.createdAt)} · ${a.type.uppercase()} · ${a.severity}", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(a.message, color = Txt, fontSize = 13.sp)
                    }
                }
            }
        }

        call?.let { c ->
            Spacer(Modifier.height(16.dp))
            SectionTitle("Automatic call chain")
            Text("Status: ${c.status}", color = if (c.status == "dispatched") Danger else if (c.status == "answered") Good else Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            c.ladder.forEach { l ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("${l.label} → ${l.to}", ladderColor(l.state), Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(l.state, color = ladderColor(l.state), fontSize = 11.sp)
                }
            }
            c.log.forEach { (t, msg) ->
                Text("${fmtTime(t)}  ${msg}", color = Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }

        esc.forEach { e ->
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Escalated to: ${e.to} · via ${e.channels.joinToString(", ")}", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${fmtTime(e.dispatchedAt)} · delivered=${if (e.delivered) "yes" else "pending"}", color = Dim, fontSize = 12.sp)
                    Text(e.simulationNotice, color = Dim, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Prototype honesty: vitals, episodes, alerts & calls are a deterministic simulation identical on every device and the website. Production build attaches real sensors and live APIs.", color = Dim, fontSize = 11.sp, textAlign = TextAlign.Start)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Metric2(label: String, value: String, status: String?, weight: androidx.compose.ui.Modifier) {
    Column(Modifier.background(Surface, RoundedCornerShape(12.dp)).border(1.dp, Surface2, RoundedCornerShape(12.dp)).padding(12.dp).then(weight)) {
        Text(label, color = Dim, fontSize = 11.sp)
        Text(value, color = if (status != null) statusColor(status) else Txt, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(status?.cap() ?: "", color = Dim, fontSize = 10.sp)
    }
}

@Composable
private fun LiveRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// tiny helpers
// ---------------------------------------------------------------------------
private fun statusColor(s: String): Color = when (s) {
    "danger" -> Danger
    "caution" -> Warn
    else -> Good
}

private fun ladderColor(s: String): Color = when (s) {
    "answered", "dispatched" -> Good
    "dialing" -> Accent
    else -> Dim
}

private fun sexLabel(s: String): String = if (s == "M") "male" else if (s == "F") "female" else s

private fun fmtTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(ts))
private fun fmtClock(ts: Long): String = SimpleDateFormat("EEEE, dd MMM yyyy · HH:mm", Locale.ENGLISH).format(Date(ts))

private fun Double.round1(): Double = Math.round(this * 10) / 10.0
private fun Double.roundToIntPct(): Int = Math.round(this * 100).toInt()