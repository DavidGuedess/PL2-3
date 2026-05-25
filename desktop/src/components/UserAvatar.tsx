const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:3001";

interface Props {
  name: string;
  profilePicture?: string | null;
  size?: "sm" | "md" | "lg";
  className?: string;
}

export function UserAvatar({ name, profilePicture, size = "md", className }: Props) {
  const initial = name.split(" ").filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join("") || "?";
  const src = profilePicture
    ? (profilePicture.startsWith("http") ? profilePicture : `${API_BASE}${profilePicture}`)
    : null;

  const sizeClass = size === "sm" ? "sm" : size === "lg" ? "lg" : "";
  const px = size === "sm" ? 28 : size === "lg" ? 56 : 36;

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={className}
        style={{ width: px, height: px, borderRadius: "50%", objectFit: "cover", flexShrink: 0, border: "1.5px solid var(--border)" }}
      />
    );
  }

  return <div className={`user-avatar${sizeClass ? ` ${sizeClass}` : ""}${className ? ` ${className}` : ""}`}>{initial}</div>;
}
