-- CreateTable
CREATE TABLE "Shortner" (
    "id" TEXT NOT NULL,
    "url" TEXT NOT NULL,
    "ttl" TIMESTAMP(3) NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "Shortner_id_key" ON "Shortner"("id");
