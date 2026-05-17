# The Guard — Mod Development Log

## Project Overview
A Minecraft Fabric mod (for MC 26.1.2) that adds **Guard** mobs to protect villages.
Guards look like armored villagers, target hostile mobs and players who attack villagers, and come in
multiple visual variants each capable of spawning with any of five weapon types.

Use `/give @p village-guard:guard_spawn_egg` in a world with cheats enabled, or search "guard" in the creative Spawn Eggs tab. Right-click ground to spawn. Natural village spawning is active in all 5 vanilla village biomes.

**Testing:** Run `./gradlew runClient` from the `The-Guard/` folder. Create a new world with **Allow Cheats ON** (or Creative mode). The mod is confirmed loading in-game as of 2026-04-29.

---

## Current Status: Feature Complete (Session 6) — Villager Face Alignment, Raised 3D Nose, Final Egg Cleanup ✓

### Environment
- **Minecraft Version:** 26.1.2
- **Mod Loader:** Fabric (loader 0.19.2)
- **Fabric API:** 0.146.1+26.1.2
- **Java Version:** 25
- **Build System:** Gradle with Fabric Loom 1.16-SNAPSHOT
- **Mod ID:** `village-guard`
- **Package:** `dev.icrabtree.villageguard`

---

## File Map

```
src/
  main/java/dev/icrabtree/villageguard/
    VillageGuard.java                       ← ModInitializer, registers entity + attributes
    init/
      ModEntities.java                      ← EntityType<GuardEntity> registration
      ModItems.java                         ← guard_spawn_egg item registration
    entity/
      goal/
        BlowHornGoal.java                   ← periodic goat horn during combat
    entity/
      GuardEntity.java                      ← core entity (AI goals, armor pickup, ranged/melee)
      GuardVariant.java                     ← enum: SCOUT / SOLDIER / CAPTAIN
      GuardWeapon.java                      ← enum: SWORD / AXE / SPEAR / BOW / CROSSBOW
          GuardRetaliateGoal.java          ← targets players who hurt nearby villagers
    mixin/
      HostileMobTargetGuardMixin.java     ← hostile mobs proactively target guards
      IronGolemMixin.java                 ← iron golems never target guards
  main/resources/
    fabric.mod.json
    village-guard.mixins.json
    assets/village-guard/textures/entity/
      guard_scout.png    ← brown leather gambeson ranger skin (64×64 player-skin UV, villager-tone head, raised eyes/brow, 4-pixel mouth #774235)
      guard_soldier.png  ← cream linen gambeson standard-guard skin (villager-tone head, raised eyes/brow, 4-pixel mouth #774235)
      guard_captain.png  ← deep navy surcoat + gold-trim captain skin (villager-tone head, raised eyes/brow, 4-pixel mouth #774235)
    assets/village-guard/textures/item/
      guard_spawn_egg.png ← villager-based egg with steel lower side thirds only on the bottom two rows
  client/java/dev/icrabtree/villageguard/client/
    VillageGuardClient.java                 ← ClientModInitializer, registers renderer + layers
    model/
      GuardModel.java                       ← HumanoidModel<GuardRenderState> + nose ModelPart child
      GuardModelLayers.java                 ← ModelLayerLocation constants + ArmorModelSet
    renderer/
      GuardRenderer.java                    ← HumanoidMobRenderer, variant texture swap
      GuardRenderState.java                 ← render state carrying GuardVariant
  client/resources/
    village-guard.client.mixins.json
```

---

## Design — Full Reference

### Guard Variants (visual)
Each variant controls starting armor and texture. Any variant can spawn with any weapon.

| Variant | Armor | Texture file |
|---|---|---|
| `SCOUT` | None | `guard_scout.png` |
| `SOLDIER` | Chainmail chest + legs | `guard_soldier.png` |
| `CAPTAIN` | Full iron set | `guard_captain.png` |

