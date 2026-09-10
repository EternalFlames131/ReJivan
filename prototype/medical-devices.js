"use strict";
/**
 * SanjivanAI — Medical Device Integration Layer
 *
 * Defines medically-approved wearable devices that can feed real data into
 * the SanjivanAI dashboard. In the prototype, device data is simulated to
 * match each device's actual specs (accuracy, refresh rate, precision).
 * In production, BLE/WiFi APIs connect to the real hardware.
 *
 * Device categories:
 *   1. Heart Rate / ECG   — e.g. SanketLife (Agatsa, India), Hexoskin
 *   2. SpO2               — e.g. ChoiceMMed MD300C228, Lepu AP-10
 *   3. Blood Pressure      — e.g. Omron medical cuff, Biobeat (aspirational)
 *   4. Temperature         — e.g. TempTraq patch, AION TempShield
 *   5. Glucose (CGM)       — e.g. FreeStyle Libre 3 (Abbott), Dexcom G7
 */

// ---- Device Catalogue (all medically approved) ----

const DEVICE_CATALOGUE = [
  // HEART RATE / ECG
  {
    id: "SANKETLIFE_12",
    name: "SanketLife 12-Lead ECG",
    manufacturer: "Agatsa (Pune, India)",
    category: "ecg",
    approval: "CDSCO Class B, CE certified",
    connectivity: "BLE",
    accuracy: "98.5% (validated at Narayana Health)",
    measures: ["hr"],
    priceINR: "5000",
    refreshSec: 5,
    madeInIndia: true,
    description: "World's smallest 12-lead ECG device. Pocket-sized, Bluetooth-connected. Clinical-grade accuracy at an affordable price.",
    productionTier: "medical",  // maps to confidence base 85
  },
  {
    id: "HEXOSKIN_MEDICAL",
    name: "Hexoskin Medical System",
    manufacturer: "Hexoskin (Canada)",
    category: "ecg",
    approval: "FDA 510(k) cleared",
    connectivity: "BLE",
    accuracy: "Clinical-grade ECG + respiration",
    measures: ["hr"],
    priceINR: "45000",
    refreshSec: 3,
    madeInIndia: false,
    description: "Smart vest with 3-lead ECG, heart rate, respiration rate, and activity tracking. Used in clinical trials and space agencies.",
    productionTier: "medical",
  },

  // SpO2
  {
    id: "CHOICEMMED_MD300",
    name: "ChoiceMMed MD300C228",
    manufacturer: "Beijing Choice (China)",
    category: "spo2",
    approval: "FDA 510(k) cleared",
    connectivity: "BLE 5.0",
    accuracy: "±2% SpO2",
    measures: ["spo2"],
    priceINR: "4000",
    refreshSec: 2,
    madeInIndia: false,
    description: "Fingertip medical pulse oximeter with Bluetooth. Measures SpO2, pulse rate, and perfusion index.",
    productionTier: "medical",
  },
  {
    id: "LEPU_AP10",
    name: "Lepu AP-10 Wrist Oximeter",
    manufacturer: "Lepu Medical",
    category: "spo2",
    approval: "FDA cleared, CE certified",
    connectivity: "BLE 4.0",
    accuracy: "±2% SpO2, continuous wrist-worn",
    measures: ["spo2"],
    priceINR: "10000",
    refreshSec: 2,
    madeInIndia: false,
    description: "Wrist-worn continuous oximeter (not just fingertip). 160-hour data storage, BLE export.",
    productionTier: "medical",
  },

  // Blood Pressure
  {
    id: "OMRON_BP",
    name: "Omron HEM-7156T",
    manufacturer: "Omron (Japan)",
    category: "bp",
    approval: "FDA, CE, CDSCO",
    connectivity: "BLE",
    accuracy: "±3 mmHg",
    measures: ["sbp", "dbp"],
    priceINR: "4500",
    refreshSec: 30,
    madeInIndia: false,
    description: "Medical-grade automatic blood pressure monitor with Bluetooth. Intellisense technology for comfortable, accurate readings.",
    productionTier: "medical",
  },
  {
    id: "BIOBEAT_CHEST",
    name: "Biobeat BB-613 Chest Patch",
    manufacturer: "Biobeat (Israel)",
    category: "bp",
    approval: "FDA 510(k) cleared, CE marked",
    connectivity: "BLE → Cloud API",
    accuracy: "Cuffless BP from PPG (±5 mmHg)",
    measures: ["hr", "sbp", "dbp", "spo2", "temp"],
    priceINR: "25000",
    refreshSec: 5,
    madeInIndia: false,
    description: "Gold-standard chest patch: 13 vitals from one wearable (BP, SpO2, HR, temperature, ECG). Partner-gated API. EMR integration (Epic). Aspirational device for SanjivanAI.",
    productionTier: "medical",
  },

  // Temperature
  {
    id: "TEMPTRAQ_PATCH",
    name: "TempTraq Continuous Temp Patch",
    manufacturer: "Blue Spark Technologies (USA)",
    category: "temperature",
    approval: "FDA Class II cleared",
    connectivity: "BLE",
    accuracy: "±0.1°C",
    measures: ["temp"],
    priceINR: "2000",
    refreshSec: 10,
    madeInIndia: false,
    description: "Disposable axillary temperature patch. 72-hour continuous monitoring with BLE fever alerts.",
    productionTier: "medical",
  },
  {
    id: "AION_TEMPSHIELD",
    name: "AION TempShield",
    manufacturer: "AION Biosystems (USA)",
    category: "temperature",
    approval: "FDA 510(k) cleared",
    connectivity: "BLE + NFC",
    accuracy: "±0.05°C (extrapolated core temp)",
    measures: ["temp"],
    priceINR: "15000",
    refreshSec: 10,
    madeInIndia: false,
    description: "90-day continuous skin temperature sensor with core-temp extrapolation. Clinical-grade fever alerts via cloud.",
    productionTier: "medical",
  },

  // Glucose (CGM)
  {
    id: "FREESTYLE_LIBRE3",
    name: "FreeStyle Libre 3",
    manufacturer: "Abbott",
    category: "glucose",
    approval: "FDA cleared, CDSCO approved (India)",
    connectivity: "BLE",
    accuracy: "MARD 7.9% (real-time, 1-min intervals)",
    measures: ["glucose"],
    priceINR: "4670",
    refreshSec: 60,
    madeInIndia: false,
    description: "Real-time continuous glucose monitor. 14-day wear, LibreLinkUp for caregiver sharing, LibreView for clinicians.",
    productionTier: "medical",
  },
  {
    id: "GLUCORX_VIXXA2",
    name: "GlucoRx Vixxa 2",
    manufacturer: "MicroTech Medical (China, sold by GlucoRx India)",
    category: "glucose",
    approval: "CE, CDSCO Class B certified",
    connectivity: "BLE",
    accuracy: "60-second readings",
    measures: ["glucose"],
    priceINR: "3200",
    refreshSec: 60,
    madeInIndia: false,
    description: "Cheapest CDSCO-certified CGM in India. 15-day wear. App-based monitoring.",
    productionTier: "medical",
  },

  // Indian multiparameter
  {
    id: "H360_HEALTH360",
    name: "H360 Health360",
    manufacturer: "Medilogy Inc (India)",
    category: "multi",
    approval: "CDSCO (Made in India)",
    connectivity: "WiFi (2.4GHz)",
    accuracy: "SpO2 ±2%, ECG clinical-grade",
    measures: ["hr", "spo2"],
    priceINR: "7000",
    refreshSec: 5,
    madeInIndia: true,
    description: "World's smallest multi-parameter device. IIT-designed. Measures SpO2, BP (PPG), 12-lead ECG, BPM. Built-in display + WiFi.",
    productionTier: "medical",
  },
];

