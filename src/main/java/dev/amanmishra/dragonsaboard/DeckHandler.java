package dev.amanmishra.dragonsaboard;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.UUID;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Core per-tick logic.
 *
 * How Sable works (verified against its source, v2.0.x):
 *  - Any entity whose Entity.move() runs while it has a "tracking sub-level" set is
 *    carried along with that sub-level's pose delta (translation + rotation) — see
 *    SubLevelEntityCollision.collide(), tracked branch.
 *  - Tracking is dropped at the end of a move when no sub-level blocks are within
 *    ~1 block of the entity's bounds. That is why a hovering/taking-off dragon is
 *    left behind by a moving ship.
 *
 * So: while a configured mob is within hoverHoldDistance above the deck it last
 * stood on, we simply re-assert tracking each tick (after the mob has moved), and
 * Sable's own carry logic does the rest next tick. No velocity math, no mixins.
 */
@EventBusSubscriber(modid = DragonsAboard.MODID)
public final class DeckHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Datapack hook: mobs tagged #dragonsaboard:affected always get deck handling. */
    public static final TagKey<EntityType<?>> AFFECTED =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(DragonsAboard.MODID, "affected"));

    /** Fail-safe: if Sable's API ever throws unexpectedly, log once and go dormant. */
    private static boolean crashed = false;

    @SubscribeEvent
    public static void onEntityTick(final EntityTickEvent.Post event) {
        if (crashed) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.level().isClientSide() || mob.isPassenger() || mob.isRemoved()) {
            return;
        }
        if (!DAConfig.SPEC.isLoaded()) {
            return;
        }
        if (!isAffected(mob)) {
            return;
        }

        try {
            final SubLevel tracking = Sable.HELPER.getTrackingSubLevel(mob);
            if (tracking != null && !tracking.isRemoved()) {
                handleAboard(mob);
            } else if (DAConfig.HOVER_HOLD.get()) {
                tryHoverHold(mob);
            }
        } catch (final Throwable t) {
            crashed = true;
            LOGGER.error("Dragons Aboard hit an unexpected error talking to Sable and has disabled itself "
                    + "for this session. Please report this with the stacktrace below.", t);
        }
    }

    private static boolean isAffected(final Mob mob) {
        final EntityType<?> type = mob.getType();
        if (type.is(AFFECTED)) {
            return true;
        }
        final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return DAConfig.ENTITY_NAMESPACES.get().contains(id.getNamespace())
                || DAConfig.EXTRA_ENTITIES.get().contains(id.toString());
    }

    /** The mob is standing on (tracked by) a sub-level right now. */
    private static void handleAboard(final Mob mob) {
        if (DAConfig.NO_FALL_DAMAGE_ABOARD.get()) {
            mob.fallDistance = 0.0F;
        }
        if (DAConfig.CALM_SITTING.get()
                && mob instanceof final TamableAnimal tamable
                && tamable.isInSittingPose()) {
            // Stop the AI fighting the moving deck: a sitting dragon stays put.
            mob.getNavigation().stop();
        }
    }

    /**
     * The mob is not tracked (it hovered or jumped), but it recently was.
     * If the deck of that ship is still within reach below it (measured in
     * ship-space, so ship rotation is handled), re-assert tracking so Sable
     * keeps carrying it on its next move.
     */
    private static void tryHoverHold(final Mob mob) {
        // Equivalent to Sable.HELPER.getLastTrackingSubLevel(mob), but null-safe
        // when the mob has changed dimension since it last stood on a ship.
        final UUID lastId = ((EntityMovementExtension) mob).sable$getLastTrackingSubLevelID();
        if (lastId == null) {
            return;
        }
        final SubLevelContainer container = SubLevelContainer.getContainer(mob.level());
        if (container == null) {
            return;
        }
        final SubLevel last = container.getSubLevel(lastId);
        if (last == null || last.isRemoved()) {
            return;
        }

        // Transform the mob's world position into the ship's plot space and
        // probe straight down for deck blocks.
        final var pose = last.logicalPose();
        final Vec3 local = pose.transformPositionInverse(mob.position());
        final double reach = DAConfig.HOVER_HOLD_DISTANCE.get();

        final BlockHitResult hit = mob.level().clip(new ClipContext(
                local,
                local.add(0.0D, -reach, 0.0D),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob));

        if (hit.getType() == HitResult.Type.MISS) {
            return; // Too far above the deck (or off the side) — let it go free.
        }

        ((EntityMovementExtension) mob).sable$setTrackingSubLevel(last);

        if (DAConfig.NO_FALL_DAMAGE_ABOARD.get()) {
            mob.fallDistance = 0.0F;
        }
    }

    private DeckHandler() {
    }
}
