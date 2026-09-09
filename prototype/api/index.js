"use strict";
/**
 * Vercel serverless entrypoint. The whole Express app is exported as a single
 * function; vercel.json rewrites every route here. Runs identically locally
 * (npm start) and on Vercel.
 */
module.exports = require("../server.js");