// ---- Device Registry (per-patient device assignments) ----

/**
 * In-memory registry: patientId -> { deviceId: { registered, lastSeen, battery, signalStrength, active } }
 * In production, this is persisted to a database. In the prototype, it's seeded with defaults.
 */
const deviceRegistry = new Map();

// Seed default devices for demo patients
function seedDeviceDefaults() {
  const defaults = {
    P1: [
      { deviceId: "SANKETLIFE_12", registered: true },
      { deviceId: "OMRON_BP", registered: true },
      { deviceId: "TEMPTRAQ_PATCH", registered: true },
    ],
    P2: [
      { deviceId: "FREESTYLE_LIBRE3", registered: true },
      { deviceId: "CHOICEMMED_MD300", registered: true },
      { deviceId: "OMRON_BP", registered: true },
    ],
    P3: [
      { deviceId: "BIOBEAT_CHEST", registered: true },
      { deviceId: "FREESTYLE_LIBRE3", registered: true },
    ],
    P4: [
      { deviceId: "BIOBEAT_CHEST", registered: true },
      { deviceId: "SANKETLIFE_12", registered: true },
      { deviceId: "TEMPTRAQ_PATCH", registered: true },
    ],
  };

  for (const [patientId, devices] of Object.entries(defaults)) {
    deviceRegistry.set(
      patientId,
      devices.map((d) => ({
        ...d,
        lastSeen: Date.now(),
        battery: Math.floor(40 + Math.random() * 60), // 40-99%
        signalStrength: Math.floor(60 + Math.random() * 40), // 60-99%
        active: true,
      }))
    );
  }
}
seedDeviceDefaults();

