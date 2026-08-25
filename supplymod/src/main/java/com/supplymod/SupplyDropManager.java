package com.supplymod;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles the "Zrzut Zaopatrzenia" (Supply Drop) feature:
 * - 20% chance each in-game day that a drop occurs
 * - random coordinates, announced to all players + shown on a boss bar
 * - the boss bar text acts as a lightweight "waypoint": it shows the live
 *   distance (in blocks) from the nearest player and the remaining time
 * - also sends a Xaero's Minimap-compatible chat waypoint message, which
 *   players with that mod installed can click to add a real waypoint
 * - falls until it hits solid ground (checked every tick), then stays sealed for 5 minutes
 * - on landing, knocks back any nearby player (shockwave effect) and the crate
 *   becomes invisible (only the boss bar + particles mark the spot) until it opens
 * - opens into a barrel full of loot rolled from SupplyLoot percentages,
 *   placed in randomized slots instead of filling from slot 0 onward
 */
public class SupplyDropManager {

    private static final Random RANDOM = new Random();

    private static final double DROP_CHANCE = 0.20; // 20%
    private static final int SEAL_TICKS = 5 * 60 * 20;  // 5 minutes
    private static final int SEARCH_RADIUS = 1000; // blocks around world spawn
    private static final int FALL_START_Y = 250;
    private static final double FALL_SPEED = 0.2; // blocks per tick while falling
    private static final double KNOCKBACK_RADIUS = 5.0;  // blocks - who gets pushed
    private static final double KNOCKBACK_STRENGTH = 1.8; // tuned to send ~5 blocks

    private static final Map<ServerWorld, Long> lastCheckedDay = new HashMap<>();
    private static final List<ActiveDrop> activeDrops = new ArrayList<>();

    public static void init() {
        // nothing to pre-load right now
    }

    public static void onWorldTick(ServerWorld world) {
        checkDailyRoll(world);
        tickActiveDrops(world);
    }

    private static void checkDailyRoll(ServerWorld world) {
        long day = world.getTimeOfDay() / 24000L;
        Long last = lastCheckedDay.get(world);
        if (last != null && last == day) {
            return;
        }
        lastCheckedDay.put(world, day);

        if (RANDOM.nextDouble() < DROP_CHANCE) {
            startDrop(world);
        }
    }

