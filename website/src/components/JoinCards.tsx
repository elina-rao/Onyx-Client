import { useState } from "react";
import { HowToJoinModal } from "./HowToJoinModal";
import { useServerStatus } from "../lib/useServerStatus";
import { formatCount, useDiscordStatus } from "../lib/useDiscordStatus";
import { SITE } from "../lib/site";

export function JoinCards() {
  const [modalOpen, setModalOpen] = useState(false);
  const status = useServerStatus();
  const discord = useDiscordStatus();

  const playingLabel = status.loading
    ? "Checking…"
    : status.online
      ? `${status.players ?? 0} playing`
      : "Launching soon";

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2">
        {/* Minecraft server card */}
        <button
          onClick={() => setModalOpen(true)}
          className="group flex flex-col rounded-2xl border border-line-soft bg-card/70 p-5 text-left transition-all hover:border-brand/60 hover:bg-card"
        >
          <div className="flex w-full items-center justify-center gap-2.5 rounded-xl bg-gradient-to-b from-brand-bright to-brand py-3.5 font-bold text-white shadow-[0_0_26px_-6px_rgba(176,96,255,0.7)]">
            <SwordIcon />
            Join the Minecraft Server
          </div>
          <div className="mt-4 space-y-1.5 px-1">
            <p className="text-sm text-muted">
              IP: <span className="font-semibold text-ink">{SITE.serverIp}</span>
            </p>
            <p className="flex items-center gap-2 text-sm text-muted">
              <span
                className={`h-2 w-2 rounded-full ${
                  status.online ? "bg-good" : "bg-faint"
                }`}
              />
              {playingLabel}
            </p>
          </div>
        </button>

        {/* Discord card */}
        <a
          href={SITE.discordInvite}
          target="_blank"
          rel="noreferrer"
          className="group flex flex-col rounded-2xl border border-line-soft bg-card/70 p-5 transition-all hover:border-[#5865F2]/70 hover:bg-card"
        >
          <div className="flex w-full items-center justify-center gap-2.5 rounded-xl bg-[#5865F2] py-3.5 font-bold text-white transition-colors group-hover:bg-[#6b78ff]">
            <DiscordIcon />
            Join the Discord
          </div>
          <div className="mt-4 space-y-1.5 px-1">
            <p className="text-sm text-muted">
              {SITE.discordInvite.replace("https://", "")}
            </p>
            <DiscordMeta discord={discord} />
          </div>
        </a>
      </div>

      <HowToJoinModal open={modalOpen} onClose={() => setModalOpen(false)} />
    </>
  );
}

function DiscordMeta({
  discord,
}: {
  discord: ReturnType<typeof useDiscordStatus>;
}) {
  if (discord.loading) {
    return <p className="text-sm text-muted">Checking community…</p>;
  }

  if (discord.online != null && discord.members != null) {
    return (
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted">
        <span className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-good" />
          {formatCount(discord.online)} online
        </span>
        <span className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-faint" />
          {formatCount(discord.members)} members
        </span>
      </div>
    );
  }

  return (
    <p className="text-sm text-muted">Register, queue &amp; find teammates</p>
  );
}

function SwordIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M14.5 3.5L20 9l-2 2-5.5-5.5 2-2zM4 20l5-1 8-8-4-4-8 8-1 5z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function DiscordIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M19.3 5.3A16.7 16.7 0 0 0 15.1 4l-.2.4a12 12 0 0 1 3.7 1.9 15.7 15.7 0 0 0-13.2 0A12 12 0 0 1 9.1 4.4L8.9 4a16.7 16.7 0 0 0-4.2 1.3C2 9.4 1.3 13.4 1.6 17.3A16.8 16.8 0 0 0 6.7 20l.4-1.1a10 10 0 0 1-2.1-1l.5-.4a11.9 11.9 0 0 0 10.2 0l.5.4a10 10 0 0 1-2.1 1L14.7 20a16.8 16.8 0 0 0 5.1-2.7c.4-4.6-.6-8.6-2.5-12zM8.9 15c-.9 0-1.6-.9-1.6-1.9s.7-1.9 1.6-1.9 1.6.9 1.6 1.9-.7 1.9-1.6 1.9zm6.2 0c-.9 0-1.6-.9-1.6-1.9s.7-1.9 1.6-1.9 1.6.9 1.6 1.9-.7 1.9-1.6 1.9z" />
    </svg>
  );
}
