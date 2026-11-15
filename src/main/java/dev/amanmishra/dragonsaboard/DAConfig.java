package dev.amanmishra.dragonsaboard;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (synced to clients, lives in serverconfig/ per world).
 */
public final class DAConfig {

    public static final ModConfigSpec SPEC;

    /** Mod namespaces whose mobs get deck handling (default: Book of Dragons). */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_NAMESPACES;

    /** Extra individual entity ids, e.g. "ice_and_fire:fire_dragon". */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_ENTITIES;

    /** Keep a dragon glued to the ship while it hovers/flies above the deck. */
    public static final ModConfigSpec.BooleanValue HOVER_HOLD;

    /** How far above the deck (in blocks, measured in ship-space) hover hold reaches. */
    public static final ModConfigSpec.DoubleValue HOVER_HOLD_DISTANCE;

    /** Stop navigation of sitting/stay-ordered tamables while aboard. */
    public static final ModConfigSpec.BooleanValue CALM_SITTING;

    /** Zero out fall distance while aboard or hover-held. */
    public static final ModConfigSpec.BooleanValue NO_FALL_DAMAGE_ABOARD;

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("entities");
        ENTITY_NAMESPACES = b
                .comment("Mobs from these mod namespaces get deck handling.",
                         "Mobs tagged #dragonsaboard:affected are always included.")
                .defineListAllowEmpty("entityNamespaces", List.of("bookofdragons"),
                        () -> "modid", o -> o instanceof String s && !s.isBlank());
        EXTRA_ENTITIES = b
                .comment("Extra individual entity types, as full ids (e.g. \"somemod:some_dragon\").")
                .defineListAllowEmpty("extraEntities", List.of(),
                        () -> "modid:entity", o -> o instanceof String s && s.contains(":"));
        b.pop();

        b.push("behaviour");
        HOVER_HOLD = b
                .comment("Keep dragons moving with a ship while they hover/fly close above its deck.",
                         "Without this, Sable drops them the moment they are ~1 block off the deck.")
                .define("hoverHold", true);
        HOVER_HOLD_DISTANCE = b
                .comment("Max height above the deck (blocks, in ship-space) at which hover hold applies.")
                .defineInRange("hoverHoldDistance", 6.0D, 1.0D, 32.0D);
        CALM_SITTING = b
                .comment("While aboard, stop the navigation of tamed mobs that are ordered to sit/stay,",
                         "so their AI doesn't fight the moving deck.")
                .define("calmSittingDragons", true);
        NO_FALL_DAMAGE_ABOARD = b
                .comment("Clear fall distance while a mob is aboard or hover-held,",
                         "so ship descents never bank up fall damage.")
                .define("noFallDamageAboard", true);
        b.pop();

        SPEC = b.build();
    }

    private DAConfig() {
    }
}