// ---- Device Status Derivation (deterministic from clock) ----

/**
 * Get device registry entries for a patient.
 * Returns array of device status objects with catalogue metadata merged.
 */
function getPatientDevices(patientId) {
  const entries = deviceRegistry.get(patientId) || [];
  return entries.map((entry) => {
    const cat = DEVICE_CATALOGUE.find((d) => d.id === entry.deviceId);
    if (!cat) return { ...entry, name: entry.deviceId, unknown: true };

    // Simulate battery drain over time (slow, deterministic from patientId)
    const batteryDrift = Math.floor((Date.now() / 60000) % 60); // changes ~1% per minute
    const battery = Math.max(5, (entry.battery - Math.floor(batteryDrift * 0.1)) % 100);

    // Signal strength fluctuates slightly
    const signal = Math.min(100, Math.max(10, entry.signalStrength + Math.floor(Math.sin(Date.now() / 30000) * 5)));

    // "last seen" updates every refreshSec
    const secondsAgo = Math.floor((Date.now() - entry.lastSeen) / 1000);
    const isRecent = secondsAgo < cat.refreshSec * 3;

    return {
      deviceId: cat.id,
      name: cat.name,
      manufacturer: cat.manufacturer,
      category: cat.category,
      approval: cat.approval,
      connectivity: cat.connectivity,
      accuracy: cat.accuracy,
      measures: cat.measures,
      priceINR: cat.priceINR,
      madeInIndia: cat.madeInIndia,
      description: cat.description,
      productionTier: cat.productionTier,
      connected: entry.active && isRecent,
      battery,
      signalStrength: signal,
      lastSeen: entry.lastSeen,
      secondsSinceLastSeen: secondsAgo,
    };
  });
}

/**
 * Get device-level metrics for the reliability system.
 * Maps device categories to vitals and returns the "best" confidence
 * per metric based on the medical devices attached.
 */
function deviceConfidenceForVitals(patientId, vitals) {
  const devices = getPatientDevices(patientId);
  const medicalDevices = devices.filter((d) => d.connected && d.productionTier === "medical");

  if (medicalDevices.length === 0) {
    return { tier: "simulated", confidenceMultiplier: 1.0, connectedDevices: 0 };
  }

  // Medical devices boost confidence
  return {
    tier: "medical",
    confidenceMultiplier: 1.4, // 40% boost to base confidence
    connectedDevices: medicalDevices.length,
    categories: [...new Set(medicalDevices.map((d) => d.category))],
  };
}

// ---- Device Catalogue API helpers ----

function getCatalogue(category = null) {
  if (category) return DEVICE_CATALOGUE.filter((d) => d.category === category);
  return DEVICE_CATALOGUE;
}

function getCatalogueItem(id) {
  return DEVICE_CATALOGUE.find((d) => d.id === id);
}

// ---- Simulate device data stream ----

/**
 * Generate a simulated reading from a specific medical device.
 * This is more realistic than pure-random simulation because it
 * respects the device's actual accuracy, refresh rate, and precision.
 */
function simulateDeviceReading(deviceId, baseVitals, nowMs) {
  const cat = getCatalogueItem(deviceId);
  if (!cat) return null;

  const reading = {};
  const accuracyRange = parseFloat(cat.accuracy) || 2; // parse ±% or ±mmHg

  for (const metric of cat.measures) {
    if (baseVitals[metric] == null) continue;

    const base = baseVitals[metric];
    // Simulate device-specific noise (smaller than random, because medical devices are accurate)
    const noisePercent = accuracyRange / 100;
    const noise = base * noisePercent * (Math.sin(nowMs / 1000 + metric.length) * 0.3);
    const rounded = metric === "temp" ? Math.round((base + noise) * 10) / 10 : Math.round(base + noise);

    reading[metric] = rounded;
  }

  return reading;
}

// ---- Exports ----
module.exports = {
  DEVICE_CATALOGUE,
  deviceRegistry,
  getPatientDevices,
  deviceConfidenceForVitals,
  getCatalogue,
  getCatalogueItem,
  simulateDeviceReading,
};
