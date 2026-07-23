import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { Logo } from "./Logo";
import { NAV_LINKS, SITE } from "../lib/site";

export function Nav() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 transition-colors duration-300 ${
        scrolled
          ? "border-b border-line/70 bg-deep/80 backdrop-blur-xl"
          : "border-b border-transparent bg-transparent"
      }`}
    >
      <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
        <Link to="/" aria-label="Onyx home">
          <Logo />
        </Link>

        <div className="hidden items-center gap-1 md:flex">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === "/"}
              className={({ isActive }) =>
                `rounded-lg px-3.5 py-2 text-[0.95rem] font-semibold transition-colors ${
                  isActive
                    ? "text-ink"
                    : "text-muted hover:text-ink"
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
          <a
            href={SITE.discordInvite}
            target="_blank"
            rel="noreferrer"
            className="rounded-lg px-3.5 py-2 text-[0.95rem] font-semibold text-muted transition-colors hover:text-ink"
          >
            Discord ↗
          </a>
        </div>

        <div className="hidden md:block">
          <Link
            to="/client"
            className="rounded-lg bg-brand px-4 py-2 text-[0.95rem] font-bold text-white shadow-[0_0_0_1px_rgba(176,96,255,0.4)] transition-all hover:bg-brand-bright hover:shadow-[0_0_24px_-4px_rgba(176,96,255,0.7)]"
          >
            Get the Client
          </Link>
        </div>

        <button
          className="rounded-lg p-2 text-ink md:hidden"
          aria-label="Toggle menu"
          onClick={() => setOpen((v) => !v)}
        >
          <div className="space-y-1.5">
            <span className="block h-0.5 w-6 bg-current" />
            <span className="block h-0.5 w-6 bg-current" />
            <span className="block h-0.5 w-6 bg-current" />
          </div>
        </button>
      </nav>

      {open && (
        <div className="border-t border-line bg-deep/95 px-5 py-3 backdrop-blur-xl md:hidden">
          {NAV_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === "/"}
              onClick={() => setOpen(false)}
              className="block rounded-lg px-3 py-2.5 font-semibold text-muted hover:text-ink"
            >
              {link.label}
            </NavLink>
          ))}
          <a
            href={SITE.discordInvite}
            target="_blank"
            rel="noreferrer"
            onClick={() => setOpen(false)}
            className="block rounded-lg px-3 py-2.5 font-semibold text-muted hover:text-ink"
          >
            Discord ↗
          </a>
          <Link
            to="/client"
            onClick={() => setOpen(false)}
            className="mt-2 block rounded-lg bg-brand px-3 py-2.5 text-center font-bold text-white"
          >
            Get the Client
          </Link>
        </div>
      )}
    </header>
  );
}
