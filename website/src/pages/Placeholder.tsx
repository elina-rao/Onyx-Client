import { Link } from "react-router-dom";
import { SITE } from "../lib/site";

// Intentional "coming in pass 2" screen — looks designed, not broken.
export function Placeholder({ title, blurb }: { title: string; blurb: string }) {
  return (
    <section className="relative flex min-h-[70vh] items-center justify-center overflow-hidden px-5">
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_50%_50%_at_50%_0%,rgba(123,47,190,0.2),transparent_70%)]" />
      <div className="max-w-lg pt-24 text-center">
        <span className="inline-flex items-center gap-2 rounded-full border border-line-soft bg-elevated/60 px-3.5 py-1.5 text-xs font-semibold text-muted">
          Coming soon
        </span>
        <h1 className="mt-6 text-5xl">{title}</h1>
        <p className="mx-auto mt-4 max-w-md text-muted">{blurb}</p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Link
            to="/"
            className="rounded-lg border border-line-soft px-5 py-2.5 font-semibold text-ink transition-colors hover:border-brand/60"
          >
            ← Back home
          </Link>
          <a
            href={SITE.discordInvite}
            target="_blank"
            rel="noreferrer"
            className="rounded-lg bg-brand px-5 py-2.5 font-bold text-white transition-colors hover:bg-brand-bright"
          >
            Join the Discord
          </a>
        </div>
      </div>
    </section>
  );
}