### Guard Weapons (behavioral)
Chosen randomly at spawn. Controls which AI combat goal registers.

| Weapon | Type | AI Goal used |
|---|---|---|
| `SWORD` | Melee | `MeleeAttackGoal` |
| `AXE` | Melee | `MeleeAttackGoal` |
| `SPEAR` | Melee (long-reach) | `MeleeAttackGoal` — uses iron sword item for now |
| `BOW` | Ranged | `RangedAttackGoal` |
| `CROSSBOW` | Ranged | `RangedAttackGoal` |

### Base Stats
| Stat | Value |
|---|---|
| Max Health | 20 HP (same as vanilla villager) |
| Move Speed | 0.35 (same as pillager) |
| Attack Damage | 6.0 (melee base; raised from 3.0) |
| Arrow Base Damage | 4.0 |
| Ranged Fire Rate | 100 ticks (5 s fixed) |
| Follow Range | 32 blocks |
| Armor | 2.0 (base, increases with equipped armor) |

### `GuardEntity` — Key Behaviors
- **Variant + weapon** stored as `SynchedEntityData<Integer>` (client-synced)
- **`finalizeSpawn`** picks random variant + weapon, equips weapon item, calls `equipRandomArmor()`; sets 200-block patrol home; on NATURAL spawn spawns 5–8 companions and enforces ≤8 guards per 200-block area
- **`setupCombatGoal()`** called once at spawn and on NBT reload — adds ranged or melee goal; guarded by `combatGoalAdded` flag to prevent duplicates
- **Target priority:**
  1. `HurtByTargetGoal(this, GuardEntity.class)` — retaliates if attacked, but **never** retaliates against other guards
  2. `GuardRetaliateGoal` — scans nearby `AbstractVillager` entities, targets any `Player` that recently hurt one
  3. `NearestAttackableTargetGoal<Monster>` — proactively hunts all `Monster` subclasses (zombies, skeletons, illagers, creepers, etc.)
- **`HostileMobTargetGuardMixin`** — injects `NearestAttackableTargetGoal<GuardEntity>` into hostile mob `registerGoals()` so enemies proactively target guards (zombies, skeletons, illagers, witches, ravagers, creepers, spiders, phantoms, breezes)
- **`IronGolemMixin`** — cancels `setTarget()` in `IronGolem` when target is a `GuardEntity`; iron golems no longer attack guards
- **Swarming** — `hurtServer()` override: when a non-creative player damages a guard, all guards within 64 blocks that have no current target acquire that player as a target (pigman-style alert)
- **`canAttack()` override** — returns `false` if target is `GuardEntity`; guards never attack each other even across goals
- **Goat horn on aggro** — `setTarget()` override plays the **Ponder** goat horn variant (index 0) when a guard acquires a first target
- **`BlowHornGoal`** (priority 2) — re-blows Ponder horn every 60–120 s (1200–2400 ticks) while a target is held
- **Ranged attack** — uses `ProjectileUtil.getMobArrow`; vertical aim corrected via `target.getY(0.333)` + distance compensation; spread scales with difficulty (14 − difficultyId × 4); fixed 5 s fire rate via `RangedAttackGoal(this, 1.0, 100, 100, 15.0f)`; arrow base damage 4.0
- **Armor randomization** — `equipRandomArmor()`: each slot has 40% chance to be bare; otherwise `buildArmorPiece()` picks: 1/100,000 diamond, else 55% leather / 20% gold / 14% chainmail / 11% iron. Pre-worn to 15–85% durability
- **Weapon randomization** — `buildWeaponStack()`: 55% wood / 20% gold / 14% stone / 11% iron sword or axe. Pre-worn to 15–85% durability
- **Patrol** — `MoveTowardsRestrictionGoal` + `setHomeTo(spawnPos, 200)` keeps guards within 200 blocks; right-clicking any guard with a banner relocates the entire patrol's center
- **Armor pickup (upgrade-aware)** — `pickUpItem(ServerLevel, ItemEntity)`: equips armor if slot empty; upgrades if dropped piece is a higher tier (leather=1 → gold=2 → chainmail=3 → iron=4 → diamond=5 → netherite=6); old piece is dropped via `spawnAtLocation`
- **Weapon pickup (upgrade-aware)** — same tier system for weapons (wood/gold=1, stone=2, iron=3, diamond=4, netherite=5, bow=3, crossbow=4); calls `rebuildCombatGoal()` if switching between melee and ranged
- **Death loot** — player-given armor always drops; spawned gear drops with `0.01 + 0.075 × damageRatio` chance (≈1% fresh → ≈8.5% heavily worn)
- **Save/load** — uses MC 26.x `ValueOutput` / `ValueInput`; saves patrol center, weapon, variant, and `givenByPlayer` bitmask

