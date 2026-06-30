# Verify - socket service (Express + Socket.IO).
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY . .
EXPOSE 3001
# bootstrap ensures the `socket` table exists (idempotent), then start.
CMD ["sh", "-c", "node db/bootstrap.js && node index.js"]
