require('dotenv').config(); 

const express = require("express");
const bodyParser = require("body-parser");
const { validationResult } = require("express-validator");
const { isDatabaseConnected } = require("./liveliness/liveliness");
const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();
const {
  validateGeneratesShotenedUrlRequest,
  validateGetShotenedUrlRequest,
} = require("./request_validator/validator");
const { generateShortUUID, isTTLValid, getFallBackUrl } = require("./helper");

const app = express();
const PORT = process.env.PORT || 3000;
app.use(bodyParser.json());

const validApiKeys = (process.env.VALID_API_KEYS || "").split(",").filter(Boolean);
const apiKeyMiddleware = (req, res, next) => {
  const apiKey = req.headers["x-api-key"];

  if (!apiKey || !validApiKeys.includes(apiKey)) {
    return res.status(401).json({ error: "Invalid API key." });
  }

  next();
};

app.post(
  "/",
  apiKeyMiddleware,
  validateGeneratesShotenedUrlRequest,
  async (req, res) => {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
      }

      if (!isTTLValid(req.body.ttl)) {
        return res.status(400).json({
          error: "Invalid TTL. TTL should be greated than the current time",
        });
      }

      const createdShortner = await prisma.shortner.create({
        data: {
          id: generateShortUUID(),
          url: req.body.url,
          ttl: new Date(req.body.ttl * 1000),
        },
      });

      return res.status(201).json(createdShortner);
    } catch (error) {
      console.error(error.message);
      return res.status(500).json({ error: error.message || "error occurred" });
    }
  }
);

app.get("/:id", validateGetShotenedUrlRequest, async (req, res) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }

    let id = req.params.id;

    if (id) {
      id = sanitizeId(id);
    }
    const shortner = await prisma.shortner.findUnique({
      where: {
        id: id,
      },
    });

    if (!shortner) return res.redirect(getFallBackUrl());

    if (shortner.ttl !== null && !isTTLValid(new Date(shortner.ttl) / 1000)) {
      await prisma.shortner.delete({
        where: {
          id: id,
        },
      });
      return res.redirect(getFallBackUrl());
    } else {
      return res.redirect(shortner.url);
    }
  } catch (error) {
    console.error(error.message);
    return res.status(500).json({ error: error.message || "error occurred" });
  }
});

app.get("/liveliness/health", async (req, res) => {
  const connected = await isDatabaseConnected();

  if (connected) {
    res.status(200).json({ status: "Healthy" });
  } else {
    res.status(500).json({ status: "Unhealthy" });
  }
});

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

function sanitizeId(str) {
  return str.replace(/[^a-zA-Z0-9]/g, "");
}
