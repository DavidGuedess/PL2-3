import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { Megaphone, Users, MessageSquare, Check, Plus } from "lucide-react";
import { useSearchParams } from "react-router-dom";

import {
  createChannel,
  deleteChannel,
  getChannels,
  getMessages,
  sendMessage,
} from "../../api/messagingApi";
import { getUsers } from "../../api/userApi";
import { UserAvatar } from "../../components/UserAvatar";
import type { Channel, ChannelMessage } from "../../models/messaging";
import type { User } from "../../models/user";
import { tokenManager } from "../../storage/tokenManager";

const INBOX_REFRESH_MS = 1000;
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:3001";

function initials(name: string) {
  return (
    name
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join("") || "?"
  );
}

function avatarSrc(pic: string | null | undefined) {
  if (!pic) return null;
  return pic.startsWith("http") ? pic : `${API_BASE}${pic}`;
}

function fmtDatetime(ts: string) {
  return new Intl.DateTimeFormat("pt-PT", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(ts));
}

function getLastMessagePreview(
  channel: Channel,
  currentUserId: number,
): string {
  const lastMsg = channel.messages?.[0];

  if (!lastMsg) {
    return "";
  }

  const sender =
    lastMsg.userId === currentUserId ? "Eu" : (lastMsg.user?.name ?? "Alguém");

  return `${sender}: ${lastMsg.content}`;
}

function dmOtherUser(
  channel: Channel,
  currentUserId: number,
  users: User[],
): User | undefined {
  const parts = channel.name.split("-");

  if (parts.length === 3 && parts[0] === "dm") {
    const a = Number(parts[1]);
    const b = Number(parts[2]);
    const otherId = a === currentUserId ? b : a;

    return users.find((u) => u.id === otherId);
  }

  return undefined;
}

function dmDisplayName(
  channel: Channel,
  currentUserId: number,
  users: User[],
): string {
  const other = dmOtherUser(channel, currentUserId, users);

  return other?.name ?? channel.name;
}

async function findOrCreateDm(
  currentUserId: number,
  targetId: number,
): Promise<Channel> {
  const name = `dm-${Math.min(currentUserId, targetId)}-${Math.max(
    currentUserId,
    targetId,
  )}`;

  try {
    return await createChannel({ name, type: "DM", memberIds: [targetId] });
  } catch {
    const list = await getChannels();
    const existing = list.find((c) => c.name === name);

    if (existing) return existing;

    throw new Error("Não foi possível abrir o chat privado.");
  }
}

type CreateStep = "type" | "form";