### `GuardRetaliateGoal`
- Each tick (with a 60-tick cooldown between scans) searches for `AbstractVillager` entities within `scanRadius` blocks
- Reads `villager.getLastHurtByMob()` — if the attacker is a non-creative `Player`, marks them as the guard's target
- No event bus needed — piggybacks on the vanilla `lastHurtByMob` field

### `GuardRenderer`
- Extends `HumanoidMobRenderer<GuardEntity, GuardRenderState, GuardModel>`
- `createRenderState()` returns a `GuardRenderState`
- `extractRenderState()` copies `entity.getVariant()` into the render state
- `getTextureLocation(GuardRenderState)` selects texture from a `Map<GuardVariant, Identifier>`
- `HumanoidArmorLayer` added using `ArmorModelSet.bake()` with `GuardModelLayers.GUARD_ARMOR_SET`

---

## Build Notes & Lessons Learned — MC 26.1.2 + Fabric

### Package renames (26.x vs older tutorials)
| Old class | New location in 26.x |
|---|---|
| `ResourceLocation` | `net.minecraft.resources.Identifier` |
| `MobSpawnType` | `net.minecraft.world.entity.EntitySpawnReason` |
| `net.minecraft.world.entity.monster.Zombie` | `net.minecraft.world.entity.monster.zombie.Zombie` |
| `net.minecraft.world.entity.monster.Skeleton` | `net.minecraft.world.entity.monster.skeleton.Skeleton` |
| `AbstractIllager` | `net.minecraft.world.entity.monster.illager.AbstractIllager` |
| `AbstractVillager` | `net.minecraft.world.entity.npc.villager.AbstractVillager` |
| `net.minecraft.world.entity.projectile.Arrow` | `net.minecraft.world.entity.projectile.arrow.Arrow` |

### Armor detection
`ArmorItem` no longer exists as a standalone class. Use:
```java
Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
if (equippable != null && equippable.slot().isArmor()) {
    EquipmentSlot slot = equippable.slot();
    // ...
}
```

### Entity save/load
`CompoundTag` parameters replaced with `ValueInput` / `ValueOutput`:
```java
@Override
protected void addAdditionalSaveData(ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putInt("GuardVariant", getVariant().id);
}

@Override
protected void readAdditionalSaveData(ValueInput input) {
    super.readAdditionalSaveData(input);
    setVariant(GuardVariant.byId(input.getIntOr("GuardVariant", GuardVariant.SCOUT.id)));
}
```

### Ranged attack goals
- `RangedBowAttackGoal<T extends Monster>` — **cannot be used** on non-Monster mobs (compile error)
- `RangedCrossbowAttackGoal<T extends Monster>` — same restriction
- Use `RangedAttackGoal` (no Monster bound) for guards. Both bow and crossbow variants use it.

