import { useEffect, useState } from "react";
import { SITE } from "./site";

export interface ServerStatus {
  loading: boolean;
  online: boolean;
  players: number | null;
  max: number | null;
}

// Live Minecraft player count via the free mcstatus.io API (no token).
// Fails gracefully: if the server is down / not resolving yet, online=false
// and the UI shows a "launching soon" fallback instead of a broken 0.
export function useServerStatus(): ServerStatus {
  const [status, setStatus] = useState<ServerStatus>({
    loading: true,
    online: false,
    players: null,
    max: null,
  });

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();

    async function load() {
      try {
        const res = await fetch(
          `https://api.mcstatus.io/v2/status/java/${encodeURIComponent(SITE.serverIp)}`,
          { signal: controller.signal }
        );
        if (!res.ok) throw new Error(`status ${res.status}`);
        const data = await res.json();
        if (cancelled) return;
        setStatus({
          loading: false,
          online: Boolean(data?.online),
          players: data?.players?.online ?? null,
          max: data?.players?.max ?? null,
        });
      } catch {
        if (cancelled) return;
        setStatus({ loading: false, online: false, players: null, max: null });
      }
    }

    load();
    const id = window.setInterval(load, 60_000);
    return () => {
      cancelled = true;
      controller.abort();
      window.clearInterval(id);
    };
  }, []);

  return status;
}
