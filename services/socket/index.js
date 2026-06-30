const express = require("express");
const socket = require("socket.io");
const Conn = require("./models/Connection");
const app = express();
const cors = require("cors");
require("dotenv").config();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
//const connections = {};

app.get("/health", async (req, res) => {
  res.send("All Looks Good!!");
});

app.post("/status", async (req, res) => {
  console.log("-------------------");
  console.log("RECEIVED REQUEST FOR KEY : " + req.query.key);
  let trxId = req.query.key;
  let cc = new Conn({ trxId, undefined });
  const result = await cc.get();
  if (result && result.length > 0) {
    for (let x = 0; x < result.length; x++) {
      console.log(result[x].socket_id);
      io.to(result[x].socket_id).emit(req.query.key, req.body);
    }
    res.sendStatus(201);
    return;
  }

  res.sendStatus(400);
});

app.post("/send", async (req, res) => {
  console.log("-------------------");
  console.log("RECEIVED REQUEST FOR KEY : " + req.query.key);
  let trxId = req.query.key;
  let cc = new Conn({ trxId, undefined });
  const result = await cc.get();
    if (result && result.length > 0) {
    for (let x = 0; x < result.length; x++) {
      console.log(result[x].socket_id);
      io.to(result[x].socket_id).emit(req.query.key, req.body);
    }
    res.sendStatus(201);
    return;
  }

  res.sendStatus(400);
});

const PORT = process.env.PORT || 3001;

const server = app.listen(PORT, () => console.log("Server running..."));

let io = socket(server, {
  pingTimeout: 3000,
  pingInterval: 5000
} );

io.use((socket, next) => {
  try {
    let trxId = socket.handshake.headers.chatcode;
    if (!trxId) {
      trxId = socket.handshake.auth.chatcode;
    }
    if (!trxId) {
      return next(new Error("invalid transaction"));
    }
    socket.trxId = trxId;
    next();
  } catch (err) {
    next(new Error(err.message));
  }
});

io.on("connection", async (socket) => {
  console.log("-------------------");
  console.log("USER CONNECTED", socket.id);
  const socId = socket.id;
  const trxId = socket.trxId;
  const cc = new Conn({ trxId, socId });
  try {
    const result = await cc.create();
console.log(result);
  } catch (err) {
    console.log(`${err}`);
    const result = await cc.delete();
    console.log(result);
    socket.disconnect(true);
    return;
  }

  socket.on("disconnect", async () => {
    const result = await cc.delete();
    console.log(result);
    console.log("-------------------");
    console.log("USER DISCONNECTED");
  });
});