### Renderer — render state architecture
MC 26.x renderers are 3-type-parameter: `EntityRenderer<T, S, M>` where `S` is a render state class.
`HumanoidMobRenderer` is the correct base for humanoid mobs:
```java
public class GuardRenderer
    extends HumanoidMobRenderer<GuardEntity, GuardRenderState, GuardModel> {

    @Override public GuardRenderState createRenderState() { ... }
    @Override public void extractRenderState(GuardEntity, GuardRenderState, float) { ... }
    @Override public Identifier getTextureLocation(GuardRenderState state) { ... }
}
```

### Armor model layer setup (26.x)
Old two-model `HumanoidArmorLayer` constructor removed. Use `ArmorModelSet`.
Each armor slot needs a **separate inflated layer** or it renders flush with (invisible on) the body:
- Outer slots (HEAD, CHEST, FEET): `CubeDeformation(1.0f)` — matches vanilla `OUTER_ARMOR_DEFORMATION`
- Inner slot (LEGS): `CubeDeformation(0.5f)` — matches vanilla `INNER_ARMOR_DEFORMATION`
- Armor texture size is **64×32**, body texture is 64×64

```java
// GuardModel.java
public static LayerDefinition createOuterArmorLayer() {
    return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0f), 0.0f), 64, 32);
}
public static LayerDefinition createInnerArmorLayer() {
    return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5f), 0.0f), 64, 32);
}

// GuardModelLayers.java
public static final ModelLayerLocation GUARD_ARMOR_OUTER = new ModelLayerLocation(GUARD_ID, "armor_outer");
public static final ModelLayerLocation GUARD_ARMOR_INNER = new ModelLayerLocation(GUARD_ID, "armor_inner");
public static final ArmorModelSet<ModelLayerLocation> GUARD_ARMOR_SET =
    new ArmorModelSet<>(GUARD_ARMOR_OUTER, GUARD_ARMOR_OUTER, GUARD_ARMOR_INNER, GUARD_ARMOR_OUTER);
// ArmorModelSet(head, chest, legs, feet)

// VillageGuardClient.java — onInitializeClient()
ModelLayerRegistry.registerModelLayer(GuardModelLayers.GUARD_ARMOR_OUTER, GuardModel::createOuterArmorLayer);
ModelLayerRegistry.registerModelLayer(GuardModelLayers.GUARD_ARMOR_INNER, GuardModel::createInnerArmorLayer);

// GuardRenderer.java constructor
ArmorModelSet<GuardModel> armorModels = ArmorModelSet.bake(
    GuardModelLayers.GUARD_ARMOR_SET,
    context.getModelSet(),
    GuardModel::new
);
this.addLayer(new HumanoidArmorLayer<>(this, armorModels, context.getEquipmentRenderer()));
```

### EntityType registration (26.x)
`EntityType.Builder.build()` now requires a `ResourceKey`:
```java
private static final ResourceKey<EntityType<?>> GUARD_KEY = ResourceKey.create(
    Registries.ENTITY_TYPE,
    Identifier.fromNamespaceAndPath(MOD_ID, "guard")
);

public static final EntityType<GuardEntity> GUARD = Registry.register(
    BuiltInRegistries.ENTITY_TYPE,
    GUARD_KEY,
    EntityType.Builder.of(GuardEntity::new, MobCategory.CREATURE)
        .sized(0.6f, 1.95f)
        .build(GUARD_KEY)
);
```

### Fabric API renames
| Old | New |
|---|---|
| `EntityRenderers.register(...)` | `EntityRendererRegistry.register(...)` |
| `EntityModelLayerRegistry.registerModelLayer(...)` | `ModelLayerRegistry.registerModelLayer(...)` |

### Spawn egg registration (26.x)
`Item.Properties` **must** have `.setId(ResourceKey<Item>)` called before `SpawnEggItem` construction — omitting it causes `NullPointerException: Item id not set` at startup.

