const uuid = require("uuid");

function generateShortUUID() {
  const originalUUID = uuid.v4();
  const base64UUID = Buffer.from(
    originalUUID.replace(/-/g, ""),
    "hex"
  ).toString("base64");
  const alphanumericBase64 = base64UUID.replace(/[^A-Za-z0-9]/g, "");
  return alphanumericBase64.slice(0, -2);
}

function isTTLValid(epochSeconds) {
  const currentEpochSeconds = Math.floor(Date.now() / 1000);
  return epochSeconds >= currentEpochSeconds;
}

function getFallBackUrl() {
  return process.env.FALLBACK_URL ? process.env.FALLBACK_URL : "https://qlikverify.com";
}

module.exports = {
  generateShortUUID,
  isTTLValid,
  getFallBackUrl
};
