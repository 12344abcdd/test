package net.minecraft.client.player;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Portal.Transition;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class LocalPlayer extends AbstractClientPlayer {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final int POSITION_REMINDER_INTERVAL = 20;
   private static final int WATER_VISION_MAX_TIME = 600;
   private static final int WATER_VISION_QUICK_TIME = 100;
   private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
   private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35D;
   private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962633907794952D;
   public final ClientPacketListener connection;
   private final StatsCounter stats;
   private final ClientRecipeBook recipeBook;
   private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
   private int permissionLevel = 0;
   private double xLast;
   private double yLast1;
   private double zLast;
   private float yRotLast;
   private float xRotLast;
   private boolean lastOnGround;
   private boolean crouching;
   private boolean wasShiftKeyDown;
   private boolean wasSprinting;
   private int positionReminder;
   private boolean flashOnSetHealth;
   public Input input;
   protected final Minecraft minecraft;
   protected int sprintTriggerTime;
   public float yBob;
   public float xBob;
   public float yBobO;
   public float xBobO;
   private int jumpRidingTicks;
   private float jumpRidingScale;
   public float spinningEffectIntensity;
   public float oSpinningEffectIntensity;
   private boolean startedUsingItem;
   @Nullable
   private InteractionHand usingItemHand;
   private boolean handsBusy;
   private boolean autoJumpEnabled = true;
   private int autoJumpTime;
   private boolean wasFallFlying;
   private int waterVisionTime;
   private boolean showDeathScreen = true;
   private boolean doLimitedCrafting = false;

   public LocalPlayer(Minecraft minecraft, ClientLevel clientLevel, ClientPacketListener clientPacketListener, StatsCounter statsCounter, ClientRecipeBook clientRecipeBook, boolean bl, boolean bl2) {
      super(clientLevel, clientPacketListener.getLocalGameProfile());
      this.minecraft = minecraft;
      this.connection = clientPacketListener;
      this.stats = statsCounter;
      this.recipeBook = clientRecipeBook;
      this.wasShiftKeyDown = bl;
      this.wasSprinting = bl2;
      this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, minecraft.getSoundManager()));
      this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
      this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, minecraft.getSoundManager(), clientLevel.getBiomeManager()));
   }

   public boolean hurt(DamageSource damageSource, float f) {
      return false;
   }

   public void heal(float f) {
   }

   public boolean startRiding(Entity entity, boolean bl) {
      if (!super.startRiding(entity, bl)) {
         return false;
      } else {
         if (entity instanceof AbstractMinecart) {
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)entity, true));
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)entity, false));
         }

         return true;
      }
   }

   public void removeVehicle() {
      super.removeVehicle();
      this.handsBusy = false;
   }

   public float getViewXRot(float f) {
      return this.getXRot();
   }

   public float getViewYRot(float f) {
      return this.isPassenger() ? super.getViewYRot(f) : this.getYRot();
   }

   public void tick() {
      if (this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())) {
         super.tick();
         if (this.isPassenger()) {
            this.connection.send(new Rot(this.getYRot(), this.getXRot(), this.onGround()));
            this.connection.send(new ServerboundPlayerInputPacket(this.xxa, this.zza, this.input.jumping, this.input.shiftKeyDown));
            Entity entity = this.getRootVehicle();
            if (entity != this && entity.isControlledByLocalInstance()) {
               this.connection.send(new ServerboundMoveVehiclePacket(entity));
               this.sendIsSprintingIfNeeded();
            }
         } else {
            this.sendPosition();
         }

         Iterator var3 = this.ambientSoundHandlers.iterator();

         while(var3.hasNext()) {
            AmbientSoundHandler ambientSoundHandler = (AmbientSoundHandler)var3.next();
            ambientSoundHandler.tick();
         }

      }
   }

   public float getCurrentMood() {
      Iterator var1 = this.ambientSoundHandlers.iterator();

      AmbientSoundHandler ambientSoundHandler;
      do {
         if (!var1.hasNext()) {
            return 0.0F;
         }

         ambientSoundHandler = (AmbientSoundHandler)var1.next();
      } while(!(ambientSoundHandler instanceof BiomeAmbientSoundsHandler));

      return ((BiomeAmbientSoundsHandler)ambientSoundHandler).getMoodiness();
   }

   private void sendPosition() {
      this.sendIsSprintingIfNeeded();
      boolean bl = this.isShiftKeyDown();
      if (bl != this.wasShiftKeyDown) {
         Action action = bl ? Action.PRESS_SHIFT_KEY : Action.RELEASE_SHIFT_KEY;
         this.connection.send(new ServerboundPlayerCommandPacket(this, action));
         this.wasShiftKeyDown = bl;
      }

      if (this.isControlledCamera()) {
         double d = this.getX() - this.xLast;
         double e = this.getY() - this.yLast1;
         double f = this.getZ() - this.zLast;
         double g = (double)(this.getYRot() - this.yRotLast);
         double h = (double)(this.getXRot() - this.xRotLast);
         ++this.positionReminder;
         boolean bl2 = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4D) || this.positionReminder >= 20;
         boolean bl3 = g != 0.0D || h != 0.0D;
         if (this.isPassenger()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.connection.send(new PosRot(vec3.x, -999.0D, vec3.z, this.getYRot(), this.getXRot(), this.onGround()));
            bl2 = false;
         } else if (bl2 && bl3) {
            this.connection.send(new PosRot(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround()));
         } else if (bl2) {
            this.connection.send(new Pos(this.getX(), this.getY(), this.getZ(), this.onGround()));
         } else if (bl3) {
            this.connection.send(new Rot(this.getYRot(), this.getXRot(), this.onGround()));
         } else if (this.lastOnGround != this.onGround()) {
            this.connection.send(new StatusOnly(this.onGround()));
         }

         if (bl2) {
            this.xLast = this.getX();
            this.yLast1 = this.getY();
            this.zLast = this.getZ();
            this.positionReminder = 0;
         }

         if (bl3) {
            this.yRotLast = this.getYRot();
            this.xRotLast = this.getXRot();
         }

         this.lastOnGround = this.onGround();
         this.autoJumpEnabled = (Boolean)this.minecraft.options.autoJump().get();
      }

   }

   private void sendIsSprintingIfNeeded() {
      boolean bl = this.isSprinting();
      if (bl != this.wasSprinting) {
         Action action = bl ? Action.START_SPRINTING : Action.STOP_SPRINTING;
         this.connection.send(new ServerboundPlayerCommandPacket(this, action));
         this.wasSprinting = bl;
      }

   }

   public boolean drop(boolean bl) {
      net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action action = bl ? net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM;
      ItemStack itemStack = this.getInventory().removeFromSelected(bl);
      this.connection.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
      return !itemStack.isEmpty();
   }

   public void swing(InteractionHand interactionHand) {
      super.swing(interactionHand);
      this.connection.send(new ServerboundSwingPacket(interactionHand));
   }

   public void respawn() {
      this.connection.send(new ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
      KeyMapping.resetToggleKeys();
   }

   protected void actuallyHurt(DamageSource damageSource, float f) {
      if (!this.isInvulnerableTo(damageSource)) {
         this.setHealth(this.getHealth() - f);
      }
   }

   public void closeContainer() {
      this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
      this.clientSideCloseContainer();
   }

   public void clientSideCloseContainer() {
      super.closeContainer();
      this.minecraft.setScreen((Screen)null);
   }

   public void hurtTo(float f) {
      if (this.flashOnSetHealth) {
         float g = this.getHealth() - f;
         if (g <= 0.0F) {
            this.setHealth(f);
            if (g < 0.0F) {
               this.invulnerableTime = 10;
            }
         } else {
            this.lastHurt = g;
            this.invulnerableTime = 20;
            this.setHealth(f);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }
      } else {
         this.setHealth(f);
         this.flashOnSetHealth = true;
      }

   }

   public void onUpdateAbilities() {
      this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
   }

   public boolean isLocalPlayer() {
      return true;
   }

   public boolean isSuppressingSlidingDownLadder() {
      return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
   }

   public boolean canSpawnSprintParticle() {
      return !this.getAbilities().flying && super.canSpawnSprintParticle();
   }

   protected void sendRidingJump() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
   }

   public void sendOpenInventory() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, Action.OPEN_INVENTORY));
   }

   public StatsCounter getStats() {
      return this.stats;
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   public void removeRecipeHighlight(RecipeHolder<?> recipeHolder) {
      if (this.recipeBook.willHighlight(recipeHolder)) {
         this.recipeBook.removeHighlight(recipeHolder);
         this.connection.send(new ServerboundRecipeBookSeenRecipePacket(recipeHolder));
      }

   }

   protected int getPermissionLevel() {
      return this.permissionLevel;
   }

   public void setPermissionLevel(int i) {
      this.permissionLevel = i;
   }

   public void displayClientMessage(Component component, boolean bl) {
      this.minecraft.getChatListener().handleSystemMessage(component, bl);
   }

   private void moveTowardsClosestSpace(double d, double e) {
      BlockPos blockPos = BlockPos.containing(d, this.getY(), e);
      if (this.suffocatesAt(blockPos)) {
         double f = d - (double)blockPos.getX();
         double g = e - (double)blockPos.getZ();
         Direction direction = null;
         double h = Double.MAX_VALUE;
         Direction[] directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
         Direction[] var14 = directions;
         int var15 = directions.length;

         for(int var16 = 0; var16 < var15; ++var16) {
            Direction direction2 = var14[var16];
            double i = direction2.getAxis().choose(f, 0.0D, g);
            double j = direction2.getAxisDirection() == AxisDirection.POSITIVE ? 1.0D - i : i;
            if (j < h && !this.suffocatesAt(blockPos.relative(direction2))) {
               h = j;
               direction = direction2;
            }
         }

         if (direction != null) {
            Vec3 vec3 = this.getDeltaMovement();
            if (direction.getAxis() == Axis.X) {
               this.setDeltaMovement(0.1D * (double)direction.getStepX(), vec3.y, vec3.z);
            } else {
               this.setDeltaMovement(vec3.x, vec3.y, 0.1D * (double)direction.getStepZ());
            }
         }

      }
   }

   private boolean suffocatesAt(BlockPos blockPos) {
      AABB aABB = this.getBoundingBox();
      AABB aABB2 = (new AABB((double)blockPos.getX(), aABB.minY, (double)blockPos.getZ(), (double)blockPos.getX() + 1.0D, aABB.maxY, (double)blockPos.getZ() + 1.0D)).deflate(1.0E-7D);
      return this.level().collidesWithSuffocatingBlock(this, aABB2);
   }

   public void setExperienceValues(float f, int i, int j) {
      this.experienceProgress = f;
      this.totalExperience = i;
      this.experienceLevel = j;
   }

   public void sendSystemMessage(Component component) {
      this.minecraft.gui.getChat().addMessage(component);
   }

   public void handleEntityEvent(byte b) {
      if (b >= 24 && b <= 28) {
         this.setPermissionLevel(b - 24);
      } else {
         super.handleEntityEvent(b);
      }

   }

   public void setShowDeathScreen(boolean bl) {
      this.showDeathScreen = bl;
   }

   public boolean shouldShowDeathScreen() {
      return this.showDeathScreen;
   }

   public void setDoLimitedCrafting(boolean bl) {
      this.doLimitedCrafting = bl;
   }

   public boolean getDoLimitedCrafting() {
      return this.doLimitedCrafting;
   }

   public void playSound(SoundEvent soundEvent, float f, float g) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), soundEvent, this.getSoundSource(), f, g, false);
   }

   public void playNotifySound(SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), soundEvent, soundSource, f, g, false);
   }

   public boolean isEffectiveAi() {
      return true;
   }

   public void startUsingItem(InteractionHand interactionHand) {
      ItemStack itemStack = this.getItemInHand(interactionHand);
      if (!itemStack.isEmpty() && !this.isUsingItem()) {
         super.startUsingItem(interactionHand);
         this.startedUsingItem = true;
         this.usingItemHand = interactionHand;
      }
   }

   public boolean isUsingItem() {
      return this.startedUsingItem;
   }

   public void stopUsingItem() {
      super.stopUsingItem();
      this.startedUsingItem = false;
   }

   public InteractionHand getUsedItemHand() {
      return (InteractionHand)Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
      super.onSyncedDataUpdated(entityDataAccessor);
      if (DATA_LIVING_ENTITY_FLAGS.equals(entityDataAccessor)) {
         boolean bl = ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
         InteractionHand interactionHand = ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (bl && !this.startedUsingItem) {
            this.startUsingItem(interactionHand);
         } else if (!bl && this.startedUsingItem) {
            this.stopUsingItem();
         }
      }

      if (DATA_SHARED_FLAGS_ID.equals(entityDataAccessor) && this.isFallFlying() && !this.wasFallFlying) {
         this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
      }

   }

   @Nullable
   public PlayerRideableJumping jumpableVehicle() {
      Entity var2 = this.getControlledVehicle();
      PlayerRideableJumping var10000;
      if (var2 instanceof PlayerRideableJumping) {
         PlayerRideableJumping playerRideableJumping = (PlayerRideableJumping)var2;
         if (playerRideableJumping.canJump()) {
            var10000 = playerRideableJumping;
            return var10000;
         }
      }

      var10000 = null;
      return var10000;
   }

   public float getJumpRidingScale() {
      return this.jumpRidingScale;
   }

   public boolean isTextFilteringEnabled() {
      return this.minecraft.isTextFilteringEnabled();
   }

   public void openTextEdit(SignBlockEntity signBlockEntity, boolean bl) {
      if (signBlockEntity instanceof HangingSignBlockEntity) {
         HangingSignBlockEntity hangingSignBlockEntity = (HangingSignBlockEntity)signBlockEntity;
         this.minecraft.setScreen(new HangingSignEditScreen(hangingSignBlockEntity, bl, this.minecraft.isTextFilteringEnabled()));
      } else {
         this.minecraft.setScreen(new SignEditScreen(signBlockEntity, bl, this.minecraft.isTextFilteringEnabled()));
      }

   }

   public void openMinecartCommandBlock(BaseCommandBlock baseCommandBlock) {
      this.minecraft.setScreen(new MinecartCommandBlockEditScreen(baseCommandBlock));
   }

   public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
      this.minecraft.setScreen(new CommandBlockEditScreen(commandBlockEntity));
   }

   public void openStructureBlock(StructureBlockEntity structureBlockEntity) {
      this.minecraft.setScreen(new StructureBlockEditScreen(structureBlockEntity));
   }

   public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
      this.minecraft.setScreen(new JigsawBlockEditScreen(jigsawBlockEntity));
   }

   public void openItemGui(ItemStack itemStack, InteractionHand interactionHand) {
      if (itemStack.is(Items.WRITABLE_BOOK)) {
         this.minecraft.setScreen(new BookEditScreen(this, itemStack, interactionHand));
      }

   }

   public void crit(Entity entity) {
      this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
   }

   public void magicCrit(Entity entity) {
      this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
   }

   public boolean isShiftKeyDown() {
      return this.input != null && this.input.shiftKeyDown;
   }

   public boolean isCrouching() {
      return this.crouching;
   }

   public boolean isMovingSlowly() {
      return this.isCrouching() || this.isVisuallyCrawling();
   }

   public void serverAiStep() {
      super.serverAiStep();
      if (this.isControlledCamera()) {
         this.xxa = this.input.leftImpulse;
         this.zza = this.input.forwardImpulse;
         this.jumping = this.input.jumping;
         this.yBobO = this.yBob;
         this.xBobO = this.xBob;
         this.xBob += (this.getXRot() - this.xBob) * 0.5F;
         this.yBob += (this.getYRot() - this.yBob) * 0.5F;
      }

   }

   protected boolean isControlledCamera() {
      return this.minecraft.getCameraEntity() == this;
   }

   public void resetPos() {
      this.setPose(Pose.STANDING);
      if (this.level() != null) {
         for(double d = this.getY(); d > (double)this.level().getMinBuildHeight() && d < (double)this.level().getMaxBuildHeight(); ++d) {
            this.setPos(this.getX(), d, this.getZ());
            if (this.level().noCollision(this)) {
               break;
            }
         }

         this.setDeltaMovement(Vec3.ZERO);
         this.setXRot(0.0F);
      }

      this.setHealth(this.getMaxHealth());
      this.deathTime = 0;
   }

   public void aiStep() {
      if (this.sprintTriggerTime > 0) {
         --this.sprintTriggerTime;
      }

      if (!(this.minecraft.screen instanceof ReceivingLevelScreen)) {
         this.handleConfusionTransitionEffect(this.getActivePortalLocalTransition() == Transition.CONFUSION);
         this.processPortalCooldown();
      }

      boolean bl = this.input.jumping;
      boolean bl2 = this.input.shiftKeyDown;
      boolean bl3 = this.hasEnoughImpulseToStartSprinting();
      Abilities abilities = this.getAbilities();
      this.crouching = !abilities.flying && !this.isSwimming() && !this.isPassenger() && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING) && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
      float f = (float)this.getAttributeValue(Attributes.SNEAKING_SPEED);
      this.input.tick(this.isMovingSlowly(), f);
      this.minecraft.getTutorial().onInput(this.input);
      if (this.isUsingItem() && !this.isPassenger()) {
         Input var10000 = this.input;
         var10000.leftImpulse *= 0.2F;
         var10000 = this.input;
         var10000.forwardImpulse *= 0.2F;
         this.sprintTriggerTime = 0;
      }

      boolean bl4 = false;
      if (this.autoJumpTime > 0) {
         --this.autoJumpTime;
         bl4 = true;
         this.input.jumping = true;
      }

      if (!this.noPhysics) {
         this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35D, this.getZ() + (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35D, this.getZ() - (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35D, this.getZ() - (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35D, this.getZ() + (double)this.getBbWidth() * 0.35D);
      }

      if (bl2) {
         this.sprintTriggerTime = 0;
      }

      boolean bl5 = this.canStartSprinting();
      boolean bl6 = this.isPassenger() ? this.getVehicle().onGround() : this.onGround();
      boolean bl7 = !bl2 && !bl3;
      if ((bl6 || this.isUnderWater()) && bl7 && bl5) {
         if (this.sprintTriggerTime <= 0 && !this.minecraft.options.keySprint.isDown()) {
            this.sprintTriggerTime = 7;
         } else {
            this.setSprinting(true);
         }
      }

      if ((!this.isInWater() || this.isUnderWater()) && bl5 && this.minecraft.options.keySprint.isDown()) {
         this.setSprinting(true);
      }

      boolean bl8;
      if (this.isSprinting()) {
         bl8 = !this.input.hasForwardImpulse() || !this.hasEnoughFoodToStartSprinting();
         boolean bl9 = bl8 || this.horizontalCollision && !this.minorHorizontalCollision || this.isInWater() && !this.isUnderWater();
         if (this.isSwimming()) {
            if (!this.onGround() && !this.input.shiftKeyDown && bl8 || !this.isInWater()) {
               this.setSprinting(false);
            }
         } else if (bl9) {
            this.setSprinting(false);
         }
      }

      bl8 = false;
      if (abilities.mayfly) {
         if (this.minecraft.gameMode.isAlwaysFlying()) {
            if (!abilities.flying) {
               abilities.flying = true;
               bl8 = true;
               this.onUpdateAbilities();
            }
         } else if (!bl && this.input.jumping && !bl4) {
            if (this.jumpTriggerTime == 0) {
               this.jumpTriggerTime = 7;
            } else if (!this.isSwimming()) {
               abilities.flying = !abilities.flying;
               if (abilities.flying && this.onGround()) {
                  this.jumpFromGround();
               }

               bl8 = true;
               this.onUpdateAbilities();
               this.jumpTriggerTime = 0;
            }
         }
      }

      if (this.input.jumping && !bl8 && !bl && !abilities.flying && !this.isPassenger() && !this.onClimbable()) {
         ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
         if (itemStack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemStack) && this.tryToStartFallFlying()) {
            this.connection.send(new ServerboundPlayerCommandPacket(this, Action.START_FALL_FLYING));
         }
      }

      this.wasFallFlying = this.isFallFlying();
      if (this.isInWater() && this.input.shiftKeyDown && this.isAffectedByFluids()) {
         this.goDownInWater();
      }

      int i;
      if (this.isEyeInFluid(FluidTags.WATER)) {
         i = this.isSpectator() ? 10 : 1;
         this.waterVisionTime = Mth.clamp(this.waterVisionTime + i, 0, 600);
      } else if (this.waterVisionTime > 0) {
         this.isEyeInFluid(FluidTags.WATER);
         this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
      }

      if (abilities.flying && this.isControlledCamera()) {
         i = 0;
         if (this.input.shiftKeyDown) {
            --i;
         }

         if (this.input.jumping) {
            ++i;
         }

         if (i != 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)((float)i * abilities.getFlyingSpeed() * 3.0F), 0.0D));
         }
      }

      PlayerRideableJumping playerRideableJumping = this.jumpableVehicle();
      if (playerRideableJumping != null && playerRideableJumping.getJumpCooldown() == 0) {
         if (this.jumpRidingTicks < 0) {
            ++this.jumpRidingTicks;
            if (this.jumpRidingTicks == 0) {
               this.jumpRidingScale = 0.0F;
            }
         }

         if (bl && !this.input.jumping) {
            this.jumpRidingTicks = -10;
            playerRideableJumping.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
            this.sendRidingJump();
         } else if (!bl && this.input.jumping) {
            this.jumpRidingTicks = 0;
            this.jumpRidingScale = 0.0F;
         } else if (bl) {
            ++this.jumpRidingTicks;
            if (this.jumpRidingTicks < 10) {
               this.jumpRidingScale = (float)this.jumpRidingTicks * 0.1F;
            } else {
               this.jumpRidingScale = 0.8F + 2.0F / (float)(this.jumpRidingTicks - 9) * 0.1F;
            }
         }
      } else {
         this.jumpRidingScale = 0.0F;
      }

      super.aiStep();
      if (this.onGround() && abilities.flying && !this.minecraft.gameMode.isAlwaysFlying()) {
         abilities.flying = false;
         this.onUpdateAbilities();
      }

   }

   public Transition getActivePortalLocalTransition() {
      return this.portalProcess == null ? Transition.NONE : this.portalProcess.getPortalLocalTransition();
   }

   protected void tickDeath() {
      ++this.deathTime;
      if (this.deathTime == 20) {
         this.remove(RemovalReason.KILLED);
      }

   }

   private void handleConfusionTransitionEffect(boolean bl) {
      this.oSpinningEffectIntensity = this.spinningEffectIntensity;
      float f = 0.0F;
      if (bl && this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
         if (this.minecraft.screen != null && !this.minecraft.screen.isPauseScreen() && !(this.minecraft.screen instanceof DeathScreen) && !(this.minecraft.screen instanceof WinScreen)) {
            if (this.minecraft.screen instanceof AbstractContainerScreen) {
               this.closeContainer();
            }

            this.minecraft.setScreen((Screen)null);
         }

         if (this.spinningEffectIntensity == 0.0F) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
         }

         f = 0.0125F;
         this.portalProcess.setAsInsidePortalThisTick(false);
      } else if (this.hasEffect(MobEffects.CONFUSION) && !this.getEffect(MobEffects.CONFUSION).endsWithin(60)) {
         f = 0.006666667F;
      } else if (this.spinningEffectIntensity > 0.0F) {
         f = -0.05F;
      }

      this.spinningEffectIntensity = Mth.clamp(this.spinningEffectIntensity + f, 0.0F, 1.0F);
   }

   public void rideTick() {
      super.rideTick();
      this.handsBusy = false;
      Entity var2 = this.getControlledVehicle();
      if (var2 instanceof Boat) {
         Boat boat = (Boat)var2;
         boat.setInput(this.input.left, this.input.right, this.input.up, this.input.down);
         this.handsBusy |= this.input.left || this.input.right || this.input.up || this.input.down;
      }

   }

   public boolean isHandsBusy() {
      return this.handsBusy;
   }

   @Nullable
   public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> holder) {
      if (holder.is(MobEffects.CONFUSION)) {
         this.oSpinningEffectIntensity = 0.0F;
         this.spinningEffectIntensity = 0.0F;
      }

      return super.removeEffectNoUpdate(holder);
   }

   public void move(MoverType moverType, Vec3 vec3) {
      double d = this.getX();
      double e = this.getZ();
      super.move(moverType, vec3);
      this.updateAutoJump((float)(this.getX() - d), (float)(this.getZ() - e));
   }

   public boolean isAutoJumpEnabled() {
      return this.autoJumpEnabled;
   }

   protected void updateAutoJump(float f, float g) {
      if (this.canAutoJump()) {
         Vec3 vec3 = this.position();
         Vec3 vec32 = vec3.add((double)f, 0.0D, (double)g);
         Vec3 vec33 = new Vec3((double)f, 0.0D, (double)g);
         float h = this.getSpeed();
         float i = (float)vec33.lengthSqr();
         float l;
         if (i <= 0.001F) {
            Vec2 vec2 = this.input.getMoveVector();
            float j = h * vec2.x;
            float k = h * vec2.y;
            l = Mth.sin(this.getYRot() * 0.017453292F);
            float m = Mth.cos(this.getYRot() * 0.017453292F);
            vec33 = new Vec3((double)(j * m - k * l), vec33.y, (double)(k * m + j * l));
            i = (float)vec33.lengthSqr();
            if (i <= 0.001F) {
               return;
            }
         }

         float n = Mth.invSqrt(i);
         Vec3 vec34 = vec33.scale((double)n);
         Vec3 vec35 = this.getForward();
         l = (float)(vec35.x * vec34.x + vec35.z * vec34.z);
         if (!(l < -0.15F)) {
            CollisionContext collisionContext = CollisionContext.of(this);
            BlockPos blockPos = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState blockState = this.level().getBlockState(blockPos);
            if (blockState.getCollisionShape(this.level(), blockPos, collisionContext).isEmpty()) {
               blockPos = blockPos.above();
               BlockState blockState2 = this.level().getBlockState(blockPos);
               if (blockState2.getCollisionShape(this.level(), blockPos, collisionContext).isEmpty()) {
                  float o = 7.0F;
                  float p = 1.2F;
                  if (this.hasEffect(MobEffects.JUMP)) {
                     p += (float)(this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.75F;
                  }

                  float q = Math.max(h * 7.0F, 1.0F / n);
                  Vec3 vec37 = vec32.add(vec34.scale((double)q));
                  float r = this.getBbWidth();
                  float s = this.getBbHeight();
                  AABB aABB = (new AABB(vec3, vec37.add(0.0D, (double)s, 0.0D))).inflate((double)r, 0.0D, (double)r);
                  Vec3 vec36 = vec3.add(0.0D, 0.5099999904632568D, 0.0D);
                  vec37 = vec37.add(0.0D, 0.5099999904632568D, 0.0D);
                  Vec3 vec38 = vec34.cross(new Vec3(0.0D, 1.0D, 0.0D));
                  Vec3 vec39 = vec38.scale((double)(r * 0.5F));
                  Vec3 vec310 = vec36.subtract(vec39);
                  Vec3 vec311 = vec37.subtract(vec39);
                  Vec3 vec312 = vec36.add(vec39);
                  Vec3 vec313 = vec37.add(vec39);
                  Iterable<VoxelShape> iterable = this.level().getCollisions(this, aABB);
                  Iterator<AABB> iterator = StreamSupport.stream(iterable.spliterator(), false).flatMap((voxelShapex) -> {
                     return voxelShapex.toAabbs().stream();
                  }).iterator();
                  float t = Float.MIN_VALUE;

                  label73:
                  while(iterator.hasNext()) {
                     AABB aABB2 = (AABB)iterator.next();
                     if (aABB2.intersects(vec310, vec311) || aABB2.intersects(vec312, vec313)) {
                        t = (float)aABB2.maxY;
                        Vec3 vec314 = aABB2.getCenter();
                        BlockPos blockPos2 = BlockPos.containing(vec314);
                        int u = 1;

                        while(true) {
                           if (!((float)u < p)) {
                              break label73;
                           }

                           BlockPos blockPos3 = blockPos2.above(u);
                           BlockState blockState3 = this.level().getBlockState(blockPos3);
                           VoxelShape voxelShape;
                           if (!(voxelShape = blockState3.getCollisionShape(this.level(), blockPos3, collisionContext)).isEmpty()) {
                              t = (float)voxelShape.max(Axis.Y) + (float)blockPos3.getY();
                              if ((double)t - this.getY() > (double)p) {
                                 return;
                              }
                           }

                           if (u > 1) {
                              blockPos = blockPos.above();
                              BlockState blockState4 = this.level().getBlockState(blockPos);
                              if (!blockState4.getCollisionShape(this.level(), blockPos, collisionContext).isEmpty()) {
                                 return;
                              }
                           }

                           ++u;
                        }
                     }
                  }

                  if (t != Float.MIN_VALUE) {
                     float v = (float)((double)t - this.getY());
                     if (!(v <= 0.5F) && !(v > p)) {
                        this.autoJumpTime = 1;
                     }
                  }
               }
            }
         }
      }
   }

   protected boolean isHorizontalCollisionMinor(Vec3 vec3) {
      float f = this.getYRot() * 0.017453292F;
      double d = (double)Mth.sin(f);
      double e = (double)Mth.cos(f);
      double g = (double)this.xxa * e - (double)this.zza * d;
      double h = (double)this.zza * e + (double)this.xxa * d;
      double i = Mth.square(g) + Mth.square(h);
      double j = Mth.square(vec3.x) + Mth.square(vec3.z);
      if (!(i < 9.999999747378752E-6D) && !(j < 9.999999747378752E-6D)) {
         double k = g * vec3.x + h * vec3.z;
         double l = Math.acos(k / Math.sqrt(i * j));
         return l < 0.13962633907794952D;
      } else {
         return false;
      }
   }

   private boolean canAutoJump() {
      return this.isAutoJumpEnabled() && this.autoJumpTime <= 0 && this.onGround() && !this.isStayingOnGroundSurface() && !this.isPassenger() && this.isMoving() && (double)this.getBlockJumpFactor() >= 1.0D;
   }

   private boolean isMoving() {
      Vec2 vec2 = this.input.getMoveVector();
      return vec2.x != 0.0F || vec2.y != 0.0F;
   }

   private boolean canStartSprinting() {
      return !this.isSprinting() && this.hasEnoughImpulseToStartSprinting() && this.hasEnoughFoodToStartSprinting() && !this.isUsingItem() && !this.hasEffect(MobEffects.BLINDNESS) && (!this.isPassenger() || this.vehicleCanSprint(this.getVehicle())) && !this.isFallFlying();
   }

   private boolean vehicleCanSprint(Entity entity) {
      return entity.canSprint() && entity.isControlledByLocalInstance();
   }

   private boolean hasEnoughImpulseToStartSprinting() {
      double d = 0.8D;
      return this.isUnderWater() ? this.input.hasForwardImpulse() : (double)this.input.forwardImpulse >= 0.8D;
   }

   private boolean hasEnoughFoodToStartSprinting() {
      return this.isPassenger() || (float)this.getFoodData().getFoodLevel() > 6.0F || this.getAbilities().mayfly;
   }

   public float getWaterVision() {
      if (!this.isEyeInFluid(FluidTags.WATER)) {
         return 0.0F;
      } else {
         float f = 600.0F;
         float g = 100.0F;
         if ((float)this.waterVisionTime >= 600.0F) {
            return 1.0F;
         } else {
            float h = Mth.clamp((float)this.waterVisionTime / 100.0F, 0.0F, 1.0F);
            float i = (float)this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp(((float)this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
            return h * 0.6F + i * 0.39999998F;
         }
      }
   }

   public void onGameModeChanged(GameType gameType) {
      if (gameType == GameType.SPECTATOR) {
         this.setDeltaMovement(this.getDeltaMovement().with(Axis.Y, 0.0D));
      }

   }

   public boolean isUnderWater() {
      return this.wasUnderwater;
   }

   protected boolean updateIsUnderwater() {
      boolean bl = this.wasUnderwater;
      boolean bl2 = super.updateIsUnderwater();
      if (this.isSpectator()) {
         return this.wasUnderwater;
      } else {
         if (!bl && bl2) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
            this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
         }

         if (bl && !bl2) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }

         return this.wasUnderwater;
      }
   }

   public Vec3 getRopeHoldPosition(float f) {
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         float g = Mth.lerp(f * 0.5F, this.getYRot(), this.yRotO) * 0.017453292F;
         float h = Mth.lerp(f * 0.5F, this.getXRot(), this.xRotO) * 0.017453292F;
         double d = this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D;
         Vec3 vec3 = new Vec3(0.39D * d, -0.6D, 0.3D);
         return vec3.xRot(-h).yRot(-g).add(this.getEyePosition(f));
      } else {
         return super.getRopeHoldPosition(f);
      }
   }

   public void updateTutorialInventoryAction(ItemStack itemStack, ItemStack itemStack2, ClickAction clickAction) {
      this.minecraft.getTutorial().onInventoryAction(itemStack, itemStack2, clickAction);
   }

   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }
}
