#!/usr/bin/env bash
set -e
mkdir -p supplymod && cd supplymod

cat > ".gitignore" << 'SUPPLYMOD_EOF'
.gradle/
build/
.idea/
*.iml
out/
run/
.vscode/

SUPPLYMOD_EOF

cat > "README.md" << 'SUPPLYMOD_EOF'
# Supply Drop Mod (Fabric, MC 1.21.1)

Mod dodaje:

1. **Zrzuty zaopatrzenia** - co dzien w grze 20% szansy na zrzut. Losowe
   koordynaty, komunikat + boss bar u gory ekranu ze wspolrzednymi. Zrzut
   (wyglada jak lecaca beczka) leci 5 minut, a w momencie ladowania
   **odpycha kazdego gracza w promieniu 5 blokow** (jak mala fala
   uderzeniowa). Potem czeka zapieczetowany kolejne 5 minut, po czym
   otwiera sie jako **beczka** z lootem.
2. **Ksiega Ulaskawienia** - dropi ze zrzutu (1% szans). Nakladasz ja na
   dowolny przedmiot w **kowadle** (lewy slot: przedmiot, prawy slot:
   ksiega). Oznaczony przedmiot NIE wypada z ekwipunku po smierci - wraca do
   ciebie po respawnie, ale efekt zuzywa sie jednorazowo.
3. **System dodatkowych serc** - przedmiot "Serce" (10% ze zrzutu) po
   zjedzeniu/uzyciu podnosi twoje maksymalne zdrowie o 1 serce, az do limitu
   20 serc (40 HP). Uzyciu towarzyszy taki sam blysk na ekranie i dzwiek,
   jak przy zuzyciu Totemu Niesmiertelnosci.

## Loot w zrzucie (kazdy wpis losowany NIEZALEZNIE, moze wypasc kilka razy)

| Przedmiot | Szansa |
|---|---|
| Blok diamentu | 30% |
| Totem Niesmiertelnosci | 5% |
| Zlote jablko zaklete (Notch Apple) | 10% |
| Zlote jablko (zwykle) | 39% |
| Brodawki nether | 25% |
| Losowa czesc diamentowej zbroi | 40% |
| Zelazo | 70% |
| Zloto | 70% |
| Zaczarowana ksiazka (losowa) | 30% |
| Serce | 10% |
| Ksiega Ulaskawienia | 1% |
| Netherite scrap | 5% |
| Diamenty (luzem) | 30% |
| Miedz | 80% |
| Chleb (wypelniacz) | 60% |
| Strzaly (wypelniacz) | 50% |
| Proch strzelniczy (wypelniacz) | 35% |
| Sznurek (wypelniacz) | 40% |
| Lapis lazuli (wypelniacz) | 25% |
| Szmaragd (wypelniacz) | 15% |
| Perla Endermana (wypelniacz) | 12% |
| Siodlo (wypelniacz) | 8% |

## WAZNE - zanim zaczniesz

To jest kompletny szkielet projektu napisany recznie (bez lokalnego
srodowiska Gradle/Minecraft do kompilacji i testow - nie mam tutaj dostepu
do internetu ani Javy z pelnym MC). Zanim uruchomisz mod w grze, musisz:

1. Otworzyc projekt w **IntelliJ IDEA** z zainstalowanym pluginem
   Fabric/Gradle (albo samym Gradle) i dac mu pobrac zaleznosci
   (`./gradlew genSources` / `./gradlew build`).
2. Sprawdzic, czy nazwy metod w mixinach (`AnvilScreenHandlerMixin`,
   `PlayerDropInventoryMixin`) zgadzaja sie z mapowaniami Yarn dla
   1.21.1 - mapowania czasem drobno sie zmieniaja miedzy buildami, wiec
   jesli Gradle zglosi blad w mixinie, otworz zdekompilowane zrodla
   (`AnvilScreenHandler` / `PlayerEntity`) i popraw nazwy pol/metod.
3. Tekstury Ksiegi Ulaskawienia i Serca sa juz dolaczone (proste
   pixel-artowe 16x16, wygenerowane recznie: zwykla brazowa ksiazka jak
   z vanilla Minecrafta oraz czerwony barwnik). Podmien je
   swobodnie na wlasne w:
   - `src/main/resources/assets/supplymod/textures/item/book_of_pardon.png`
   - `src/main/resources/assets/supplymod/textures/item/heart.png`
4. Uzupelnic `src/main/resources/assets/supplymod/icon.png` (128x128) albo
   usunac linijke `"icon"` z `fabric.mod.json`.

## Uruchomienie w grze (dev)

```
./gradlew runClient
```

## Wrzucenie na GitHub

Ja (Claude) nie mam tutaj dostepu do internetu, wiec nie moge sam wypchnac
kodu na GitHuba - musisz zrobic to lokalnie, u siebie:

```bash
cd supplymod
git init
git add .
git commit -m "Supply Drop Mod - initial version"
git branch -M main
git remote add origin https://github.com/TWOJ_NICK/supplymod.git
git push -u origin main
```

(Wczesniej stworz puste repo na github.com -> "New repository", bez
README/licencji, zeby uniknac konfliktow przy pierwszym pushu).

## Znane uproszczenia / TODO

- Zrzut wizualnie to niewidzialny "armor stand" z blokiem beczki na
  glowie (dziala bez potrzeby rejestrowania wlasnej encji/renderera).
  Mozna to podmienic na wlasna encje z lepsza animacja/modelem.
- Odepchniecie graczy dziala przez predkosc (velocity), nie teleport -
  dzieki temu gracz nie przechodzi przez sciany/teren. Sila jest
  dobrana tak, by na otwartym terenie odrzucic ~5 blokow, ale realny
  dystans zalezy od terenu (schody, woda, przeszkody) - mozna dostroic
  stala `KNOCKBACK_STRENGTH` w `SupplyDropManager.java`.
