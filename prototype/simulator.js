"use strict";
/**
 * VitalSim — simulated vital-signs engine.
 *
 * REAL vs SIMULATED (prototype honesty, required by project spec):
 *   - The data this module produces is SIMULATED. In the real product this
 *     comes from a wearable / At-Home Monitor plus contact-free camera sensing.
 *   - Everything downstream in the dashboard, rules engine, alerts and
 *     escalation is REAL logic.
 */
const { random, round } = Math;

function clamp(v, lo, hi) {
  return Math.max(lo, Math.min(hi, v));
}

function gauss(mean = 0, sd = 1) {
  // Box-Muller
  let u = 0, v = 0;
  while (u === 0) u = random();
  while (v === 0) v = random();
  return mean + sd * Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
}

function drift(prev, target, rate, sd) {
  // Move a value toward a target at `rate` with noise `sd`.
  const pulled = prev + (target - prev) * rate;
  return pulled + gauss(0, sd);
}

/**
 * A single simulated patient with live vitals.
 * id: string, name, age, sex, condition (baseline profile)
 */
class Patient {
  constructor(spec) {
    this.id = spec.id;
    this.name = spec.name;
    this.age = spec.age;
    this.sex = spec.sex || "M";
    this.location = spec.location || "Home — Room 1";
    this.ward = spec.ward || null; // e.g. "Ward A / Bed 3" when in hospital
    this.condition = spec.condition || "healthy";

    // Baseline targets per condition
    const base = BASELINES[this.condition] || BASELINES.healthy;
    this.base = base;

    // Current-ish state (start near baseline)
    this.vitals = {
      hr: base.hr,
      spo2: base.spo2,
      sbp: base.sbp,
      dbp: base.dbp,
      temp: base.temp,
      glucose: base.glucose,
    };

    // Slow-moving "health drift" component so values look alive over minutes
    this.healthDrift = gauss(0, 0.5);

    // Episode scheduler: occasionally trigger a spike/danger for demo realism
    this.nextEpisodeAt = performance_seconds() + 30 + random() * 90;
    this.episode = null; // { type, until }
    this.history = {};

    this.lastUpdated = Date.now();
  }

  tick(dtSec) {
    const b = this.base;

    // Manage an active episode (temporary deviation toward a danger band)
    if (this.episode) {
      if (performance_seconds() > this.episode.until) {
        this.episode = null;
      }
    } else if (performance_seconds() > this.nextEpisodeAt) {
      const types = Object.keys(EPISODES[this.condition] || EPISODES.healthy);
      const type = types[(random() * types.length) | 0];
      this.episode = {
        type,
        until: performance_seconds() + 20 + random() * 40,
      };
      this.nextEpisodeAt = performance_seconds() + 90 + random() * 180;
    }

    let hrTarget = b.hr, spo2Target = b.spo2;
    let sbpTarget = b.sbp, dbpTarget = b.dbp;
    let tempTarget = b.temp, gluTarget = b.glucose;

    if (this.episode) {
      const e = EPISODES[this.condition]?.[this.episode.type];
      if (e) {
        if (e.hr) hrTarget = e.hr;
        if (e.spo2) spo2Target = e.spo2;
        if (e.sbp) sbpTarget = e.sbp;
        if (e.dbp) dbpTarget = e.dbp;
        if (e.temp) tempTarget = e.temp;
        if (e.glucose) gluTarget = e.glucose;
      }
    }

    // Slow wander plus episode pull
    this.healthDrift = clamp(this.healthDrift + gauss(0, 0.15), -1.5, 1.5);

    const v = this.vitals;
    v.hr = round(clamp(drift(v.hr, hrTarget + this.healthDrift * 2, 0.15, 1.6), 40, 200));
    v.spo2 = clamp(drift(v.spo2, spo2Target, 0.1, 0.4), 70, 100).toFixed(1);
    v.sbp = round(clamp(drift(v.sbp, sbpTarget + this.healthDrift, 0.12, 3), 70, 200));
    v.dbp = round(clamp(drift(v.dbp, dbpTarget, 0.12, 2), 40, 140));
    v.temp = clamp(drift(v.temp, tempTarget, 0.08, 0.05), 34, 42).toFixed(1);
    v.glucose = round(clamp(drift(v.glucose, gluTarget, 0.1, 4), 60, 300));

    v.hr = Number(v.hr);
    v.sbp = Number(v.sbp);
    v.dbp = Number(v.dbp);
    v.spo2 = Number(v.spo2);
    v.temp = Number(v.temp);
    v.glucose = Number(v.glucose);
    v.sys = Number(v.sbp);

    this.lastUpdated = Date.now();
    return { ...this.vitals };
  }

  snapshot() {
    return {
      id: this.id,
      name: this.name,
      age: this.age,
      sex: this.sex,
      location: this.location,
      ward: this.ward,
      condition: this.condition,
      vitals: { ...this.vitals },
      lastUpdated: this.lastUpdated,
      episode: this.episode,
    };
  }
}

function performance_seconds() {
  // Stable seconds clock for episode scheduling
  return Date.now() / 1000;
}

// Baseline profiles
const BASELINES = {
  healthy:        { hr: 74,  spo2: 98, sbp: 120, dbp: 80, temp: 36.8, glucose: 100 },
  "heart-arrhythmia": { hr: 88, spo2: 96, sbp: 128, dbp: 82, temp: 36.9, glucose: 104 },
  "post-surgery": { hr: 80,  spo2: 97, sbp: 118, dbp: 76, temp: 37.0, glucose: 110 },
  diabetes:       { hr: 78,  spo2: 97, sbp: 130, dbp: 85, temp: 36.7, glucose: 190 },
  hypertension:   { hr: 80,  spo2: 97, sbp: 150, dbp: 96, temp: 36.8, glucose: 108 },
  elderly:        { hr: 76,  spo2: 96, sbp: 135, dbp: 82, temp: 36.5, glucose: 105 },
};

// Episodes map: per condition, named events → target danger values
const EPISODES = {
  healthy: {
    "tachycardia spike": { hr: 168 },
    "brief desaturation": { spo2: 88 },
    "stress rise": { sbp: 152, dbp: 98 },
  },
  "heart-arrhythmia": {
    "arrhythmia episode": { hr: 172, sbp: 140 },
    "rapid HR": { hr: 182 },
  },
  "post-surgery": {
    "fever spike": { temp: 39.1 },
    "desaturation": { spo2: 86 },
  },
  diabetes: {
    "hypoglycaemia": { glucose: 62 },
    "hyperglycaemia": { glucose: 255 },
  },
  hypertension: {
    "hypertensive crisis": { sbp: 178, dbp: 112 },
  },
  elderly: {
    "bradycardia": { hr: 46 },
    "desaturation": { spo2: 87 },
    "fall-risk episode": { sbp: 146 },
  },
};

module.exports = { Patient, BASELINES, EPISODES };
