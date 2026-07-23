import { Link } from "react-router-dom";
import { SITE } from "../lib/site";

const SECTIONS = [
  {
    id: "conduct",
    title: "Conduct & chat",
    rules: [
      "Treat every player with basic respect. Harassment, hate speech, and targeted abuse are bans.",
      "No spam, scam links, or advertising other servers in chat or DMs.",
      "English is preferred in public chat so staff can moderate — other languages are fine in private parties.",
      "Do not impersonate staff, streamers, or other players.",
    ],
  },
  {
    id: "gameplay",
    title: "Gameplay",
    rules: [
      "No intentional teaming across teams in ranked queues. Party with teammates only through the intended party system.",
      "No stalling or refusing to play after a clear win condition solely to waste the opponent's time.",
      "Bed defense and island play are allowed; griefing spawn or lobby areas is not.",
      "Do not abuse map bugs, item glitches, or unintended mechanics. Report them in Discord instead.",
      "Rage-quitting mid-game may count as a loss and can be punished if repeated.",
    ],
  },
  {
    id: "clients",
    title: "Clients & mods",
    rules: [
      `The official Onyx Client is allowed and recommended for ranked on ${SITE.serverIp}.`,
      "Performance and QoL mods that do not give unfair PvP advantages are generally fine (FPS, HUD, keystrokes, etc.).",
      "Cheats, killaura, reach, velocity, scaffold, auto-block, freecam combat assists, and similar are permanent-ban territory.",
      "Macros that automate combat or bridging for advantage are not allowed.",
      "If you are unsure whether a mod is allowed, ask in Discord before queueing.",
    ],
  },
  {
    id: "accounts",
    title: "Accounts & nicknames",
    rules: [
      "One person, one primary account for ranked. Account sharing to evade bans is a ban on all involved accounts.",
      "Nicknames and skins must not be offensive, misleading, or impersonating staff.",
      "Boosting (win-trading, smurfing to farm lower ranks unfairly) can reset rating and lead to suspensions.",
    ],
  },
  {
    id: "punishments",
    title: "Punishments & appeals",
    rules: [
      "Staff may mute, kick, temp-ban, or permanently ban based on severity and history.",
      "Cheating and severe harassment are typically permanent.",
      `Appeals go through the Onyx Discord only (${SITE.discordInvite.replace("https://", "")}) — do not spam staff DMs.`,
      "False reports and ban evasion make punishments worse, not better.",
    ],
  },
] as const;

export function Rules() {
  return (
    <div>
      <section className="relative overflow-hidden pt-32 pb-12">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_50%_50%_at_50%_0%,rgba(123,47,190,0.28),transparent_70%)]" />
        <div className="mx-auto max-w-3xl px-5 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-line-soft bg-elevated/60 px-3.5 py-1.5 text-xs font-semibold text-muted">
            Season 1 rulebook
          </span>
          <h1 className="mt-6 text-5xl leading-[0.95] sm:text-6xl">
            Server <span className="text-gradient">Rules</span>
          </h1>
          <p className="mx-auto mt-5 max-w-xl text-lg text-muted">
            Play fair, climb clean, and keep ranked enjoyable for everyone.
            Breaking these can cost your rating — or your account.
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-3xl px-5 pb-8">
        <nav className="flex flex-wrap gap-2 border-b border-line/60 pb-6">
          {SECTIONS.map((s) => (
            <a
              key={s.id}
              href={`#${s.id}`}
              className="rounded-lg border border-line-soft bg-elevated/40 px-3 py-1.5 text-sm font-semibold text-muted transition-colors hover:border-brand/50 hover:text-ink"
            >
              {s.title}
            </a>
          ))}
        </nav>
      </div>

      <div className="mx-auto max-w-3xl space-y-14 px-5 pb-20">
        {SECTIONS.map((section) => (
          <section key={section.id} id={section.id} className="scroll-mt-28">
            <h2 className="font-display text-2xl font-extrabold sm:text-3xl">
              {section.title}
            </h2>
            <ol className="mt-6 space-y-4">
              {section.rules.map((rule, i) => (
                <li key={i} className="flex gap-3.5">
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand/20 text-xs font-bold text-brand-bright">
                    {i + 1}
                  </span>
                  <p className="pt-0.5 text-[0.97rem] leading-relaxed text-ink/90">
                    {rule}
                  </p>
                </li>
              ))}
            </ol>
          </section>
        ))}

        <div className="rounded-2xl border border-line-soft bg-card/60 p-6 text-center">
          <p className="text-muted">
            Questions about a ruling? Staff answers live in Discord.
          </p>
          <div className="mt-5 flex flex-wrap justify-center gap-3">
            <a
              href={SITE.discordInvite}
              target="_blank"
              rel="noreferrer"
              className="rounded-lg bg-brand px-5 py-2.5 font-bold text-white transition-colors hover:bg-brand-bright"
            >
              Join the Discord
            </a>
            <Link
              to="/"
              className="rounded-lg border border-line-soft px-5 py-2.5 font-semibold text-ink transition-colors hover:border-brand/60"
            >
              ← Back home
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
