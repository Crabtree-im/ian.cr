package dev.icrabtree.villageguard.entity;

import dev.icrabtree.villageguard.entity.goal.GuardRetaliateGoal;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import dev.icrabtree.villageguard.entity.goal.BlowHornGoal;
import dev.icrabtree.villageguard.init.ModEntities;
import dev.icrabtree.villageguard.init.ModItems;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.AABB;

public class GuardEntity extends PathfinderMob implements RangedAttackMob, CrossbowAttackMob {

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WEAPON =
            SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.INT);

    // Prevents the combat goal from being registered twice (e.g. on NBT load after finalizeSpawn)
    private boolean combatGoalAdded = false;

    // Tracks which equipment slots received items from players (always dropped on death)
    private final EnumSet<EquipmentSlot> givenByPlayer = EnumSet.noneOf(EquipmentSlot.class);

    // True when this guard was spawned by a GuardBannerItem — excluded from wave-cap counts
    private boolean bannerBound = false;
    private boolean hasBannerAnchor = false;
    private BlockPos bannerAnchorPos = BlockPos.ZERO;
    private UUID bannerOwnerUuid = null;

    public boolean isBannerBound() { return bannerBound; }
    public void setBannerBound(boolean v) { this.bannerBound = v; }
    public boolean hasBannerAnchor() { return hasBannerAnchor; }
    public BlockPos getBannerAnchorPos() { return bannerAnchorPos; }
    public void setBannerAnchorPos(BlockPos pos) {
        this.hasBannerAnchor = true;
        this.bannerAnchorPos = pos.immutable();
    }
    public UUID getBannerOwnerUuid() { return bannerOwnerUuid; }
    public void setBannerOwnerUuid(UUID owner) { this.bannerOwnerUuid = owner; }

    public GuardEntity(EntityType<? extends GuardEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)  // same as pillager
                .add(Attributes.ATTACK_DAMAGE, 6.0)    // iron sword-level melee
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0);
    }

    // -------------------------------------------------------------------------
    // Synced data
    // -------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, GuardVariant.SCOUT.id);
        builder.define(DATA_WEAPON, GuardWeapon.SWORD.id);
    }

    public GuardVariant getVariant() {
        return GuardVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(GuardVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.id);
    }

    public GuardWeapon getWeapon() {
        return GuardWeapon.byId(this.entityData.get(DATA_WEAPON));
    }

    public void setWeapon(GuardWeapon weapon) {
        this.entityData.set(DATA_WEAPON, weapon.id);
    }

    // -------------------------------------------------------------------------
    // Guards cannot attack other guards under any circumstances
    // -------------------------------------------------------------------------

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof GuardEntity) && super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        // Banner guards should only exist while their placed banner block exists.
        if (!this.level().isClientSide() && bannerBound && hasBannerAnchor && (this.tickCount % 20 == 0)) {
            if (!this.level().getBlockState(bannerAnchorPos).is(BlockTags.BANNERS)) {
                // When survival-breaking a linked banner, vanilla drops a normal banner item.
                // Convert that nearby drop back into Guard Banner while preserving banner data.
                List<ItemEntity> nearbyDrops = this.level().getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(bannerAnchorPos).inflate(1.25),
                        ie -> ie.isAlive() && ie.getItem().is(ItemTags.BANNERS)
                );
                for (ItemEntity drop : nearbyDrops) {
                    ItemStack stack = drop.getItem();
                    if (stack.is(ModItems.GUARD_BANNER)) continue;
                    ItemStack converted = stack.transmuteCopy(ModItems.GUARD_BANNER, stack.getCount());
                    converted.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    drop.setItem(converted);
                    break;
                }
                this.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Hurt override — pigman-style: one attacked guard alerts all nearby guards
    // -------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean result = super.hurtServer(level, source, amount);
        if (result && source.getEntity() instanceof Player player && !player.isCreative()) {
            // Alert every idle guard within 64 blocks to target this player
            level.getEntitiesOfClass(GuardEntity.class,
                    this.getBoundingBox().inflate(64),
                    g -> g != this && g.getTarget() == null)
                .forEach(g -> g.setTarget(player));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Target acquisition — blow horn on first aggro
    // -------------------------------------------------------------------------

    @Override
    public void setTarget(LivingEntity target) {
        boolean wasWithoutTarget = this.getTarget() == null;
        super.setTarget(target);
        if (target != null && wasWithoutTarget) {
            // Always "Ponder" (index 0) on first aggro
            this.level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0),
                    SoundSource.NEUTRAL, 1.4f, 1.0f);
        }
    }

    // -------------------------------------------------------------------------
    // AI goals
    // -------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // Combat goal is added after variant/weapon are decided (see setupCombatGoal)
        this.goalSelector.addGoal(2, new BlowHornGoal(this));
        this.goalSelector.addGoal(7, new MoveTowardsRestrictionGoal(this, 0.7));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, GuardEntity.class)); // never retaliate vs fellow guards
        this.targetSelector.addGoal(2, new GuardRetaliateGoal(this, 16.0));
        // Attack all hostile mobs (Monster covers zombies, skeletons, creepers, spiders, illagers, etc.)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    /**
     * Registers the correct combat goal based on the guard's current weapon type.
     * Safe to call from both finalizeSpawn and readAdditionalSaveData.
     */
    private void setupCombatGoal() {
        if (combatGoalAdded) return;
        combatGoalAdded = true;

        switch (getWeapon()) {
            case BOW, CROSSBOW ->
                // 100 ticks = 5 seconds between shots (deliberate, powerful sniper rhythm)
                this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 100, 100, 15.0f));
            default ->
                this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        }
    }

    // -------------------------------------------------------------------------
    // Spawn setup
    // -------------------------------------------------------------------------

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        GuardVariant[] variants = GuardVariant.values();
        GuardVariant variant = variants[this.random.nextInt(variants.length)];
        setVariant(variant);

        GuardWeapon[] weapons = GuardWeapon.values();
        GuardWeapon weapon = weapons[this.random.nextInt(weapons.length)];
        setWeapon(weapon);

        // Equip the weapon item in the main hand
        this.setItemSlot(EquipmentSlot.MAINHAND, buildWeaponStack(weapon));

        // Outfit based on variant rank
        equipRandomArmor();

        setupCombatGoal();

        // Set 200-block patrol radius centered on spawn position
        this.setHomeTo(this.blockPosition(), 200);

        // On natural spawn: enforce patrol cap, then spawn companion guards (total 5–8)
        if (spawnType == EntitySpawnReason.NATURAL && level instanceof ServerLevel serverLevel) {
            List<GuardEntity> nearby = serverLevel.getEntitiesOfClass(
                    GuardEntity.class, this.getBoundingBox().inflate(200));
            if (nearby.size() >= 8) {
                this.remove(Entity.RemovalReason.DISCARDED);
                return data;
            }
            int companions = Math.min(4 + this.random.nextInt(4), (int)(8 - nearby.size()));
            BlockPos patrolCenter = this.blockPosition();
            for (int i = 0; i < companions; i++) {
                GuardEntity companion = ModEntities.GUARD.create(serverLevel, EntitySpawnReason.REINFORCEMENT);
                if (companion != null) {
                    double ox = (this.random.nextDouble() - 0.5) * 16;
                    double oz = (this.random.nextDouble() - 0.5) * 16;
                    companion.setPos(getX() + ox, getY(), getZ() + oz);
                    companion.setYRot(this.random.nextFloat() * 360f);
                    companion.finalizeSpawn(level, difficulty, EntitySpawnReason.REINFORCEMENT, null);
                    companion.setHomeTo(patrolCenter, 200);
                    serverLevel.addFreshEntity(companion);
                }
            }
        }
        return data;
    }

    private ItemStack buildWeaponStack(GuardWeapon weapon) {
        Item item;
        if (weapon == GuardWeapon.BOW) {
            item = Items.BOW;
        } else if (weapon == GuardWeapon.CROSSBOW) {
            item = Items.CROSSBOW;
        } else {
            // Material tier: 55% wood, 20% gold, 14% stone, 11% iron
            int roll = this.random.nextInt(100);
            if (weapon == GuardWeapon.AXE) {
                item = roll < 55 ? Items.WOODEN_AXE
                     : roll < 75 ? Items.GOLDEN_AXE
                     : roll < 89 ? Items.STONE_AXE
                     : Items.IRON_AXE;
            } else {
                // SWORD and SPEAR both use sword items
                item = roll < 55 ? Items.WOODEN_SWORD
                     : roll < 75 ? Items.GOLDEN_SWORD
                     : roll < 89 ? Items.STONE_SWORD
                     : Items.IRON_SWORD;
            }
        }
        ItemStack stack = new ItemStack(item);
        int maxDmg = stack.getMaxDamage();
        if (maxDmg > 0) {
            int dmg = (int)(maxDmg * (0.10f + this.random.nextFloat() * 0.75f));
            stack.setDamageValue(Math.min(maxDmg - 1, dmg));
        }
        return stack;
    }

    /** Equips each armor slot with a random piece (leather→iron, 1/100k diamond), pre-worn. */
    private void equipRandomArmor() {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (this.random.nextFloat() < 0.40f) continue; // 40% chance the slot is bare
            ItemStack stack = buildArmorPiece(slot);
            if (!stack.isEmpty()) this.setItemSlot(slot, stack);
        }
    }

    private ItemStack buildArmorPiece(EquipmentSlot slot) {
        Item item;
        // 1 in 100,000 chance for a diamond piece — extremely rare
        if (this.random.nextInt(100_000) == 0) {
            item = switch (slot) {
                case HEAD  -> Items.DIAMOND_HELMET;
                case CHEST -> Items.DIAMOND_CHESTPLATE;
                case LEGS  -> Items.DIAMOND_LEGGINGS;
                case FEET  -> Items.DIAMOND_BOOTS;
                default    -> Items.AIR;
            };
        } else {
            // Weighted tier: 55% leather, 20% golden, 14% chainmail, 11% iron
            int roll = this.random.nextInt(100);
            item = switch (slot) {
                case HEAD -> roll < 55 ? Items.LEATHER_HELMET
                           : roll < 75 ? Items.GOLDEN_HELMET
                           : roll < 89 ? Items.CHAINMAIL_HELMET
                           : Items.IRON_HELMET;
                case CHEST -> roll < 55 ? Items.LEATHER_CHESTPLATE
                            : roll < 75 ? Items.GOLDEN_CHESTPLATE
                            : roll < 89 ? Items.CHAINMAIL_CHESTPLATE
                            : Items.IRON_CHESTPLATE;
                case LEGS -> roll < 55 ? Items.LEATHER_LEGGINGS
                           : roll < 75 ? Items.GOLDEN_LEGGINGS
                           : roll < 89 ? Items.CHAINMAIL_LEGGINGS
                           : Items.IRON_LEGGINGS;
                case FEET -> roll < 55 ? Items.LEATHER_BOOTS
                           : roll < 75 ? Items.GOLDEN_BOOTS
                           : roll < 89 ? Items.CHAINMAIL_BOOTS
                           : Items.IRON_BOOTS;
                default   -> Items.AIR;
            };
        }
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        // Pre-wear: 15–85% durability damage so items look used
        int maxDmg = stack.getMaxDamage();
        if (maxDmg > 0) {
            int dmg = (int)(maxDmg * (0.15f + this.random.nextFloat() * 0.70f));
            stack.setDamageValue(Math.min(maxDmg - 1, dmg));
        }
        return stack;
    }

    // -------------------------------------------------------------------------
    // RangedAttackMob — bow / crossbow attack (both use arrow projectiles)
    // -------------------------------------------------------------------------

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        // Exact vanilla skeleton approach: use getMobArrow so the arrow spawns at eye height
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, pullProgress, this.getMainHandItem());
        // Target the lower 1/3 of the mob's body (same constant as vanilla AbstractSkeleton)
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        // Difficulty-scaled spread: Easy=10, Normal=6, Hard=2  (vanilla skeleton formula)
        float spread = (float)(14 - this.level().getDifficulty().getId() * 4);
        arrow.setBaseDamage(4.0);  // boosted arrow damage (default is ~2.0)
        arrow.shoot(dx, dy + dist * 0.20, dz, 1.6f, spread);
        this.level().addFreshEntity(arrow);
        // Crossbow makes a distinct fire sound; bow uses skeleton's twang
        if (getWeapon() == GuardWeapon.CROSSBOW) {
            this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f,
                    1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
        } else {
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f,
                    1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
        }
    }

    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        return new ItemStack(Items.ARROW);
    }

    // -------------------------------------------------------------------------
    // CrossbowAttackMob interface
    // -------------------------------------------------------------------------

    @Override
    public void setChargingCrossbow(boolean charging) {
        // Charging state managed by RangedAttackGoal; no extra sync needed for now
    }

    @Override
    public void onCrossbowAttackPerformed() {
        // No extra animation state needed yet
    }

    // -------------------------------------------------------------------------
    // Item pickup — upgrade-aware armor + weapon pickup
    // Guards swap to better gear and toss the old piece back on the ground.
    // -------------------------------------------------------------------------

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity itemEntity) {
        ItemStack dropped = itemEntity.getItem();

        // --- Armor: pick up if slot is empty OR dropped piece is a higher tier ---
        Equippable equippable = dropped.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().isArmor()) {
            EquipmentSlot slot = equippable.slot();
            ItemStack current = this.getItemBySlot(slot);
            if (current.isEmpty() || itemTier(dropped.getItem()) > itemTier(current.getItem())) {
                if (!current.isEmpty()) this.spawnAtLocation(level, current.copy());
                this.setItemSlot(slot, dropped.copyWithCount(1));
                this.givenByPlayer.add(slot);
                this.onItemPickup(itemEntity);
                itemEntity.discard();
            }
            return;
        }

        // --- Weapon: upgrade main hand if higher tier ---
        int droppedTier = itemTier(dropped.getItem());
        if (droppedTier > 0) {
            ItemStack current = this.getItemBySlot(EquipmentSlot.MAINHAND);
            if (current.isEmpty() || droppedTier > itemTier(current.getItem())) {
                if (!current.isEmpty()) this.spawnAtLocation(level, current.copy());
                this.setItemSlot(EquipmentSlot.MAINHAND, dropped.copyWithCount(1));
                this.givenByPlayer.add(EquipmentSlot.MAINHAND);
                rebuildCombatGoal(dropped.getItem());
                this.onItemPickup(itemEntity);
                itemEntity.discard();
            }
        }
    }

    /**
     * Numeric tier for armor and weapons so we can decide whether a dropped item
     * is an upgrade. Returns 0 for items guards should ignore.
     */
    private static int itemTier(Item item) {
        if (item == Items.LEATHER_HELMET     || item == Items.LEATHER_CHESTPLATE   ||
            item == Items.LEATHER_LEGGINGS   || item == Items.LEATHER_BOOTS)        return 1;
        if (item == Items.GOLDEN_HELMET      || item == Items.GOLDEN_CHESTPLATE    ||
            item == Items.GOLDEN_LEGGINGS    || item == Items.GOLDEN_BOOTS)         return 2;
        if (item == Items.CHAINMAIL_HELMET   || item == Items.CHAINMAIL_CHESTPLATE  ||
            item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS)      return 3;
        if (item == Items.IRON_HELMET        || item == Items.IRON_CHESTPLATE      ||
            item == Items.IRON_LEGGINGS      || item == Items.IRON_BOOTS)           return 4;
        if (item == Items.DIAMOND_HELMET     || item == Items.DIAMOND_CHESTPLATE   ||
            item == Items.DIAMOND_LEGGINGS   || item == Items.DIAMOND_BOOTS)        return 5;
        if (item == Items.NETHERITE_HELMET   || item == Items.NETHERITE_CHESTPLATE ||
            item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS)      return 6;
        // Weapons — wood/gold share tier 1 (gold is weak despite the look)
        if (item == Items.WOODEN_SWORD  || item == Items.WOODEN_AXE  ||
            item == Items.GOLDEN_SWORD  || item == Items.GOLDEN_AXE)               return 1;
        if (item == Items.STONE_SWORD   || item == Items.STONE_AXE)                return 2;
        if (item == Items.IRON_SWORD    || item == Items.IRON_AXE)                 return 3;
        if (item == Items.DIAMOND_SWORD || item == Items.DIAMOND_AXE)              return 4;
        if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_AXE)          return 5;
        if (item == Items.BOW)      return 3;
        if (item == Items.CROSSBOW) return 4;
        return 0;
    }

    /**
     * Swaps the combat goal when a picked-up weapon changes attack style
     * (e.g. melee guard picks up a bow, or ranged guard picks up a sword).
     */
    private void rebuildCombatGoal(Item newWeapon) {
        boolean newIsRanged = (newWeapon == Items.BOW || newWeapon == Items.CROSSBOW);
        if (newIsRanged == getWeapon().isRanged()) return;
        GuardWeapon gw;
        if      (newWeapon == Items.BOW)                                                gw = GuardWeapon.BOW;
        else if (newWeapon == Items.CROSSBOW)                                           gw = GuardWeapon.CROSSBOW;
        else if (newWeapon == Items.WOODEN_AXE || newWeapon == Items.STONE_AXE   ||
                 newWeapon == Items.GOLDEN_AXE || newWeapon == Items.IRON_AXE    ||
                 newWeapon == Items.DIAMOND_AXE|| newWeapon == Items.NETHERITE_AXE)     gw = GuardWeapon.AXE;
        else                                                                            gw = GuardWeapon.SWORD;
        setWeapon(gw);
        combatGoalAdded = false;
        setupCombatGoal();
    }

    // -------------------------------------------------------------------------
    // Death loot — given items always drop; spawned gear drops with rarity
    // More total items dropped = rarer (each successive item needs its own roll)
    // -------------------------------------------------------------------------

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Items given by players always drop intact
        for (EquipmentSlot slot : givenByPlayer) {
            ItemStack stack = getItemBySlot(slot);
            if (!stack.isEmpty()) spawnAtLocation(level, stack.copy());
        }

        // Spawned gear: drop chance scales with how worn the item is.
        // Intact (low damage) = very rare drop; heavily worn = up to 8.5% (vanilla zombie).
        // Formula: 1% base + 7.5% * damageRatio  →  1% at 0% worn, 8.5% at 100% worn
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (givenByPlayer.contains(slot)) continue;
            ItemStack stack = getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            int maxDmg = stack.getMaxDamage();
            float damageRatio = maxDmg > 0 ? (float) stack.getDamageValue() / maxDmg : 0.5f;
            float dropChance = 0.01f + 0.075f * damageRatio;

            if (this.random.nextFloat() < dropChance) {
                ItemStack drop = stack.copy();
                // Add a little extra wear from the killing blow
                if (maxDmg > 0) {
                    int extraWear = this.random.nextInt(Math.max(1, maxDmg / 10));
                    drop.setDamageValue(Math.min(maxDmg - 1, drop.getDamageValue() + extraWear));
                }
                spawnAtLocation(level, drop);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Right-click with any banner — moves the patrol center here
    // All guards within 200 blocks share the new center
    // -------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player.getItemInHand(hand).is(ItemTags.BANNERS)
                && this.level() instanceof ServerLevel serverLevel) {
            BlockPos newCenter = this.blockPosition();
            this.setHomeTo(newCenter, 200);
            serverLevel.getEntitiesOfClass(GuardEntity.class, this.getBoundingBox().inflate(200))
                    .forEach(g -> { if (g != this) g.setHomeTo(newCenter, 200); });
            this.playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // -------------------------------------------------------------------------
    // Save data (MC 26.x ValueInput/ValueOutput system replaced CompoundTag)
    // -------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("GuardVariant", getVariant().id);
        output.putInt("GuardWeapon", getWeapon().id);
        // Patrol center
        output.putBoolean("HasPatrol", hasHome());
        if (hasHome()) {
            BlockPos pos = getHomePosition();
            output.putInt("PatrolX", pos.getX());
            output.putInt("PatrolY", pos.getY());
            output.putInt("PatrolZ", pos.getZ());
        }
        // Bitmask of player-given slots so they always drop on death
        int givenMask = 0;
        for (EquipmentSlot slot : givenByPlayer) givenMask |= (1 << slot.ordinal());
        output.putInt("GuardGivenMask", givenMask);
        output.putBoolean("BannerBound", bannerBound);
        output.putBoolean("HasBannerAnchor", hasBannerAnchor);
        if (hasBannerAnchor) {
            output.putInt("BannerAnchorX", bannerAnchorPos.getX());
            output.putInt("BannerAnchorY", bannerAnchorPos.getY());
            output.putInt("BannerAnchorZ", bannerAnchorPos.getZ());
        }
        boolean hasOwner = bannerOwnerUuid != null;
        output.putBoolean("HasBannerOwner", hasOwner);
        if (hasOwner) {
            output.putLong("BannerOwnerMSB", bannerOwnerUuid.getMostSignificantBits());
            output.putLong("BannerOwnerLSB", bannerOwnerUuid.getLeastSignificantBits());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(GuardVariant.byId(input.getIntOr("GuardVariant", GuardVariant.SCOUT.id)));
        GuardWeapon weapon = GuardWeapon.byId(input.getIntOr("GuardWeapon", GuardWeapon.SWORD.id));
        setWeapon(weapon);
        setupCombatGoal();
        // Patrol center
        if (input.getBooleanOr("HasPatrol", false)) {
            setHomeTo(new BlockPos(
                    input.getIntOr("PatrolX", 0),
                    input.getIntOr("PatrolY", 64),
                    input.getIntOr("PatrolZ", 0)
            ), 200);
        }
        // Restore player-given slot bitmask
        int givenMask = input.getIntOr("GuardGivenMask", 0);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if ((givenMask & (1 << slot.ordinal())) != 0) givenByPlayer.add(slot);
        }
        bannerBound = input.getBooleanOr("BannerBound", false);
        hasBannerAnchor = input.getBooleanOr("HasBannerAnchor", false);
        if (hasBannerAnchor) {
            bannerAnchorPos = new BlockPos(
                    input.getIntOr("BannerAnchorX", 0),
                    input.getIntOr("BannerAnchorY", 64),
                    input.getIntOr("BannerAnchorZ", 0)
            );
        }
        if (input.getBooleanOr("HasBannerOwner", false)) {
            bannerOwnerUuid = new UUID(
                    input.getLongOr("BannerOwnerMSB", 0L),
                    input.getLongOr("BannerOwnerLSB", 0L)
            );
        } else {
            bannerOwnerUuid = null;
        }
    }
}
