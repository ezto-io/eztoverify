# Verify - url-shortener service (Express + Prisma).
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY . .
RUN npx prisma generate
EXPOSE 3000
# Apply migrations once the DB is reachable, then start.
CMD ["sh", "-c", "npx prisma migrate deploy && node index.js"]
