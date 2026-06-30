const db = require("../db");

function Connection({ trxId, socId }) {
  this.trxId = trxId;
  this.socId = socId;
}

Connection.prototype.create = async function () {
  try {
    const { rows } = await db.query(
      `INSERT INTO socket(trx_id,socket_id) 
            VALUES ($1, $2)`,
      [this.trxId, this.socId]
    );
    return rows;
  } catch (error) {
    throw error;
  }
};

Connection.prototype.delete = async function () {
  try {
    const { rows } = await db.query(
      `DELETE FROM socket
            WHERE socket_id=$1`,
      [this.socId]
    );
    return rows;
  } catch (error) {
    throw error;
  }
};

Connection.prototype.get = async function () {
  try {
    const { rows } = await db.query(`SELECT * FROM socket WHERE trx_id=$1`, [
      this.trxId,
    ]);
    return rows;
  } catch (error) {
    throw error;
  }
};

module.exports = Connection;
