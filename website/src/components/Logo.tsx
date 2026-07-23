interface LogoProps {
  size?: number;
  withWordmark?: boolean;
}

// Onyx mark: derived from OnyxLauncher/assets/icons/icon.svg — a purple
// rounded-square "O" on near-black, refined with a subtle gradient stroke.
export function Logo({ size = 34, withWordmark = true }: LogoProps) {
  return (
    <span className="inline-flex items-center gap-2.5 select-none">
      <svg
        width={size}
        height={size}
        viewBox="0 0 256 256"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id="onyxStroke" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#c490ff" />
            <stop offset="1" stopColor="#7b2fbe" />
          </linearGradient>
        </defs>
        <rect width="256" height="256" rx="56" fill="#0d0d0d" />
        <rect
          x="10"
          y="10"
          width="236"
          height="236"
          rx="48"
          fill="none"
          stroke="url(#onyxStroke)"
          strokeWidth="10"
        />
        <text
          x="128"
          y="176"
          textAnchor="middle"
          fontFamily="Outfit, Arial Black, sans-serif"
          fontSize="150"
          fontWeight="900"
          fill="url(#onyxStroke)"
        >
          O
        </text>
      </svg>
      {withWordmark && (
        <span className="font-display text-[1.15rem] font-extrabold tracking-tight">
          ONYX
          <span className="ml-1.5 text-[0.62rem] font-semibold uppercase tracking-[0.22em] text-muted align-middle">
            RBW
          </span>
        </span>
      )}
    </span>
  );
}
