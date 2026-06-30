const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function isDatabaseConnected() {
  try {
    await prisma.$connect();
    return true;
  } catch (error) {
    console.error('Prisma failed to connect to the database:', error);
    return false;
  } finally {
    await prisma.$disconnect();
  }
}

module.exports = {
    isDatabaseConnected
}


