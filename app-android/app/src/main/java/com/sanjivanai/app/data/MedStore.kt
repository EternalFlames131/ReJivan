package com.sanjivanai.app.data

import android.content.Context
import com.sanjivanai.app.core.Medication

// Offline-first persistence for medications via SharedPreferences.
// No network access — works fully offline.
object MedStore {
    private const val PREFS = "sanjivanai_meds"
    private const val KEY_LIST = "list"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Medication> {
        val raw = prefs(ctx).getString(KEY_LIST, null) ?: return mutableListOf()
        return try {
            val out = mutableListOf<Medication>()
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val times = mutableListOf<String>()
                val ta = o.optJSONArray("times") ?: org.json.JSONArray()
                for (j in 0 until ta.length()) times.add(ta.getString(j))
                out.add(Medication(
                    o.getString("id"), o.getString("patientId"), o.getString("name"),
                    o.optString("dose"), o.optString("frequency"), times,
                    o.optString("notes"), o.optBoolean("active", true)
                ))
            }
            out
        } catch (t: Throwable) { mutableListOf() }
    }

    fun init(ctx: Context, seed: List<Medication>) {
        val cur = load(ctx)
        if (cur.isEmpty()) save(ctx, seed)
    }

    fun save(ctx: Context, list: List<Medication>) {
        val arr = org.json.JSONArray()
        for (m in list) {
            val o = org.json.JSONObject()
            o.put("id", m.id); o.put("patientId", m.patientId); o.put("name", m.name)
            o.put("dose", m.dose); o.put("frequency", m.frequency)
            val ta = org.json.JSONArray()
            m.times.forEach { ta.put(it) }
            o.put("times", ta); o.put("notes", m.notes); o.put("active", m.active)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun add(ctx: Context, med: Medication) {
        val list = load(ctx).toMutableList()
        list.add(med)
        save(ctx, list)
    }

    fun remove(ctx: Context, id: String) {
        save(ctx, load(ctx).filter { it.id != id })
    }

    fun update(ctx: Context, med: Medication) {
        val list = load(ctx).map { if (it.id == med.id) med else it }
        save(ctx, list)
    }
}
