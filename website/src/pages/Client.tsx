import { motion } from "motion/react";
import { DOWNLOADS, SITE } from "../lib/site";

const FEATURES = [
  {
    title: "Built for 1.8.9",
    body: "Forge-based client tuned for classic Bedwars PvP — the version ranked is played on.",
  },
  {
    title: "High, stable FPS",
    body: "Performance-first rendering so your frames stay smooth mid-fight, even on modest hardware.",
  },
  {
    title: "Ranked-ready QoL",
    body: "Clean HUD, keystrokes, FPS/ping display, and the mods that matter — nothing you don't need.",
  },
  {
    title: "One-click launch",
    body: "Sign in, hit play, and drop straight onto " + SITE.serverIp + ". The launcher handles the rest.",
  },
  {
    title: "Auto-updates",
    body: "The launcher keeps your client and mods current so you're always on the latest ranked build.",
  },
  {
    title: "Hypixel-safe by design",
    body: "Focused on performance and quality-of-life — no unfair advantages, no bans.",
  },
];

export function Client() {
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden pt-32 pb-16">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_50%_50%_at_50%_0%,rgba(123,47,190,0.28),transparent_70%)]" />
        <div className="mx-auto max-w-4xl px-5 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-line-soft bg-elevated/60 px-3.5 py-1.5 text-xs font-semibold text-muted">
            Onyx Client · v{SITE.launcherVersion}
          </span>
          <h1 className="mt-6 text-5xl leading-[0.95] sm:text-7xl">
            The <span className="text-gradient">Onyx Client</span>
          </h1>
          <p className="mx-auto mt-5 max-w-xl text-lg text-muted">
            A fast, clean 1.8.9 client for Ranked Bedwars. Download the launcher,
            sign in, and climb.
          </p>
          <div className="mt-9">
            <DownloadButtons />
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-6xl scroll-mt-24 px-5 py-16">
        <h2 className="text-center text-3xl font-extrabold sm:text-4xl">
          Everything you need. Nothing you don't.
        </h2>
        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f, i) => (
            <motion.div
              key={f.title}
              initial={{ opacity: 0, y: 18 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: (i % 3) * 0.06, duration: 0.45 }}
              className="rounded-2xl border border-line-soft bg-card/60 p-6 transition-colors hover:border-brand/50"
            >
              <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-brand/20 text-brand-bright">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M5 13l4 4L19 7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </div>
              <h3 className="text-lg font-bold">{f.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">{f.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Install */}
      <section id="install" className="scroll-mt-24 border-y border-line/60 bg-void">
        <div className="mx-auto max-w-4xl px-5 py-16">
          <h2 className="text-center text-3xl font-extrabold sm:text-4xl">
            Installing the launcher
          </h2>
          <p className="mx-auto mt-4 max-w-2xl text-center text-sm text-muted">
            The Onyx Launcher isn't code-signed yet, so your OS will show a
            one-time warning. Here's how to open it safely — it takes about ten
            seconds.
          </p>

          <div className="mt-10 grid gap-5 md:grid-cols-2">
            <InstallCard os="macOS">
              <Step n={1}>Open the downloaded <code>.dmg</code> and drag Onyx Launcher to Applications.</Step>
              <Step n={2}>Double-click the app. macOS will say it can't verify the developer — click <strong>Done</strong>.</Step>
              <Step n={3}>Go to <strong>System Settings → Privacy &amp; Security</strong>, scroll down, and click <strong>Open Anyway</strong>.</Step>
              <Step n={4}>Still blocked? Run in Terminal:
                <code className="mt-1.5 block rounded bg-deep px-2.5 py-1.5 text-xs text-ink">xattr -c "/Applications/Onyx Launcher.app"</code>
              </Step>
            </InstallCard>

            <InstallCard os="Windows">
              <Step n={1}>Run the downloaded <code>.exe</code> installer.</Step>
              <Step n={2}>If SmartScreen shows "Windows protected your PC", click <strong>More info</strong>.</Step>
              <Step n={3}>Click <strong>Run anyway</strong> to continue.</Step>
              <Step n={4}>Finish the installer and launch Onyx from your Start menu.</Step>
            </InstallCard>
          </div>

          <p className="mt-8 text-center text-xs text-faint">
            Why the warning? We're an independent project and not code-signed yet.
            The launcher is safe — signing is on the roadmap.
          </p>
        </div>
      </section>

      {/* Bottom CTA */}
      <section className="mx-auto max-w-4xl px-5 py-16 text-center">
        <h2 className="text-3xl font-extrabold sm:text-4xl">Ready to climb?</h2>
        <p className="mx-auto mt-3 max-w-md text-muted">
          Download the client, then join {SITE.serverIp} and start your run to Onyx.
        </p>
        <div className="mt-8">
          <DownloadButtons />
        </div>
      </section>
    </div>
  );
}

