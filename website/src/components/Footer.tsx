import { Link } from "react-router-dom";
import { Logo } from "./Logo";
import { SITE } from "../lib/site";

const SOCIALS = [
  { label: "Discord", href: SITE.discordInvite, icon: DiscordIcon },
  { label: "YouTube", href: SITE.youtubeUrl, icon: YouTubeIcon },
  { label: "TikTok", href: SITE.tiktokUrl, icon: TikTokIcon },
  { label: "Instagram", href: SITE.instagramUrl, icon: InstagramIcon },
].filter((s) => Boolean(s.href.trim()));

export function Footer() {
  return (
    <footer className="relative z-10 border-t border-line/70 bg-void">
      <div className="mx-auto grid max-w-6xl gap-10 px-5 py-14 md:grid-cols-[1.5fr_1fr_1fr_1fr]">
        <div>
          <Logo />
          <p className="mt-4 max-w-xs text-sm leading-relaxed text-muted">
            Competitive Ranked Bedwars on {SITE.serverIp} — plus the Onyx Client,
            a fast 1.8.9 client built for ranked.
          </p>
          {SOCIALS.length > 0 && (
            <div className="mt-5 flex flex-wrap items-center gap-2">
              {SOCIALS.map((s) => (
                <a
                  key={s.label}
                  href={s.href}
                  target="_blank"
                  rel="noreferrer"
                  aria-label={s.label}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-line-soft text-muted transition-colors hover:border-brand/50 hover:text-ink"
                >
                  <s.icon />
                </a>
              ))}
            </div>
          )}
        </div>

        <FooterCol title="Play">
          <FooterLink to="/leaderboard">Leaderboard</FooterLink>
          <FooterLink to="/rules">Rules</FooterLink>
          <FooterLink to="/store">Store</FooterLink>
        </FooterCol>

        <FooterCol title="Client">
          <FooterLink to="/client">Download</FooterLink>
          <FooterLink to="/client#features">Features</FooterLink>
          <FooterLink to="/client#install">Install guide</FooterLink>
        </FooterCol>

        <FooterCol title="Community">
          <FooterExternal href={SITE.discordInvite}>Discord</FooterExternal>
          <FooterLink to="/terms">Terms</FooterLink>
          <FooterLink to="/privacy">Privacy</FooterLink>
        </FooterCol>
      </div>

      <div className="border-t border-line/50">
        <div className="mx-auto flex max-w-6xl flex-col gap-2 px-5 py-5 text-xs text-faint md:flex-row md:items-center md:justify-between">
          <span>© {new Date().getFullYear()} Onyx RBW. All rights reserved.</span>
          <span>Not affiliated with Mojang or Microsoft.</span>
        </div>
      </div>
    </footer>
  );
}

function FooterCol({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h4 className="mb-3 text-xs font-bold uppercase tracking-[0.18em] text-faint">
        {title}
      </h4>
      <ul className="space-y-2 text-sm">{children}</ul>
    </div>
  );
}

function FooterLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <li>
      <Link to={to} className="text-muted transition-colors hover:text-ink">
        {children}
      </Link>
    </li>
  );
}

function FooterExternal({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <li>
      <a
        href={href}
        target="_blank"
        rel="noreferrer"
        className="text-muted transition-colors hover:text-ink"
      >
        {children} ↗
      </a>
    </li>
  );
}

function DiscordIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M19.3 5.3A16.7 16.7 0 0 0 15.1 4l-.2.4a12 12 0 0 1 3.7 1.9 15.7 15.7 0 0 0-13.2 0A12 12 0 0 1 9.1 4.4L8.9 4a16.7 16.7 0 0 0-4.2 1.3C2 9.4 1.3 13.4 1.6 17.3A16.8 16.8 0 0 0 6.7 20l.4-1.1a10 10 0 0 1-2.1-1l.5-.4a11.9 11.9 0 0 0 10.2 0l.5.4a10 10 0 0 1-2.1 1L14.7 20a16.8 16.8 0 0 0 5.1-2.7c.4-4.6-.6-8.6-2.5-12zM8.9 15c-.9 0-1.6-.9-1.6-1.9s.7-1.9 1.6-1.9 1.6.9 1.6 1.9-.7 1.9-1.6 1.9zm6.2 0c-.9 0-1.6-.9-1.6-1.9s.7-1.9 1.6-1.9 1.6.9 1.6 1.9-.7 1.9-1.6 1.9z" />
    </svg>
  );
}

function YouTubeIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.5 12 3.5 12 3.5s-7.5 0-9.4.6A3 3 0 0 0 .5 6.2C0 8.1 0 12 0 12s0 3.9.5 5.8a3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1c.5-1.9.5-5.8.5-5.8s0-3.9-.5-5.8zM9.5 15.6V8.4L15.8 12l-6.3 3.6z" />
    </svg>
  );
}

function TikTokIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12.5.02c1.3 0 2.6 0 3.9-.02.1 1.53.63 3.09 1.75 4.17A5.9 5.9 0 0 0 22.4 6v4.03a9.3 9.3 0 0 1-4.2-.97 8.4 8.4 0 0 1-1.62-.93v8.75a6.3 6.3 0 0 1-1.35 3.94 6.2 6.2 0 0 1-5.91 3.21 6.1 6.1 0 0 1-4.08-1.03A6.3 6.3 0 0 1 2.6 17.3c0-.5 0-1 0-1.49.18-1.9 1.12-3.72 2.58-4.96a6.2 6.2 0 0 1 6.15-1.72v4.44a3.4 3.4 0 0 0-3.02.37 3.1 3.1 0 0 0-1.36 1.75c-.21.51-.15 1.07-.14 1.61.24 1.64 1.82 3.02 3.5 2.87a3.1 3.1 0 0 0 2.77-1.61c.2-.33.4-.67.41-1.06.1-1.79.06-3.57.07-5.36.01-4.03 0-8.05.02-12.07z" />
    </svg>
  );
}

function InstagramIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M7.8 2h8.4C19.4 2 22 4.6 22 7.8v8.4a5.8 5.8 0 0 1-5.8 5.8H7.8C4.6 22 2 19.4 2 16.2V7.8A5.8 5.8 0 0 1 7.8 2m-.2 2A3.6 3.6 0 0 0 4 7.6v8.8A3.6 3.6 0 0 0 7.6 20h8.8a3.6 3.6 0 0 0 3.6-3.6V7.6A3.6 3.6 0 0 0 16.4 4H7.6m9.65 1.5a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5M12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10m0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6z" />
    </svg>
  );
}
