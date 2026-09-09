"use strict";
/**
 * AuthStore — user accounts + sessions for the prototype.
 * - Passwords hashed with Node's built-in scrypt (never stored in plain text).
 * - Session tokens issued on login; users are looked up in memory.
 * - Data isolation: every patient / medication / alert / camera belongs to a
 *   userId, so each account only ever sees its own registered patients.
 */
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

class AuthStore {
  constructor(dataDir) {
    this.file = path.join(dataDir, "users.json");
    this.users = [];
    this.sessions = new Map(); // token -> userId
    this._load();
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
    fs.writeFileSync(this.file, JSON.stringify(this.users, null, 2), "utf8");
  }

  _hash(password, salt) {
    return crypto.scryptSync(String(password), salt, 32).toString("hex");
  }

  _issueToken(userId) {
    const token = crypto.randomBytes(24).toString("hex");
    this.sessions.set(token, userId);
    return token;
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
    const token = this._issueToken(u.id);
    return { token, user: this.publicUser(u) };
  }

  logout(token) {
    if (token) this.sessions.delete(token);
  }

  userForToken(token) {
    if (!token) return null;
    const id = this.sessions.get(token);
    return id ? this.users.find((u) => u.id === id) || null : null;
  }

  /**
   * Demo seeding — creates the account only if the email is not already present.
   */
  seedUser({ name, email, password, role = "family" }) {
    const existing = this.findByEmail(email);
    if (existing) return existing;
    const salt = crypto.randomBytes(8).toString("hex");
    const user = {
      id: "U" + crypto.randomBytes(4).toString("hex"),
      name,
      email,
      role,
      password_hash: this._hash(password, salt),
      salt,
      createdAt: Date.now(),
    };
    this.users.push(user);
    this._save();
    return user;
  }
}

module.exports = { AuthStore };