export function InboxScreen() {
  const [searchParams] = useSearchParams();
  const dmParam = searchParams.get("dm");

  const currentUserId = tokenManager.getUserId();
  const role = tokenManager.getUserRole() ?? "EMPLOYEE";
  const isEmployee = role === "EMPLOYEE";
  const isAdmin = role === "ADMIN";

  const [channels, setChannels] = useState<Channel[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChannelMessage[]>([]);
  const [msgInput, setMsgInput] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [isLoadingMsg, setIsLoadingMsg] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [createStep, setCreateStep] = useState<CreateStep>("type");
  const [createType, setCreateType] = useState<"GROUP" | "ANNOUNCEMENT">(
    "GROUP",
  );
  const [newName, setNewName] = useState("");
  const [selectedMembers, setSelectedMembers] = useState<number[]>([]);
  const [allMembers, setAllMembers] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const bottomRef = useRef<HTMLDivElement>(null);
  const activeIdRef = useRef<number | null>(null);

  const activeChannel = useMemo(
    () => channels.find((c) => c.id === activeId) ?? null,
    [channels, activeId],
  );

  useEffect(() => {
    activeIdRef.current = activeId;
  }, [activeId]);

  function showToast(msg: string) {
    setToast(msg);
    window.setTimeout(() => setToast(null), 3000);
  }

  async function loadChannels() {
    const list = await getChannels();
    setChannels(list);
    return list;
  }

  async function loadMessages(channelId: number, showLoading = false) {
    if (showLoading) {
      setIsLoadingMsg(true);
    }

    try {
      const msgs = await getMessages(channelId);

      if (activeIdRef.current === channelId) {
        setMessages(msgs);
      }

      return msgs;
    } finally {
      if (showLoading && activeIdRef.current === channelId) {
        setIsLoadingMsg(false);
      }
    }
  }

  async function loadUsers() {
    const list = await getUsers();
    setUsers(list.filter((u) => u.active && u.id !== currentUserId));
  }

  async function openChannel(id: number) {
    setActiveId(id);
    activeIdRef.current = id;
    setMessages([]);

    try {
      await loadMessages(id, true);
    } catch {
      showToast("Erro ao carregar mensagens.");
    }

    window.setTimeout(
      () => bottomRef.current?.scrollIntoView({ behavior: "smooth" }),
      80,
    );
  }

  async function refreshInboxSilently() {
    try {
      await loadChannels();

      const selectedId = activeIdRef.current;

      if (selectedId) {
        await loadMessages(selectedId, false);
      }
    } catch {
      // refresh silencioso
    }
  }

  useEffect(() => {
    if (!dmParam) return;

    const targetId = Number(dmParam);

    if (!targetId) return;

    void (async () => {
      try {
        await loadUsers();

        const ch = await findOrCreateDm(currentUserId, targetId);
        const list = await loadChannels();

        if (!list.find((c) => c.id === ch.id)) {
          setChannels((prev) => [...prev, ch]);
        }

        await openChannel(ch.id);
      } catch {
        showToast("Erro ao abrir chat privado.");
      }
    })();
  }, [dmParam]);

  useEffect(() => {
    void loadChannels();
    void loadUsers();
  }, []);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      void refreshInboxSilently();
    }, INBOX_REFRESH_MS);

    const handleFocus = () => {
      void refreshInboxSilently();
    };

    window.addEventListener("focus", handleFocus);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", handleFocus);
    };
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function handleSend(e: FormEvent) {
    e.preventDefault();

    if (!activeId || !msgInput.trim() || isSending) return;

    setIsSending(true);

    try {
      const msg = await sendMessage(activeId, { content: msgInput.trim() });

      setMessages((prev) => [...prev, msg]);
      setMsgInput("");

      await loadChannels();
      await loadMessages(activeId, false);
    } catch {
      showToast("Erro ao enviar mensagem.");
    } finally {
      setIsSending(false);
    }
  }

  async function handleDelete() {
    if (!activeId) return;
    if (!window.confirm("Apagar esta conversa?")) return;

    try {
      await deleteChannel(activeId);

      setActiveId(null);
      activeIdRef.current = null;
      setMessages([]);

      await loadChannels();

      showToast("Conversa apagada.");
    } catch {
      showToast("Erro ao apagar conversa.");
    }
  }

  async function handleCreateDm(targetId: number) {
    try {
      const ch = await findOrCreateDm(currentUserId, targetId);
      const list = await loadChannels();

      if (!list.find((c) => c.id === ch.id)) {
        setChannels((prev) => [...prev, ch]);
      }

      await openChannel(ch.id);
    } catch {
      showToast("Erro ao abrir chat privado.");
    }
  }

  async function handleCreateChannel(e: FormEvent) {
    e.preventDefault();

    if (!newName.trim()) return;

    const memberIds = allMembers ? users.map((u) => u.id) : selectedMembers;

    try {
      const ch = await createChannel({
        name: newName.trim(),
        type: createType,
        memberIds,
      });

      await loadChannels();

      setShowCreate(false);
      setNewName("");
      setSelectedMembers([]);
      setAllMembers(false);

      await openChannel(ch.id);

      showToast("Canal criado.");
    } catch {
      showToast("Erro ao criar canal.");
    }
  }

  function toggleMember(id: number) {
    setSelectedMembers((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  }

  const groupChannels = channels.filter(
    (c) => c.type === "GROUP" || c.type === "ANNOUNCEMENT" || !c.type,
  );

  const dmChannels = channels.filter((c) => c.type === "DM");

  const canSend =
    activeChannel && !(isEmployee && activeChannel.type === "ANNOUNCEMENT");

  const canDelete =
    activeChannel &&
    (activeChannel.type === "DM" ||
      (activeChannel.type === "GROUP" && !isEmployee) ||
      (activeChannel.type === "ANNOUNCEMENT" && isAdmin));

  return (
    <div className="inbox-page">
      <div className="inbox-sidebar">
        <div className="inbox-header">
          <h2>Chats</h2>
        </div>

        <div className="channel-section">
          <div className="channel-section-header">
            <span>Canais</span>

            {!isEmployee && (
              <button
                title="Novo canal"
                onClick={() => {
                  setShowCreate(true);
                  setCreateStep("type");
                }}
              >
                <Plus size={14} />
              </button>
            )}
          </div>

          {groupChannels.map((ch) => (
            <button
              key={ch.id}
              className={`channel-item${ch.id === activeId ? " active" : ""}`}
              onClick={() => openChannel(ch.id)}
            >
              <div
                className={`channel-icon ${
                  ch.type === "ANNOUNCEMENT" ? "announcement" : "group"
                }`}
              >
                {ch.type === "ANNOUNCEMENT" ? (
                  <Megaphone size={16} />
                ) : (
                  <Users size={16} />
                )}
              </div>

              <div className="channel-item-text">
                <div className="channel-item-name">
                  {ch.name === "Todos" ? "Todos" : ch.name}
                </div>

                <div className="channel-item-preview">
                  {getLastMessagePreview(ch, currentUserId) || "Sem mensagens"}
                </div>
              </div>
            </button>
          ))}
        </div>

        <div className="channel-section dm-section">
          <div className="channel-section-header">
            <span>Mensagens diretas</span>

            <button
              title="Nova mensagem privada"
              onClick={() => {
                setShowCreate(true);
                setCreateStep("type");
              }}
            >
              <Plus size={14} />
            </button>
          </div>

          {dmChannels.map((ch) => {
            const displayName = dmDisplayName(ch, currentUserId, users);
            const other = dmOtherUser(ch, currentUserId, users);

            return (
              <button
                key={ch.id}
                className={`channel-item${ch.id === activeId ? " active" : ""}`}
                onClick={() => openChannel(ch.id)}
              >
                <UserAvatar
                  name={displayName}
                  profilePicture={other?.profilePicture}
                  size="sm"
                  className="channel-icon dm"
                />

                <div className="channel-item-text">
                  <div className="channel-item-name">{displayName}</div>

                  <div className="channel-item-preview">
                    {getLastMessagePreview(ch, currentUserId) ||
                      "Sem mensagens"}
                  </div>
                </div>
              </button>
            );
          })}

          {users.map((u) => {
            const hasDm = dmChannels.some((ch) => {
              const p = ch.name.split("-");

              return (
                p.length === 3 &&
                p[0] === "dm" &&
                (Number(p[1]) === u.id || Number(p[2]) === u.id)
              );
            });

            if (hasDm) return null;

            return (
              <button
                key={`new-dm-${u.id}`}
                className="channel-item"
                onClick={() => handleCreateDm(u.id)}
                title={`Mensagem para ${u.name}`}
              >
                <UserAvatar
                  name={u.name}
                  profilePicture={u.profilePicture}
                  size="sm"
                  className="channel-icon dm"
                />

                <div className="channel-item-text">
                  <div
                    className="channel-item-name"
                    style={{ color: "var(--text-faint)" }}
                  >
                    {u.name}
                  </div>

                  <div className="channel-item-preview">Iniciar conversa</div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {activeChannel ? (
        <div className="chat-area">
          <div className="chat-header">
            <div className="chat-header-left">
              {activeChannel.type === "DM" ? (
                <UserAvatar
                  name={dmDisplayName(activeChannel, currentUserId, users)}
                  profilePicture={
                    dmOtherUser(activeChannel, currentUserId, users)
                      ?.profilePicture
                  }
                  size="md"
                  className="channel-icon dm"
                />
              ) : (
                <div
                  className={`channel-icon ${
                    activeChannel.type === "ANNOUNCEMENT"
                      ? "announcement"
                      : "group"
                  }`}
                  style={{ width: 36, height: 36 }}
                >
                  {activeChannel.type === "ANNOUNCEMENT" ? (
                    <Megaphone size={16} />
                  ) : (
                    <Users size={16} />
                  )}
                </div>
              )}

              <div>
                <div className="chat-header-name">
                  {activeChannel.type === "DM"
                    ? dmDisplayName(activeChannel, currentUserId, users)
                    : activeChannel.name}
                </div>

                <div className="chat-header-desc">
                  {activeChannel.type === "DM"
                    ? "Mensagem privada"
                    : activeChannel.type === "ANNOUNCEMENT"
                      ? "Canal de anúncios"
                      : "Canal de grupo"}
                </div>
              </div>
            </div>

            {canDelete && (
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>
                Apagar conversa
              </button>
            )}
          </div>

          {isLoadingMsg ? (
            <div className="loading-state">A carregar mensagens...</div>
          ) : (
            <div className="chat-messages">
              {messages.length === 0 && (
                <div className="chat-empty">
                  Sem mensagens. Seja o primeiro a escrever!
                </div>
              )}

              {messages.map((msg) => {
                const isOwn = msg.userId === currentUserId;
                const senderName = isOwn
                  ? "Eu"
                  : (msg.user?.name ?? `Utilizador #${msg.userId}`);
                const pic = isOwn
                  ? tokenManager.getProfilePicture()
                  : msg.user?.profilePicture;

                return (
                  <div
                    key={msg.id}
                    className={`chat-msg${isOwn ? " own" : ""}`}
                  >
                    {avatarSrc(pic) ? (
                      <img
                        src={avatarSrc(pic)!}
                        alt={senderName}
                        className="msg-avatar"
                        style={{ objectFit: "cover", borderRadius: "50%" }}
                      />
                    ) : (
                      <div className="msg-avatar">{initials(senderName)}</div>
                    )}

                    <div className="msg-body">
                      <div className="msg-meta">
                        {senderName} · {fmtDatetime(msg.createdAt)}
                      </div>

                      <div className="msg-bubble">{msg.content}</div>
                    </div>
                  </div>
                );
              })}

              <div ref={bottomRef} />
            </div>
          )}

          {canSend ? (
            <form className="chat-input-row" onSubmit={handleSend}>
              <input
                value={msgInput}
                onChange={(e) => setMsgInput(e.target.value)}
                placeholder="Enviar uma mensagem..."
                autoFocus
              />

              <button
                type="submit"
                className="btn btn-primary btn-sm"
                disabled={isSending || !msgInput.trim()}
              >
                {isSending ? "..." : "Enviar"}
              </button>
            </form>
          ) : (
            <div className="chat-readonly-notice">
              Apenas gestores podem enviar mensagens neste canal.
            </div>
          )}
        </div>
      ) : (
        <div className="chat-area">
          <div className="chat-welcome">
            <MessageSquare size={36} color="var(--text-faint)" />
            <span>Seleciona um canal ou conversa para começar</span>
          </div>
        </div>
      )}

      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            {createStep === "type" ? (
              <>
                <h3>Nova conversa</h3>

                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: 10,
                    marginBottom: 20,
                  }}
                >
                  {!isEmployee && (
                    <>
                      <button
                        className="btn btn-secondary"
                        style={{
                          justifyContent: "flex-start",
                          gap: 12,
                          padding: "14px 16px",
                        }}
                        onClick={() => {
                          setCreateType("GROUP");
                          setCreateStep("form");
                        }}
                      >
                        <Users size={22} color="var(--text-muted)" />

                        <span>
                          <strong>Grupo</strong>
                          <br />
                          <small style={{ color: "var(--text-muted)" }}>
                            Chat com vários membros
                          </small>
                        </span>
                      </button>

                      <button
                        className="btn btn-secondary"
                        style={{
                          justifyContent: "flex-start",
                          gap: 12,
                          padding: "14px 16px",
                        }}
                        onClick={() => {
                          setCreateType("ANNOUNCEMENT");
                          setCreateStep("form");
                        }}
                      >
                        <Megaphone size={22} color="var(--text-muted)" />

                        <span>
                          <strong>Anúncios</strong>
                          <br />
                          <small style={{ color: "var(--text-muted)" }}>
                            Canal de comunicados
                          </small>
                        </span>
                      </button>
                    </>
                  )}

                  <p
                    style={{
                      fontSize: 13,
                      color: "var(--text-muted)",
                      textAlign: "center",
                    }}
                  >
                    Ou clica num utilizador na lista para iniciar uma mensagem
                    privada.
                  </p>
                </div>

                <div style={{ display: "flex", justifyContent: "flex-end" }}>
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={() => setShowCreate(false)}
                  >
                    Cancelar
                  </button>
                </div>
              </>
            ) : (
              <form onSubmit={handleCreateChannel}>
                <h3>
                  {createType === "GROUP"
                    ? "Novo grupo"
                    : "Novo canal de anúncios"}
                </h3>

                <div className="form-group">
                  <label>Nome</label>

                  <input
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    placeholder="Nome do canal..."
                    required
                    autoFocus
                  />
                </div>

                {createType === "ANNOUNCEMENT" && (
                  <div
                    className="form-group"
                    style={{ display: "flex", alignItems: "center", gap: 8 }}
                  >
                    <input
                      type="checkbox"
                      id="allm"
                      checked={allMembers}
                      onChange={(e) => setAllMembers(e.target.checked)}
                      style={{ width: "auto" }}
                    />

                    <label
                      htmlFor="allm"
                      style={{
                        textTransform: "none",
                        letterSpacing: 0,
                        fontSize: 14,
                      }}
                    >
                      Adicionar todos os membros
                    </label>
                  </div>
                )}

                {!allMembers && (
                  <div className="form-group">
                    <label>Membros</label>

                    <div className="member-list">
                      {users.map((u) => (
                        <div
                          key={u.id}
                          className={`member-item${
                            selectedMembers.includes(u.id) ? " selected" : ""
                          }`}
                          onClick={() => toggleMember(u.id)}
                        >
                          <UserAvatar
                            name={u.name}
                            profilePicture={u.profilePicture}
                            size="sm"
                          />

                          <span className="member-item-name">{u.name}</span>

                          <span className="member-item-role">
                            {u.employeeNumber}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="form-actions">
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => setCreateStep("type")}
                  >
                    Voltar
                  </button>

                  <button type="submit" className="btn btn-primary btn-sm">
                    Criar
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {toast && (
        <div className="toast-container">
          <div className="toast">
            <Check size={15} /> {toast}
          </div>
        </div>
      )}
    </div>
  );
}