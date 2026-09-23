# Verify - url-shortener service (Express + Prisma).
FROM node:18-alpine
WORKDIR /app

# Prisma's query engine needs OpenSSL to detect the platform on Alpine;
# without it `prisma generate` picks the wrong engine at runtime. The
# published image carries this layer, this file never did.
RUN apk add --no-cache openssl

COPY package*.json ./
RUN npm install --omit=dev
COPY . .
RUN npx prisma generate
EXPOSE 3000
# Apply migrations once the DB is reachable, then start.
CMD ["sh", "-c", "npx prisma migrate deploy && node index.js"]
