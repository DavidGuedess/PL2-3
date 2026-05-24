import { apiClient } from "./apiClient";
import type {
  Channel,
  ChannelMessage,
  CreateChannelRequest,
  SendMessageRequest
} from "../models/messaging";

export async function getChannels(): Promise<Channel[]> {
  const response = await apiClient.get<Channel[]>("/channels");
  return response.data;
}

export async function createChannel(
  request: CreateChannelRequest
): Promise<Channel> {
  const response = await apiClient.post<Channel>("/channels", request);
  return response.data;
}

export async function getMessages(
  channelId: number,
  limit = 50,
  before?: number | null
): Promise<ChannelMessage[]> {
  const response = await apiClient.get<ChannelMessage[]>(
    `/channels/${channelId}/messages`,
    {
      params: {
        limit,
        before: before ?? undefined
      }
    }
  );

  return response.data;
}

export async function sendMessage(
  channelId: number,
  request: SendMessageRequest
): Promise<ChannelMessage> {
  const response = await apiClient.post<ChannelMessage>(
    `/channels/${channelId}/messages`,
    request
  );

  return response.data;
}

export async function deleteChannel(id: number): Promise<void> {
  await apiClient.delete(`/channels/${id}`);
}