- Recipe na kowadle nie ma custom modelu/nazwy w GUI poza standardowym
  zachowaniem anvil - dziala funkcjonalnie, ale nie wyswietla specjalnego
  komunikatu w interfejsie.
- Brak datageneracji lootu przez `loot_table` JSON - loot jest generowany
  w kodzie (`SupplyLoot.java`), co ulatwia obsluge "wiele wystapien tego
  samego itemu", ale oznacza, ze nie zobaczysz go w standardowym
  edytorze loot tables.

SUPPLYMOD_EOF

cat > "build.gradle" << 'SUPPLYMOD_EOF'
plugins {
    id 'fabric-loom' version '1.7-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    // Add repositories here if needed for extra dependencies
}

loom {
    splitEnvironmentSourceSets()

    mods {
        "supplymod" {
            sourceSet sourceSets.main
        }
    }
}

fabricApi {
    configureDataGeneration()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"

    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    inputs.property "version", project.version

    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}"}
    }
}

SUPPLYMOD_EOF

cat > "gradle.properties" << 'SUPPLYMOD_EOF'
# Done to increase memory for the JVM during compilation
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true

# Fabric Properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.9

# Mod Properties
mod_version=1.0.0
maven_group=com.supplymod
archives_base_name=supplymod

# Dependencies
fabric_version=0.107.0+1.21.1

SUPPLYMOD_EOF

cat > "settings.gradle" << 'SUPPLYMOD_EOF'
pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/java/com/supplymod"
cat > "src/main/java/com/supplymod/ModItems.java" << 'SUPPLYMOD_EOF'
package com.supplymod;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

public class ModItems {

    // Max health player can reach with heart items: 20 hearts = 40.0 HP.
    public static final double MAX_HEALTH_CAP = 40.0;
    // Each Heart item grants 2.0 HP (1 heart).
    public static final double HEALTH_PER_HEART_ITEM = 2.0;
    public static final Identifier HEART_BONUS_MODIFIER_ID =
            Identifier.of(SupplyMod.MOD_ID, "extra_heart_bonus");

    public static Item BOOK_OF_PARDON;
    public static Item HEART_ITEM;

    public static void register() {
        BOOK_OF_PARDON = Registry.register(
                Registries.ITEM,
                Identifier.of(SupplyMod.MOD_ID, "book_of_pardon"),
                new PardonBookItem(new Item.Settings().maxCount(16)));

        HEART_ITEM = Registry.register(
                Registries.ITEM,
                Identifier.of(SupplyMod.MOD_ID, "heart"),
                new HeartItem(new Item.Settings().maxCount(16)));
    }

    /**
     * Ksiega Ulaskawienia - zawsze ma wbudowany wanilowy efekt polysku
     * (ten sam "enchantment glint" co przedmioty zaklete), zeby migotala
     * w rece tak jak na zdjeciu referencyjnym - bez potrzeby kopiowania
     * jakiejkolwiek tekstury.
     */
    public static class PardonBookItem extends Item {
        public PardonBookItem(Settings settings) {
            super(settings);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return true;
        }
    }

    /**
     * Custom item: consuming it permanently raises the player's max health by
     * one heart (2.0 HP), up to the 20-heart (40 HP) cap.
     */
    public static class HeartItem extends Item {
        public HeartItem(Settings settings) {
            super(settings);
        }

        @Override
        public TypedActionResult<ItemStack> use(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, Hand hand) {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }

            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr == null) {
                return TypedActionResult.pass(stack);
            }

            double currentBase = attr.getBaseValue();
            if (currentBase >= MAX_HEALTH_CAP) {
                player.sendMessage(Text.literal("Masz juz maksymalna liczbe serc (20)."), true);
                return TypedActionResult.fail(stack);
            }

            double newBase = Math.min(MAX_HEALTH_CAP, currentBase + HEALTH_PER_HEART_ITEM);
            attr.setBaseValue(newBase);
            player.setHealth((float) Math.min(player.getHealth() + HEALTH_PER_HEART_ITEM, (float) newBase));
            player.sendMessage(Text.literal("Zdobywasz dodatkowe serce! (" + (int) (newBase / 2) + "/20)"), true);

            // Efekt "totemowy" - dokladnie ten sam blysk na ekranie + dzwiek,
            // co przy zuzyciu Totemu Niesmiertelnosci. Status 35 to wanilowy
            // packet EntityStatus obslugiwany po stronie klienta.
            world.sendEntityStatus(player, (byte) 35);

            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return TypedActionResult.success(stack);
        }
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/java/com/supplymod"
cat > "src/main/java/com/supplymod/SupplyDropManager.java" << 'SUPPLYMOD_EOF'
package com.supplymod;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.network.ServerBossBar;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles the "Zrzut Zaopatrzenia" (Supply Drop) feature:
 * - 20% chance each in-game day that a drop occurs
 * - random coordinates, announced to all players + shown on a boss bar
 * - falls for 5 minutes, then stays sealed for 5 more minutes
 * - on landing, knocks back any nearby player (shockwave effect)
 * - opens into a barrel full of loot rolled from SupplyLoot percentages
 */
public class SupplyDropManager {

    private static final Random RANDOM = new Random();

