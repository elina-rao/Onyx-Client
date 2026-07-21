# Premium HUD Baseline Audit

## Objective
Establish measurable visual and interaction baselines before premium HUD rollout.

## Acceptance Criteria
- No text clipping at `HUD Scale` 0.5, 1.0, 1.5, 2.0.
- Drag hitbox fully matches visible module bounds.
- Card padding/radius/border are consistent across migrated modules.
- No visible shimmer/jitter while frequently updating values (ELO, timers, resource counts).
- Legacy fallback remains available per migrated module.

## Initial Findings
- `HudModule` provided scale and text shadow settings, but modules applied scale inconsistently.
- Most gameplay modules used vanilla `fontRendererObj` directly.
- HUD editor relied on module-reported width/height, which drifted when modules applied custom scale math.

## Screenshot Matrix (to capture per release candidate)
- Resolution: 1280x720, 1920x1080, 2560x1440
- GUI Scale: Small, Normal, Large, Auto
- Module set:
  - `RankElo`
  - `LiveStats`
  - `ResourceOverlay`
  - `BedStatus`
- State combinations:
  - Premium renderer ON / OFF
  - Premium card ON / OFF
  - HUD Scale: 0.5 / 1.0 / 1.5 / 2.0

## Guardrails
- If premium renderer causes measurable FPS drop in active Bedwars combat scenes, keep legacy renderer as default until optimized.
- No permanent switch-over without hitbox and clipping verification at all matrix points.
