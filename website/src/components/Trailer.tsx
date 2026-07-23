import { SITE } from "../lib/site";

/**
 * Trailer / media strip. Only renders when VITE_TRAILER_URL is set
 * (YouTube watch/share/embed URL or a direct video file).
 */
export function Trailer() {
  const raw = SITE.trailerUrl.trim();
  if (!raw) return null;

  const embed = toEmbedSrc(raw);

  return (
    <section className="relative mx-auto max-w-4xl px-5 py-16">
      <div className="text-center">
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-bright">
          Watch
        </p>
        <h2 className="mt-2 font-display text-2xl font-extrabold sm:text-3xl">
          {SITE.name} Ranked — Official Trailer
        </h2>
      </div>

      <div className="relative mt-8 overflow-hidden rounded-2xl border border-line-soft bg-panel shadow-2xl">
        <div className="relative aspect-video w-full bg-void">
          {embed ? (
            <iframe
              title={`${SITE.name} trailer`}
              src={embed}
              className="absolute inset-0 h-full w-full"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
              loading="lazy"
              referrerPolicy="strict-origin-when-cross-origin"
            />
          ) : (
            <video
              className="absolute inset-0 h-full w-full object-cover"
              src={raw}
              controls
              playsInline
              preload="metadata"
            />
          )}
        </div>
      </div>
    </section>
  );
}

function toEmbedSrc(url: string): string | null {
  try {
    const u = new URL(url);
    const host = u.hostname.replace(/^www\./, "");

    if (host === "youtu.be") {
      const id = u.pathname.split("/").filter(Boolean)[0];
      return id ? `https://www.youtube.com/embed/${id}` : null;
    }

    if (host === "youtube.com" || host === "m.youtube.com") {
      if (u.pathname.startsWith("/embed/")) return url;
      const id = u.searchParams.get("v");
      if (id) return `https://www.youtube.com/embed/${id}`;
      const shorts = u.pathname.match(/^\/shorts\/([^/]+)/);
      if (shorts?.[1]) return `https://www.youtube.com/embed/${shorts[1]}`;
    }

    return null;
  } catch {
    return null;
  }
}
