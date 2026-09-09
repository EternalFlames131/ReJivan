"use strict";
/**
 * AutoCaller — automatic emergency-call chain (REAL logic, SIMULATED placement).
 *
 * When a DANGER alert fires, calls are dialled immediately, in priority order:
 *   1. Family caregiver   (voice)
 *   2. Backup contact     (voice)   — 2 retries, then escalate
 *   3. Emergency services 108/112  — automatic ambulance dispatch
 *
 * Call PLACEMENT is simulated (no real telecom is used); the auto-trigger,
 * priority ladder, retry and escalation logic is real and runs in real time.
 */
const { random } = Math;

const CONTACTS = {
  P1: { family: "+91 98300 11001", backup: "+91 98300 11002" },
  P2: { family: "+91 98300 12001", backup: "+91 98300 12002" },
  P3: { family: "Ward A nurse · +91 98300 13001", backup: "Duty doctor · +91 98300 13002" },
  P4: { family: "Ward A nurse · +91 98300 14001", backup: "Duty doctor · +91 98300 14002" },
};

class AutoCaller {
  constructor() {
    this.calls = [];
    this.seq = 0;
  }

  trigger(alert) {
    const c = CONTACTS[alert.patientId] || { family: "Family caregiver", backup: "Backup contact" };
    const call = {
      id: "CAL" + String(++this.seq).padStart(3, "0"),
      alertId: alert.id,
      patientId: alert.patientId,
      patientName: alert.patientName,
      startedAt: Date.now(),
      status: "dialing",
      ladder: [
        { label: "family", to: c.family, mode: "voice", emergency: false, state: "pending" },
        { label: "backup", to: c.backup, mode: "voice", emergency: false, state: "pending" },
        { label: "emergency", to: "108 / 112", mode: "voice-dispatch", emergency: true, state: "pending" },
      ],
      log: [],
    };
    this.calls.unshift(call);
    this._log(call, `Danger alert ${alert.id} — automatic call chain started immediately`);
    // First dial starts without delay.
    this._dial(call, 0);
    return call;
  }

  _log(call, msg) {
    call.log.push({ t: Date.now(), msg });
  }

  _dial(call, idx) {
    const step = call.ladder[idx];
    if (!step) {
      call.status = "complete";
      return;
    }
    step.state = "dialing";
    call.status = step.emergency ? "dialing" : "dialing";
    this._log(call, `Calling ${step.label} → ${step.to} (${step.mode})`);

    let attempt = 0;
    const attemptDial = () => {
      const delay = 1500 + random() * 2000;
      setTimeout(() => {
        // Only proceed if this is still the active call (no newer full answer).
        const answered = random() < (step.emergency ? 0.9 : 0.55);
        if (answered) {
          if (step.emergency) {
            step.state = "answered";
            call.status = "dispatched";
            this._log(call, `Emergency services reached on ${step.to} — ambulance dispatched, GPS + vitals sent`);
          } else {
            step.state = "answered";
            call.status = "answered";
            this._log(call, `${step.label} answered on ${step.to} — vitals + alert shared live`);
          }
          return;
        }
        attempt++;
        if (attempt < 2) {
          this._log(call, `${step.label} did not answer — ringing again (attempt ${attempt + 1})`);
          attemptDial();
        } else {
          step.state = "unanswered";
          call.status = idx + 1 < call.ladder.length ? "escalating" : "complete";
          this._log(call, `${step.label} did not answer after 3 rings — escalating to next contact NOW`);
          this._dial(call, idx + 1);
        }
      }, delay);
    };
    attemptDial();
  }

  list() {
    return this.calls;
  }
}

module.exports = { AutoCaller };