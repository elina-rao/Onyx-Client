import { useState } from "react";
import { motion, useReducedMotion } from "motion/react";
import { Link } from "react-router-dom";
import { SITE, TIERS } from "../lib/site";
import { HowToJoinModal } from "./HowToJoinModal";

// Ranked ladder, high -> low (Onyx first). Unranked never appears in the hero.
const LADDER = TIERS.filter((t) => t.name !== "Unranked");

const EASE = [0.22, 1, 0.36, 1] as const;

function rangeLabel(min: number, max: number | null) {
  return max == null ? `${min}+` : `${min}–${max}`;
}

/**
 * Home hero — split editorial layout (shipped from Take 2) with A's
 * prestige headline, support copy, and season badge.
 */
export function Hero() {
  const reduceMotion = useReducedMotion();
  const [joinOpen, setJoinOpen] = useState(false);

  return (
    <section className="relative overflow-hidden bg-void">
      {/* ---- Backdrop ---- */}
      <div aria-hidden className="pointer-events-none absolute inset-0">
        {/* Blurred Bedwars map — darkened so UI stays primary */}
        <img
          src="/hero-map.png"
          alt=""
          className="absolute inset-0 h-full w-full scale-110 object-cover opacity-45 blur-[2px]"
        />
        <div className="absolute inset-0 bg-void/75" />
        <div className="absolute inset-0 bg-gradient-to-b from-void/40 via-transparent to-deep" />
        {/* Drifting grid, faded toward the edges so it reads as texture, not wallpaper */}
        <div
          className="absolute -inset-14 bg-grid animate-drift opacity-70"
          style={{
            maskImage:
              "radial-gradient(120% 90% at 35% 30%, black 0%, transparent 72%)",
            WebkitMaskImage:
              "radial-gradient(120% 90% at 35% 30%, black 0%, transparent 72%)",
          }}
        />
        {/* Soft top radial glow for premium atmosphere */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_55%_at_50%_0%,rgba(123,47,190,0.35),transparent_70%)]" />
        {/* Ambient brand glow behind the ladder panel */}
        <motion.div
          className="absolute right-[-8rem] top-1/2 h-[34rem] w-[34rem] -translate-y-1/2 rounded-full bg-brand/25 blur-[130px]"
          animate={reduceMotion ? undefined : { opacity: [0.55, 0.9, 0.55] }}
          transition={{ duration: 7, repeat: Infinity, ease: "easeInOut" }}
        />
        {/* Cool counter-glow bottom-left so the frame doesn't feel one-sided */}
        <div className="absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-brand/10 blur-[120px]" />
        {/* Giant stroked wordmark, editorial watermark */}
        <div
          className="absolute -right-8 top-1/2 hidden -translate-y-1/2 select-none font-display text-[15rem] font-black leading-none tracking-tighter lg:block"
          style={{
            color: "transparent",
            WebkitTextStroke: "1.5px rgba(176, 96, 255, 0.10)",
          }}
        >
          ONYX
        </div>
        {/* Fade into the page below (Stats row overlaps the hero bottom) */}
        <div className="absolute inset-x-0 bottom-0 h-32 bg-gradient-to-b from-transparent to-deep" />
      </div>

      {/* ---- Content ---- */}
      <div className="relative mx-auto grid max-w-6xl items-center gap-14 px-5 pb-28 pt-32 lg:min-h-svh lg:grid-cols-[1.05fr_0.95fr] lg:gap-10 lg:pb-32">
        <div>
          {/* News-style announcement badge */}
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: EASE }}
            className="inline-flex items-center gap-2 rounded-full border border-line-soft bg-elevated/60 px-3 py-1.5 text-xs font-semibold text-muted backdrop-blur"
          >
            <span className="rounded-md bg-brand/25 px-1.5 py-0.5 font-display text-[0.65rem] font-bold uppercase tracking-wider text-brand-glow">
              New
            </span>
            Season 1 live
          </motion.div>

          {/* Headline — A's prestige line, left-aligned for the split layout */}
          <motion.h1
            initial={{ opacity: 0, y: 22 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: EASE, delay: 0.05 }}
            className="mt-6 font-display text-[2.75rem] font-black leading-[0.95] tracking-tight sm:text-6xl xl:text-7xl"
          >
            Minecraft's Premier
            <br />
            <span
              className="text-gradient"
              style={{
                filter:
                  "drop-shadow(0 0 18px rgba(176,96,255,0.55)) drop-shadow(0 0 40px rgba(123,47,190,0.35))",
                WebkitTextStroke: "1px rgba(192,144,255,0.22)",
              }}
            >
              Ranked Bedwars
            </span>
          </motion.h1>

          {/* Support line (from Hero A) */}
          <motion.p
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: EASE, delay: 0.15 }}
            className="mt-6 max-w-md text-lg font-medium text-muted"
          >
            Climb from Wood to Onyx on a competitive 1.8.9 server built for
            speed — powered by the Onyx Client.
          </motion.p>

          {/* CTA group */}
          <motion.div
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: EASE, delay: 0.28 }}
            className="mt-9 flex flex-wrap items-center gap-3"
          >
            <button
              type="button"
              onClick={() => setJoinOpen(true)}
              className="inline-flex items-center gap-3 rounded-xl bg-brand px-5 py-3 font-display text-sm font-bold uppercase tracking-wider text-white shadow-[0_0_0_1px_rgba(176,96,255,0.4)] transition-all hover:bg-brand-bright hover:shadow-[0_0_30px_-6px_rgba(176,96,255,0.8)]"
            >
              Join the Minecraft Server
            </button>
            <a
              href={SITE.discordInvite}
              target="_blank"
              rel="noreferrer"
              className="rounded-xl border border-line-soft bg-elevated/50 px-5 py-3 font-display text-sm font-bold uppercase tracking-wider text-ink transition-colors hover:border-brand/60 hover:bg-elevated"
            >
              Join the Discord ↗
            </a>
            <Link
              to="/client"
              className="px-2 py-3 font-display text-sm font-bold uppercase tracking-wider text-muted transition-colors hover:text-brand-glow"
            >
              Get the Client →
            </Link>
          </motion.div>
          <p className="mt-3 text-sm text-muted">
            IP:{" "}
            <span className="font-semibold tracking-wide text-ink">
              {SITE.serverIp}
            </span>
          </p>
        </div>

        {/* ---- Ladder panel ---- */}
        <motion.div
          initial={{ opacity: 0, x: 36 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.8, ease: EASE, delay: 0.3 }}
          className="relative mx-auto w-full max-w-sm lg:mr-4"
        >
          <motion.div
            animate={reduceMotion ? undefined : { y: [0, -7, 0] }}
            transition={{ duration: 7, repeat: Infinity, ease: "easeInOut" }}
          >
            <LadderPanel reduceMotion={!!reduceMotion} />
          </motion.div>
        </motion.div>
      </div>

      <HowToJoinModal open={joinOpen} onClose={() => setJoinOpen(false)} />
    </section>
  );
}

