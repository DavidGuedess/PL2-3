export interface MessageSender {
  id: number;
  name: string;
}

export interface ChannelMessage {
  id: number;
  channelId: number;
  userId: number;
  content: string;
  createdAt: string;
  user: MessageSender;
}

export interface Channel {
  id: number;
  name: string;
  description: string | null;
  isPublic: boolean;
  type: string;
  createdById: number;
  createdAt: string;
  updatedAt: string;
  messages: ChannelMessage[];
}

export interface CreateChannelRequest {
  name: string;
  description?: string | null;
  isPublic?: boolean;
  type?: string;
  memberIds?: number[];
}

export interface SendMessageRequest {
  content: string;
}