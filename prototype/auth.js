"use strict";
/**
 * AuthStore — user accounts + stateless sessions for the prototype.
 * - Passwords hashed with Node's built-in scrypt (never stored in plain text).
 * - Login issues a signed, stateless bearer token (HMAC) — no server-side
 *   session memory, so it works on serverless platforms (Vercel) where each
 *   request may land on a different instance.
 * - Data isolation: every patient / medication / alert / camera belongs to a
 *   userId, so each account only ever sees its own registered patients.
 *
 * Storage: kept in data/users.json when writable (local), otherwise the demo
 * accounts below are seeded in memory (cloud). No failure is fatal.
 */
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const SESSION_SECRET = process.env.SESSION_SECRET || "sanjivanai-prototype-dev-secret";
const TTL_MS = 7 * 24 * 3600 * 1000; // 7 days

class AuthStore {
  constructor(dataDir) {
    this.file = path.join(dataDir, "users.json");
    this.users = [];
    this._load();
    this._seedDefaults();
  }

  _seedDefaults() {
    // Demo accounts always exist (disk file OR in-memory for the cloud).
    for (const d of [
      { name: "Sharma Family", email: "asharma@demo.in", role: "family" },
      { name: "Prakash Family", email: "rprakash@demo.in", role: "family" },
      { name: "Ward Nurse Station", email: "wardnurse@demo.in", role: "ward" },
    ]) {
      if (!this.findByEmail(d.email)) {
        const salt = crypto.randomBytes(8).toString("hex");
        this.users.push({
          id: "U" + crypto.randomBytes(4).toString("hex"),
          name: d.name,
          email: d.email,
          role: d.role,
          password_hash: this._hash("demo123", salt),
          salt,
          createdAt: Date.now(),
        });
      }
    }
    try {
      if (this.users.length >= 3) this._save();
    } catch (e) {
      /* read-only cloud fs — fine, in-memory copy works */
    }
  }

  _load() {
    try {
      if (fs.existsSync(this.file)) {
        this.users = JSON.parse(fs.readFileSync(this.file, "utf8"));
      }
    } catch (e) {
      console.error("users load error", e.message);
    }
  }

  _save() {
    try {
      fs.writeFileSync(this.file, JSON.stringify(this.users, null, 2), "utf8");
    } catch (e) {
      /* cloud read-only fs — ignore, auth still works in memory */
    }
  }

  _hash(password, salt) {
    return crypto.scryptSync(String(password), salt, 32).toString("hex");
  }

  _b64url(buf) {
    return Buffer.from(buf).toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  _issueToken(userId) {
    const payload = this._b64url(JSON.stringify({ uid: userId, exp: Date.now() + TTL_MS }));
    const sig = this._b64url(crypto.createHmac("sha256", SESSION_SECRET).update(payload).digest());
    return payload + "." + sig;
  }

  _verifyToken(token) {
    try {
      const parts = String(token || "").split(".");
      if (parts.length !== 2) return null;
      const expected = this._b64url(
        crypto.createHmac("sha256", SESSION_SECRET).update(parts[0]).digest()
      );
      if (expected !== parts[1]) return null;
      const payload = JSON.parse(Buffer.from(parts[0].replace(/-/g, "+").replace(/_/g, "/"), "base64").toString("utf8"));
      if (!payload || !payload.uid || payload.exp < Date.now()) return null;
      return payload;
    } catch (e) {
      return null;
    }
  }

  publicUser(u) {
    return { id: u.id, name: u.name, email: u.email, role: u.role };
  }

  findByEmail(email) {
    return this.users.find(
      (u) => u.email.toLowerCase() === String(email || "").trim().toLowerCase()
    );
  }

  register({ name, email, password, role = "family" }) {
    if (this.findByEmail(email)) return { error: "email_taken" };
    const salt = crypto.randomBytes(8).toString("hex");
    const user = {
      id: "U" + crypto.randomBytes(4).toString("hex"),
      name: String(name).trim(),
      email: String(email).trim(),
      role,
      password_hash: this._hash(String(password).trim(), salt),
      salt,
      createdAt: Date.now(),
    };
    this.users.push(user);
    this._save();
    const token = this._issueToken(user.id);
    return { token, user: this.publicUser(user) };
  }

  login(email, password) {
    const u = this.findByEmail(email);
    if (!u) return null;
    if (u.password_hash !== this._hash(String(password || "").trim(), u.salt)) return null;
    return { token: this._issueToken(u.id), user: this.publicUser(u) };
  }

  logout() {
    // Stateless tokens — the client simply discards it; nothing to revoke.
  }

  userForToken(token) {
    const payload = this._verifyToken(token);
    if (!payload) return null;
    return this.users.find((u) => u.id === payload.uid) || null;
  }
}

module.exports = { AuthStore };