function LadderPanel({ reduceMotion }: { reduceMotion: boolean }) {
  const rows = LADDER.length;

  return (
    <div className="rounded-2xl border border-line-soft bg-panel/80 p-5 shadow-2xl backdrop-blur">
      {/* Panel header */}
      <div className="flex items-center justify-between border-b border-line/70 pb-3">
        <span className="font-display text-xs font-bold uppercase tracking-[0.22em] text-faint">
          Season ladder
        </span>
        <span className="rounded-full border border-brand/40 bg-brand/15 px-2.5 py-0.5 font-display text-[0.65rem] font-bold uppercase tracking-widest text-brand-glow">
          Live
        </span>
      </div>

      <div className="relative mt-4 flex gap-4">
        {/* Rating beam that climbs Wood -> Onyx */}
        <div className="relative w-1 shrink-0 self-stretch overflow-hidden rounded-full bg-elevated">
          <motion.div
            className="absolute bottom-0 left-0 w-full rounded-full"
            style={{
              background:
                "linear-gradient(to top, #8a5a3b, #7b2fbe 55%, #b060ff)",
            }}
            initial={{ height: reduceMotion ? "100%" : "0%" }}
            animate={{ height: "100%" }}
            transition={{ duration: 1.5, ease: "easeInOut", delay: 0.9 }}
          />
        </div>

        {/* Tier rows, Onyx on top; revealed bottom-up alongside the beam */}
        <ol className="flex-1 space-y-1.5">
          {LADDER.map((tier, i) => {
            const isOnyx = tier.name === "Onyx";
            const delay = 0.55 + (rows - 1 - i) * 0.09;
            return (
              <motion.li
                key={tier.name}
                initial={{ opacity: 0, x: 16 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.5, ease: EASE, delay }}
                className={`flex items-center gap-3 rounded-lg px-3 py-1.5 ${
                  isOnyx
                    ? "border border-brand/50 bg-brand/15 shadow-[0_0_26px_-6px_rgba(176,96,255,0.65)]"
                    : "border border-transparent"
                }`}
              >
                <span className="w-6 font-display text-xs font-bold text-faint">
                  {String(i + 1).padStart(2, "0")}
                </span>
                <span
                  className="h-2.5 w-2.5 rounded-sm"
                  style={{
                    background: tier.color,
                    boxShadow: isOnyx ? `0 0 10px ${tier.color}` : undefined,
                  }}
                />
                <span
                  className={`flex-1 font-display text-sm font-bold uppercase tracking-wide ${
                    isOnyx ? "text-gradient" : "text-ink"
                  }`}
                >
                  {tier.name}
                </span>
                {isOnyx && (
                  <span className="font-display text-[0.6rem] font-bold uppercase tracking-[0.2em] text-brand-glow">
                    Summit
                  </span>
                )}
                <span className="text-xs font-semibold tabular-nums text-muted">
                  {rangeLabel(tier.min, tier.max)}
                </span>
              </motion.li>
            );
          })}
        </ol>
      </div>

      {/* Panel footer */}
      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6, delay: 1.5 }}
        className="mt-4 border-t border-line/70 pt-3 text-center text-xs font-semibold text-faint"
      >
        Every queue is scored. Every season, the climb resets.
      </motion.p>
    </div>
  );
}
