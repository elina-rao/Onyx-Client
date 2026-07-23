import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { type TierName, tierForElo } from "../lib/site";
import { MOCK_LEADERBOARD, type LeaderboardEntry } from "../lib/leaderboard";

type SortKey = "elo" | "wins" | "wlr";

function wlrOf(p: LeaderboardEntry) {
  return p.wins / Math.max(1, p.losses);
}

function sortEntries(list: LeaderboardEntry[], sort: SortKey) {
  return [...list].sort((a, b) => {
    if (sort === "wins") return b.wins - a.wins || b.elo - a.elo;
    if (sort === "wlr") return wlrOf(b) - wlrOf(a) || b.elo - a.elo;
    return b.elo - a.elo || b.wins - a.wins;
  });
}

export function Leaderboard() {
  const [sort, setSort] = useState<SortKey>("elo");
  const [tierFilter, setTierFilter] = useState<TierName | "all">("all");

  const rows = useMemo(() => {
    const filtered =
      tierFilter === "all"
        ? MOCK_LEADERBOARD
        : MOCK_LEADERBOARD.filter((p) => tierForElo(p.elo).name === tierFilter);
    return sortEntries(filtered, sort);
  }, [sort, tierFilter]);

  return (
    <div>
      <section className="relative overflow-hidden pt-32 pb-10">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_50%_50%_at_50%_0%,rgba(123,47,190,0.28),transparent_70%)]" />
        <div className="mx-auto max-w-5xl px-5 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-line-soft bg-elevated/60 px-3.5 py-1.5 text-xs font-semibold text-muted">
            Season 1 · sample data
          </span>
          <h1 className="mt-6 text-5xl leading-[0.95] sm:text-6xl">
            <span className="text-gradient">Leaderboard</span>
          </h1>
          <p className="mx-auto mt-5 max-w-xl text-lg text-muted">
            Season rankings — sort by rating, wins, or win rate. Live API wiring
            comes next; this table is seeded mock data.
          </p>
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-5 pb-20">
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-2" role="group" aria-label="Sort leaderboard">
            {(
              [
                { key: "elo", label: "Rating" },
                { key: "wins", label: "Wins" },
                { key: "wlr", label: "W/L" },
              ] as const
            ).map(({ key, label }) => {
              const active = sort === key;
              return (
                <button
                  key={key}
                  type="button"
                  aria-pressed={active}
                  onClick={() => setSort(key)}
                  className={`rounded-lg px-3 py-1.5 text-sm font-bold uppercase tracking-wide transition-colors ${
                    active
                      ? "bg-brand text-white"
                      : "border border-line-soft text-muted hover:border-brand/50 hover:text-ink"
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>
          <select
            value={tierFilter}
            onChange={(e) => setTierFilter(e.target.value as TierName | "all")}
            className="rounded-lg border border-line-soft bg-elevated/60 px-3 py-2 text-sm font-semibold text-ink"
            aria-label="Filter by tier"
          >
            <option value="all">All tiers</option>
            {(
              [
                "Onyx",
                "Obsidian",
                "Amethyst",
                "Diamond",
                "Gold",
                "Iron",
                "Coal",
                "Wood",
              ] as TierName[]
            ).map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>

        <div className="overflow-hidden rounded-2xl border border-line-soft bg-card/50">
          <div className="grid grid-cols-[3rem_1fr_5.5rem_4.5rem_5rem] gap-2 border-b border-line/70 px-4 py-3 text-xs font-bold uppercase tracking-[0.14em] text-faint sm:grid-cols-[4rem_1fr_6rem_5rem_5.5rem_5rem] sm:px-5">
            <span>#</span>
            <span>Player</span>
            <SortHeader
              label="Rating"
              active={sort === "elo"}
              onClick={() => setSort("elo")}
              align="right"
            />
            <span className="hidden text-right sm:block">Tier</span>
            <SortHeader
              label="W–L"
              active={sort === "wins"}
              onClick={() => setSort("wins")}
              align="right"
            />
            <SortHeader
              label="W/L"
              active={sort === "wlr"}
              onClick={() => setSort("wlr")}
              align="right"
            />
          </div>

          {rows.length === 0 ? (
            <p className="px-5 py-12 text-center text-muted">
              No players in this tier yet.
            </p>
          ) : (
            <ul>
              {rows.map((player, i) => {
                const tier = tierForElo(player.elo);
                const wlr = wlrOf(player);
                return (
                  <motion.li
                    key={`${sort}-${player.name}`}
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: Math.min(i, 10) * 0.015, duration: 0.28 }}
                    className="grid grid-cols-[3rem_1fr_5.5rem_4.5rem_5rem] items-center gap-2 border-b border-line/40 px-4 py-3 last:border-0 sm:grid-cols-[4rem_1fr_6rem_5rem_5.5rem_5rem] sm:px-5"
                  >
                    <span className="font-display text-sm font-bold text-faint">
                      {String(i + 1).padStart(2, "0")}
                    </span>
                    <span className="truncate font-semibold text-ink">
                      {player.name}
                    </span>
                    <span
                      className={`text-right font-display font-bold tabular-nums ${
                        sort === "elo" ? "text-brand-bright" : "text-ink"
                      }`}
                    >
                      {player.elo}
                    </span>
                    <span className="hidden justify-end sm:flex">
                      <TierBadge name={tier.name} color={tier.color} />
                    </span>
                    <span
                      className={`text-right text-sm tabular-nums ${
                        sort === "wins" ? "font-semibold text-brand-bright" : "text-muted"
                      }`}
                    >
                      {player.wins}–{player.losses}
                    </span>
                    <span
                      className={`text-right text-sm tabular-nums ${
                        sort === "wlr" ? "font-bold text-brand-bright" : "font-semibold text-muted"
                      }`}
                    >
                      {wlr.toFixed(2)}
                    </span>
                  </motion.li>
                );
              })}
            </ul>
          )}
        </div>

        <p className="mt-6 text-center text-xs text-faint">
          Sorted by{" "}
          {sort === "elo" ? "rating" : sort === "wins" ? "wins" : "win rate"} ·{" "}
          {MOCK_LEADERBOARD.length} mock players
        </p>

        <div className="mt-8 text-center">
          <Link
            to="/"
            className="inline-flex rounded-lg border border-line-soft px-5 py-2.5 font-semibold text-ink transition-colors hover:border-brand/60"
          >
            ← Back home
          </Link>
        </div>
      </section>
    </div>
  );
}

function SortHeader({
  label,
  active,
  onClick,
  align,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
  align: "left" | "right";
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`${align === "right" ? "text-right" : "text-left"} transition-colors hover:text-ink ${
        active ? "text-brand-bright" : "text-faint"
      }`}
    >
      {label}
      {active ? " ↓" : ""}
    </button>
  );
}

function TierBadge({ name, color }: { name: string; color: string }) {
  return (
    <span
      className="inline-flex items-center rounded-md border px-2 py-0.5 text-[0.65rem] font-bold uppercase tracking-wide"
      style={{
        color,
        borderColor: `${color}55`,
        background: `${color}18`,
      }}
    >
      {name}
    </span>
  );
}
