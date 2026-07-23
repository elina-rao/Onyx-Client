import { motion } from "motion/react";
import { Link } from "react-router-dom";
import { Hero } from "../components/Hero";
import { Stats } from "../components/Stats";
import { JoinCards } from "../components/JoinCards";
import { Trailer } from "../components/Trailer";
import { TIERS } from "../lib/site";

export function Home() {
  return (
    <div>
      <Hero />

      {/* Stats + Join cards */}
      <section className="relative z-10 mx-auto -mt-10 max-w-4xl px-5 pb-8">
        <Stats />
        <div className="mt-10">
          <JoinCards />
        </div>
      </section>

      <Trailer />
      <TierLadder />
      <ClientTeaser />
    </div>
  );
}

function TierLadder() {
  return (
    <section className="mx-auto max-w-5xl px-5 py-24">
      <div className="text-center">
        <h2 className="text-3xl font-extrabold sm:text-5xl">
          Nine tiers. One <span className="text-gradient">summit.</span>
        </h2>
        <p className="mx-auto mt-4 max-w-xl text-muted">
          Every win pushes your rating. Climb from Wood all the way to Onyx and
          take your place at the top of the leaderboard.
        </p>
      </div>

      <div className="mt-12 flex flex-wrap items-end justify-center gap-3">
        {[...TIERS]
          .filter((t) => t.name !== "Unranked")
          .reverse()
          .map((tier, i) => (
            <motion.div
              key={tier.name}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.05, duration: 0.4 }}
              className="flex flex-col items-center gap-2"
            >
              <span
                className="flex h-12 w-12 items-center justify-center rounded-xl border text-lg font-black"
                style={{
                  color: tier.color,
                  borderColor: `${tier.color}55`,
                  background: `${tier.color}14`,
                  boxShadow:
                    tier.name === "Onyx"
                      ? `0 0 24px -4px ${tier.color}aa`
                      : undefined,
                }}
              >
                {tier.name[0]}
              </span>
              <span className="text-xs font-semibold text-muted">{tier.name}</span>
            </motion.div>
          ))}
      </div>

      <div className="mt-10 text-center">
        <Link
          to="/leaderboard"
          className="inline-flex items-center gap-2 rounded-lg border border-line-soft bg-elevated/60 px-5 py-2.5 font-semibold text-ink transition-colors hover:border-brand/60"
        >
          View the Leaderboard →
        </Link>
      </div>
    </section>
  );
}

function ClientTeaser() {
  return (
    <section className="relative overflow-hidden border-y border-line/60 bg-void">
      <div className="absolute -right-24 top-1/2 h-80 w-80 -translate-y-1/2 rounded-full bg-brand/20 blur-[110px]" />
      <div className="mx-auto grid max-w-6xl items-center gap-10 px-5 py-20 md:grid-cols-2">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-bright">
            The Onyx Client
          </p>
          <h2 className="mt-3 text-3xl font-extrabold sm:text-4xl">
            A 1.8.9 client built for ranked.
          </h2>
          <p className="mt-4 max-w-md text-muted">
            Buttery FPS, tuned QoL mods, and a launcher that drops you straight
            into the queue. Free for every Onyx player.
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link
              to="/client"
              className="rounded-lg bg-brand px-5 py-3 font-bold text-white transition-all hover:bg-brand-bright hover:shadow-[0_0_28px_-6px_rgba(176,96,255,0.8)]"
            >
              Download the Client
            </Link>
            <Link
              to="/client#features"
              className="rounded-lg border border-line-soft px-5 py-3 font-semibold text-ink transition-colors hover:border-brand/60"
            >
              See features
            </Link>
          </div>
        </div>

        <div className="relative">
          <div className="rounded-2xl border border-line-soft bg-gradient-to-b from-elevated to-panel p-2 shadow-2xl">
            <div className="rounded-xl bg-deep p-5">
              <div className="flex items-center gap-1.5">
                <span className="h-3 w-3 rounded-full bg-bad/70" />
                <span className="h-3 w-3 rounded-full bg-[#e8b23a]/70" />
                <span className="h-3 w-3 rounded-full bg-good/70" />
              </div>
              <div className="mt-5 space-y-3">
                <div className="h-8 w-2/3 rounded bg-brand/25" />
                <div className="h-4 w-full rounded bg-elevated" />
                <div className="h-4 w-5/6 rounded bg-elevated" />
                <div className="mt-4 h-11 w-40 rounded-lg bg-gradient-to-r from-brand to-brand-bright" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
