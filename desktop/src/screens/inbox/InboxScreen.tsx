import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import {
  createChannel,
  getChannels,
  getMessages,
  sendMessage,
} from "../../api/messagingApi";
import { getUsers } from "../../api/userApi";

import type { Channel, ChannelMessage } from "../../models/messaging";
import type { User } from "../../models/user";

import { routes } from "../../navigation/routes";
import { tokenManager } from "../../storage/tokenManager";

type InboxView =
  | { kind: "main" }
  | { kind: "browse" }
  | {
      kind: "chat";
      channelId: number;
      channelName: string;
      channelType: string;
      createdById: number;
    };

function getInitial(name: string): string {
  return name.trim().charAt(0).toUpperCase() || "?";
}

function getInitials(name: string): string {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join("") || "?"
  );
}

function formatMessageTime(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(date));
  } catch {
    return date;
  }
}

function formatMessageDate(date: string): string {
  try {
    return new Intl.DateTimeFormat("pt-PT", {
      day: "2-digit",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(date));
  } catch {
    return date;
  }
}

function getDisplayChannelName(
  channel: Channel,
  currentUserId: number,
): string {
  if (channel.type !== "DM") {
    return channel.name;
  }

  const parts = channel.name.split("-");

  if (parts.length === 3 && parts[0] === "dm") {
    const firstId = Number(parts[1]);
    const secondId = Number(parts[2]);
    const otherId = firstId === currentUserId ? secondId : firstId;

    return `Mensagem privada #${otherId}`;
  }

  return channel.name;
}

async function findOrCreateDmChannel(
  currentUserId: number,
  targetUserId: number,
): Promise<Channel | null> {
  const dmName = `dm-${Math.min(currentUserId, targetUserId)}-${Math.max(
    currentUserId,
    targetUserId,
  )}`;

  try {
    return await createChannel({
      name: dmName,
      description: null,
      isPublic: false,
      type: "DM",
      memberIds: [targetUserId],
    });
  } catch {
    const channels = await getChannels();
    return channels.find((channel) => channel.name === dmName) ?? null;
  }
}

export function InboxScreen() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const currentUserId = tokenManager.getUserId();
  const currentRole = tokenManager.getUserRole() ?? "EMPLOYEE";
  const canCreateChannels =
    currentRole === "ADMIN" || currentRole === "MANAGER";

  const [view, setView] = useState<InboxView>({ kind: "main" });

  const [channels, setChannels] = useState<Channel[]>([]);
  const [users, setUsers] = useState<User[]>([]);

  const [isLoadingChannels, setIsLoadingChannels] = useState(false);
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [query, setQuery] = useState("");

  const [newChannelName, setNewChannelName] = useState("");
  const [newChannelDescription, setNewChannelDescription] = useState("");
  const [newChannelType, setNewChannelType] = useState<
    "GROUP" | "ANNOUNCEMENT"
  >("GROUP");
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([]);
  const [isCreatingChannel, setIsCreatingChannel] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  async function loadChannels() {
    try {
      setIsLoadingChannels(true);
      setErrorMessage(null);

      const result = await getChannels();
      setChannels(result);
    } catch {
      setErrorMessage("Erro ao carregar canais.");
    } finally {
      setIsLoadingChannels(false);
    }
  }

  async function loadUsers() {
    try {
      setIsLoadingUsers(true);

      const result = await getUsers();
      setUsers(result.filter((user) => user.id !== currentUserId));
    } catch {
      setErrorMessage("Erro ao carregar membros da equipa.");
    } finally {
      setIsLoadingUsers(false);
    }
  }

  async function openDm(targetUser: User) {
    try {
      setErrorMessage(null);

      const channel = await findOrCreateDmChannel(currentUserId, targetUser.id);

      if (!channel) {
        setErrorMessage("Não foi possível abrir a mensagem privada.");
        return;
      }

      setView({
        kind: "chat",
        channelId: channel.id,
        channelName: targetUser.name,
        channelType: channel.type,
        createdById: channel.createdById,
      });

      await loadChannels();
    } catch {
      setErrorMessage("Erro ao abrir mensagem privada.");
    }
  }

  async function handleCreateChannel(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (newChannelName.trim() === "") {
      setErrorMessage("O nome do canal é obrigatório.");
      return;
    }

    try {
      setIsCreatingChannel(true);
      setErrorMessage(null);

      const created = await createChannel({
        name: newChannelName.trim(),
        description: newChannelDescription.trim() || null,
        isPublic: true,
        type: newChannelType,
        memberIds: selectedMemberIds,
      });

      setNewChannelName("");
      setNewChannelDescription("");
      setSelectedMemberIds([]);
      setShowCreateForm(false);

      await loadChannels();

      setView({
        kind: "chat",
        channelId: created.id,
        channelName: created.name,
        channelType: created.type,
        createdById: created.createdById,
      });
    } catch {
      setErrorMessage(
        "Erro ao criar canal. Pode já existir um canal com esse nome.",
      );
    } finally {
      setIsCreatingChannel(false);
    }
  }

  useEffect(() => {
    void loadChannels();
    void loadUsers();
  }, []);

  useEffect(() => {
    const dmParam = searchParams.get("dm");

    if (!dmParam) {
      return;
    }

    const targetUserId = Number(dmParam);

    if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
      return;
    }

    async function openInitialDm() {
      try {
        const allUsers = users.length > 0 ? users : await getUsers();
        const targetUser = allUsers.find((user) => user.id === targetUserId);

        if (!targetUser) {
          return;
        }

        await openDm(targetUser);
      } catch {
        setErrorMessage("Erro ao abrir conversa privada.");
      }
    }

    void openInitialDm();
  }, [searchParams, users]);

  if (view.kind === "chat") {
    return (
      <ChatRoomScreen
        channelId={view.channelId}
        channelName={view.channelName}
        channelType={view.channelType}
        createdById={view.createdById}
        currentUserId={currentUserId}
        currentRole={currentRole}
        onBack={() => {
          setView({ kind: "main" });
          void loadChannels();
        }}
      />
    );
  }

  if (view.kind === "browse") {
    const filteredChannels = channels.filter((channel) => {
      return channel.name.toLowerCase().includes(query.trim().toLowerCase());
    });

    const filteredUsers = users.filter((user) => {
      const q = query.trim().toLowerCase();

      if (!q) {
        return true;
      }

      return (
        user.name.toLowerCase().includes(q) ||
        user.email.toLowerCase().includes(q) ||
        user.employeeNumber.toLowerCase().includes(q)
      );
    });

    return (
      <main className="inbox-page">
        <section className="inbox-shell">
          <header className="screen-header dark-header">
            <button
              className="secondary-button"
              onClick={() => setView({ kind: "main" })}
            >
              Cancelar
            </button>

            <div>
              <h1>Procurar Canais</h1>
              <p>Canais, grupos e mensagens privadas</p>
            </div>
          </header>

          {errorMessage && (
            <div className="dashboard-error">{errorMessage}</div>
          )}

          <section className="panel-card inbox-search-card">
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Pesquisar membros ou canais"
            />

            {canCreateChannels && (
              <button
                className={
                  showCreateForm ? "secondary-pill-button" : "blue-pill-button"
                }
                onClick={() => setShowCreateForm((current) => !current)}
              >
                {showCreateForm ? "Fechar" : "Criar canal"}
              </button>
            )}
          </section>

          {showCreateForm && canCreateChannels && (
            <form
              className="panel-card inbox-create-form"
              onSubmit={handleCreateChannel}
            >
              <h2>Novo canal</h2>

              <label>
                <span>Tipo</span>
                <select
                  value={newChannelType}
                  onChange={(event) =>
                    setNewChannelType(
                      event.target.value as "GROUP" | "ANNOUNCEMENT",
                    )
                  }
                >
                  <option value="GROUP">Grupo</option>
                  <option value="ANNOUNCEMENT">Canal de anúncios</option>
                </select>
              </label>

              <label>
                <span>Nome</span>
                <input
                  value={newChannelName}
                  onChange={(event) => setNewChannelName(event.target.value)}
                  placeholder="Nome do canal"
                />
              </label>

              <label>
                <span>Descrição</span>
                <input
                  value={newChannelDescription}
                  onChange={(event) =>
                    setNewChannelDescription(event.target.value)
                  }
                  placeholder="Descrição opcional"
                />
              </label>

              <div className="member-picker">
                <strong>Membros</strong>

                {users.map((user) => {
                  const checked = selectedMemberIds.includes(user.id);

                  return (
                    <label className="member-check-row" key={user.id}>
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={(event) => {
                          if (event.target.checked) {
                            setSelectedMemberIds((current) => [
                              ...current,
                              user.id,
                            ]);
                          } else {
                            setSelectedMemberIds((current) =>
                              current.filter((id) => id !== user.id),
                            );
                          }
                        }}
                      />

                      <span>{user.name}</span>
                    </label>
                  );
                })}
              </div>

              <button className="primary-button" disabled={isCreatingChannel}>
                {isCreatingChannel ? "A criar..." : "Criar"}
              </button>
            </form>
          )}

          <section className="inbox-browse-layout">
            <article className="panel-card">
              <div className="panel-header">
                <h2>Canais sugeridos</h2>
              </div>

              {isLoadingChannels ? (
                <p className="muted-text">A carregar canais...</p>
              ) : filteredChannels.length === 0 ? (
                <p className="muted-text">Sem canais encontrados.</p>
              ) : (
                <div className="browse-list">
                  {filteredChannels.map((channel) => (
                    <button
                      className="browse-row"
                      key={channel.id}
                      onClick={() =>
                        setView({
                          kind: "chat",
                          channelId: channel.id,
                          channelName: getDisplayChannelName(
                            channel,
                            currentUserId,
                          ),
                          channelType: channel.type,
                          createdById: channel.createdById,
                        })
                      }
                    >
                      <span className="channel-avatar">
                        {getInitial(channel.name)}
                      </span>

                      <span>
                        <strong>
                          {getDisplayChannelName(channel, currentUserId)}
                        </strong>
                        <small>{channel.description ?? channel.type}</small>
                      </span>

                      <em>›</em>
                    </button>
                  ))}
                </div>
              )}
            </article>

            <article className="panel-card">
              <div className="panel-header">
                <h2>Membros da equipa</h2>
              </div>

              {isLoadingUsers ? (
                <p className="muted-text">A carregar membros...</p>
              ) : filteredUsers.length === 0 ? (
                <p className="muted-text">Sem membros encontrados.</p>
              ) : (
                <div className="browse-list">
                  {filteredUsers.map((user) => (
                    <button
                      className="browse-row"
                      key={user.id}
                      onClick={() => openDm(user)}
                    >
                      <span className="dm-avatar">
                        {getInitials(user.name)}
                      </span>

                      <span>
                        <strong>{user.name}</strong>
                        <small>
                          {user.employeeNumber} · {user.role}
                        </small>
                      </span>

                      <em>Mensagem</em>
                    </button>
                  ))}
                </div>
              )}
            </article>
          </section>
        </section>
      </main>
    );
  }

  const sortedChannels = [...channels].sort((a, b) => {
    const aLast = a.messages?.[0]?.createdAt ?? a.updatedAt;
    const bLast = b.messages?.[0]?.createdAt ?? b.updatedAt;

    return bLast.localeCompare(aLast);
  });

  return (
    <main className="inbox-page">
      <section className="inbox-shell">
        <header className="screen-header dark-header">
          <button
            className="secondary-button"
            onClick={() => navigate(routes.dashboard)}
          >
            Voltar
          </button>

          <div>
            <h1>Caixa</h1>
            <p>Mensagens internas e canais da equipa</p>
          </div>

          <button
            className="primary-small-button"
            onClick={() => setView({ kind: "browse" })}
          >
            +
          </button>
        </header>

        {errorMessage && <div className="dashboard-error">{errorMessage}</div>}

        <section className="panel-card">
          <div className="panel-header">
            <h2>Canais</h2>

            <button onClick={loadChannels} disabled={isLoadingChannels}>
              {isLoadingChannels ? "A carregar..." : "Atualizar"}
            </button>
          </div>

          {isLoadingChannels ? (
            <p className="muted-text">A carregar canais...</p>
          ) : sortedChannels.length === 0 ? (
            <p className="muted-text">Sem canais disponíveis.</p>
          ) : (
            <div className="channel-list">
              {sortedChannels.map((channel) => {
                const lastMessage = channel.messages?.[0];

                return (
                  <button
                    className="channel-row"
                    key={channel.id}
                    onClick={() =>
                      setView({
                        kind: "chat",
                        channelId: channel.id,
                        channelName: getDisplayChannelName(
                          channel,
                          currentUserId,
                        ),
                        channelType: channel.type,
                        createdById: channel.createdById,
                      })
                    }
                  >
                    <span className="channel-avatar">
                      {getInitial(
                        getDisplayChannelName(channel, currentUserId),
                      )}
                    </span>

                    <span>
                      <strong>
                        {getDisplayChannelName(channel, currentUserId)}
                      </strong>

                      {lastMessage ? (
                        <small>
                          {lastMessage.user.name}: {lastMessage.content}
                        </small>
                      ) : (
                        <small>{channel.description ?? "Sem mensagens"}</small>
                      )}
                    </span>

                    <em>
                      {lastMessage
                        ? formatMessageTime(lastMessage.createdAt)
                        : "›"}
                    </em>
                  </button>
                );
              })}
            </div>
          )}

          <button
            className="browse-more-row"
            onClick={() => setView({ kind: "browse" })}
          >
            <span>+ Procurar canais ou mensagens privadas</span>
            <small>
              Encontra canais, grupos ou abre conversa direta com um colega.
            </small>
          </button>
        </section>
      </section>
    </main>
  );
}

