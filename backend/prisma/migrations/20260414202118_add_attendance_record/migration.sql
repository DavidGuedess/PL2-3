-- CreateEnum
CREATE TYPE "AttendanceType" AS ENUM ('IN', 'OUT');

-- CreateTable
CREATE TABLE "AttendanceRecord" (
    "id" SERIAL NOT NULL,
    "userId" INTEGER NOT NULL,
    "type" "AttendanceType" NOT NULL,
    "timestamp" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "AttendanceRecord_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "AttendanceRecord_userId_idx" ON "AttendanceRecord"("userId");

-- CreateIndex
CREATE INDEX "AttendanceRecord_timestamp_idx" ON "AttendanceRecord"("timestamp");

-- AddForeignKey
ALTER TABLE "AttendanceRecord" ADD CONSTRAINT "AttendanceRecord_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
