import { useEffect, useRef, useState } from "react";
import { motion, useInView, useReducedMotion } from "motion/react";

const STATS = [
  { target: 10_000, prefix: "", suffix: "+", label: "Players", accent: false },
  { target: 250_000, prefix: "", suffix: "+", label: "Games Played", accent: false },
  { target: 5_000, prefix: "$", suffix: "+", label: "Prizes Awarded", accent: true },
];

export function Stats() {
  return (
    <div className="grid grid-cols-3 gap-4 sm:gap-10">
      {STATS.map((s, i) => (
        <StatItem key={s.label} {...s} delay={i * 0.08} />
      ))}
    </div>
  );
}

function StatItem({
  target,
  prefix,
  suffix,
  label,
  accent,
  delay,
}: {
  target: number;
  prefix: string;
  suffix: string;
  label: string;
  accent: boolean;
  delay: number;
}) {
  const reduceMotion = useReducedMotion();
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, amount: 0.6 });
  const value = useCountUp(inView ? target : 0, Boolean(reduceMotion) || !inView);

  return (
    <motion.div
      ref={ref}
      className="text-center"
      initial={{ opacity: 0, y: 16 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay, duration: 0.5 }}
    >
      <div
        className={`font-display text-2xl font-extrabold tabular-nums sm:text-4xl ${
          accent ? "text-brand-bright" : ""
        }`}
      >
        {prefix}
        {value.toLocaleString("en-US")}
        <span className={accent ? "text-brand-glow" : "text-brand-bright"}>
          {suffix}
        </span>
      </div>
      <div className="mt-1 text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-faint sm:text-xs">
        {label}
      </div>
    </motion.div>
  );
}

function useCountUp(target: number, skip: boolean) {
  const [value, setValue] = useState(skip ? target : 0);

  useEffect(() => {
    if (skip) {
      setValue(target);
      return;
    }
    if (target === 0) {
      setValue(0);
      return;
    }

    let raf = 0;
    const duration = 1100;
    const start = performance.now();

    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      setValue(Math.round(target * eased));
      if (t < 1) raf = requestAnimationFrame(tick);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target, skip]);

  return value;
}