Entity type is set via `DataComponents.ENTITY_DATA` + `TypedEntityData.of(entityType, new CompoundTag())`:
```java
private static final ResourceKey<Item> GUARD_SPAWN_EGG_KEY = ResourceKey.create(
    Registries.ITEM,
    Identifier.fromNamespaceAndPath(MOD_ID, "guard_spawn_egg")
);
public static final Item GUARD_SPAWN_EGG = Registry.register(
    BuiltInRegistries.ITEM,
    GUARD_SPAWN_EGG_KEY,
    new SpawnEggItem(new Item.Properties()
        .setId(GUARD_SPAWN_EGG_KEY)
        .component(DataComponents.ENTITY_DATA,
            TypedEntityData.of(ModEntities.GUARD, new CompoundTag())))
);

// VillageGuard.java — onInitialize(), after ModEntities.register()
ModItems.register();

// add to creative Spawn Eggs tab (optional)
ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
    .register(entries -> entries.accept(ModItems.GUARD_SPAWN_EGG));
```

---

## Planned Feature Roadmap

### Session 1 — ✓ Complete
- [x] Project scaffold compiles and builds
- [x] `GuardEntity` with melee + ranged AI (weapon-conditional)
- [x] 3 visual variants (Scout / Soldier / Captain)
- [x] 5 weapon types (Sword / Axe / Spear / Bow / Crossbow)
- [x] Armor pickup when armor is thrown at the guard
- [x] `GuardRetaliateGoal` — targets players who hurt villagers
- [x] Variant-based texture renderer with armor layer

### Session 2 — ✓ Complete
- [x] `guard_spawn_egg` — creative Spawn Eggs tab; `/give @p village-guard:guard_spawn_egg`
- [x] Goat horn on combat entry + `BlowHornGoal` for periodic horn during combat
- [x] Tiered death loot (given armor always drops; spawned gear 15% / 5% / 1%)
- [x] 200-block patrol radius with `MoveTowardsRestrictionGoal`
- [x] Banner right-click to relocate entire patrol center
- [x] Natural village spawning in all 5 biome types (plains, savanna, desert, snowy, taiga)
- [x] Group spawn: 5–8 guards per patrol, max 8 per village area
- [x] `runs { client }` block in `build.gradle` — `./gradlew runClient` works from The-Guard/
- [x] Confirmed loading in-game (village-guard 1.0.0 shown in mod list)

### Session 3 — ✓ Complete
- [x] **Friendly fire fix** — `HurtByTargetGoal(this, GuardEntity.class)` prevents guards retaliating against each other
- [x] **Speed fix** — movement speed corrected to 0.35 (pillager speed)
- [x] **Fire rate fix** — 5-arg `RangedAttackGoal(this, 1.0, 20, 40, 15.0f)` → 1–2 s random interval
- [x] **Ranged aiming fix** — `ProjectileUtil.getMobArrow` + `target.getY(0.333)` + difficulty-scaled spread
- [x] **Target acquisition** — `NearestAttackableTargetGoal<Monster>` covers all hostile mobs
- [x] **`HostileMobTargetGuardMixin`** — hostile mobs (zombie, skeleton, illager, witch, ravager, creeper, spider, phantom, breeze) proactively target guards
- [x] **Armor randomization** — per-slot 40% bare chance; buildArmorPiece: 1/100k diamond, 55% leather, 20% gold, 14% chainmail, 11% iron; pre-worn 15–85%
- [x] **Weapon tiers** — 55% wood, 20% gold, 14% stone, 11% iron; pre-worn
- [x] **Death drops** — durability-ratio-based chance (1% fresh → 8.5% heavily worn)
- [x] **Upgrade-aware pickup** — guards equip better armor/weapons and drop old piece; guards accept Netherite
- [x] **Horn** — locked to Ponder variant (index 0); 60–120 s interval
- [x] **Armor rendering fix** — separate OUTER (1.0f) and INNER (0.5f) `CubeDeformation` layers; armor now visually appears on guards
- [x] **Textures created (first pass)** — `guard_scout.png`, `guard_soldier.png`, `guard_captain.png`, all 64×64 player-skin UV format (rough first pass)

