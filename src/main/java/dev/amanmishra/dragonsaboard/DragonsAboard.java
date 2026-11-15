package dev.amanmishra.dragonsaboard;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;

/**
 * Dragons Aboard — makes Book of Dragons dragons (and any other configured mobs)
 * behave on Create Aeronautics / Sable physics contraptions.
 *
 * Sable already carries any entity it is "tracking" along with a moving sub-level
 * (ship). The problem for dragons is that tracking is dropped the moment the mob
 * is more than ~1 block off the deck — i.e. the instant it hovers or takes off —
 * so the ship sails out from under it. This mod:
 *
 *  1. Re-asserts tracking every tick while a dragon hovers within a configurable
 *     distance above the deck it was last standing on ("hover hold"), letting
 *     Sable's own carry logic keep it moving with the ship.
 *  2. Stops sitting (stay-ordered) dragons' navigation while aboard, so their AI
 *     doesn't fight the moving deck.
 *  3. Clears fall distance while aboard/hover-held, so deck motion never banks
 *     up bogus fall damage.
 *
 * No mixins — only Sable's public API and its runtime-applied entity interfaces.
 */
@Mod(DragonsAboard.MODID)
public final class DragonsAboard {

    public static final String MODID = "dragonsaboard";

    public DragonsAboard(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, DAConfig.SPEC);
        // DeckHandler registers itself via @EventBusSubscriber.
    }
}
