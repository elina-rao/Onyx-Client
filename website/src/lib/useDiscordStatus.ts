import { useEffect, useState } from "react";
import { SITE } from "./site";

export interface DiscordStatus {
  loading: boolean;
  online: number | null;
  members: number | null;
}

function inviteCodeFromUrl(url: string): string | null {
  try {
    const path = new URL(url).pathname.replace(/\/+$/, "");
    const code = path.split("/").filter(Boolean).pop();
    return code || null;
  } catch {
    return null;
  }
}

function formatCount(n: number): string {
  return n.toLocaleString("en-US");
}

export { formatCount };

// Live Discord presence via the public invite counts endpoint (no token).
// Falls back to env overrides, then to a quiet "Community" label in the UI.
export function useDiscordStatus(): DiscordStatus {
  const [status, setStatus] = useState<DiscordStatus>({
    loading: true,
    online: null,
    members: null,
  });

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();

    const envOnline = Number(import.meta.env.VITE_DISCORD_ONLINE);
    const envMembers = Number(import.meta.env.VITE_DISCORD_MEMBERS);
    const hasEnv =
      Number.isFinite(envOnline) &&
      envOnline >= 0 &&
      Number.isFinite(envMembers) &&
      envMembers >= 0;

    async function load() {
      const code = inviteCodeFromUrl(SITE.discordInvite);
      if (!code) {
        if (cancelled) return;
        setStatus({
          loading: false,
          online: hasEnv ? envOnline : null,
          members: hasEnv ? envMembers : null,
        });
        return;
      }

      try {
        const res = await fetch(
          `https://discord.com/api/v10/invites/${encodeURIComponent(code)}?with_counts=true`,
          { signal: controller.signal }
        );
        if (!res.ok) throw new Error(`status ${res.status}`);
        const data = await res.json();
        if (cancelled) return;
        const online = data?.approximate_presence_count;
        const members = data?.approximate_member_count;
        setStatus({
          loading: false,
          online: typeof online === "number" ? online : hasEnv ? envOnline : null,
          members: typeof members === "number" ? members : hasEnv ? envMembers : null,
        });
      } catch {
        if (cancelled) return;
        setStatus({
          loading: false,
          online: hasEnv ? envOnline : null,
          members: hasEnv ? envMembers : null,
        });
      }
    }

    load();
    const id = window.setInterval(load, 120_000);
    return () => {
      cancelled = true;
      controller.abort();
      window.clearInterval(id);
    };
  }, []);

  return status;
}