### Session 4 — ✓ Complete
- [x] **Combat overhaul** — melee damage raised to 6.0; arrow base damage 4.0; fire rate fixed to 100 ticks (5 s)
- [x] **Swarming behavior** — `hurtServer()` alerts all guards within 64 blocks to target the attacker (pigman-style)
- [x] **Friendly fire fix (guards)** — `canAttack()` override prevents guards from ever targeting other guards
- [x] **Iron golem fix** — `IronGolemMixin` cancels golem targeting when target is a `GuardEntity`
- [x] **Villager-accurate textures (first pass)** — head copied pixel-perfect from `villager.png`; clothing per variant; spawn egg assets created
- [x] **Spawn egg assets** — `items/guard_spawn_egg.json`, `models/item/guard_spawn_egg.json`, `textures/item/guard_spawn_egg.png`, `lang/en_us.json`

### Session 5 — ✓ Complete
- [x] **3D villager nose** — `GuardModel.createBodyLayer()` adds a `nose` child to the `head` part via `addOrReplaceChild()`; initial custom nose support landed with dedicated nose UV pixels in all three textures
- [x] **Improved clothing** — scout: brown leather with tan stitching, shoulder straps, back pouch; soldier: grey robe with chainmail-dither on arms + silver chest badge + leather belt; captain: navy with gold collar/cuffs/belt buckle/epaulettes/rank star + piping
- [x] **Spawn egg polish** — upper egg body (y<10) uses original `villager_spawn_egg.png` warm skin tones unchanged; lower shadow (y≥10) remapped to dark steel grey

### Session 6 — ✓ Complete
- [x] **Raised villager-sized nose** — `GuardModel.createBodyLayer()` now uses `addBox(-1.0, -3, -6, 2, 4, 2)` so the nose keeps the larger villager silhouette and sits one pixel higher on the face
- [x] **Villager face alignment pass** — all three guard textures now use villager skin tones, a single-tone villager nose UV, one raised eye row, and a 4-pixel mouth (`#774235`) placed one row lower behind the 3D nose
- [x] **Final egg polish (complete)** — `guard_spawn_egg.png` has a dark steel frame around the lower portion: y=14 (bottom) = 6 steel pixels, y=13 = all 8 steel pixels, y=12 = 3 left + 3 right steel (4 center open), y=11 = 4 left + 4 right steel (4 center open), y=10 = 2 left + 2 right steel; the warm villager nose and upper egg remain intact

### Session 7 — ✓ Complete
- [x] **Village wave spawning** — `VillageWaveSpawner` (server tick event, not a mixin) checks every 24000 ticks whether a village has zero living guards within 128 blocks; if so it spawns a fresh wave of 5–8 guards centred on the nearest village bell. Villages must be fully cleared before a new wave arrives (pillager-outpost parity). Guard waves do not affect villager breeding or panic states.
- [x] **Guard Banner item (first pass)** — `GuardBannerItem` crafted from **any banner** surrounded by **8 emerald blocks** (shapeless), with linked guard spawn/despawn behavior implemented as the initial prototype.
- [x] **File additions** — `item/GuardBannerItem.java`, `init/ModItems.java` (adds `GUARD_BANNER`), recipe `data/village-guard/recipe/guard_banner.json`, `assets/village-guard/items/guard_banner.json`, `assets/village-guard/models/item/guard_banner.json`, `lang/en_us.json` entry

### Session 8 — ✓ Complete
- [x] **Banner placement behavior fixed** — `GuardBannerItem` now extends `BannerItem` and calls `super.useOn(ctx)` first, so it places exactly like a normal Minecraft banner (no more spawn-egg interaction behavior).
- [x] **Banner visual fixed** — `assets/village-guard/items/guard_banner.json` now points to `minecraft:item/white_banner`, so the item renders as a normal vanilla banner model while still showing foil glint.
- [x] **Guard spawn timing adjusted** — guard spawn/despawn logic now runs only after a successful banner placement action.
- [x] **Known limitation documented** — current shapeless JSON recipe does not preserve arbitrary input-banner patterns/colors; preserving exact banner NBT requires a custom/special recipe serializer.

