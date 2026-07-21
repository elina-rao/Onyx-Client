# Premium HUD QA Rollout

## Release Gates

### Performance Budget Gate
- Baseline scene: active Bedwars match with scoreboard, particles, and overlays visible.
- Compare:
  - Legacy renderer enabled
  - Premium renderer enabled on migrated modules
- Pass condition:
  - No noticeable FPS degradation in gameplay (target: within normal run-to-run variance).

### Legacy Compatibility Gate
- Each migrated module must keep:
  - `Premium Renderer` toggle
  - `Premium Card` toggle
- Pass condition:
  - Module remains fully readable and draggable when toggles are OFF.

## Test Matrix
- Resolutions: 1280x720, 1920x1080, 2560x1440
- HUD Scales: 0.5, 1.0, 1.5, 2.0
- Modules:
  - RankElo
  - LiveStats
  - ResourceOverlay (horizontal + vertical)
  - BedStatus (text + compact)

## Manual Checks
- Card/text alignment remains stable while values update.
- Drag/resize handles in `HudEditor` stay aligned with visual bounds.
- Top-edge movement remains unobstructed in HUD editor.
- Long lines in compact modes truncate gracefully (no hard substring clipping).

## Rollout Sequence
1. Enable premium path for ranked modules (`RankElo`, `LiveStats`).
2. Enable premium path for core bedwars overlays (`ResourceOverlay`, `BedStatus`).
3. Keep toggles exposed and monitor reports before wider HUD migration.
