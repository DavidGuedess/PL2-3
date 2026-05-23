-- CreateEnum
CREATE TYPE "ChannelType" AS ENUM ('GROUP', 'ANNOUNCEMENT', 'DM');

-- DropForeignKey
ALTER TABLE "Message" DROP CONSTRAINT "Message_channelId_fkey";

-- AlterTable
ALTER TABLE "Channel" ADD COLUMN     "type" "ChannelType" NOT NULL DEFAULT 'GROUP';

-- AlterTable
ALTER TABLE "Message" ALTER COLUMN "channelId" DROP NOT NULL;

-- AddForeignKey
ALTER TABLE "Message" ADD CONSTRAINT "Message_channelId_fkey" FOREIGN KEY ("channelId") REFERENCES "Channel"("id") ON DELETE SET NULL ON UPDATE CASCADE;