interface ChatRoomScreenProps {
  channelId: number;
  channelName: string;
  channelType: string;
  createdById: number;
  currentUserId: number;
  currentRole: string;
  onBack: () => void;
}

function ChatRoomScreen({
  channelId,
  channelName,
  channelType,
  currentUserId,
  currentRole,
  onBack,
}: ChatRoomScreenProps) {
  const [messages, setMessages] = useState<ChannelMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const canSendMessage = useMemo(() => {
    if (channelType !== "ANNOUNCEMENT") {
      return true;
    }

    return currentRole === "ADMIN" || currentRole === "MANAGER";
  }, [channelType, currentRole]);

  async function loadMessages() {
    try {
      setIsLoadingMessages(true);
      setErrorMessage(null);

      const result = await getMessages(channelId);
      setMessages(
        result.sort((a, b) => a.createdAt.localeCompare(b.createdAt)),
      );
    } catch {
      setErrorMessage("Erro ao carregar mensagens.");
    } finally {
      setIsLoadingMessages(false);
    }
  }

  async function handleSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const content = draft.trim();

    if (!content) {
      return;
    }

    try {
      setIsSending(true);
      setErrorMessage(null);

      const created = await sendMessage(channelId, { content });

      setMessages((current) => [...current, created]);
      setDraft("");
    } catch {
      setErrorMessage("Erro ao enviar mensagem.");
    } finally {
      setIsSending(false);
    }
  }

  useEffect(() => {
    void loadMessages();
  }, [channelId]);

  return (
    <main className="chat-page">
      <section className="chat-shell">
        <header className="chat-header">
          <button className="secondary-button" onClick={onBack}>
            Voltar
          </button>

          <div>
            <h1>{channelName}</h1>
            <p>{channelType === "DM" ? "Mensagem privada" : channelType}</p>
          </div>

          <button onClick={loadMessages} disabled={isLoadingMessages}>
            Atualizar
          </button>
        </header>

        {errorMessage && <div className="dashboard-error">{errorMessage}</div>}

        <section className="chat-panel-unified">
          <div className="chat-scroll-area">
            {isLoadingMessages ? (
              <p className="muted-text">A carregar mensagens...</p>
            ) : messages.length === 0 ? (
              <p className="muted-text">Ainda não existem mensagens.</p>
            ) : (
              <div className="chat-message-list">
                {messages.map((message) => {
                  const isMine = message.userId === currentUserId;

                  return (
                    <article
                      className={`chat-message ${isMine ? "mine" : "theirs"}`}
                      key={message.id}
                    >
                      <div>
                        <strong>{isMine ? "Tu" : message.user.name}</strong>
                        <p>{message.content}</p>
                        <small>{formatMessageDate(message.createdAt)}</small>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>

          {canSendMessage ? (
            <form className="chat-input-bar" onSubmit={handleSend}>
              <input
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Mensagem"
                disabled={isSending}
              />

              <button disabled={isSending || draft.trim() === ""}>
                {isSending ? "..." : "Enviar"}
              </button>
            </form>
          ) : (
            <section className="chat-input-bar disabled">
              Apenas administradores/gestores podem escrever neste canal.
            </section>
          )}
        </section>
      </section>
    </main>
  );
}
