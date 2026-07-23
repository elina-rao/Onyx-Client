import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { Link } from "react-router-dom";
import { SITE } from "../lib/site";

export function HowToJoinModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  useEffect(() => {
    if (!open) setCopied(false);
  }, [open]);

  async function copyIp() {
    try {
      await navigator.clipboard.writeText(SITE.serverIp);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1600);
    } catch {
      /* clipboard blocked — ignore */
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-[100] flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <div
            className="absolute inset-0 bg-black/70 backdrop-blur-sm"
            onClick={onClose}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label="How to join"
            className="relative w-full max-w-md overflow-hidden rounded-2xl border border-line-soft bg-panel shadow-2xl"
            initial={{ opacity: 0, scale: 0.96, y: 12 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.97, y: 8 }}
            transition={{ type: "spring", stiffness: 320, damping: 26 }}
          >
            <div className="flex items-start justify-between border-b border-line/70 px-6 py-5">
              <div>
                <h3 className="text-xl font-extrabold">How to Join</h3>
                <p className="mt-0.5 text-sm text-muted">
                  Four steps. Then you're in queue.
                </p>
              </div>
              <button
                onClick={onClose}
                aria-label="Close"
                className="rounded-lg p-1.5 text-muted transition-colors hover:bg-elevated hover:text-ink"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </button>
            </div>

            <ol className="space-y-4 px-6 py-6">
              <Step n={1}>Launch Minecraft 1.8.9</Step>
              <Step n={2}>
                Join{" "}
                <button
                  type="button"
                  onClick={copyIp}
                  title="Copy IP"
                  className="font-semibold text-brand-bright underline decoration-dotted underline-offset-4 transition-colors hover:text-brand-glow"
                >
                  {copied ? "Copied!" : SITE.serverIp}
                </button>
              </Step>
              <Step n={3}>Click the link in chat to register</Step>
              <Step n={4}>Have fun climbing to Onyx</Step>
            </ol>

            <div className="flex items-center gap-2 border-t border-line/70 px-6 py-4">
              <code className="flex-1 rounded-lg border border-line bg-void px-3 py-2 font-mono text-sm text-ink">
                {SITE.serverIp}
              </code>
              <button
                onClick={copyIp}
                className="rounded-lg bg-brand px-4 py-2 text-sm font-bold text-white transition-colors hover:bg-brand-bright"
              >
                {copied ? "Copied" : "Copy IP"}
              </button>
            </div>

            <div className="border-t border-line/70 bg-elevated/40 px-6 py-5">
              <p className="text-xs font-bold uppercase tracking-[0.16em] text-faint">
                Using the Onyx Client?
              </p>
              <p className="mt-1.5 text-sm text-muted">
                Download our 1.8.9 client built for ranked and launch straight in.{" "}
                <Link
                  to="/client"
                  onClick={onClose}
                  className="font-semibold text-brand-bright hover:underline"
                >
                  Get the Onyx Client ↗
                </Link>
              </p>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function Step({ n, children }: { n: number; children: React.ReactNode }) {
  return (
    <li className="flex gap-3.5">
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand/20 text-sm font-bold text-brand-bright">
        {n}
      </span>
      <span className="pt-0.5 text-[0.97rem] leading-snug text-ink/90">{children}</span>
    </li>
  );
}
