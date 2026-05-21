-- CreateTable
CREATE TABLE "WeekAssignment" (
    "id" SERIAL NOT NULL,
    "userId" INTEGER NOT NULL,
    "weekStart" DATE NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "WeekAssignment_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "WeekAssignment_userId_weekStart_key" ON "WeekAssignment"("userId", "weekStart");

-- AddForeignKey
ALTER TABLE "WeekAssignment" ADD CONSTRAINT "WeekAssignment_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;