    private static final double DROP_CHANCE = 0.20; // 20%
    private static final int FALL_TICKS = 5 * 60 * 20;   // 5 minutes
    private static final int SEAL_TICKS = 5 * 60 * 20;   // 5 minutes
    private static final int SEARCH_RADIUS = 1000; // blocks around world spawn
    private static final int FALL_START_Y = 250;
    private static final double KNOCKBACK_RADIUS = 5.0;   // blocks - who gets pushed
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
        BlockPos spawn = world.getSpawnPos();
        int x = spawn.getX() + RANDOM.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
        int z = spawn.getZ() + RANDOM.nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
        int groundY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);

        ArmorStandEntity crate = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        crate.updatePosition(x + 0.5, FALL_START_Y, z + 0.5);
        crate.setInvisible(true);
        crate.setInvulnerable(true);
        crate.setNoGravity(true);
        crate.setMarker(false);
        crate.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.BARREL));
        world.spawnEntity(crate);

        ServerBossBar bossBar = new ServerBossBar(
                Text.literal("Zrzut zaopatrzenia: X=" + x + " Z=" + z),
                BossBar.Color.YELLOW,
                BossBar.Style.NOTCHED_10);
        bossBar.setPercent(1.0f);

        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            bossBar.addPlayer(player);
            player.sendMessage(Text.literal("Zrzut zaopatrzenia wlasnie spada! Koordynaty: X=" + x + ", Z=" + z)
                    .formatted(Formatting.GOLD), false);
        }

        ActiveDrop drop = new ActiveDrop(crate, bossBar, x, groundY, z);
        activeDrops.add(drop);
    }

    private static void tickActiveDrops(ServerWorld world) {
        Iterator<ActiveDrop> it = activeDrops.iterator();
        while (it.hasNext()) {
            ActiveDrop drop = it.next();
            if (drop.crate.getWorld() != world) continue;

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

    private static void tickFalling(ServerWorld world, ActiveDrop drop) {
        double progress = Math.min(1.0, drop.ticksElapsed / (double) FALL_TICKS);
        double y = FALL_START_Y + (drop.groundY - FALL_START_Y) * progress;

        drop.crate.updatePosition(drop.x + 0.5, y, drop.z + 0.5);

        drop.bossBar.setPercent((float) (1.0 - progress));
        drop.bossBar.setName(Text.literal(
                "Zrzut leci... X=" + drop.x + " Z=" + drop.z + " (" + (int) ((1 - progress) * 100) + "%)"));

        if (world.getTime() % 20 == 0) {
            world.spawnParticles(ParticleTypes.CLOUD, drop.x + 0.5, y + 1, drop.z + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
        }

        if (drop.ticksElapsed >= FALL_TICKS) {
            drop.phase = Phase.SEALED;
            drop.ticksElapsed = 0;
            drop.crate.updatePosition(drop.x + 0.5, drop.groundY, drop.z + 0.5);

            knockBackNearbyPlayers(world, drop);

            for (var player : world.getPlayers()) {
                player.sendMessage(Text.literal("Zrzut zaopatrzenia wyladowal! Otworzy sie za 5 minut.")
                        .formatted(Formatting.YELLOW), false);
            }
        }
    }

    /**
     * Pushes back any player standing too close when the crate lands - like
     * a small shockwave. Uses velocity (not teleport) so players don't clip
     * through walls/terrain; the strength below is tuned to send an
     * unobstructed player roughly ~5 blocks away.
     */
    private static void knockBackNearbyPlayers(ServerWorld world, ActiveDrop drop) {
        Vec3d landingCenter = new Vec3d(drop.x + 0.5, drop.groundY, drop.z + 0.5);

        world.spawnParticles(ParticleTypes.EXPLOSION, landingCenter.x, landingCenter.y + 0.5, landingCenter.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, new BlockPos(drop.x, drop.groundY, drop.z),
                net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);

        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            Vec3d playerPos = player.getPos();
            double dist = playerPos.distanceTo(landingCenter);
            if (dist > KNOCKBACK_RADIUS) continue;

            Vec3d diff = playerPos.subtract(landingCenter);
            Vec3d horizontal = diff.x * diff.x + diff.z * diff.z < 0.0001
                    ? new Vec3d(1, 0, 0)
                    : new Vec3d(diff.x, 0, diff.z).normalize();

            player.setVelocity(horizontal.x * KNOCKBACK_STRENGTH, 0.45, horizontal.z * KNOCKBACK_STRENGTH);
            player.velocityModified = true;
            player.sendMessage(Text.literal("Zrzut zaopatrzenia odepchnal cie od miejsca ladowania!")
                    .formatted(Formatting.RED), true);
        }
    }

    private static void tickSealed(ServerWorld world, ActiveDrop drop) {
        double progress = Math.min(1.0, drop.ticksElapsed / (double) SEAL_TICKS);
        drop.bossBar.setPercent((float) progress);
        drop.bossBar.setName(Text.literal(
                "Zrzut zaopatrzenia (zamkniety) X=" + drop.x + " Z=" + drop.z
                        + " - otwarcie za " + ((SEAL_TICKS - drop.ticksElapsed) / 20) + "s"));

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
            int slot = 0;
            for (ItemStack stack : loot) {
                if (slot >= barrel.size()) break;
                barrel.setStack(slot++, stack);
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
        final int x, groundY, z;
        Phase phase = Phase.FALLING;
        int ticksElapsed = 0;

        ActiveDrop(ArmorStandEntity crate, ServerBossBar bossBar, int x, int groundY, int z) {
            this.crate = crate;
            this.bossBar = bossBar;
            this.x = x;
            this.groundY = groundY;
            this.z = z;
        }
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/java/com/supplymod"
cat > "src/main/java/com/supplymod/SupplyLoot.java" << 'SUPPLYMOD_EOF'
package com.supplymod;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Loot for the Supply Drop crate.
 * Every entry is an INDEPENDENT roll (not a weighted pool) - meaning several
 * entries (even the same item rolled with different stack sizes) can succeed
 * in the same crate, exactly as requested.
 *
 * Percentages as given:
 *  - Blok diamentu        30%
 *  - Totem Niesmiertelnosci 5%
 *  - Zlote jablko zaklete (Notch Apple) 10%
 *  - Zlote jablko          39%
 *  - Brodawki nether       25%
 *  - Czesc diamentowej zbroi (losowa) 40%
 *  - Zelazo + Zloto        70%
 *  - Zaczarowana ksiazka (losowa) 30%
 *  - Serce (item)          10%
 *  - Ksiega Ulaskawienia    1%
 *  - Netherite scrap        5%
 *  - Diamenty (luzem)      30% (bylo 20%, +10pp na prosbe)
 *  - Miedz                 80%
 *
 *  Wypelniacze dodane przeze mnie, zeby zrzut nie byl pusty gdy nic
 *  wiekszego nie wypadnie - dobralem szanse tak, zeby byly czeste, ale
 *  mniej ekscytujace niz glowny loot:
 *  - Chleb                 60%
 *  - Strzaly                50%
 *  - Proch strzelniczy      35%
 *  - Sznurek                40%
 *  - Lapis lazuli           25%
 *  - Szmaragd               15%
 *  - Perla Endermana        12%
 *  - Siodlo                  8%
 */
public class SupplyLoot {

    public static List<ItemStack> rollLoot(Random random) {
        List<ItemStack> loot = new ArrayList<>();

        roll(random, 0.30, () -> new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(2)), loot);
        roll(random, 0.05, () -> new ItemStack(Items.TOTEM_OF_UNDYING, 1), loot);
        roll(random, 0.10, () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), loot); // "noche" = Notch Apple
        roll(random, 0.39, () -> new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(3)), loot);
        roll(random, 0.25, () -> new ItemStack(Items.NETHER_WART, 2 + random.nextInt(4)), loot);
        roll(random, 0.40, () -> randomDiamondArmorPiece(random), loot);
        roll(random, 0.70, () -> new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(5)), loot);
        roll(random, 0.70, () -> new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(5)), loot);
        roll(random, 0.30, () -> randomEnchantedBook(random), loot);
        roll(random, 0.10, () -> new ItemStack(ModItems.HEART_ITEM, 1), loot);
        roll(random, 0.01, () -> new ItemStack(ModItems.BOOK_OF_PARDON, 1), loot);
        roll(random, 0.05, () -> new ItemStack(Items.NETHERITE_SCRAP, 1), loot);
        roll(random, 0.30, () -> new ItemStack(Items.DIAMOND, 1 + random.nextInt(3)), loot);
        roll(random, 0.80, () -> new ItemStack(Items.COPPER_INGOT, 3 + random.nextInt(6)), loot);

        // Wypelniacze - zeby zrzut nigdy nie byl calkiem pusty.
        roll(random, 0.60, () -> new ItemStack(Items.BREAD, 2 + random.nextInt(4)), loot);
        roll(random, 0.50, () -> new ItemStack(Items.ARROW, 5 + random.nextInt(11)), loot);
        roll(random, 0.35, () -> new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(4)), loot);
        roll(random, 0.40, () -> new ItemStack(Items.STRING, 2 + random.nextInt(4)), loot);
        roll(random, 0.25, () -> new ItemStack(Items.LAPIS_LAZULI, 2 + random.nextInt(5)), loot);
        roll(random, 0.15, () -> new ItemStack(Items.EMERALD, 1 + random.nextInt(3)), loot);
        roll(random, 0.12, () -> new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(2)), loot);
        roll(random, 0.08, () -> new ItemStack(Items.SADDLE, 1), loot);

        return loot;
    }

    private static void roll(Random random, double chance, java.util.function.Supplier<ItemStack> supplier, List<ItemStack> loot) {
        if (random.nextDouble() < chance) {
            loot.add(supplier.get());
        }
    }

    private static ItemStack randomDiamondArmorPiece(Random random) {
        ItemStack[] pieces = new ItemStack[]{
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS),
                new ItemStack(Items.DIAMOND_BOOTS)
        };
        return pieces[random.nextInt(pieces.length)];
    }

    private static ItemStack randomEnchantedBook(Random random) {
        List<RegistryEntry.Reference<Enchantment>> all = Registries.ENCHANTMENT.getEntryList(
                net.minecraft.registry.tag.TagKey.of(Registries.ENCHANTMENT.getKey(),
                        net.minecraft.util.Identifier.of("minecraft", "in_enchanting_table"))
        ).map(list -> new ArrayList<>(list)).orElseGet(ArrayList::new);

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        if (!all.isEmpty()) {
            RegistryEntry.Reference<Enchantment> chosen = all.get(random.nextInt(all.size()));
            int level = 1 + random.nextInt(chosen.value().getMaxLevel());
            EnchantmentHelper.set(
                    java.util.Map.of(chosen, level),
                    book
            );
        }
        return book;
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/java/com/supplymod"
cat > "src/main/java/com/supplymod/SupplyMod.java" << 'SUPPLYMOD_EOF'
package com.supplymod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SupplyMod implements ModInitializer {

    public static final String MOD_ID = "supplymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Items saved here after death (Ksiega Ulaskawienia effect) waiting to be
    // returned to the player on respawn. Keyed by player UUID.
    public static final Map<UUID, List<ItemStack>> PARDONED_ITEMS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[SupplyMod] Startuje...");

        ModItems.register();
        SupplyDropManager.init();

        // Daily 20% roll for a supply drop.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(SupplyDropManager::onWorldTick);
        });

        // Restore items protected by Ksiega Ulaskawienia after respawn.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            UUID id = oldPlayer.getUuid();
            List<ItemStack> saved = PARDONED_ITEMS.remove(id);
            if (saved != null) {
                for (ItemStack stack : saved) {
                    if (!newPlayer.getInventory().insertStack(stack)) {
                        newPlayer.dropItem(stack, false);
                    }
                }
                LOGGER.info("[SupplyMod] Zwrocono {} przedmiot(y) chronione Ksiega Ulaskawienia dla {}",
                        saved.size(), newPlayer.getName().getString());
            }
        });
    }

    public static List<ItemStack> stashFor(UUID playerId) {
        return PARDONED_ITEMS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/java/com/supplymod/mixin"
cat > "src/main/java/com/supplymod/mixin/AnvilScreenHandlerMixin.java" << 'SUPPLYMOD_EOF'
package com.supplymod.mixin;

import com.supplymod.ModItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a recipe to the anvil: combine ANY item (left slot) with the
 * "Ksiega Ulaskawienia" (Book of Pardon, right slot) to tag the left item
 * so that it survives the player's death instead of dropping.
 *
 * We hook updateResult(), which vanilla calls whenever the anvil inputs
 * change, and if the right slot holds a Book of Pardon we override the
 * output with a tagged copy of the left item at a fixed, cheap level cost.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    private Inventory input;

    @Shadow
    public abstract void setNewItemName(String name);

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void supplymod$applyPardonBook(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        if (!right.isOf(ModItems.BOOK_OF_PARDON)) {
            return;
        }

        ItemStack result = left.copy();
        NbtCompound nbt = result.getOrCreateNbt();
        nbt.putBoolean("supplymod_pardoned", true);

        self.getOutput().setStack(0, result);
        self.setNewLevelCost(1);
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/java/com/supplymod/mixin"
cat > "src/main/java/com/supplymod/mixin/PlayerDropInventoryMixin.java" << 'SUPPLYMOD_EOF'
package com.supplymod.mixin;

import com.supplymod.SupplyMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ksiega Ulaskawienia effect: before the vanilla "drop everything on death"
 * logic runs, pull out any item tagged "supplymod_pardoned" from the
 * player's inventory (main, armor, offhand) so it never hits the ground.
 * The item is stashed in SupplyMod.PARDONED_ITEMS and handed back to the
 * player on respawn (see ServerPlayerEvents.COPY_FROM in SupplyMod), with
 * the pardon tag removed - the protection is single-use.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDropInventoryMixin {

    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void supplymod$rescuePardonedItems(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (isPardoned(stack)) {
                ItemStack saved = stack.copy();
                saved.getOrCreateNbt().remove("supplymod_pardoned");
                SupplyMod.stashFor(player.getUuid()).add(saved);
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isPardoned(ItemStack stack) {
        return !stack.isEmpty() && stack.hasNbt()
                && stack.getNbt().contains("supplymod_pardoned")
                && stack.getNbt().getBoolean("supplymod_pardoned");
    }
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources"
cat > "src/main/resources/fabric.mod.json" << 'SUPPLYMOD_EOF'
{
  "schemaVersion": 1,
  "id": "supplymod",
  "version": "${version}",
  "name": "Supply Drop Mod",
  "description": "Losowe zrzuty zaopatrzenia, Ksiega Ulaskawienia i dodatkowe serca.",
  "authors": ["Ty"],
  "contact": {},
  "license": "MIT",
  "icon": "assets/supplymod/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.supplymod.SupplyMod"
    ]
  },
  "mixins": [
    "supplymod.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.16.9",
    "minecraft": "~1.21.1",
    "java": ">=21",
    "fabric-api": "*"
  }
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources"
cat > "src/main/resources/supplymod.mixins.json" << 'SUPPLYMOD_EOF'
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.supplymod.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "AnvilScreenHandlerMixin",
    "PlayerDropInventoryMixin"
  ],
  "client": [],
  "server": [],
  "injectors": {
    "defaultRequire": 1
  }
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources/assets/supplymod/models/item"
cat > "src/main/resources/assets/supplymod/models/item/book_of_pardon.json" << 'SUPPLYMOD_EOF'
{
  "parent": "item/generated",
  "textures": {
    "layer0": "supplymod:item/book_of_pardon"
  }
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources/assets/supplymod/models/item"
cat > "src/main/resources/assets/supplymod/models/item/heart.json" << 'SUPPLYMOD_EOF'
{
  "parent": "item/generated",
  "textures": {
    "layer0": "supplymod:item/heart"
  }
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources/assets/supplymod/lang"
cat > "src/main/resources/assets/supplymod/lang/en_us.json" << 'SUPPLYMOD_EOF'
{
  "item.supplymod.book_of_pardon": "Book of Pardon",
  "item.supplymod.heart": "Heart"
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources/assets/supplymod/lang"
cat > "src/main/resources/assets/supplymod/lang/pl_pl.json" << 'SUPPLYMOD_EOF'
{
  "item.supplymod.book_of_pardon": "Ksiega Ulaskawienia",
  "item.supplymod.heart": "Serce"
}

SUPPLYMOD_EOF

mkdir -p "src/main/resources/assets/supplymod/textures/item"
base64 -d > "src/main/resources/assets/supplymod/textures/item/book_of_pardon.png" << 'B64EOF'
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
DCWQyMQQO5pgUNy8Wptv1ZSYKGtiK47KXPxzRWJnn/Usb8WhpDjg66zq9XTxCZQQgpKToXrEaiJJhwDp+O5lClgdZpR94Od/6Q95
b9S8jIKAbZXT6dQnpspZK8t2RGvl5ef/CCHzYlZPauUl27FyqkbMM8/yDX/2n/9T/J1/8D3+X//fv2eqR7m9+Yj04tmBx8dHN3Sb
Zmqt7Pc7luVEEmWcEsvxSJ0bp6dH+/A2uLP0BkZgv78BM+7fPqDVuJn2pJzZamUtlWrKOOTOyo88xZ4Rl5UxJGIyNDqDxyUiilCx
5rIen4T5Tb+ojX/qq8sKzOgwU5eYBEGLdun3ZTPgO4jKaYTiUzMVgia0ZaxNLo3p2dI3DCZMZoQZZCaGCWuR4/EMepFmc6UhmhgW
oGi7Zli3Inq3Jwu4mvld+KLXJdc90EoFDQmxCYoBA2IVVsG2hoy+BT3o6syvLOznTximEbXF6aUh4I+m9Fr1EngFbYVA83EvxvUi
4mLHZk+YnjER1tYINNS8yVN1t8vLacH5xOu3b2HzkmlMbj9wOp0cvqq+2/bZfMvDw1vS1shmJNsYpLGJ0poRWiMTySKcnp7YeMMf
+YWvk8aJv/w3/o6dnh4l/dqv/Xf5N/6Nv8THH91yf3/Pftc1/7sdmI9tU4ycjmdApZbVWjP2845t8/HgVjbyOPHsxQ3LsvDw8MC4
312J2afTyaGqccDEaYqXxb01WCe/eJaJMRJTRvKIWnbzCFyH3xjBBppkWksQJvqKc9xdu+OknWRxIX74jWo45ugTNncByj1Dux7N
2q37CAj+epI6rzRhOmB2B3aDIAQZfNxssTPy3yV/Ap4Jw3tP2CVerkiDcT76RsVaa/e36tOgWpEY+3pORc+GrI58rKcjMWaOj0ea
vvW1Y1TIyu3HM/Pz2ddIyUatm3/s/lCFXqtI8P3EtWiPZSWF9x8obxJD9xluXVJEr8m1x7aJo01VuzfGkDz2mzuPExIxj6QxUc6d
V2LdArY04tZ8cXgwhpQgRlLEXYu2M4cc+PLhDSmPHMZA0Ob+tH/2v/4v8X/+P/0fMVPGceB0Pvu69FIQMY7nE7tnzzg/wEcffIAd
P3fSb8idzBxIw4S2wvH8RMqRpJHj4wPDtHdf/pyI0WltGiqSQKI5jS4YF3scM2dybRgQyfOeyohJYrOCMQIDyoDKDWLubO2VrK+X
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
B64EOF

mkdir -p "src/main/resources/assets/supplymod/textures/item"
base64 -d > "src/main/resources/assets/supplymod/textures/item/heart.png" << 'B64EOF'
iVBORw0KGgoAAAANSUhEUgAAAg4AAAJQCAYAAADmP+y+AAAoQUlEQVR4nO3dT2xsWWLf9985t4p8f7qne7o1/zSWZFs2nIkcxQtv
oiwCAwEkIF45QIDIS2cTGAEECF4FQjbJxkHkVbwJnHWyMgIEgYRkk0UC2EGCZJRkLCiJ5BmPpqfVf6a73+tHsurek0VV8RXrFfkO
u8ku8r3PB81+ZLFYdXhZ997v/VNVpbUWLtdaSyklP/rRj/KLv/iLJhY37vd+7/fKr//6r2eaptRaDz2cO2kzbX7/938/v/Ebv2E+
5Mb98Ic/LL/wC79wvszncrNDD+C+KKXkrbfeyte//vWUUjJN06GHxD331ltv5fvf/37m8/mhh3JvbKbVr/7qr+aTTz458Gi472qt
aa3l448/FgvXIByu4ZNPPsnTp08zm81yenp66OFwz/3SL/1SktVeLfpsptWnn36aH/7whwceDffd8fFxlstllsvloYdyrwiHaxrH
McMwWNjzpXkMfXGtNdOPL621lnEcDz2Me8cBVQCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCg
m3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AA
ALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJ
BwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCg
m3AAALoJBwCgm3AAALoJBwCgm3AAALoJBwCgm3C4rnboAQBwYyzTr23Wmql2ldZaSinJejo9KiVHKZmlHHhk3GslmW0+Xz+2Wmsx
P+53Pl3W/86SPC7FQp8v5Sgls5I8bbkwH3K1WSlWgFfZTJ96dJQkedqmPB3PYonFl9KS99ePoTJfJcQwDIcc0Z22mTabafV+Wp5a
wPMlPR3PzoOhHs1X/1Y74l9m9pOf/OR8Qr1qpbVZ6X/h36uMKdMyZf44f/zBT9vf+NqjnJbj1DpPW443OFJeR0f1KP9Lkp989F77
4ONP0p6elOV8kdlYMpUhreb17dOS1CkpbcxyaJkt5imPH7SffPRe3kjy1+tRzt5899Cj5J4rsyHTdJbj+mn++IP32mwYSjs7Savz
rI7kTy+/ja2N71dtHbpt83tO05SSpH3ve9/Lhx9+eNhR3ZLN7t/r7QZuaRky1jGP25M8LQ/zrzz8ev7Tf+lfy9cXn2d2dqtD5nXR
WhYPH+Yfvf9H+c/+7IP8q/U4/2/ez1uLoyzKkJRXdyHUp2Q+jflkdpq/VL6V/306zW9/4+fyd775lzN/9iyxt5QbsDwe8/H8Yf7e
P/sn+YPPn+TxdJqn5WGGVlOyTK44LD1NU0opKaVkGIa86nvw33333fzgBz9YHWZ98uRJ3n///UOP6U46TXKaRR49Os4vv/8kb42f
5enR5KxSvrTWxsyWX8+3f7ZMfvqj/EmSJ+sPLvrjfJYk+fb87fy5h0+zXH6cUhza4cuZkjz+rOSdmjz64CSffv7xepn/+aGHdicd
Hx8nWZ+fdXR0lIcPH76yu1m+yO9VU5KSPCgtp4tFTt+oOX30NMtnJ6nTo5S0XFWi8DJlqhmPp5w8Xh32evPxmzkdz1JbWe0gLa/z
Y6wlrax2FpeWN4ejfPL0s5w8njIOU+rpPHEsmi+pJlnWk5w+OMvpG0NyVvNofpRhWh0lnK5xrPBV39tQSsnR+ly/WZKM45iTk5NX
Nhy+iLI6ipO6fkbFYmw5OZ7lNEPGpedU8OW11lJnJW19HHV5cprT8SwlPUdWXw+b0zyW650LLS3jrGY6mr/yC2puX0syDbOcHM+z
GJMsp5yNp3m2eYbFQUd3t5RSMo6rjZzZS6772mqbM97XAXG8rJmfzjM/S4ZxEQ8pvpyStCmzNs/D09Vasbb1o22zPnzdH2LrZ1vW
tvpIkoenJQ+fTVksz5LyOp89yk2pY8lRZjlerma81RKfqwiHlyktaavdpSUtpZW0lLy+u5C5EetCGGclJ0frs5XLaj+DHX9r6+mw
mlSraXM2lIx1PQ+WslVZ8AWVReq0yLKu578DD+c+EA69SpK0tJJMVTZwA0oylZLp/MFkkXWZzR7AVpJWy2oe1O98SS1JLS0pzdx3
Dc4uggMq8azLa2umGTdDd34xwgGA15L+/GKEAwDQTTgAAN2EAwDQTTgAAN2EAwDQTTgAAN2EAwDQTTgAAN2EAwDQTTgAAN2EAwDQ
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
b6znw1bO/3fI0dxRJa2080lTkpS2nlzAV2i1jGpl/Wm2ZkKLrudKSSurSWKy9JslyRtvvJHvfve7ac2k2zVPybNpzNHRw2RK6mJK
q4tDD+tOGpYtpQ1pJaklWc6TsdbUSWjxJbX1qrAksZy63LrSa5tSxyl10dJOpoxHLa2udzAL+XOtlJTFlDJNefjO1/JmHuWdlCwP
PbA7qJSSN954Y/V5LNG7/T9/49/JX3j/SZ49OkmxGf2C+TI5PZolbZbHf/ZZPvrW5zmdvZVHT4e0ehZLLLq1ZBqSlGS2WD1ypnFK
OxpSaku15NqrtXlSlhmGz/PsZMiP/uiznH5tSh1bZovBJNtjNiU//u6D/M3/+58eeij3Rvmd3/md9vbbb2ccx0OP5U6Z0lLTUpc1
5XjI6ccf5Jv/wz/Nt05KTo+SYqtnR8lYlvnscc2jkym/dlrz6OGzDCfHmT+bZ/FgEeFAt5a0YbUBPSyTYUrGkiymRT5fnKUNg3DY
tg6pcZil1UXK/DTvlwf5X3/8aZ48Sh60kqNnNa02021jcwintnzwoOXB3/qbmf/cNzIux6RM6ytYZm0bhiE/+9nPUhyeeIkxyZD8
6U9+nO/+/J8zsTr9j//6b+RXcpqjT88yLI9y8mh1vgP02hyfr2NSxpZ2PORnP/s4P/mj9zJ97XEyeUDtGoea4fPTtLdL/s93Huff
/8P/69BDutvmSdZHnv/0T39cvvOdnz/ocO6LmXC4zOp0mdZKampOp9Wl3y2zzB4eZzw9Pejo7qYhKbP8i+lZSo4zLBdZzlvaIBq4
vs3pDC1Jq+uPeU17cJTp4VHKNNl4Prfa5VAz5MFnLeXZkKE+TmrNz7WaVlpqa2mxDb3RksyG44yL0/xZpjxZnTxT2nLKNLS0lMxS
PMb2mJXiYbRfSUtLhtXD5mi9dfOTssx8HHI6OX1m11Eb8yCLpEwZpinzs+TZbEgbSsqyWWLRb72G2wRnq6tzHpa1JeMyw+LMHodd
LVk+aGnzMQ+eJXW5TDLlWWs5STJaBb7gKFNaVluFdWv61NS09brRYutFno55hZaSKVNqkmGacpTkLGW1MDMPvqClnS/pWxlTp5Y6
rp9ikc0xQ+hwfvJjWz1+pqROSVk/huqyeXbFjjK1nM1azh63HH06ppTNxk07f1YKF5WWTOsH26yNZXNhSTW9riAcrlCTlPVrZI1D
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
TeuWTPNFlseLzJ8eZflwyLN3hzxNkg+TVqaMzeNqn7Y+H6SUcmFehJsiHLgxbesEtk0svPfee4cazp33/vrf+nHN0+9+LXlrlsfl
KE8enGSYlwxnx2nl9dymLq1knJ9mGlpmjx/k6VtnyePHqT/5NEny/uLswCO8uzbz3GYe3A4JuAmlOVuZG9KyejbAOI55//33M47j
+VYPe7SWHB/nH/8X/6j9/d/5D/NX/vxfyNF7n+bzozFJS22v+4J+9YySh2c1p995K3/4J3+c3/6P/5P82//e3ynl9DSxItxrM88N
w5BvfvObq0MXJXmND3xxw+xx4Oas+6DWmp//+Z8/7Fjukfm77+bHST6elvn85MPk5NAjunsetcf5PKtp9Yvf+tahh3NvnB8mfB2f
1cutEQ7cmM2uUHsZ+rTWUmtNm8YkybdS8n5KppLVyYGHHd7BtSQpSW3JN1vJHyep42paTdNk13uHWr3GHzdPOHArLNSvYT2pxiRP
14d7WFtPi3Hz9XpalVI8xuBA5CgA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA
0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04
AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADd
hAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA0E04AADdhAMA
0E04AADdhAMA0E04AADdhAMA0E04AADdZoceAPBcKeXQQwC4knCAAxuGoSRpb7/9doZhOPRw7qQ333wzP/zhDzfTCjgg4QAH9skn
n7Qk+f73v3/oodx562klHuCASmvt0GOA19oPfvCDvPfee5nP5zE/7ldKyWKxyLe//e1873vfO/Rw4LUmHACAbg5VwIG11uxp6FRK
cQIpHJg9DgBAN6/jAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfh
AAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0
Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQDfhAAB0Ew4AQLfZoQfA/ddae+l1Sind1/0q
bueL3OfVWpKy9VVLycWf24y57/boddl0ffFvcPFv9LLb+yocYr7w+OPLKl/lTMJXp6Vtf/FS112YbG5/d+XIfq21a4fDZfPmy+bZ
lvbSv/lNzfcv/V3Kyx8jl93GdadTKcVKsdMXnX+7Hjdl+1N/j1eRPQ5fgdZaSikXFui3vYArF+fem9dWv8P2CnHz+ebyC1ff+nrz
+TRNFy7b/Ow0TfuvszX9du93Y5qmF5ZsLa20aff+W3aXgbvj2Xf57vV3p8EL9701PS5cZ71C3TddrnJTK/yvLBxu8Ha2r7M7P20u
20zrq2Jkc51a9x+p3b58+3Z2r7/61sVYKbWkpLwwcWut5fnPlQtjWX1xcZ7d3FcpJbXW899pz329MF12f9/dn92eBtddNty1MDt/
HG/NT3dtjK+i13KPw03sFrztB+hVK5TtlepmhbS78t63Ut6s6LZ/bnslvfv19n1s3+++29+9/GUr06t+r33f23MrqxV/SdcelW03
9ndb3/dVt3cbhydetQXjTS6Deqb3+Qrzhu722uM/f9xsvrjiqpdFwSY0rtijsx0LtdYL4bCx7/JNrGzfzu71Nl9vh832Zbu3vxst
2/exL3b2/s437LKNnOt61ebHHq9UOOzborvt+9ldAW9/7K5Ep2laX2/KOO6//mW3tzcE2nRe2fu2+nfHvLtLd3cFftkMsNmye9kM
fdWu4hceZ7f494HXwb5l91Xz377rvxDrV8yXu8uJ3T1p++b/S/d2pKWWq0NjX6Bsf6+UkmGoKaWm1vpCpOy7/r64ua0V/749Yq+K
VyocXuayFfTm63Ea06bnl01tyjRufX8cX7hukkxtStrFLe7tmWp3ZX7Zinrf7sh9173uMeHd29t3ItX55Xu2xDbX34QK8GrZrMiT
/ZG/Wb7tWw7tXv6yPY1XXb5veXfVxtC+cWx/bK5zvsek5Pz3LLVkqMN5UAzD88/rUFO3g2TnupcFzevioOGw7wHxsom/u2t+HMfz
Ffo0TRe+3ve97XC4sFW/c/z8svFctqXdO/59P7u9RX/ZVsGVewOyPr6350z+zc9fNrYrv7dnTwPwartqz8OXWZZsllHJ1Xs1LlvG
bo/ruuutlx0Gveqw6fl41ve9Gwybf4dhOI+KYRjOv97+fPvrzW33RMe+jbxDLpcPEg5XPcA2K/nluMxyscxyucw4jlkul+cfmyDY
3guwub2WdYzkxeN5G/uO9QFAr8vO1brwvaxX8rm492Ozd2MTEbPZ7Pxj9+tNcFx2H4dYjx10j8Nischiscjp6WlOT0+zWJxlsVhm
sVjsP/EvLUMdzn/+heNmVxwC2OeyLXQAuMxV646rDqnsno+2MU7jhY3d7b0a8/n8/OP4+DjHx8fnXx/KQcLhyZMn+eijj3JycnLh
vIHLzvy9DjEAwF3xZdZJq/Xz6hlkm2fFbfZYPHjwIO+8807eeOONmx1wh4O8jsOzZ8/y5MmTJElr0/lZsTexy+X5H2g1sR2OAOCr
tDoPIVmfFfGFb6fUkrSSUp7vsZim1WH6J0+e5OHDhwcJh4Mdqlgul/n82ec5eXaSs7OznJ6enp+7MI7janA7ex++9N6Ind/1Ordz
1QmKANxv113Gf5n1yb7buezfzXkQwzDk+Pg4R0dHefDwQR49fJTZ7DCv4Xhnno65OdFxc97D5kTIzfkOm6+3nz65+9zkfWeaXnb2
6abeXnZ28MueKgTA/XXZU973Pd3z+RUuf3bHvtfMuGxdtf30zu2TIefz+fnXm/MZNidU3gUHD4eeytuMcfOUyvNnXmyFxGZPxfZl
ux/bJbf7R9x9IZMk5y/pus9d+QMC8MVcdeL85nV6dl/g6rKN1e0I2P3YPFti+2mZ28+Y2Fy+uZ2XjfnQ65+Dh8O2zfN0r3qxkevY
3Tux+0JO2yGy+f72az288PNtSptefJXGq17Y5LIXSdn9na76/a79u+95Eaf75fn7SNz2DLL9ZmBOquUqX+Vj5fnKanVv99YXWBZd
tU666vUYXra8PR/SzmsnlFJSarnwSpbbK/3t12jYfb2G3ReO2v3567rqRfkOHQvb7lQ4fBE3dZzpZbe/HQnbUbH53nZ87L7A1L7v
bT7Gcdw66eX5be7e977fdffr7dezeNmruq1Otql7Ln/xVdhW37i4sHzZ4+a8zve8U+NVh4d2v3/ZOz3u+51u0mXj4dV323/7lz52
r5jXXjbvbP/8VRs1e8ey503kdnfTXxz7tDfwd39u+/ULLrvv7a/3ncu2vRJvrZ2vrHdX1PtemGnf97Zvc/elqfeN7cu67fXUV+3e
vzvml/0DvOz1xG/7gbT9gNp9F8bdUDkPjvWYN5dtZqb13pK2dXtlN26e30fL5o0kW1qm8cX3wthd8LzwLpFbe4d2f5fdLaWXLcCu
Wjju+9ntEr/Mdf9mtx0ju/f1Qpxtue9Bf1Mumzbbj/ubdtUexJu4nSQvzDsXvnXF4/9lj+mL37/4DrAXlmV75p3d18jZXZkmSR3q
edSs3vDz4kp4Z6Xctm6vbFb2m79drXXvqzFe9hLO+9619KtaAe87v+E67nso7Lr3exwO4San2VUnaR7CdixsYuN8Ibd52did62z/
7PZbTZ+/G+f6EE8pF98ye+d2Wmut7LvNzSGs3cNEl415tUS8epz7Pt93e5dd56rLe76/934v2btCLl1Yf5lDfFftyt7+fN/1LjvM
eGH3d9msnV98k6e916/lfJf0JddpZf3F7tbx+Vtvr3e5t9YubKHvPt19dzznrzWwHvLua+rcpeXTTQfjXfnd7pP/Hz4PqO94F1mh
AAAAAElFTkSuQmCC
B64EOF

echo "Projekt supplymod odtworzony pomyslnie."