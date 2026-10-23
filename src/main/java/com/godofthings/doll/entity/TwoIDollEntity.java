package com.godofthings.doll.entity;

import com.godofthings.doll.config.DollConfig;
import com.godofthings.doll.item.TwoIDollAcquisition;
import com.godofthings.doll.item.TwoIDollItem;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollSounds;
import com.godofthings.doll.init.DollComponents;
import com.godofthings.doll.mixin.LivingHealthDataAccessor;
import com.godofthings.doll.network.DollNetwork;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TwoIDollEntity extends PathfinderMob {
   private static final Map<MinecraftServer, Map<UUID, Set<TwoIDollEntity>>> TRACKED = new HashMap<>();
   private static final EntityDataAccessor<Integer> POSE_ACTION = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Long> POSE_STARTED_AT = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Integer> ENVIRONMENT_ACTION = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Long> ENVIRONMENT_STARTED_AT = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Boolean> SHARED_UMBRELLA = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SHARED_UMBRELLA_HOLDER = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<String> BUBBLE_TEXT = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Long> BUBBLE_UNTIL = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Component> BUBBLE_MESSAGE = SynchedEntityData.defineId(TwoIDollEntity.class, EntityDataSerializers.COMPONENT);
   public static final int ACTION_NONE = 0;
   public static final int ACTION_GREETING = 1;
   public static final int ACTION_CURIOUS = 2;
   public static final int ACTION_PATTED = 3;
   public static final int ACTION_RECEIVE_GIFT = 4;
   public static final int ACTION_INVITE = 5;
   public static final int ACTION_SWING_FEET = 6;
   public static final int ACTION_UMBRELLA = 7;
   public static final int ACTION_SNEEZE = 8;
   public static final int ACTION_SHIVER = 9;
   public static final int ACTION_STRETCH = 10;
   public static final int ACTION_DUST = 11;
   public static final int ACTION_SHY = 12;
   private static final int MIN_BOND = -100;
   private static final int MAX_BOND = 100;
   private static final int MIN_PRANK_COOLDOWN = 300;
   private static final int MAX_PRANK_COOLDOWN = 4800;
   private static final int TIME_STOP_COOLDOWN = 24000;
   private static final int TIME_STOP_CHANCE_DENOMINATOR = 10000;
   private static final int PANIC_COOLDOWN = 6000;
   public static final double SHARED_UMBRELLA_DISTANCE = 0.34;
   private static final int GIFT_COOLDOWN_TICKS = 1200;
   private static final int TALK_COOLDOWN_TICKS = 200;
   private static final Set<String> SPECIAL_GIFTS = Set.of(
      "avaritia:infinity_ingot",
      "mekanism_extras:qio_drive_singularity",
      "modern_industrialization:quantum_upgrade",
      "bigreactors:inanite_block",
      "draconicevolution:chaotic_core",
      "occultism:ritual_dummy/misc_celestial_chalice",
      "appflux:core_256m",
      "advanced_ae:data_entangler",
      "advanced_ae:quantum_multi_threader",
      "megacells:cell_component_256m",
      "ae2omnicells:quantum_omni_cell_component_256m",
      "mekanism_extras:infinite_induction_cell",
      "mekanism_extras:infinite_induction_provider",
      "mekanism:pellet_antimatter",
      "mekanism_extras:infinite_control_circuit",
      "ars_nouveau:wilden_tribute",
      "minecraft:elytra",
      "minecraft:heavy_core",
      "minecraft:dragon_head",
      "ae2:256k_crafting_storage",
      "mekanism:ultimate_induction_cell",
      "mekanism:ultimate_induction_provider",
      "pneumaticcraft:micromissiles",
      "minecraft:dragon_egg",
      "minecraft:heart_of_the_sea",
      "minecraft:echo_shard",
      "minecraft:nether_star",
      "minecraft:torchflower",
      "minecraft:recovery_compass"
   );
   private static final List<List<EntityType<?>>> PRANK_MOB_TIERS = List.of(
      List.of(
         EntityType.CHICKEN,
         EntityType.COW,
         EntityType.SHEEP,
         EntityType.PIG,
         EntityType.RABBIT,
         EntityType.BEE,
         EntityType.CAT,
         EntityType.WOLF,
         EntityType.FOX,
         EntityType.GOAT,
         EntityType.AXOLOTL,
         EntityType.LLAMA,
         EntityType.PARROT,
         EntityType.TURTLE
      ),
      List.of(
         EntityType.FOX, EntityType.GOAT, EntityType.AXOLOTL, EntityType.LLAMA, EntityType.PARROT, EntityType.TURTLE, EntityType.PANDA, EntityType.MOOSHROOM
      ),
      List.of(EntityType.SLIME, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.CAVE_SPIDER, EntityType.PHANTOM, EntityType.MAGMA_CUBE),
      List.of(EntityType.WITCH, EntityType.WARDEN)
   );
   private static final String PRANK_OWNER_TAG = "useless_stretcher_2i_prank_owner";
   private UUID ownerId;
   private int giftScore;
   private boolean invitationUsed;
   private int prankCooldown;
   private int lastPrank = -1;
   private int patCooldown;
   private int giftCooldown;
   private int talkCooldown;
   private final Map<UUID, TwoIStorySession> storySessions = new HashMap<>();
   private int timeStopCooldown;
   private int panicCooldown;
   private boolean panicDragonSpawned;
   private ItemStack itemTemplate = ItemStack.EMPTY;
   private TwoIDollMemory.Interaction interaction = TwoIDollMemory.Interaction.empty();
   private boolean authorizedReclaim;
   private UUID friendId;
   private final Map<UUID, Long> interactionViewers = new HashMap<>();
   private long lastCompanionSample = -1L;

   public TwoIDollEntity(EntityType<? extends TwoIDollEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.setInvulnerable(true);
      this.setNoGravity(false);
      this.prankCooldown = 1200 + this.random.nextInt(1200);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new Goal() {
         {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
         }

         public boolean canUse() {
            return TwoIDollEntity.this.isInteractionHeld();
         }

         public void start() {
            TwoIDollEntity.this.getNavigation().stop();
         }

         public void tick() {
            TwoIDollEntity.this.getNavigation().stop();
            TwoIDollEntity.this.setDeltaMovement(0.0, TwoIDollEntity.this.isNoGravity() ? 0.0 : TwoIDollEntity.this.getDeltaMovement().y, 0.0);
         }
      });
      this.goalSelector.addGoal(1, new TwoIDollEntity.FollowOwnerGoal(this));
      this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.55));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public UUID getOwnerId() {
      return this.ownerId;
   }

   public void setOwnerId(UUID ownerId) {
      if (this.ownerId != null && !this.ownerId.equals(ownerId)) {
         this.clearFriendship();
      }

      this.untrack();
      this.ownerId = ownerId;
      if (this.isAddedToLevel()) {
         this.track();
      }
   }

   public int getGiftScore() {
      return this.giftScore;
   }

   public ItemStack getPickResult() {
      return this.toDollItem();
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData group) {
      this.setSpawnCancelled(true);
      return group;
   }

   public boolean isInvitationUsed() {
      return this.invitationUsed;
   }

   public boolean hasSharedUmbrella() {
      return (Boolean)this.entityData.get(SHARED_UMBRELLA);
   }

   public boolean isSharedUmbrellaHolder() {
      return (Boolean)this.entityData.get(SHARED_UMBRELLA_HOLDER);
   }

   public void copyClientPreviewStateFrom(TwoIDollEntity source) {
      if (source != null && source != this) {
         this.entityData.set(POSE_ACTION, source.getPoseAction());
         this.entityData.set(POSE_STARTED_AT, (Long)source.entityData.get(POSE_STARTED_AT));
         this.entityData.set(ENVIRONMENT_ACTION, source.getEnvironmentAction());
         this.entityData.set(ENVIRONMENT_STARTED_AT, (Long)source.entityData.get(ENVIRONMENT_STARTED_AT));
         this.entityData.set(SHARED_UMBRELLA, source.hasSharedUmbrella());
         this.entityData.set(SHARED_UMBRELLA_HOLDER, source.isSharedUmbrellaHolder());
         this.entityData.set(BUBBLE_TEXT, (String)source.entityData.get(BUBBLE_TEXT));
         this.entityData.set(BUBBLE_UNTIL, (Long)source.entityData.get(BUBBLE_UNTIL));
         this.entityData.set(BUBBLE_MESSAGE, (Component)source.entityData.get(BUBBLE_MESSAGE));
      }
   }

   public static List<TwoIDollEntity> trackedFor(MinecraftServer server, UUID owner) {
      Map<UUID, Set<TwoIDollEntity>> byOwner = TRACKED.get(server);
      if (byOwner == null) {
         return List.of();
      } else {
         Set<TwoIDollEntity> entities = byOwner.get(owner);
         return (List<TwoIDollEntity>)(entities == null ? List.of() : new ArrayList<>(entities));
      }
   }

   public static void clearTracking() {
      TRACKED.clear();
   }

   public void onAddedToLevel() {
      super.onAddedToLevel();
      this.track();
   }

   public void onRemovedFromLevel() {
      if (!TwoILifecycleGuard.blocks(this)) {
         this.storySessions.clear();
         this.interactionViewers.clear();
         this.clearFriendship();
         this.untrack();
         super.onRemovedFromLevel();
      }
   }

   private void track() {
      if (!this.level().isClientSide && this.ownerId != null && this.level() instanceof ServerLevel level) {
         TRACKED.computeIfAbsent(level.getServer(), ignored -> new HashMap<>()).computeIfAbsent(this.ownerId, ignored -> new HashSet<>()).add(this);
      }
   }

   private void untrack() {
      if (this.level() instanceof ServerLevel level && this.ownerId != null) {
         Map<UUID, Set<TwoIDollEntity>> byOwner = TRACKED.get(level.getServer());
         if (byOwner == null) {
            return;
         }

         Set<TwoIDollEntity> entities = byOwner.get(this.ownerId);
         if (entities != null) {
            entities.remove(this);
            if (entities.isEmpty()) {
               byOwner.remove(this.ownerId);
            }
         }

         if (byOwner.isEmpty()) {
            TRACKED.remove(level.getServer());
         }

         return;
      }
   }

   public void restoreBond(int score, boolean invitationUsed) {
      this.giftScore = clampBond(score);
      this.invitationUsed = invitationUsed;
   }

   public void restoreFromItem(ItemStack stack) {
      this.itemTemplate = stack.copyWithCount(1);
      UUID savedOwner = (UUID)stack.get((DataComponentType)DollComponents.TWO_I_OWNER.get());
      if (this.ownerId == null && savedOwner != null) {
         this.setOwnerId(savedOwner);
      }

      this.restoreBond(
         (Integer)stack.getOrDefault(
            (DataComponentType)DollComponents.TWO_I_BOND.get(),
            (Integer)stack.getOrDefault((DataComponentType)DollComponents.TWO_I_GIFT_SCORE.get(), 0) * 10
         ),
         (Boolean)stack.getOrDefault((DataComponentType)DollComponents.TWO_I_INVITATION_USED.get(), false)
      );
      TwoIDollMemory memory = (TwoIDollMemory)stack.get((DataComponentType)DollComponents.TWO_I_MEMORY.get());
      if (memory != null) {
         this.restoreMemory(memory);
      }

      UUID dollId = (UUID)stack.get((DataComponentType)DollComponents.TWO_I_DOLL_ID.get());
      if (dollId != null && this.level() instanceof ServerLevel server) {
         this.restoreMemory(TwoICompanionshipData.get(server.getServer()).progress(dollId, this.memory()).merge(this.memory()));
      }
   }

   public ItemStack toDollItem() {
      ItemStack stack = this.itemTemplate.isEmpty() ? new ItemStack((ItemLike)DollRegistry.TWO_I_DOLL_ITEM.get()) : this.itemTemplate.copyWithCount(1);
      stack.set((DataComponentType)DollComponents.TWO_I_BOND.get(), this.giftScore);
      stack.set((DataComponentType)DollComponents.TWO_I_INVITATION_USED.get(), this.invitationUsed);
      stack.set((DataComponentType)DollComponents.TWO_I_MEMORY.get(), this.memory());
      if (this.ownerId != null) {
         stack.set((DataComponentType)DollComponents.TWO_I_OWNER.get(), this.ownerId);
      }

      if (!this.level().isClientSide) {
         if (this.itemTemplate.isEmpty()) {
            this.itemTemplate = stack.copyWithCount(1);
         }

         UUID dollId = TwoIDollItem.ensureIdentity(this.itemTemplate);
         stack.set((DataComponentType)DollComponents.TWO_I_DOLL_ID.get(), dollId);
      }

      return stack;
   }

   private TwoIDollMemory memory() {
      return new TwoIDollMemory(
         new TwoIDollMemory.Cooldowns(
            this.patCooldown,
            this.giftCooldown,
            this.talkCooldown,
            this.prankCooldown,
            this.timeStopCooldown,
            this.panicCooldown,
            this.lastPrank,
            this.panicDragonSpawned
         ),
         this.interaction,
         this.lastCompanionSample
      );
   }

   private void restoreMemory(TwoIDollMemory memory) {
      TwoIDollMemory.Cooldowns c = memory.cooldowns();
      this.patCooldown = c.pat();
      this.giftCooldown = c.gift();
      this.talkCooldown = c.talk();
      this.prankCooldown = c.prank();
      this.timeStopCooldown = c.timeStop();
      this.panicCooldown = c.panic();
      this.lastPrank = c.lastPrank();
      this.panicDragonSpawned = c.panicDragonSpawned();
      this.interaction = memory.interaction();
      this.lastCompanionSample = memory.lastCompanionSample();
   }

   private void refreshInteractionDay() {
      this.interaction = this.interaction.forDay(this.level().getGameTime());
   }

   private void tickCompanionship(ServerLevel server) {
      long time = server.getGameTime();
      if (time % 100L == 0L && this.ownerId != null) {
         this.refreshInteractionDay();
         ServerPlayer owner = server.getServer().getPlayerList().getPlayer(this.ownerId);
         if (owner != null && owner.serverLevel() == server && !(this.distanceToSqr(owner) > 144.0)) {
            if (this.itemTemplate.isEmpty()) {
               this.itemTemplate = new ItemStack((ItemLike)DollRegistry.TWO_I_DOLL_ITEM.get());
            }

            UUID dollId = TwoIDollItem.ensureIdentity(this.itemTemplate);
            TwoICompanionshipData.Sample sample = TwoICompanionshipData.get(server.getServer()).sample(dollId, this.memory(), time);
            TwoIDollMemory merged = sample.progress().merge(this.memory());
            this.interaction = merged.interaction();
            this.lastCompanionSample = merged.lastCompanionSample();
            if (sample.awarded()) {
               this.changeBond(1);
               owner.displayClientMessage(Component.translatable("msg.godofthings.two_i.companionship"), true);
               DollNetwork.updateDialogue(owner, this.getId(), this.canInvite(), this.giftScore);
            }
         }
      }
   }

   private void tickDollFriendship(ServerLevel server) {
      if (this.friendId == null) {
         if (this.tickCount % 5 == 0) {
            if (this.ownerId != null && this.friendshipOwnerPresent(server)) {
               TwoIDollEntity candidate = null;
               double best = 5.0625;

               for (TwoIDollEntity other : server.getEntitiesOfClass(
                  TwoIDollEntity.class,
                  this.getBoundingBox().inflate(2.25),
                  otherx -> otherx != this && otherx.friendId == null && otherx.ownerId != null && otherx.friendshipOwnerPresent(server)
               )) {
                  double distance = this.distanceToSqr(other);
                  if (distance <= best) {
                     best = distance;
                     candidate = other;
                  }
               }

               if (candidate != null) {
                  this.establishFriendship(candidate, server);
               }
            }
         }
      } else {
         if (server.getEntity(this.friendId) instanceof TwoIDollEntity friend
            && !friend.isRemoved()
            && friend.friendId != null
            && friend.friendId.equals(this.getUUID())
            && this.friendshipOwnerPresent(server)
            && friend.friendshipOwnerPresent(server)) {
            this.maintainSharedUmbrella(friend);
            return;
         }

         this.clearFriendship();
      }
   }

   private boolean friendshipOwnerPresent(ServerLevel server) {
      if (this.ownerId == null) {
         return false;
      } else {
         ServerPlayer owner = server.getServer().getPlayerList().getPlayer(this.ownerId);
         return owner != null && owner.isAlive() && !owner.isSpectator() && owner.serverLevel() == server && this.distanceToSqr(owner) <= 2304.0;
      }
   }

   private void establishFriendship(TwoIDollEntity other, ServerLevel server) {
      this.friendId = other.getUUID();
      other.friendId = this.getUUID();
      boolean thisHolder = this.getUUID().compareTo(other.getUUID()) < 0;
      this.entityData.set(SHARED_UMBRELLA, true);
      this.entityData.set(SHARED_UMBRELLA_HOLDER, thisHolder);
      other.entityData.set(SHARED_UMBRELLA, true);
      other.entityData.set(SHARED_UMBRELLA_HOLDER, !thisHolder);
      this.playPoseAction(1);
      other.playPoseAction(1);
      this.maintainSharedUmbrella(other);
   }

   private void maintainSharedUmbrella(TwoIDollEntity other) {
      if (this.getUUID().compareTo(other.getUUID()) <= 0) {
         double dx = other.getX() - this.getX();
         double dz = other.getZ() - this.getZ();
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         if (horizontal < 1.0E-4) {
            dx = 1.0;
            dz = 0.0;
            horizontal = 1.0;
         }

         double nx = dx / horizontal;
         double nz = dz / horizontal;
         double midX = (this.getX() + other.getX()) * 0.5;
         double midY = (this.getY() + other.getY()) * 0.5;
         double midZ = (this.getZ() + other.getZ()) * 0.5;
         double half = 0.17;
         this.noPhysics = true;
         other.noPhysics = true;
         this.setNoGravity(true);
         other.setNoGravity(true);
         this.setPos(midX - nx * half, midY, midZ - nz * half);
         other.setPos(midX + nx * half, midY, midZ + nz * half);
         this.setDeltaMovement(Vec3.ZERO);
         other.setDeltaMovement(Vec3.ZERO);
      }
   }

   private void clearFriendship() {
      UUID previous = this.friendId;
      this.friendId = null;
      this.noPhysics = false;
      this.setNoGravity(false);
      this.entityData.set(SHARED_UMBRELLA, false);
      this.entityData.set(SHARED_UMBRELLA_HOLDER, true);
      if (previous != null && this.level() instanceof ServerLevel server) {
         if (server.getEntity(previous) instanceof TwoIDollEntity other && this.getUUID().equals(other.friendId)) {
            other.friendId = null;
            other.noPhysics = false;
            other.setNoGravity(false);
            other.entityData.set(SHARED_UMBRELLA, false);
            other.entityData.set(SHARED_UMBRELLA_HOLDER, true);
         }
      }
   }

   public void keepInteraction(ServerPlayer player, boolean active) {
      if (!active) {
         this.interactionViewers.remove(player.getUUID());
      } else if (this.canReadStory(player)) {
         this.interactionViewers.put(player.getUUID(), this.level().getGameTime() + 60L);
         this.getNavigation().stop();
         this.setDeltaMovement(0.0, this.isNoGravity() ? 0.0 : this.getDeltaMovement().y, 0.0);
      }
   }

   public boolean isInteractionHeld() {
      return !this.interactionViewers.isEmpty();
   }

   private void expireInteractionViewers() {
      if (this.level() instanceof ServerLevel server && !this.interactionViewers.isEmpty()) {
         long now = server.getGameTime();
         this.interactionViewers.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(entry.getKey());
            return entry.getValue() <= now || player == null || !this.canReadStory(player);
         });
         return;
      }
   }

   private static int clampBond(int score) {
      return Math.max(-100, Math.min(100, score));
   }

   private void changeBond(int delta) {
      this.giftScore = clampBond(this.giftScore + delta);
   }

   public void tick() {
      this.expireInteractionViewers();
      if (this.isNoGravity() && !this.isInteractionHeld() && this.level() instanceof ServerLevel server) {
         ServerPlayer owner = this.ownerId == null ? null : server.getServer().getPlayerList().getPlayer(this.ownerId);
         if (owner == null || owner.level() != this.level() || !owner.isAlive() || owner.isSpectator() || !owner.getAbilities().flying && !owner.isFallFlying()
            )
          {
            this.setNoGravity(false);
         }
      }

      this.setInvulnerable(true);
      this.clearFire();
      if (!this.isRemoved()) {
         if (super.getHealth() != 20.0F) {
            super.setHealth(20.0F);
         }

         this.dead = false;
         this.deathTime = 0;
         this.hurtTime = 0;
      }

      super.tick();
      if (this.isInteractionHeld()) {
         this.getNavigation().stop();
         this.setDeltaMovement(0.0, this.isNoGravity() ? 0.0 : this.getDeltaMovement().y, 0.0);
      }

      if (!this.level().isClientSide && this.level() instanceof ServerLevel serverx) {
         this.updateEnvironmentReaction(serverx);
         if (this.isRemoved()) {
            return;
         }

         this.tickDollFriendship(serverx);
         this.tickCompanionship(serverx);
         if (this.prankCooldown > 0) {
            this.prankCooldown--;
         }

         if (this.patCooldown > 0) {
            this.patCooldown--;
         }

         if (this.giftCooldown > 0) {
            this.giftCooldown--;
         }

         if (this.talkCooldown > 0) {
            this.talkCooldown--;
         }

         if (this.tickCount % 20 == 0) {
            this.storySessions.values().removeIf(session -> session.expired(serverx.getGameTime()));
         }

         if (this.timeStopCooldown > 0) {
            this.timeStopCooldown--;
         }

         if (this.panicCooldown > 0) {
            this.panicCooldown--;
         }

         if (this.getPoseAction() != 0 && this.getPoseActionProgress(0.0F) >= 1.0F) {
            this.playPoseAction(0);
         }

         if (this.getPoseAction() == 0 && this.tickCount % 20 == 0 && serverx.getRandom().nextInt(18) == 0) {
            int[] idle = new int[]{6, 2, 1, 10, 11, 12};
            this.playPoseAction(idle[serverx.getRandom().nextInt(idle.length)]);
         }

         if (!this.isInteractionHeld() && this.prankCooldown == 0 && this.tickCount % 20 == 0) {
            this.performPrank(serverx);
            this.prankCooldown = prankCooldownForBond(this.giftScore, serverx.getRandom());
         }
      }
   }

   private void updateEnvironmentReaction(ServerLevel server) {
      int flags = TwoIDollEnvironment.conditions(server, this.blockPosition());
      if (flags != this.getEnvironmentAction()) {
         this.entityData.set(ENVIRONMENT_ACTION, flags);
         this.entityData.set(ENVIRONMENT_STARTED_AT, server.getGameTime());
      }

      long now = server.getGameTime();
      if ((flags & 2) != 0 && Math.floorMod(now + (long)this.getUUID().hashCode(), 420) == 18) {
         server.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.62, this.getZ(), 4, 0.08, 0.04, 0.08, 0.01);
         this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 0.22F, 1.8F);
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public boolean isInvulnerableTo(DamageSource source) {
      return true;
   }

   public boolean isInvulnerable() {
      return true;
   }

   public void setInvulnerable(boolean invulnerable) {
      super.setInvulnerable(true);
   }

   public float getHealth() {
      return 20.0F;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      EntityDataAccessor<Float> healthKey = LivingHealthDataAccessor.godofthings$healthKey();
      if (key.equals(healthKey) && (Float)this.getEntityData().get(healthKey) != 20.0F) {
         this.getEntityData().set(healthKey, 20.0F);
      }

      if (key.equals(DATA_SHARED_FLAGS_ID)) {
         byte flags = (Byte)this.getEntityData().get(DATA_SHARED_FLAGS_ID);
         if ((flags & 32) != 0) {
            this.getEntityData().set(DATA_SHARED_FLAGS_ID, (byte)(flags & -33));
         }
      } else if (key.equals(DATA_POSE) && this.getPose() == Pose.DYING) {
         this.getEntityData().set(DATA_POSE, Pose.STANDING);
      }

      super.onSyncedDataUpdated(key);
   }

   public void setInvisible(boolean invisible) {
      super.setInvisible(false);
   }

   public void setPose(Pose pose) {
      if (pose != Pose.DYING) {
         super.setPose(pose);
      }
   }

   public boolean blocksInvalidPosition(double x, double y, double z) {
      return this.isAddedToLevel()
         && !this.isRemoved()
         && (
            !Double.isFinite(x)
               || !Double.isFinite(y)
               || !Double.isFinite(z)
               || Math.abs(x) > 3.0E7
               || Math.abs(z) > 3.0E7
               || y < (double)this.level().getMinBuildHeight() - 16.0
         );
   }

   public void handleEntityEvent(byte id) {
      if (id != 3) {
         super.handleEntityEvent(id);
      }
   }

   public void setLevelCallback(EntityInLevelCallback callback) {
      if (callback != EntityInLevelCallback.NULL || !TwoILifecycleGuard.blocks(this)) {
         super.setLevelCallback(callback);
      }
   }

   public boolean isAlive() {
      return !this.isRemoved();
   }

   public void setHealth(float health) {
      super.setHealth(20.0F);
   }

   public double getAttributeValue(Holder<Attribute> attribute) {
      return attribute.is(Attributes.MAX_HEALTH) ? 20.0 : super.getAttributeValue(attribute);
   }

   public boolean isDeadOrDying() {
      return false;
   }

   protected void tickDeath() {
      this.deathTime = 0;
      this.dead = false;
   }

   public boolean blocksDestructiveRemoval(RemovalReason reason) {
      return this.isAddedToLevel()
         && !this.authorizedReclaim
         && !TwoILifecycleGuard.allows(this)
         && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED);
   }

   public void remove(RemovalReason reason) {
      if (!this.blocksDestructiveRemoval(reason)) {
         super.remove(reason);
      }
   }

   public boolean discardForReclaim() {
      if (this.isRemoved()) {
         return false;
      } else {
         this.authorizedReclaim = true;

         boolean var1;
         try {
            this.setRemoved(RemovalReason.DISCARDED);
            var1 = this.isRemoved();
         } finally {
            this.authorizedReclaim = false;
         }

         return var1;
      }
   }

   public void die(DamageSource source) {
   }

   public void kill() {
   }

   public boolean isAttackable() {
      return false;
   }

   public boolean canBeSeenAsEnemy() {
      return false;
   }

   public boolean isPickable() {
      return true;
   }

   public boolean canBeHitByProjectile() {
      return false;
   }

   public boolean isPushable() {
      return !this.isInteractionHeld() && !this.hasSharedUmbrella();
   }

   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public void openDialogue(ServerPlayer player) {
      if (!this.isOwner(player)) {
         player.displayClientMessage(Component.translatable("msg.godofthings.two_i.owner_only"), true);
      } else if (this.canReadStory(player) && !(this.distanceToSqr(player) > 64.0)) {
         this.keepInteraction(player, true);
         this.getLookControl().setLookAt(player, 45.0F, 45.0F);
         this.playPoseAction(2);
         DollNetwork.sendDialogue(player, this.getId(), this.canInvite(), this.giftScore);
      }
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(POSE_ACTION, 0);
      builder.define(POSE_STARTED_AT, 0L);
      builder.define(ENVIRONMENT_ACTION, 0);
      builder.define(ENVIRONMENT_STARTED_AT, 0L);
      builder.define(SHARED_UMBRELLA, false);
      builder.define(SHARED_UMBRELLA_HOLDER, true);
      builder.define(BUBBLE_TEXT, "");
      builder.define(BUBBLE_UNTIL, 0L);
      builder.define(BUBBLE_MESSAGE, Component.empty());
   }

   public int getPoseAction() {
      return (Integer)this.entityData.get(POSE_ACTION);
   }

   public int getEnvironmentAction() {
      return (Integer)this.entityData.get(ENVIRONMENT_ACTION);
   }

   public String getBubbleText() {
      return !((String)this.entityData.get(BUBBLE_TEXT)).isEmpty() && this.level().getGameTime() <= this.entityData.get(BUBBLE_UNTIL)
         ? (String)this.entityData.get(BUBBLE_TEXT)
         : "";
   }

   public Component getBubbleMessage() {
      return (Component)(this.getBubbleText().isEmpty() ? Component.empty() : (Component)this.entityData.get(BUBBLE_MESSAGE));
   }

   private void setBubbleText(String key, Object... arguments) {
      if (!this.level().isClientSide) {
         this.entityData.set(BUBBLE_TEXT, key == null ? "" : key);
         this.entityData.set(BUBBLE_MESSAGE, key == null ? Component.empty() : Component.translatable(key, arguments));
         this.entityData.set(BUBBLE_UNTIL, this.level().getGameTime() + 200L);
      }
   }

   public float getEnvironmentActionProgress(float partialTick) {
      float elapsed = Math.max(0.0F, (float)(this.level().getGameTime() - (Long)this.entityData.get(ENVIRONMENT_STARTED_AT)) + partialTick);
      return Math.min(1.0F, elapsed / 40.0F);
   }

   public float getPoseActionProgress(float partialTick) {
      int duration = switch (this.getPoseAction()) {
         case 1 -> 36;
         case 2 -> 42;
         case 3 -> 40;
         case 4 -> 48;
         case 5 -> 38;
         case 6, 10, 11, 12 -> 64;
         default -> 1;
      };
      long now = this.level().getGameTime();
      float elapsed = Math.max(0.0F, (float)(now - (Long)this.entityData.get(POSE_STARTED_AT)) + partialTick);
      return Math.min(1.0F, elapsed / (float)duration);
   }

   void playPoseAction(int action) {
      if (!this.level().isClientSide) {
         this.entityData.set(POSE_ACTION, action);
         this.entityData.set(POSE_STARTED_AT, this.level().getGameTime());
      }
   }

   public boolean patBy(ServerPlayer player) {
      if (this.canReadStory(player) && !(this.distanceToSqr(player) > 64.0) && this.patCooldown <= 0) {
         this.patCooldown = 30;
         this.refreshInteractionDay();
         boolean rewarded = this.interaction.pats() < 1;
         if (rewarded) {
            this.changeBond(1);
            this.interaction = new TwoIDollMemory.Interaction(
               this.interaction.day(),
               this.interaction.pats() + 1,
               this.interaction.talks(),
               this.interaction.stories(),
               this.interaction.companionship(),
               this.interaction.companionTicks(),
               this.interaction.lastChat(),
               this.interaction.lastGift(),
               this.interaction.repeatedGift(),
               this.interaction.gifts()
            );
         }

         this.getLookControl().setLookAt(player, 45.0F, 45.0F);
         this.playPoseAction(3);
         if (this.level() instanceof ServerLevel server) {
            SoundEvent sound = (SoundEvent)DollSounds.TWO_I_PAT.get();
            server.playSound(null, this.blockPosition(), sound, SoundSource.NEUTRAL, 0.8F, 1.05F + this.random.nextFloat() * 0.2F);
         }

         player.displayClientMessage(
            Component.translatable(rewarded ? "msg.godofthings.two_i.patted" : "msg.godofthings.two_i.patted_familiar"), true
         );
         DollNetwork.updateDialogue(player, this.getId(), this.canInvite(), this.giftScore);
         return true;
      } else {
         return false;
      }
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (player.isShiftKeyDown()) {
            this.patBy(serverPlayer);
         } else {
            this.openDialogue(serverPlayer);
         }
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide());
   }

   public void reclaim(ServerPlayer player) {
      if (!(this.distanceToSqr(player) > 64.0) && !this.isRemoved()) {
         if (this.ownerId != null && this.ownerId.equals(player.getUUID())) {
            ItemStack stack = this.toDollItem();
            ItemStack acquired = stack.copy();
            if (this.discardForReclaim()) {
               TwoIDollEntity.ItemStackFactory.give(player, stack);
               TwoIDollAcquisition.notifyAcquired(player, acquired);
            }
         }
      }
   }

   public void reclaimRemote(ServerPlayer player) {
      if (!this.isRemoved() && this.ownerId != null && this.ownerId.equals(player.getUUID())) {
         ItemStack stack = this.toDollItem();
         ItemStack acquired = stack.copy();
         if (this.discardForReclaim()) {
            TwoIDollEntity.ItemStackFactory.give(player, stack);
            TwoIDollAcquisition.notifyAcquired(player, acquired);
            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.reclaimed"), true);
         }
      }
   }

   public void acceptGift(ServerPlayer player) {
      if (this.canReadStory(player)) {
         ItemStack stack = player.getMainHandItem();
         if (stack.isEmpty()) {
            this.setBubbleText("msg.godofthings.two_i.no_gift");
            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.no_gift"), true);
         } else if (isCockroachGift(stack)) {
            stack.shrink(1);
            this.giftScore = -100;
            this.giftCooldown = 0;
            this.playPoseAction(2);
            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.cockroach_panic"), true);
            if (this.panicCooldown == 0) {
               this.panicCooldown = 6000;
               this.panicDragonSpawned = false;
               this.spawnPanicDragon(player);
            }

            DollNetwork.updateDialogue(player, this.getId(), this.canInvite(), this.giftScore);
         } else {
            boolean specialGift = isSpecialGift(stack);
            int bondChange = giftBondChange(stack);
            this.refreshInteractionDay();
            if (!specialGift && bondChange > 0 && this.interaction.gifts() >= 1) {
               this.acceptGiftWithoutBond(player, stack, "msg.godofthings.two_i.gift_daily_limit");
            } else if (!specialGift && this.giftCooldown > 0) {
               this.acceptGiftWithoutBond(player, stack, "msg.godofthings.two_i.gift_cooldown");
            } else {
               String giftId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
               int repetitions = giftId.equals(this.interaction.lastGift()) ? Math.min(4, this.interaction.repeatedGift() + 1) : 1;
               boolean repeated = !specialGift && bondChange > 0 && repetitions >= 3;
               if (repeated) {
                  this.acceptGiftWithoutBond(player, stack, "msg.godofthings.two_i.gift_repeated");
               } else if (this.giftScore >= 100 && bondChange > 0) {
                  this.setBubbleText("msg.godofthings.two_i.gift_full");
                  player.displayClientMessage(Component.translatable("msg.godofthings.two_i.gift_full"), true);
               } else {
                  stack.shrink(1);
                  this.interaction = new TwoIDollMemory.Interaction(
                     this.interaction.day(),
                     this.interaction.pats(),
                     this.interaction.talks(),
                     this.interaction.stories(),
                     this.interaction.companionship(),
                     this.interaction.companionTicks(),
                     this.interaction.lastChat(),
                     giftId,
                     repetitions,
                     !specialGift && bondChange > 0 ? 1 : this.interaction.gifts()
                  );
                  this.changeBond(bondChange);
                  if (!specialGift) {
                     this.giftCooldown = 1200;
                  }

                  this.playPoseAction(4);
                  this.setBubbleText("msg.godofthings.two_i.gift." + (bondChange > 0 ? "liked" : "disliked"), Math.abs(bondChange), this.giftScore, 100);
                  player.displayClientMessage(
                     bondChange == 0
                        ? Component.translatable("msg.godofthings.two_i.gift.neutral", new Object[]{this.giftScore, 100})
                        : Component.translatable(
                           "msg.godofthings.two_i.gift." + (bondChange > 0 ? "liked" : "disliked"),
                           new Object[]{Math.abs(bondChange), this.giftScore, 100}
                        ),
                     true
                  );
                  if (repeated) {
                     player.displayClientMessage(Component.translatable("msg.godofthings.two_i.gift_repeated"), false);
                  }

                  DollNetwork.updateDialogue(player, this.getId(), this.canInvite(), this.giftScore);
               }
            }
         }
      }
   }

   private void acceptGiftWithoutBond(ServerPlayer player, ItemStack stack, String bubbleKey) {
      stack.shrink(1);
      this.playPoseAction(4);
      this.setBubbleText(bubbleKey);
      player.displayClientMessage(Component.translatable(bubbleKey), true);
      DollNetwork.updateDialogue(player, this.getId(), this.canInvite(), this.giftScore);
   }

   public void talkTo(ServerPlayer player) {
      if (this.canReadStory(player)) {
         if (this.talkCooldown > 0) {
            this.setBubbleText("msg.godofthings.two_i.talk_cooldown", Math.max(1, (this.talkCooldown + 19) / 20));
            player.displayClientMessage(
               Component.translatable("msg.godofthings.two_i.talk_cooldown", new Object[]{Math.max(1, (this.talkCooldown + 19) / 20)}), true
            );
         } else {
            this.talkCooldown = 200;
            this.refreshInteractionDay();

            int line;
            do {
               line = this.random.nextInt(4) == 0
                  ? (this.giftScore < 0 ? 34 : (this.giftScore >= 70 ? 38 : 30)) + this.random.nextInt(4)
                  : this.random.nextInt(30);
            } while (line == this.interaction.lastChat());

            int bondChange = this.interaction.talks() < 2 ? 1 : 0;
            this.changeBond(bondChange);
            this.interaction = new TwoIDollMemory.Interaction(
               this.interaction.day(),
               this.interaction.pats(),
               Math.min(2, this.interaction.talks() + 1),
               this.interaction.stories(),
               this.interaction.companionship(),
               this.interaction.companionTicks(),
               line,
               this.interaction.lastGift(),
               this.interaction.repeatedGift(),
               this.interaction.gifts()
            );
            this.getLookControl().setLookAt(player, 45.0F, 45.0F);
            this.playPoseAction(bondChange > 0 ? 1 : 2);
            this.setBubbleText("msg.godofthings.two_i.talk." + line);
            if (bondChange == 0) {
               this.entityData
                  .set(
                     BUBBLE_MESSAGE, this.getBubbleMessage().copy().append("\n").append(Component.translatable("msg.godofthings.two_i.talk_daily_limit"))
                  );
            }

            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.talk." + line), false);
            DollNetwork.updateDialogue(player, this.getId(), this.canInvite(), this.giftScore);
         }
      }
   }

   private static boolean isSpecialGift(ItemStack stack) {
      ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return key != null && SPECIAL_GIFTS.contains(key.toString());
   }

   private static int giftBondChange(ItemStack stack) {
      if (isSpecialGift(stack)) {
         return 5;
      } else {
         ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
         if (key == null) {
            return 0;
         } else {
            String path = key.getPath().toLowerCase(Locale.ROOT);
            if (path.contains("ginger")) {
               return -6;
            } else if (path.contains("roach") || path.contains("cockroach")) {
               return -5;
            } else if (isBugGift(path)) {
               return -2;
            } else if (!path.contains("flower")
               && !path.contains("cookie")
               && !path.contains("cake")
               && !path.contains("honey")
               && !path.contains("berry")
               && !path.contains("berries")
               && !path.contains("sweet")
               && !path.contains("chocolate")
               && !path.contains("candy")) {
               return Set.of(
                        "minecraft:diamond",
                        "minecraft:emerald",
                        "minecraft:gold_ingot",
                        "minecraft:netherite_ingot",
                        "minecraft:golden_apple",
                        "minecraft:enchanted_golden_apple"
                     )
                     .contains(key.toString())
                  ? 1
                  : 1;
            } else {
               return 1;
            }
         }
      }
   }

   private static boolean isCockroachGift(ItemStack stack) {
      ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (key == null) {
         return false;
      } else {
         String id = key.toString().toLowerCase(Locale.ROOT);
         String path = key.getPath().toLowerCase(Locale.ROOT);
         return id.contains("cockroach")
            || id.contains("roach")
            || path.contains("蜚蠊")
            || path.contains("蟑螂")
            || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains("cockroach")
            || stack.getHoverName().getString().contains("蟑螂")
            || stack.getHoverName().getString().contains("蜚蠊");
      }
   }

   private void spawnPanicDragon(ServerPlayer player) {
      if (!this.panicDragonSpawned && this.level() instanceof ServerLevel server) {
         Entity dragon = EntityType.ENDER_DRAGON.create(server);
         if (dragon != null) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 spawn = player.position().add(look.x * 12.0, 3.0, look.z * 12.0);
            double y = Math.max((double)server.getMinBuildHeight() + 8.0, Math.min((double)server.getMaxBuildHeight() - 8.0, spawn.y));
            dragon.moveTo(spawn.x, y, spawn.z, player.getYRot(), 0.0F);
            dragon.getPersistentData().putUUID("useless_stretcher_2i_prank_owner", player.getUUID());
            if (server.addFreshEntity(dragon)) {
               this.panicDragonSpawned = true;
            }
         }
      }
   }

   private static boolean isBugGift(String path) {
      Set<String> bugs = Set.of(
         "roach",
         "cockroach",
         "insect",
         "bug",
         "beetle",
         "spider",
         "ant",
         "termite",
         "mosquito",
         "wasp",
         "bee",
         "beehive",
         "cricket",
         "centipede",
         "millipede",
         "locust",
         "mantis",
         "scorpion",
         "fly",
         "butterfly",
         "caterpillar",
         "moth",
         "silverfish"
      );

      for (String word : path.split("[/_\\-.]")) {
         if (bugs.contains(word)) {
            return true;
         }
      }

      return false;
   }

   public boolean canInvite() {
      return true && (this.giftScore >= 100 || this.invitationUsed);
   }

   /**
    * 接受「去星空玩一会儿」的邀请。
    *
    * <p>移植版<b>没有星光庭院维度</b>，所以这里只发一条提示，不做传送。
    * 保留这个方法是为了让界面上的按钮不至于点了没反应（也给以后加回来留接口）。</p>
    */
   public void acceptInvitation(ServerPlayer player) {
      if (this.canReadStory(player) && this.canInvite()) {
         TwoIDollService.message(player, "starfield_not_ported", true);
      }
   }

   private static int prankCooldownForBond(int bond, RandomSource random) {
      int clampedBond = clampBond(bond);
      int normalizedBond = clampedBond - -100;
      int range = 4500;
      int baseline = 300 + range * normalizedBond / 200;
      return baseline + random.nextInt(Math.max(1, baseline / 4));
   }

   private void performPrank(ServerLevel server) {
      RandomSource random = server.getRandom();
      this.playPoseAction(1);
      int intensity = Math.min(4, Math.max(0, -this.giftScore / 25));

      int action;
      do {
         action = random.nextInt(12);
      } while (action == this.lastPrank);

      this.lastPrank = action;
      ServerPlayer owner = this.ownerId == null ? null : server.getServer().getPlayerList().getPlayer(this.ownerId);
      Vec3 center = owner != null && owner.serverLevel() == server ? owner.position() : this.position();
      if (action == 1) {
         this.spawnPrankMobs(server, center, intensity, random);
      } else if (action == 3) {
         this.performToolPrank(server, owner, center, random);
      } else if (action == 4) {
         performMagnetPrank(server, owner, center);
      } else if (action == 5) {
         this.performBuffPrank(owner);
      } else if (action == 6) {
         performSparkPrank(server, center, random);
      } else if (action == 7) {
         this.performBlinkPrank(server, owner, center, random);
      } else if (action == 8) {
         performSoundPrank(server, center, random);
      } else if (action == 9) {
         performEmotionPrank(server, owner, center);
      } else if (action == 10) {
         performGlowPrank(server, center);
      } else if (action == 11) {
         performTrailPrank(server, owner, center);
      } else {
         ParticleOptions particle = action == 0 ? ParticleTypes.WITCH : ParticleTypes.END_ROD;
         int count = 8 + intensity * 8;
         server.sendParticles(particle, center.x, center.y + 1.0, center.z, count, 0.45 + (double)intensity * 0.08, 0.55, 0.45 + (double)intensity * 0.08, 0.02);
         server.playSound(
            null,
            BlockPos.containing(center),
            action == 0 ? SoundEvents.ENDERMAN_TELEPORT : SoundEvents.VILLAGER_AMBIENT,
            SoundSource.NEUTRAL,
            0.35F + (float)intensity * 0.08F,
            0.8F + random.nextFloat() * (0.25F + (float)intensity * 0.08F)
         );
      }


      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.godofthings.two_i.prank"), true);
      }
   }

   private void performToolPrank(ServerLevel server, ServerPlayer owner, Vec3 center, RandomSource random) {
      BlockPos origin = BlockPos.containing(center);
      boolean foundLeaves = false;

      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 3, 3))) {
         BlockState state = server.getBlockState(pos);
         if (state.is(BlockTags.LEAVES)) {
            foundLeaves = true;
            server.sendParticles(ParticleTypes.CRIT, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 8, 0.35, 0.35, 0.35, 0.02);
         }
      }

      if (!foundLeaves) {
         server.sendParticles(ParticleTypes.CRIT, center.x, center.y + 1.0, center.z, 14, 0.45, 0.6, 0.45, 0.03);
      }

      server.playSound(null, origin, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 0.55F, 1.25F + random.nextFloat() * 0.15F);
      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.godofthings.two_i.staff_prank.tool"), true);
      }
   }

   private static void performMagnetPrank(ServerLevel server, ServerPlayer owner, Vec3 center) {
      AABB area = new AABB(center, center).inflate(8.0);

      for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, area)) {
         Vec3 delta = center.add(0.0, 0.8, 0.0).subtract(item.position());
         if (!(delta.lengthSqr() < 0.01)) {
            item.setDeltaMovement(item.getDeltaMovement().scale(0.55).add(delta.normalize().scale(0.08)));
            item.hasImpulse = true;
         }
      }

      server.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.8, center.z, 12, 0.55, 0.35, 0.55, 0.025);
      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.godofthings.two_i.staff_prank.magnet"), true);
      }
   }

   private void performBuffPrank(ServerPlayer owner) {
      if (owner != null) {
         owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, true, false, true));
         owner.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 0, true, false, true));
         owner.displayClientMessage(Component.translatable("msg.godofthings.two_i.staff_prank.buff"), true);
      }
   }

   private static void performSparkPrank(ServerLevel server, Vec3 center, RandomSource random) {
      server.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1.0, center.z, 18, 0.35, 0.5, 0.35, 0.025);
      server.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 1.0, center.z, 12, 0.45, 0.55, 0.45, 0.04);
      server.playSound(null, BlockPos.containing(center), SoundEvents.FLINTANDSTEEL_USE, SoundSource.NEUTRAL, 0.45F, 1.0F + random.nextFloat() * 0.2F);
   }

   private void performBlinkPrank(ServerLevel server, ServerPlayer owner, Vec3 center, RandomSource random) {
      if (owner != null && owner.serverLevel() == server) {
         Vec3 side = owner.position().add(owner.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale(random.nextBoolean() ? 1.5 : -1.5));
         double y = owner.getY();
         BlockPos feet = BlockPos.containing(side.x, y, side.z);
         if (server.isEmptyBlock(feet) && server.isEmptyBlock(feet.above())) {
            this.moveTo(side.x, y, side.z, this.getYRot() + 180.0F, this.getXRot());
         }
      }

      server.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.6, this.getZ(), 16, 0.3, 0.45, 0.3, 0.08);
      server.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.35F, 1.25F + random.nextFloat() * 0.15F);
   }

   private static void performSoundPrank(ServerLevel server, Vec3 center, RandomSource random) {
      SoundEvent[] sounds = new SoundEvent[]{
         SoundEvents.VILLAGER_AMBIENT, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundEvents.ENDER_PEARL_THROW, SoundEvents.EXPERIENCE_ORB_PICKUP
      };
      server.playSound(null, BlockPos.containing(center), sounds[random.nextInt(sounds.length)], SoundSource.NEUTRAL, 0.45F, 0.7F + random.nextFloat() * 0.6F);
      server.sendParticles(ParticleTypes.NOTE, center.x, center.y + 1.2, center.z, 8, 0.35, 0.4, 0.35, 0.02);
   }

   private static void performEmotionPrank(ServerLevel server, ServerPlayer owner, Vec3 center) {
      if (owner != null) {
         owner.addEffect(new MobEffectInstance(MobEffects.JUMP, 70, 0, true, false, true));
         owner.addEffect(new MobEffectInstance(MobEffects.GLOWING, 70, 0, true, false, true));
      }

      server.sendParticles(ParticleTypes.HEART, center.x, center.y + 1.0, center.z, 10, 0.45, 0.55, 0.45, 0.02);
   }

   private static void performGlowPrank(ServerLevel server, Vec3 center) {
      AABB area = new AABB(center, center).inflate(8.0);
      List<Mob> mobs = server.getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive() && !(mob instanceof TwoIDollEntity));
      if (!mobs.isEmpty()) {
         Mob target = mobs.get(server.getRandom().nextInt(mobs.size()));
         target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false, true));
         server.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + (double)target.getBbHeight(), target.getZ(), 10, 0.3, 0.25, 0.3, 0.01);
      } else {
         server.sendParticles(ParticleTypes.GLOW, center.x, center.y + 1.0, center.z, 10, 0.4, 0.5, 0.4, 0.01);
      }
   }

   private static void performTrailPrank(ServerLevel server, ServerPlayer owner, Vec3 center) {
      Vec3 origin = owner == null ? center : owner.position();
      server.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y + 0.15, origin.z, 12, 0.65, 0.08, 0.65, 0.015);
      server.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, origin.x, origin.y + 1.0, origin.z, 8, 0.55, 0.5, 0.55, 0.01);
   }

   public void startStory(ServerPlayer player) {
      if (!this.canReadStory(player)) {
         DollNetwork.sendStoryState(player, this.getId(), 0L, 0, this.giftScore, 2);
      } else {
         long now = this.level().getGameTime();
         this.keepInteraction(player, true);
         TwoIStorySession session = this.storySessions.get(player.getUUID());
         if (session != null && session.expired(now)) {
            this.storySessions.remove(player.getUUID());
            session = null;
         }

         if (session == null) {
            if (this.talkCooldown > 0) {
               player.displayClientMessage(
                  Component.translatable("msg.godofthings.two_i.talk_cooldown", new Object[]{Math.max(1, (this.talkCooldown + 19) / 20)}), true
               );
               DollNetwork.sendStoryState(player, this.getId(), 0L, 0, this.giftScore, 2);
               return;
            }

            TwoIStoryData progress = TwoIStoryData.get(player.serverLevel().getServer());
            int chapter = progress.nextChapter(player.getUUID(), this.giftScore);
            session = new TwoIStorySession(player.getUUID(), chapter, now);
            this.storySessions.put(player.getUUID(), session);
            this.talkCooldown = 200;
         }

         this.getLookControl().setLookAt(player, 45.0F, 45.0F);
         this.playPoseAction(2);
         DollNetwork.sendStoryState(player, this.getId(), session.id(), session.chapter(), this.giftScore, 0, session.expectedRound());
      }
   }

   private boolean canReadStory(ServerPlayer player) {
      return !this.level().isClientSide
         && !this.isRemoved()
         && this.isAlive()
         && player.isAlive()
         && this.isOwner(player)
         && this.level() == player.level()
         && this.distanceToSqr(player) <= 100.0;
   }

   public boolean isOwner(Player player) {
      return this.ownerId != null && this.ownerId.equals(player.getUUID());
   }

   public void storyChoice(ServerPlayer player, long sessionId, int choice) {
      TwoIStorySession session = this.storySessions.get(player.getUUID());
      if (this.canReadStory(player) && session != null && session.accept(player.getUUID(), sessionId, choice, this.level().getGameTime())) {
         if (session.finished()) {
            this.storySessions.remove(player.getUUID());
         }

         int delta = TwoIStorySession.bondDelta(TwoIStorySession.choiceIndex(choice));
         this.refreshInteractionDay();
         if (delta > 0) {
            if (this.interaction.stories() >= 3) {
               delta = 0;
            } else {
               this.interaction = new TwoIDollMemory.Interaction(
                  this.interaction.day(),
                  this.interaction.pats(),
                  this.interaction.talks(),
                  this.interaction.stories() + 1,
                  this.interaction.companionship(),
                  this.interaction.companionTicks(),
                  this.interaction.lastChat(),
                  this.interaction.lastGift(),
                  this.interaction.repeatedGift(),
                  this.interaction.gifts()
               );
            }
         }

         this.changeBond(delta);
         this.talkCooldown = Math.max(this.talkCooldown, 200);
         if (session.finished()) {
            TwoIStoryData.get(player.serverLevel().getServer()).complete(player.getUUID(), session.chapter());
         }

         this.getLookControl().setLookAt(player, 45.0F, 45.0F);
         this.playPoseAction(delta < 0 ? 2 : 1);
         DollNetwork.sendStoryState(player, this.getId(), sessionId, session.chapter(), this.giftScore, 1, TwoIStorySession.choiceRound(choice));
         DollNetwork.updateDialogue(player, this.getId(), this.canInvite(), this.giftScore);
      } else {
         DollNetwork.sendStoryState(player, this.getId(), sessionId, 0, this.giftScore, 2, TwoIStorySession.choiceRound(choice));
      }
   }

   private void spawnPrankMobs(ServerLevel server, Vec3 center, int intensity, RandomSource random) {
      if (this.ownerId != null) {
         long nearbySummons = (long)server.getEntities(
               this,
               this.getBoundingBox().inflate(48.0),
               entity -> entity.getPersistentData().hasUUID("useless_stretcher_2i_prank_owner")
                     && this.ownerId.equals(entity.getPersistentData().getUUID("useless_stretcher_2i_prank_owner"))
            )
            .size();
         if (nearbySummons < 6L) {
            int count = 1 + intensity / 2;
            int attemptsPerMob = 4;
            List<EntityType<?>> candidates = PRANK_MOB_TIERS.get(Math.min(3, intensity));

            for (int i = 0; i < count && nearbySummons < 6L; i++) {
               EntityType<?> type = candidates.get(random.nextInt(candidates.size()));
               if (isSafePrankMob(type)) {
                  Entity created = type.create(server);
                  if (created instanceof Mob) {
                     Mob mob = (Mob)created;
                     mob.setNoAi(false);

                     for (int attempt = 0; attempt < attemptsPerMob; attempt++) {
                        int radius = 1 + intensity / 2;
                        BlockPos feet = BlockPos.containing(
                           center.x + (double)random.nextInt(radius * 2 + 1) - (double)radius,
                           center.y,
                           center.z + (double)random.nextInt(radius * 2 + 1) - (double)radius
                        );
                        if (server.hasChunkAt(feet)
                           && server.getBlockState(feet.below()).isFaceSturdy(server, feet.below(), Direction.UP)
                           && server.isEmptyBlock(feet)
                           && server.isEmptyBlock(feet.above())) {
                           created.moveTo((double)feet.getX() + 0.5, (double)feet.getY(), (double)feet.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                           created.getPersistentData().putUUID("useless_stretcher_2i_prank_owner", this.ownerId);
                           if (server.addFreshEntity(created)) {
                              nearbySummons++;
                              break;
                           }
                        }
                     }

                     if (!created.isAddedToLevel()) {
                        created.discard();
                     }
                  } else if (created != null) {
                     created.discard();
                  }
               }
            }
         }
      }
   }

   private static boolean isSafePrankMob(EntityType<?> type) {
      if (type != EntityType.ENDER_DRAGON && type != EntityType.WITHER && !PRANK_MOB_TIERS.stream().noneMatch(tier -> tier.contains(type))) {
         ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
         if (id == null) {
            return false;
         } else {
            String path = id.getPath().toLowerCase(Locale.ROOT);
            return !path.contains("dragon") && !path.contains("chaos") && !path.contains("boss");
         }
      } else {
         return false;
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.itemTemplate = ItemStack.EMPTY;
      this.interaction = TwoIDollMemory.Interaction.empty();
      this.patCooldown = 0;
      this.ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
      this.giftScore = tag.contains("bond") ? clampBond(tag.getInt("bond")) : clampBond(Math.max(0, tag.getInt("gift_score")) * 10);
      this.invitationUsed = tag.getBoolean("invitation_used");
      this.prankCooldown = Math.clamp(tag.contains("prank_cooldown") ? (long)tag.getInt("prank_cooldown") : 1200L, 0, 24000);
      this.lastPrank = Math.clamp((long)tag.getInt("last_prank"), -1, 11);
      this.timeStopCooldown = Math.clamp((long)tag.getInt("time_stop_cooldown"), 0, 24000);
      this.panicCooldown = Math.clamp((long)tag.getInt("panic_cooldown"), 0, 24000);
      this.panicDragonSpawned = tag.getBoolean("panic_dragon_spawned");
      this.giftCooldown = Math.clamp((long)tag.getInt("gift_cooldown"), 0, 24000);
      this.talkCooldown = Math.clamp((long)tag.getInt("talk_cooldown"), 0, 24000);
      if (tag.contains("doll_item")) {
         ItemStack.parse(this.registryAccess(), tag.getCompound("doll_item")).ifPresent(this::restoreFromItem);
      }

      this.storySessions.clear();
      this.setNoGravity(false);
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.put("doll_item", this.toDollItem().save(this.registryAccess()));
      if (this.ownerId != null) {
         tag.putUUID("owner", this.ownerId);
      }

      tag.putInt("bond", this.giftScore);
      tag.putBoolean("invitation_used", this.invitationUsed);
      tag.putInt("prank_cooldown", this.prankCooldown);
      tag.putInt("last_prank", this.lastPrank);
      tag.putInt("time_stop_cooldown", this.timeStopCooldown);
      tag.putInt("panic_cooldown", this.panicCooldown);
      tag.putBoolean("panic_dragon_spawned", this.panicDragonSpawned);
      tag.putInt("gift_cooldown", this.giftCooldown);
      tag.putInt("talk_cooldown", this.talkCooldown);
   }

   public Component getName() {
      return Component.translatable("entity.godofthings.two_i_doll");
   }

   private static final class FollowOwnerGoal extends Goal {
      private static final double START_DISTANCE_SQR = 100.0;
      private static final double STOP_DISTANCE_SQR = 16.0;
      private final TwoIDollEntity doll;
      private Player owner;
      private int recalc;

      private FollowOwnerGoal(TwoIDollEntity doll) {
         this.doll = doll;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.doll.level() instanceof ServerLevel level && this.doll.ownerId != null) {
            this.owner = level.getServer().getPlayerList().getPlayer(this.doll.ownerId);
            return !this.doll.isInteractionHeld()
               && this.owner != null
               && this.owner.isAlive()
               && !this.owner.isSpectator()
               && level.getServer().getPlayerList().getPlayer(this.doll.ownerId) == this.owner
               && this.owner.level() == this.doll.level()
               && (this.doll.distanceToSqr(this.owner) > 100.0 || this.doll.isNoGravity() && this.ownerFlying());
         }

         return false;
      }

      public boolean canContinueToUse() {
         return !this.doll.isInteractionHeld()
            && this.owner != null
            && this.owner.isAlive()
            && !this.owner.isSpectator()
            && ((ServerLevel)this.doll.level()).getServer().getPlayerList().getPlayer(this.doll.ownerId) == this.owner
            && this.owner.level() == this.doll.level()
            && (this.doll.distanceToSqr(this.owner) > 16.0 || this.doll.isNoGravity() && this.ownerFlying());
      }

      public void tick() {
         if (this.owner != null) {
            this.doll.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);
            if (this.ownerFlying()) {
               this.doll.setNoGravity(true);
               this.doll.getNavigation().stop();
               this.doll.fallDistance = 0.0F;
               double distance = this.doll.distanceToSqr(this.owner);
               if (!(distance > 1024.0) || !this.catchUpFlying()) {
                  Vec3 offset = this.owner.position().add(0.0, 0.3, 0.0).subtract(this.doll.position());
                  Vec3 desired = distance > 16.0 ? offset.normalize().scale(Math.min(1.1, 0.35 + Math.sqrt(distance) * 0.025)) : Vec3.ZERO;
                  this.doll.setDeltaMovement(this.doll.getDeltaMovement().scale(0.65).add(desired.scale(0.35)));
                  this.faceOwnerDirectly();
               }
            } else {
               this.doll.setNoGravity(false);
               if (++this.recalc % 5 == 0) {
                  double distance = this.doll.distanceToSqr(this.owner);
                  if (!(distance > 196.0) || !this.catchUp()) {
                     this.doll.getNavigation().moveTo(this.owner, distance > 64.0 ? 1.8 : 1.35);
                  }
               }
            }
         }
      }

      private boolean ownerFlying() {
         return this.owner != null && (this.owner.getAbilities().flying || this.owner.isFallFlying());
      }

      private void faceOwnerDirectly() {
         if (this.owner != null) {
            Vec3 target = this.owner.getEyePosition();
            Vec3 delta = target.subtract(this.doll.getEyePosition());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float yaw = (float)(Mth.atan2(delta.z, delta.x) * 180.0F / (float)Math.PI) - 90.0F;
            float pitch = (float)(-(Mth.atan2(delta.y, horizontal) * 180.0F / (float)Math.PI));
            this.doll.setYRot(yaw);
            this.doll.setXRot(pitch);
            this.doll.yRotO = yaw;
            this.doll.xRotO = pitch;
            this.doll.yHeadRot = yaw;
            this.doll.yHeadRotO = yaw;
            this.doll.yBodyRot = yaw;
            this.doll.yBodyRotO = yaw;
         }
      }

      private boolean catchUpFlying() {
         ServerLevel level = (ServerLevel)this.doll.level();

         for (int attempt = 0; attempt < 12; attempt++) {
            Vec3 destination = this.owner
               .position()
               .add((double)(this.doll.random.nextInt(7) - 3), (double)this.doll.random.nextInt(3), (double)(this.doll.random.nextInt(7) - 3));
            BlockPos pos = BlockPos.containing(destination);
            if (!(destination.distanceToSqr(this.owner.position()) < 4.0)
               && level.hasChunkAt(pos)
               && level.isInWorldBounds(pos)
               && level.getWorldBorder().isWithinBounds(pos)
               && level.getFluidState(pos).isEmpty()
               && level.noCollision(this.doll, this.doll.getBoundingBox().move(destination.subtract(this.doll.position())))) {
               this.doll.teleportTo(destination.x, destination.y, destination.z);
               this.doll.setDeltaMovement(Vec3.ZERO);
               return true;
            }
         }

         return false;
      }

      private boolean catchUp() {
         ServerLevel level = (ServerLevel)this.doll.level();
         BlockPos center = this.owner.blockPosition();

         for (int attempt = 0; attempt < 12; attempt++) {
            int x = this.doll.random.nextInt(7) - 3;
            int z = this.doll.random.nextInt(7) - 3;
            if (Math.abs(x) >= 2 || Math.abs(z) >= 2) {
               BlockPos pos = center.offset(x, this.doll.random.nextInt(3) - 1, z);
               if (level.hasChunkAt(pos)
                  && level.getWorldBorder().isWithinBounds(pos)
                  && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                  && level.getFluidState(pos).isEmpty()) {
                  Vec3 destination = Vec3.atBottomCenterOf(pos);
                  if (level.noCollision(this.doll, this.doll.getBoundingBox().move(destination.subtract(this.doll.position())))) {
                     this.doll.getNavigation().stop();
                     this.doll.teleportTo(destination.x, destination.y, destination.z);
                     this.doll.setDeltaMovement(Vec3.ZERO);
                     return true;
                  }
               }
            }
         }

         return false;
      }

      public void stop() {
         this.owner = null;
         this.recalc = 0;
         this.doll.getNavigation().stop();
         if (!this.doll.isInteractionHeld()) {
            this.doll.setNoGravity(false);
         }

         this.doll.setDeltaMovement(Vec3.ZERO);
      }
   }

   private static final class ItemStackFactory {
      private static void give(ServerPlayer player, ItemStack stack) {
         if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
         }

         player.getInventory().setChanged();
      }
   }
}
