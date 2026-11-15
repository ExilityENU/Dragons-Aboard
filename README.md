# Dragons Aboard

A tiny NeoForge **1.21.1** addon that makes **Book of Dragons** dragons behave on
**Create Aeronautics / Sable** physics ships.

## What it fixes

Sable (the physics engine behind Create Aeronautics) already carries any entity
"tracked" by a ship — but it drops tracking the instant the entity is more than
~1 block off the deck. For dragons that means:

- a hovering or taking-off dragon is left behind by a moving ship,
- sitting dragons' AI fights the moving deck,
- ship descents can bank up fall damage.

Dragons Aboard fixes all three, with **zero mixins** — it only uses Sable's
public API:

| Feature | What it does |
|---|---|
| **Hover hold** | While a dragon flies/hovers within `hoverHoldDistance` (default 6) blocks above the deck it last stood on, tracking is re-asserted every tick, so Sable's own carry logic keeps it with the ship — rotation included. Fly higher than that and it detaches normally. |
| **Calm sitting** | Tamed mobs ordered to sit/stay get their navigation stopped while aboard. |
| **No deck fall damage** | Fall distance is cleared while aboard / hover-held. |

By default it applies to every mob from the `bookofdragons` namespace. Add more
via the server config (`extraEntities`, `entityNamespaces`) or the entity-type
tag `#dragonsaboard:affected` in a datapack.

## Requirements (runtime)

- Minecraft 1.21.1, NeoForge
- Sable `[2.0.0, 3.0.0)` + Create + Create Aeronautics
- Book of Dragons 1.4+ for 1.21.1 NeoForge (optional dep — the mod simply
  idles for mobs that don't exist)

## Building

```bash
./gradlew build          # Windows: gradlew.bat build
```

- Needs internet on first run (downloads Gradle 9.5, NeoForge, and the Sable
  jars from `maven.ryanhcode.dev`). JDK 21 is auto-provisioned if missing.
- Output: `build/libs/dragonsaboard-1.0.0.jar` → drop it into your pack's
  `mods/` folder (client **and** server).
- `runClient`/`runServer` are intentionally not configured — test the jar in
  your real modpack instance.

## In-game test checklist

1. Pack loads with the jar added; no crash; `dragonsaboard` listed in Mods.
2. Land a tamed dragon on a stationary assembled ship, order it to **sit**,
   fly the ship around (including turns) → dragon stays glued to the deck.
3. With the ship cruising, make the dragon **hover/jump** just above the deck
   → it keeps pace with the ship instead of being left behind.
4. Fly the dragon well above `hoverHoldDistance` → it detaches normally.
5. Descend sharply with a dragon aboard, then land → no fall damage.
6. Check `serverconfig/dragonsaboard-server.toml` in the world folder to tune.

## Config (`serverconfig/dragonsaboard-server.toml`)

| Key | Default | |
|---|---|---|
| `entityNamespaces` | `["bookofdragons"]` | Mod namespaces handled |
| `extraEntities` | `[]` | Extra entity ids, e.g. `"somemod:wyvern"` |
| `hoverHold` | `true` | Keep hovering dragons with the ship |
| `hoverHoldDistance` | `6.0` | Reach (blocks above deck, ship-space) |
| `calmSittingDragons` | `true` | Stop sitting mobs' navigation aboard |
| `noFallDamageAboard` | `true` | Clear fall distance aboard |

## Known limits (v1)

- Dragons don't *choose* to land on moving ships — their AI targets world
  coordinates. Expect clean landings while the ship is slow or docked.
- A carried dragon's body yaw doesn't rotate with the ship (position does).
- If Sable's internals change across major versions, the mod logs one error
  and goes dormant rather than crashing the game.

## License

MIT. Not affiliated with the Sable/Aeronautics or Book of Dragons teams.