### Session 9 — ✓ Complete
- [x] **Banner removal now removes guard** — banner-bound guards now store a linked placed-banner block position and self-despawn when that block is no longer a banner (`BlockTags.BANNERS` check every 20 ticks).
- [x] **One guard per banner** — each placed guard banner now creates and owns its own guard; multiple banners can coexist, each with its own linked guard.
- [x] **Persistence updates** — banner anchor position and optional owner UUID are now saved/loaded in `GuardEntity` NBT (MC 26.x `ValueOutput`/`ValueInput`).

### Session 10 — ✓ Complete
- [x] **Hotbar icon fix for Guard Banner** — aligned both item-definition and legacy model JSON to vanilla white banner so the hotbar/inventory icon renders as a standard Minecraft white banner.
- [x] **Asset path consistency** — `assets/village-guard/items/guard_banner.json` and `assets/village-guard/models/item/guard_banner.json` now both resolve to vanilla banner rendering instead of a custom generated texture path.

### Session 11 — ✓ Complete
- [x] **Pattern-preserving Guard Banner crafting** — replaced the plain shapeless JSON result recipe with a custom crafting serializer/recipe (`guard_banner_copy`) that requires a strict 3×3 layout: any banner in the center, emerald blocks in all 8 surrounding slots.
- [x] **Input banner appearance is preserved** — crafting now transmutes the center banner stack into `village-guard:guard_banner` while keeping its existing banner data (base color and pattern stack), so white stays white, black stays black, and designed banners keep their full design.
- [x] **Recipe registration wired** — added `ModRecipes` registration in mod init and recipe class `GuardBannerCopyRecipe` under `recipe/` package.

### Session 12 — ✓ Complete
- [x] **Broken-banner identity fix** — when a linked placed banner is broken in survival, nearby dropped vanilla banner item is now converted back to `village-guard:guard_banner` (with its banner appearance data preserved), so it does not downgrade into a normal banner item.
- [x] **Hotbar icon fix (final)** — `assets/village-guard/items/guard_banner.json` now uses the same `minecraft:special` banner item schema as vanilla banner items (`template_banner` + `minecraft:banner` special model), eliminating purple/black missing-texture squares.

### Session 13 — ✓ Complete
- [x] **Enchanted-hue icon fallback** — moved Guard Banner icon rendering to `item/generated` with layered textures and added a dedicated translucent purple overlay (`guard_banner_glint_overlay.png`) as `layer1` so the hotbar/inventory icon always shows an enchanted-style hue even when runtime foil glint is not visible on banner render paths.
- [x] **Asset pipeline stabilized** — Guard Banner icon now uses `guard_banner.png` base + `guard_banner_glint_overlay.png` overlay in `models/item/guard_banner.json` for consistent visual readability.

### Next Steps
- [ ] **Custom `SpearItem`** — currently an iron sword placeholder
- [ ] **Loot table JSON** — guards drop nothing from loot tables yet (gear drop handled in code)
- [ ] **Hurt / death / ambient sounds** — override `getHurtSound`, `getDeathSound`, `getAmbientSound` in `GuardEntity`
- [ ] **Verify in-game behavior** — patrol radius, banner relocation, armor rendering, goat horn, loot drops, swarm alert, nose rendering

---

## Possible Future Features
- **Guard Captain banner** — captain variant could hold a banner (like a pillager captain), triggering a raid if killed
- **Guard training** — player can "hire" or upgrade a scout to soldier with emeralds
- **Guard shelter** — custom structure (guardhouse/barracks) that guards spawn in and return to at night
- **Crossbow charge animation** — currently not animated; would require custom render state fields