function DownloadButtons() {
  const preferred = detectOs();

  return (
    <div className="flex flex-col items-center gap-3">
      {preferred && (
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-faint">
          Detected {preferred === "mac" ? "macOS" : "Windows"} — highlighted below
        </p>
      )}
      <div className="flex flex-wrap justify-center gap-3">
        <DownloadButton
          os="macOS"
          href={DOWNLOADS.mac}
          icon={<AppleIcon />}
          preferred={preferred === "mac"}
        />
        <DownloadButton
          os="Windows"
          href={DOWNLOADS.windows}
          icon={<WindowsIcon />}
          preferred={preferred === "win"}
        />
      </div>
    </div>
  );
}

function detectOs(): "mac" | "win" | null {
  if (typeof navigator === "undefined") return null;
  const ua = navigator.userAgent.toLowerCase();
  const platform = (navigator.platform || "").toLowerCase();
  if (platform.includes("mac") || ua.includes("mac")) return "mac";
  if (platform.includes("win") || ua.includes("windows")) return "win";
  return null;
}

function DownloadButton({
  os,
  href,
  icon,
  preferred,
}: {
  os: string;
  href: string;
  icon: React.ReactNode;
  preferred: boolean;
}) {
  const ready = href.length > 0;
  if (!ready) {
    return (
      <span
        className={`inline-flex cursor-not-allowed items-center gap-2.5 rounded-xl border bg-elevated/40 px-6 py-3.5 font-bold text-faint ${
          preferred ? "border-brand/40 ring-1 ring-brand/30" : "border-line-soft"
        }`}
        title="Build coming soon"
      >
        {icon}
        <span className="flex flex-col items-start leading-tight">
          <span className="text-xs font-medium uppercase tracking-wide text-faint/70">
            Download for
          </span>
          {os}
          <span className="text-[0.65rem] font-medium text-faint/70">building…</span>
        </span>
      </span>
    );
  }
  return (
    <a
      href={href}
      className={`inline-flex items-center gap-2.5 rounded-xl px-6 py-3.5 font-bold text-white transition-all ${
        preferred
          ? "bg-brand shadow-[0_0_28px_-6px_rgba(176,96,255,0.8)] hover:bg-brand-bright"
          : "border border-line-soft bg-elevated/80 hover:border-brand/60 hover:bg-brand"
      }`}
    >
      {icon}
      <span className="flex flex-col items-start leading-tight">
        <span className="text-xs font-medium uppercase tracking-wide text-white/70">
          {preferred ? "Recommended for" : "Download for"}
        </span>
        {os}
      </span>
    </a>
  );
}

function InstallCard({ os, children }: { os: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-line-soft bg-card/60 p-6">
      <h3 className="mb-4 flex items-center gap-2 text-lg font-bold">
        {os === "macOS" ? <AppleIcon /> : <WindowsIcon />}
        {os}
      </h3>
      <ol className="space-y-3.5">{children}</ol>
    </div>
  );
}

function Step({ n, children }: { n: number; children: React.ReactNode }) {
  return (
    <li className="flex gap-3">
      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand/20 text-xs font-bold text-brand-bright">
        {n}
      </span>
      <span className="pt-0.5 text-sm leading-snug text-ink/90">{children}</span>
    </li>
  );
}

function AppleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M16.4 12.7c0-2.3 1.9-3.4 2-3.5-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.1-2.8.9-3.5.9s-1.8-.8-3-.8c-1.5 0-2.9.9-3.7 2.3-1.6 2.7-.4 6.8 1.1 9 .7 1.1 1.6 2.3 2.8 2.3 1.1 0 1.5-.7 2.9-.7s1.7.7 2.9.7 2-1.1 2.7-2.1c.8-1.2 1.2-2.4 1.2-2.4s-2.3-.9-2.3-3.6zM14.2 5.9c.6-.8 1-1.8.9-2.9-.9 0-2 .6-2.6 1.4-.6.7-1.1 1.7-.9 2.7 1 .1 2-.5 2.6-1.2z" />
    </svg>
  );
}

function WindowsIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M3 5.5l7-1v7H3v-6zm0 13l7 1v-7H3v6zM11 4.3L21 3v9h-10V4.3zM11 13h10v8l-10-1.4V13z" />
    </svg>
  );
}
