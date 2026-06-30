-- Socket connection registry. Maps a transaction id (chatcode) to live
-- Socket.IO socket ids so /status and /send can route to connected clients.
CREATE TABLE IF NOT EXISTS socket (
  trx_id    text NOT NULL,
  socket_id text NOT NULL
);

CREATE INDEX IF NOT EXISTS socket_trx_id_idx    ON socket (trx_id);
CREATE INDEX IF NOT EXISTS socket_socket_id_idx ON socket (socket_id);