    private static void startDrop(ServerWorld world) {
        BlockPos spawn = BlockPos.ORIGIN;
        int x = spawn.getX() + RANDOM.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
        int z = spawn.getZ() + RANDOM.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;

        world.getChunk(x >> 4, z >> 4);

        ArmorStandEntity crate = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        crate.updatePosition(x + 0.5, FALL_START_Y, z + 0.5);
        crate.setInvisible(false);
        crate.setInvulnerable(true);
        crate.setNoGravity(true);
        crate.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.BARREL));
        world.spawnEntity(crate);

        ServerBossBar bossBar = new ServerBossBar(
                Text.literal("Zrzut Zaopatrzenia"),
                BossBar.Color.YELLOW,
                BossBar.Style.NOTCHED_10);
        bossBar.setPercent(1.0f);

        int groundYEstimate = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);

        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            bossBar.addPlayer(player);
            player.sendMessage(Text.literal("Zrzut zaopatrzenia wlasnie spada! Koordynaty: X=" + x + ", Z=" + z)
                    .formatted(Formatting.GOLD), false);
        }

        sendXaeroWaypoint(world, x, groundYEstimate, z);

        ActiveDrop drop = new ActiveDrop(crate, bossBar, x, z);
        activeDrops.add(drop);
    }

    /**
     * Sends a Xaero's Minimap-compatible waypoint share message to chat.
     * Players who have Xaero's Minimap installed will see a clickable [Add]
     * button that places a real waypoint at the drop's location. Players
     * without the mod just see this as harmless plain text.
     */
    private static void sendXaeroWaypoint(ServerWorld world, int x, int groundY, int z) {
        String dimensionId;
        if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
            dimensionId = "Internal-overworld";
        } else if (world.getRegistryKey() == net.minecraft.world.World.NETHER) {
            dimensionId = "Internal-the_nether";
        } else if (world.getRegistryKey() == net.minecraft.world.World.END) {
            dimensionId = "Internal-the_end";
        } else {
            dimensionId = "Internal-overworld";
        }

        String waypointMsg = "xaero-waypoint:Zrzut_Zaopatrzenia:ZZ:" + x + ":" + groundY + ":" + z
                + ":2:false:0:" + dimensionId;

        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.literal(waypointMsg), false);
        }
    }

    private static void tickActiveDrops(ServerWorld world) {
        Iterator<ActiveDrop> it = activeDrops.iterator();
        while (it.hasNext()) {
            ActiveDrop drop = it.next();
            if (drop.crate.getEntityWorld() != world) continue;

            drop.ticksElapsed++;

            switch (drop.phase) {
                case FALLING -> tickFalling(world, drop);
                case SEALED -> tickSealed(world, drop);
            }

            if (drop.phase == Phase.DONE) {
                drop.bossBar.clearPlayers();
                it.remove();
            }
        }
    }

    /**
     * Builds the "waypoint" style label shown on the boss bar: the drop's
     * name plus the live distance (in blocks) from the nearest online
     * player, so it acts like a simple in-HUD marker without needing a
     * dedicated waypoint/compass system.
     */
    private static String buildWaypointLabel(ServerWorld world, ActiveDrop drop, String suffix) {
        double nearestDist = Double.MAX_VALUE;
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            double dist = new Vec3d(player.getX(), player.getY(), player.getZ())
                    .distanceTo(new Vec3d(drop.x + 0.5, player.getY(), drop.z + 0.5));
            if (dist < nearestDist) {
                nearestDist = dist;
            }
        }

        String distText = (nearestDist == Double.MAX_VALUE) ? "?" : (int) Math.round(nearestDist) + "m";
        return "Zrzut Zaopatrzenia | " + distText + (suffix.isEmpty() ? "" : " | " + suffix);
    }

    private static void tickFalling(ServerWorld world, ActiveDrop drop) {
        double currentY = drop.crate.getY();
        double nextY = currentY - FALL_SPEED;

        BlockPos checkPos = new BlockPos(drop.x, (int) Math.floor(nextY), drop.z);

        boolean hitGround = !world.getBlockState(checkPos).isAir() || nextY <= world.getBottomY();

        if (hitGround) {
            int groundY = checkPos.getY() + 1;
            drop.groundY = groundY;
            drop.crate.updatePosition(drop.x + 0.5, groundY, drop.z + 0.5);
            drop.crate.setInvisible(true);

            drop.phase = Phase.SEALED;
            drop.ticksElapsed = 0;

            knockBackNearbyPlayers(world, drop);

            for (var player : world.getPlayers()) {
                player.sendMessage(Text.literal("Zrzut zaopatrzenia wyladowal! Otworzy sie za 5 minut.")
                        .formatted(Formatting.YELLOW), false);
            }
            return;
        }

        drop.crate.updatePosition(drop.x + 0.5, nextY, drop.z + 0.5);

        double progress = Math.min(1.0, (FALL_START_Y - nextY) / (double) FALL_START_Y);
        drop.bossBar.setPercent((float) (1.0 - progress));

        if (world.getTime() % 20 == 0) {
            drop.bossBar.setName(Text.literal(buildWaypointLabel(world, drop, "")));
        }

        if (world.getTime() % 10 == 0) {
            world.spawnParticles(ParticleTypes.CLOUD, drop.x + 0.5, nextY + 1, drop.z + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private static void knockBackNearbyPlayers(ServerWorld world, ActiveDrop drop) {
        Vec3d landingCenter = new Vec3d(drop.x + 0.5, drop.groundY, drop.z + 0.5);

        world.spawnParticles(ParticleTypes.EXPLOSION, landingCenter.x, landingCenter.y + 0.5, landingCenter.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, new BlockPos(drop.x, drop.groundY, drop.z),
                net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE.value(), net.minecraft.sound.SoundCategory.BLOCKS);

        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            double dist = playerPos.distanceTo(landingCenter);
            if (dist > KNOCKBACK_RADIUS) continue;

            Vec3d diff = playerPos.subtract(landingCenter);
            Vec3d horizontal = diff.x * diff.x + diff.z * diff.z < 0.0001
                    ? new Vec3d(1, 0, 0)
                    : new Vec3d(diff.x, 0, diff.z).normalize();

            player.setVelocity(horizontal.x * KNOCKBACK_STRENGTH, 0.45, horizontal.z * KNOCKBACK_STRENGTH);
            player.sendMessage(Text.literal("Zrzut zaopatrzenia odepchnal cie od miejsca ladowania!")
                    .formatted(Formatting.RED), true);
        }
    }

    private static void tickSealed(ServerWorld world, ActiveDrop drop) {
        double progress = Math.min(1.0, drop.ticksElapsed / (double) SEAL_TICKS);
        drop.bossBar.setPercent((float) progress);

        if (world.getTime() % 20 == 0) {
            int secondsLeft = (SEAL_TICKS - drop.ticksElapsed) / 20;
            String timeText = (secondsLeft >= 60) ? (secondsLeft / 60) + "min" : secondsLeft + "s";
            drop.bossBar.setName(Text.literal(buildWaypointLabel(world, drop, timeText)));
        }

        if (world.getTime() % 40 == 0) {
            world.spawnParticles(ParticleTypes.END_ROD, drop.x + 0.5, drop.groundY + 1, drop.z + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
        }

        if (drop.ticksElapsed >= SEAL_TICKS) {
            openCrate(world, drop);
            drop.phase = Phase.DONE;
        }
    }

    private static void openCrate(ServerWorld world, ActiveDrop drop) {
        BlockPos pos = new BlockPos(drop.x, drop.groundY, drop.z);
        world.setBlockState(pos, Blocks.BARREL.getDefaultState());

        if (world.getBlockEntity(pos) instanceof net.minecraft.block.entity.BarrelBlockEntity barrel) {
            List<ItemStack> loot = SupplyLoot.rollLoot(RANDOM);

            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < barrel.size(); i++) {
                slots.add(i);
            }
            Collections.shuffle(slots, RANDOM);

            int i = 0;
            for (ItemStack stack : loot) {
                if (i >= slots.size()) break;
                barrel.setStack(slots.get(i), stack);
                i++;
            }
        }

        drop.crate.discard();
        world.spawnParticles(ParticleTypes.FIREWORK, drop.x + 0.5, drop.groundY + 1, drop.z + 0.5, 20, 0.5, 0.5, 0.5, 0.05);

        for (var player : world.getPlayers()) {
            player.sendMessage(Text.literal("Zrzut zaopatrzenia zostal otwarty! X=" + drop.x + " Z=" + drop.z)
                    .formatted(Formatting.GREEN), false);
        }
    }

    private enum Phase { FALLING, SEALED, DONE }

    private static class ActiveDrop {
        final ArmorStandEntity crate;
        final ServerBossBar bossBar;
        final int x, z;
        int groundY;
        Phase phase = Phase.FALLING;
        int ticksElapsed = 0;

        ActiveDrop(ArmorStandEntity crate, ServerBossBar bossBar, int x, int z) {
            this.crate = crate;
            this.bossBar = bossBar;
            this.x = x;
            this.z = z;
        }
    }
             }
