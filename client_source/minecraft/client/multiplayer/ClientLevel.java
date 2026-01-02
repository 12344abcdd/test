package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientLevel extends Level {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final double FLUID_PARTICLE_SPAWN_OFFSET = 0.05D;
   private static final int NORMAL_LIGHT_UPDATES_PER_FRAME = 10;
   private static final int LIGHT_UPDATE_QUEUE_SIZE_THRESHOLD = 1000;
   final EntityTickList tickingEntities = new EntityTickList();
   private final TransientEntitySectionManager<Entity> entityStorage = new TransientEntitySectionManager(Entity.class, new ClientLevel.EntityCallbacks());
   private final ClientPacketListener connection;
   private final LevelRenderer levelRenderer;
   private final ClientLevel.ClientLevelData clientLevelData;
   private final DimensionSpecialEffects effects;
   private final TickRateManager tickRateManager;
   private final Minecraft minecraft = Minecraft.getInstance();
   final List<AbstractClientPlayer> players = Lists.newArrayList();
   private final Map<MapId, MapItemSavedData> mapData = Maps.newHashMap();
   private static final long CLOUD_COLOR = 16777215L;
   private int skyFlashTime;
   private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = (Object2ObjectArrayMap)Util.make(new Object2ObjectArrayMap(3), (object2ObjectArrayMap) -> {
      object2ObjectArrayMap.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache((blockPos) -> {
         return this.calculateBlockTint(blockPos, BiomeColors.GRASS_COLOR_RESOLVER);
      }));
      object2ObjectArrayMap.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache((blockPos) -> {
         return this.calculateBlockTint(blockPos, BiomeColors.FOLIAGE_COLOR_RESOLVER);
      }));
      object2ObjectArrayMap.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache((blockPos) -> {
         return this.calculateBlockTint(blockPos, BiomeColors.WATER_COLOR_RESOLVER);
      }));
   });
   private final ClientChunkCache chunkSource;
   private final Deque<Runnable> lightUpdateQueue = Queues.newArrayDeque();
   private int serverSimulationDistance;
   private final BlockStatePredictionHandler blockStatePredictionHandler = new BlockStatePredictionHandler();
   private static final Set<Item> MARKER_PARTICLE_ITEMS;

   public void handleBlockChangedAck(int i) {
      this.blockStatePredictionHandler.endPredictionsUpTo(i, this);
   }

   public void setServerVerifiedBlockState(BlockPos blockPos, BlockState blockState, int i) {
      if (!this.blockStatePredictionHandler.updateKnownServerState(blockPos, blockState)) {
         super.setBlock(blockPos, blockState, i, 512);
      }

   }

   public void syncBlockState(BlockPos blockPos, BlockState blockState, Vec3 vec3) {
      BlockState blockState2 = this.getBlockState(blockPos);
      if (blockState2 != blockState) {
         this.setBlock(blockPos, blockState, 19);
         Player player = this.minecraft.player;
         if (this == player.level() && player.isColliding(blockPos, blockState)) {
            player.absMoveTo(vec3.x, vec3.y, vec3.z);
         }
      }

   }

   BlockStatePredictionHandler getBlockStatePredictionHandler() {
      return this.blockStatePredictionHandler;
   }

   public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
      if (this.blockStatePredictionHandler.isPredicting()) {
         BlockState blockState2 = this.getBlockState(blockPos);
         boolean bl = super.setBlock(blockPos, blockState, i, j);
         if (bl) {
            this.blockStatePredictionHandler.retainKnownServerState(blockPos, blockState2, this.minecraft.player);
         }

         return bl;
      } else {
         return super.setBlock(blockPos, blockState, i, j);
      }
   }

   public ClientLevel(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey<Level> resourceKey, Holder<DimensionType> holder, int i, int j, Supplier<ProfilerFiller> supplier, LevelRenderer levelRenderer, boolean bl, long l) {
      super(clientLevelData, resourceKey, clientPacketListener.registryAccess(), holder, supplier, true, bl, l, 1000000);
      this.connection = clientPacketListener;
      this.chunkSource = new ClientChunkCache(this, i);
      this.tickRateManager = new TickRateManager();
      this.clientLevelData = clientLevelData;
      this.levelRenderer = levelRenderer;
      this.effects = DimensionSpecialEffects.forType((DimensionType)holder.value());
      this.setDefaultSpawnPos(new BlockPos(8, 64, 8), 0.0F);
      this.serverSimulationDistance = j;
      this.updateSkyBrightness();
      this.prepareWeather();
   }

   public void queueLightUpdate(Runnable runnable) {
      this.lightUpdateQueue.add(runnable);
   }

   public void pollLightUpdates() {
      int i = this.lightUpdateQueue.size();
      int j = i < 1000 ? Math.max(10, i / 10) : i;

      for(int k = 0; k < j; ++k) {
         Runnable runnable = (Runnable)this.lightUpdateQueue.poll();
         if (runnable == null) {
            break;
         }

         runnable.run();
      }

   }

   public boolean isLightUpdateQueueEmpty() {
      return this.lightUpdateQueue.isEmpty();
   }

   public DimensionSpecialEffects effects() {
      return this.effects;
   }

   public void tick(BooleanSupplier booleanSupplier) {
      this.getWorldBorder().tick();
      if (this.tickRateManager().runsNormally()) {
         this.tickTime();
      }

      if (this.skyFlashTime > 0) {
         this.setSkyFlashTime(this.skyFlashTime - 1);
      }

      this.getProfiler().push("blocks");
      this.chunkSource.tick(booleanSupplier, true);
      this.getProfiler().pop();
   }

   private void tickTime() {
      this.setGameTime(this.levelData.getGameTime() + 1L);
      if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
         this.setDayTime(this.levelData.getDayTime() + 1L);
      }

   }

   public void setGameTime(long l) {
      this.clientLevelData.setGameTime(l);
   }

   public void setDayTime(long l) {
      if (l < 0L) {
         l = -l;
         ((BooleanValue)this.getGameRules().getRule(GameRules.RULE_DAYLIGHT)).set(false, (MinecraftServer)null);
      } else {
         ((BooleanValue)this.getGameRules().getRule(GameRules.RULE_DAYLIGHT)).set(true, (MinecraftServer)null);
      }

      this.clientLevelData.setDayTime(l);
   }

   public Iterable<Entity> entitiesForRendering() {
      return this.getEntities().getAll();
   }

   public void tickEntities() {
      ProfilerFiller profilerFiller = this.getProfiler();
      profilerFiller.push("entities");
      this.tickingEntities.forEach((entity) -> {
         if (!entity.isRemoved() && !entity.isPassenger() && !this.tickRateManager.isEntityFrozen(entity)) {
            this.guardEntityTick(this::tickNonPassenger, entity);
         }
      });
      profilerFiller.pop();
      this.tickBlockEntities();
   }

   public boolean shouldTickDeath(Entity entity) {
      return entity.chunkPosition().getChessboardDistance(this.minecraft.player.chunkPosition()) <= this.serverSimulationDistance;
   }

   public void tickNonPassenger(Entity entity) {
      entity.setOldPosAndRot();
      ++entity.tickCount;
      this.getProfiler().push(() -> {
         return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
      });
      entity.tick();
      this.getProfiler().pop();
      Iterator var2 = entity.getPassengers().iterator();

      while(var2.hasNext()) {
         Entity entity2 = (Entity)var2.next();
         this.tickPassenger(entity, entity2);
      }

   }

   private void tickPassenger(Entity entity, Entity entity2) {
      if (!entity2.isRemoved() && entity2.getVehicle() == entity) {
         if (entity2 instanceof Player || this.tickingEntities.contains(entity2)) {
            entity2.setOldPosAndRot();
            ++entity2.tickCount;
            entity2.rideTick();
            Iterator var3 = entity2.getPassengers().iterator();

            while(var3.hasNext()) {
               Entity entity3 = (Entity)var3.next();
               this.tickPassenger(entity2, entity3);
            }

         }
      } else {
         entity2.stopRiding();
      }
   }

   public void unload(LevelChunk levelChunk) {
      levelChunk.clearAllBlockEntities();
      this.chunkSource.getLightEngine().setLightEnabled(levelChunk.getPos(), false);
      this.entityStorage.stopTicking(levelChunk.getPos());
   }

   public void onChunkLoaded(ChunkPos chunkPos) {
      this.tintCaches.forEach((colorResolver, blockTintCache) -> {
         blockTintCache.invalidateForChunk(chunkPos.x, chunkPos.z);
      });
      this.entityStorage.startTicking(chunkPos);
      this.levelRenderer.onChunkLoaded(chunkPos);
   }

   public void clearTintCaches() {
      this.tintCaches.forEach((colorResolver, blockTintCache) -> {
         blockTintCache.invalidateAll();
      });
   }

   public boolean hasChunk(int i, int j) {
      return true;
   }

   public int getEntityCount() {
      return this.entityStorage.count();
   }

   public void addEntity(Entity entity) {
      this.removeEntity(entity.getId(), RemovalReason.DISCARDED);
      this.entityStorage.addEntity(entity);
   }

   public void removeEntity(int i, RemovalReason removalReason) {
      Entity entity = (Entity)this.getEntities().get(i);
      if (entity != null) {
         entity.setRemoved(removalReason);
         entity.onClientRemoval();
      }

   }

   @Nullable
   public Entity getEntity(int i) {
      return (Entity)this.getEntities().get(i);
   }

   public void disconnect() {
      this.connection.getConnection().disconnect(Component.translatable("multiplayer.status.quitting"));
   }

   public void animateTick(int i, int j, int k) {
      int l = true;
      RandomSource randomSource = RandomSource.create();
      Block block = this.getMarkerParticleTarget();
      MutableBlockPos mutableBlockPos = new MutableBlockPos();

      for(int m = 0; m < 667; ++m) {
         this.doAnimateTick(i, j, k, 16, randomSource, block, mutableBlockPos);
         this.doAnimateTick(i, j, k, 32, randomSource, block, mutableBlockPos);
      }

   }

   @Nullable
   private Block getMarkerParticleTarget() {
      if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
         ItemStack itemStack = this.minecraft.player.getMainHandItem();
         Item item = itemStack.getItem();
         if (MARKER_PARTICLE_ITEMS.contains(item) && item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem)item;
            return blockItem.getBlock();
         }
      }

      return null;
   }

   public void doAnimateTick(int i, int j, int k, int l, RandomSource randomSource, @Nullable Block block, MutableBlockPos mutableBlockPos) {
      int m = i + this.random.nextInt(l) - this.random.nextInt(l);
      int n = j + this.random.nextInt(l) - this.random.nextInt(l);
      int o = k + this.random.nextInt(l) - this.random.nextInt(l);
      mutableBlockPos.set(m, n, o);
      BlockState blockState = this.getBlockState(mutableBlockPos);
      blockState.getBlock().animateTick(blockState, this, mutableBlockPos, randomSource);
      FluidState fluidState = this.getFluidState(mutableBlockPos);
      if (!fluidState.isEmpty()) {
         fluidState.animateTick(this, mutableBlockPos, randomSource);
         ParticleOptions particleOptions = fluidState.getDripParticle();
         if (particleOptions != null && this.random.nextInt(10) == 0) {
            boolean bl = blockState.isFaceSturdy(this, mutableBlockPos, Direction.DOWN);
            BlockPos blockPos = mutableBlockPos.below();
            this.trySpawnDripParticles(blockPos, this.getBlockState(blockPos), particleOptions, bl);
         }
      }

      if (block == blockState.getBlock()) {
         this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState), (double)m + 0.5D, (double)n + 0.5D, (double)o + 0.5D, 0.0D, 0.0D, 0.0D);
      }

      if (!blockState.isCollisionShapeFullBlock(this, mutableBlockPos)) {
         ((Biome)this.getBiome(mutableBlockPos).value()).getAmbientParticle().ifPresent((ambientParticleSettings) -> {
            if (ambientParticleSettings.canSpawn(this.random)) {
               this.addParticle(ambientParticleSettings.getOptions(), (double)mutableBlockPos.getX() + this.random.nextDouble(), (double)mutableBlockPos.getY() + this.random.nextDouble(), (double)mutableBlockPos.getZ() + this.random.nextDouble(), 0.0D, 0.0D, 0.0D);
            }

         });
      }

   }

   private void trySpawnDripParticles(BlockPos blockPos, BlockState blockState, ParticleOptions particleOptions, boolean bl) {
      if (blockState.getFluidState().isEmpty()) {
         VoxelShape voxelShape = blockState.getCollisionShape(this, blockPos);
         double d = voxelShape.max(Axis.Y);
         if (d < 1.0D) {
            if (bl) {
               this.spawnFluidParticle((double)blockPos.getX(), (double)(blockPos.getX() + 1), (double)blockPos.getZ(), (double)(blockPos.getZ() + 1), (double)(blockPos.getY() + 1) - 0.05D, particleOptions);
            }
         } else if (!blockState.is(BlockTags.IMPERMEABLE)) {
            double e = voxelShape.min(Axis.Y);
            if (e > 0.0D) {
               this.spawnParticle(blockPos, particleOptions, voxelShape, (double)blockPos.getY() + e - 0.05D);
            } else {
               BlockPos blockPos2 = blockPos.below();
               BlockState blockState2 = this.getBlockState(blockPos2);
               VoxelShape voxelShape2 = blockState2.getCollisionShape(this, blockPos2);
               double f = voxelShape2.max(Axis.Y);
               if (f < 1.0D && blockState2.getFluidState().isEmpty()) {
                  this.spawnParticle(blockPos, particleOptions, voxelShape, (double)blockPos.getY() - 0.05D);
               }
            }
         }

      }
   }

   private void spawnParticle(BlockPos blockPos, ParticleOptions particleOptions, VoxelShape voxelShape, double d) {
      this.spawnFluidParticle((double)blockPos.getX() + voxelShape.min(Axis.X), (double)blockPos.getX() + voxelShape.max(Axis.X), (double)blockPos.getZ() + voxelShape.min(Axis.Z), (double)blockPos.getZ() + voxelShape.max(Axis.Z), d, particleOptions);
   }

   private void spawnFluidParticle(double d, double e, double f, double g, double h, ParticleOptions particleOptions) {
      this.addParticle(particleOptions, Mth.lerp(this.random.nextDouble(), d, e), h, Mth.lerp(this.random.nextDouble(), f, g), 0.0D, 0.0D, 0.0D);
   }

   public CrashReportCategory fillReportDetails(CrashReport crashReport) {
      CrashReportCategory crashReportCategory = super.fillReportDetails(crashReport);
      crashReportCategory.setDetail("Server brand", () -> {
         return this.minecraft.player.connection.serverBrand();
      });
      crashReportCategory.setDetail("Server type", () -> {
         return this.minecraft.getSingleplayerServer() == null ? "Non-integrated multiplayer server" : "Integrated singleplayer server";
      });
      crashReportCategory.setDetail("Tracked entity count", () -> {
         return String.valueOf(this.getEntityCount());
      });
      return crashReportCategory;
   }

   public void playSeededSound(@Nullable Player player, double d, double e, double f, Holder<SoundEvent> holder, SoundSource soundSource, float g, float h, long l) {
      if (player == this.minecraft.player) {
         this.playSound(d, e, f, (SoundEvent)holder.value(), soundSource, g, h, false, l);
      }

   }

   public void playSeededSound(@Nullable Player player, Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l) {
      if (player == this.minecraft.player) {
         this.minecraft.getSoundManager().play(new EntityBoundSoundInstance((SoundEvent)holder.value(), soundSource, f, g, entity, l));
      }

   }

   public void playLocalSound(Entity entity, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
      this.minecraft.getSoundManager().play(new EntityBoundSoundInstance(soundEvent, soundSource, f, g, entity, this.random.nextLong()));
   }

   public void playLocalSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl) {
      this.playSound(d, e, f, soundEvent, soundSource, g, h, bl, this.random.nextLong());
   }

   private void playSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl, long l) {
      double i = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(d, e, f);
      SimpleSoundInstance simpleSoundInstance = new SimpleSoundInstance(soundEvent, soundSource, g, h, RandomSource.create(l), d, e, f);
      if (bl && i > 100.0D) {
         double j = Math.sqrt(i) / 40.0D;
         this.minecraft.getSoundManager().playDelayed(simpleSoundInstance, (int)(j * 20.0D));
      } else {
         this.minecraft.getSoundManager().play(simpleSoundInstance);
      }

   }

   public void createFireworks(double d, double e, double f, double g, double h, double i, List<FireworkExplosion> list) {
      if (list.isEmpty()) {
         for(int j = 0; j < this.random.nextInt(3) + 2; ++j) {
            this.addParticle(ParticleTypes.POOF, d, e, f, this.random.nextGaussian() * 0.05D, 0.005D, this.random.nextGaussian() * 0.05D);
         }
      } else {
         this.minecraft.particleEngine.add(new FireworkParticles.Starter(this, d, e, f, g, h, i, this.minecraft.particleEngine, list));
      }

   }

   public void sendPacketToServer(Packet<?> packet) {
      this.connection.send(packet);
   }

   public RecipeManager getRecipeManager() {
      return this.connection.getRecipeManager();
   }

   public TickRateManager tickRateManager() {
      return this.tickRateManager;
   }

   public LevelTickAccess<Block> getBlockTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public LevelTickAccess<Fluid> getFluidTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public ClientChunkCache getChunkSource() {
      return this.chunkSource;
   }

   @Nullable
   public MapItemSavedData getMapData(MapId mapId) {
      return (MapItemSavedData)this.mapData.get(mapId);
   }

   public void overrideMapData(MapId mapId, MapItemSavedData mapItemSavedData) {
      this.mapData.put(mapId, mapItemSavedData);
   }

   public void setMapData(MapId mapId, MapItemSavedData mapItemSavedData) {
   }

   public MapId getFreeMapId() {
      return new MapId(0);
   }

   public Scoreboard getScoreboard() {
      return this.connection.scoreboard();
   }

   public void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i) {
      this.levelRenderer.blockChanged(this, blockPos, blockState, blockState2, i);
   }

   public void setBlocksDirty(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
      this.levelRenderer.setBlockDirty(blockPos, blockState, blockState2);
   }

   public void setSectionDirtyWithNeighbors(int i, int j, int k) {
      this.levelRenderer.setSectionDirtyWithNeighbors(i, j, k);
   }

   public void destroyBlockProgress(int i, BlockPos blockPos, int j) {
      this.levelRenderer.destroyBlockProgress(i, blockPos, j);
   }

   public void globalLevelEvent(int i, BlockPos blockPos, int j) {
      this.levelRenderer.globalLevelEvent(i, blockPos, j);
   }

   public void levelEvent(@Nullable Player player, int i, BlockPos blockPos, int j) {
      try {
         this.levelRenderer.levelEvent(i, blockPos, j);
      } catch (Throwable var8) {
         CrashReport crashReport = CrashReport.forThrowable(var8, "Playing level event");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Level event being played");
         crashReportCategory.setDetail("Block coordinates", CrashReportCategory.formatLocation(this, blockPos));
         crashReportCategory.setDetail("Event source", player);
         crashReportCategory.setDetail("Event type", i);
         crashReportCategory.setDetail("Event data", j);
         throw new ReportedException(crashReport);
      }
   }

   public void addParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
      this.levelRenderer.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter(), d, e, f, g, h, i);
   }

   public void addParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
      this.levelRenderer.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter() || bl, d, e, f, g, h, i);
   }

   public void addAlwaysVisibleParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
      this.levelRenderer.addParticle(particleOptions, false, true, d, e, f, g, h, i);
   }

   public void addAlwaysVisibleParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
      this.levelRenderer.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter() || bl, true, d, e, f, g, h, i);
   }

   public List<AbstractClientPlayer> players() {
      return this.players;
   }

   public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
      return this.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
   }

   public float getSkyDarken(float f) {
      float g = this.getTimeOfDay(f);
      float h = 1.0F - (Mth.cos(g * 6.2831855F) * 2.0F + 0.2F);
      h = Mth.clamp(h, 0.0F, 1.0F);
      h = 1.0F - h;
      h *= 1.0F - this.getRainLevel(f) * 5.0F / 16.0F;
      h *= 1.0F - this.getThunderLevel(f) * 5.0F / 16.0F;
      return h * 0.8F + 0.2F;
   }

   public Vec3 getSkyColor(Vec3 vec3, float f) {
      float g = this.getTimeOfDay(f);
      Vec3 vec32 = vec3.subtract(2.0D, 2.0D, 2.0D).scale(0.25D);
      BiomeManager biomeManager = this.getBiomeManager();
      Vec3 vec33 = CubicSampler.gaussianSampleVec3(vec32, (ix, jx, kx) -> {
         return Vec3.fromRGB24(((Biome)biomeManager.getNoiseBiomeAtQuart(ix, jx, kx).value()).getSkyColor());
      });
      float h = Mth.cos(g * 6.2831855F) * 2.0F + 0.5F;
      h = Mth.clamp(h, 0.0F, 1.0F);
      float i = (float)vec33.x * h;
      float j = (float)vec33.y * h;
      float k = (float)vec33.z * h;
      float l = this.getRainLevel(f);
      float m;
      float n;
      if (l > 0.0F) {
         m = (i * 0.3F + j * 0.59F + k * 0.11F) * 0.6F;
         n = 1.0F - l * 0.75F;
         i = i * n + m * (1.0F - n);
         j = j * n + m * (1.0F - n);
         k = k * n + m * (1.0F - n);
      }

      m = this.getThunderLevel(f);
      float o;
      if (m > 0.0F) {
         n = (i * 0.3F + j * 0.59F + k * 0.11F) * 0.2F;
         o = 1.0F - m * 0.75F;
         i = i * o + n * (1.0F - o);
         j = j * o + n * (1.0F - o);
         k = k * o + n * (1.0F - o);
      }

      int p = this.getSkyFlashTime();
      if (p > 0) {
         o = (float)p - f;
         if (o > 1.0F) {
            o = 1.0F;
         }

         o *= 0.45F;
         i = i * (1.0F - o) + 0.8F * o;
         j = j * (1.0F - o) + 0.8F * o;
         k = k * (1.0F - o) + 1.0F * o;
      }

      return new Vec3((double)i, (double)j, (double)k);
   }

   public Vec3 getCloudColor(float f) {
      float g = this.getTimeOfDay(f);
      float h = Mth.cos(g * 6.2831855F) * 2.0F + 0.5F;
      h = Mth.clamp(h, 0.0F, 1.0F);
      float i = 1.0F;
      float j = 1.0F;
      float k = 1.0F;
      float l = this.getRainLevel(f);
      float m;
      float n;
      if (l > 0.0F) {
         m = (i * 0.3F + j * 0.59F + k * 0.11F) * 0.6F;
         n = 1.0F - l * 0.95F;
         i = i * n + m * (1.0F - n);
         j = j * n + m * (1.0F - n);
         k = k * n + m * (1.0F - n);
      }

      i *= h * 0.9F + 0.1F;
      j *= h * 0.9F + 0.1F;
      k *= h * 0.85F + 0.15F;
      m = this.getThunderLevel(f);
      if (m > 0.0F) {
         n = (i * 0.3F + j * 0.59F + k * 0.11F) * 0.2F;
         float o = 1.0F - m * 0.95F;
         i = i * o + n * (1.0F - o);
         j = j * o + n * (1.0F - o);
         k = k * o + n * (1.0F - o);
      }

      return new Vec3((double)i, (double)j, (double)k);
   }

   public float getStarBrightness(float f) {
      float g = this.getTimeOfDay(f);
      float h = 1.0F - (Mth.cos(g * 6.2831855F) * 2.0F + 0.25F);
      h = Mth.clamp(h, 0.0F, 1.0F);
      return h * h * 0.5F;
   }

   public int getSkyFlashTime() {
      return (Boolean)this.minecraft.options.hideLightningFlash().get() ? 0 : this.skyFlashTime;
   }

   public void setSkyFlashTime(int i) {
      this.skyFlashTime = i;
   }

   public float getShade(Direction direction, boolean bl) {
      boolean bl2 = this.effects().constantAmbientLight();
      if (!bl) {
         return bl2 ? 0.9F : 1.0F;
      } else {
         switch(direction) {
         case DOWN:
            return bl2 ? 0.9F : 0.5F;
         case UP:
            return bl2 ? 0.9F : 1.0F;
         case NORTH:
         case SOUTH:
            return 0.8F;
         case WEST:
         case EAST:
            return 0.6F;
         default:
            return 1.0F;
         }
      }
   }

   public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      BlockTintCache blockTintCache = (BlockTintCache)this.tintCaches.get(colorResolver);
      return blockTintCache.getColor(blockPos);
   }

   public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      int i = (Integer)Minecraft.getInstance().options.biomeBlendRadius().get();
      if (i == 0) {
         return colorResolver.getColor((Biome)this.getBiome(blockPos).value(), (double)blockPos.getX(), (double)blockPos.getZ());
      } else {
         int j = (i * 2 + 1) * (i * 2 + 1);
         int k = 0;
         int l = 0;
         int m = 0;
         Cursor3D cursor3D = new Cursor3D(blockPos.getX() - i, blockPos.getY(), blockPos.getZ() - i, blockPos.getX() + i, blockPos.getY(), blockPos.getZ() + i);

         int n;
         for(MutableBlockPos mutableBlockPos = new MutableBlockPos(); cursor3D.advance(); m += n & 255) {
            mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
            n = colorResolver.getColor((Biome)this.getBiome(mutableBlockPos).value(), (double)mutableBlockPos.getX(), (double)mutableBlockPos.getZ());
            k += (n & 16711680) >> 16;
            l += (n & '\uff00') >> 8;
         }

         return (k / j & 255) << 16 | (l / j & 255) << 8 | m / j & 255;
      }
   }

   public void setDefaultSpawnPos(BlockPos blockPos, float f) {
      this.levelData.setSpawn(blockPos, f);
   }

   public String toString() {
      return "ClientLevel";
   }

   public ClientLevel.ClientLevelData getLevelData() {
      return this.clientLevelData;
   }

   public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) {
   }

   protected Map<MapId, MapItemSavedData> getAllMapData() {
      return ImmutableMap.copyOf(this.mapData);
   }

   protected void addMapData(Map<MapId, MapItemSavedData> map) {
      this.mapData.putAll(map);
   }

   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityStorage.getEntityGetter();
   }

   public String gatherChunkSourceStats() {
      String var10000 = this.chunkSource.gatherStats();
      return "Chunks[C] W: " + var10000 + " E: " + this.entityStorage.gatherStats();
   }

   public void addDestroyBlockEffect(BlockPos blockPos, BlockState blockState) {
      this.minecraft.particleEngine.destroy(blockPos, blockState);
   }

   public void setServerSimulationDistance(int i) {
      this.serverSimulationDistance = i;
   }

   public int getServerSimulationDistance() {
      return this.serverSimulationDistance;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.connection.enabledFeatures();
   }

   public PotionBrewing potionBrewing() {
      return this.connection.potionBrewing();
   }

   // $FF: synthetic method
   public LevelData getLevelData() {
      return this.getLevelData();
   }

   // $FF: synthetic method
   public ChunkSource getChunkSource() {
      return this.getChunkSource();
   }

   static {
      MARKER_PARTICLE_ITEMS = Set.of(Items.BARRIER, Items.LIGHT);
   }

   @Environment(EnvType.CLIENT)
   private final class EntityCallbacks implements LevelCallback<Entity> {
      EntityCallbacks() {
      }

      public void onCreated(Entity entity) {
      }

      public void onDestroyed(Entity entity) {
      }

      public void onTickingStart(Entity entity) {
         ClientLevel.this.tickingEntities.add(entity);
      }

      public void onTickingEnd(Entity entity) {
         ClientLevel.this.tickingEntities.remove(entity);
      }

      public void onTrackingStart(Entity entity) {
         if (entity instanceof AbstractClientPlayer) {
            ClientLevel.this.players.add((AbstractClientPlayer)entity);
         }

      }

      public void onTrackingEnd(Entity entity) {
         entity.unRide();
         ClientLevel.this.players.remove(entity);
      }

      public void onSectionChange(Entity entity) {
      }

      // $FF: synthetic method
      public void onSectionChange(final Object object) {
         this.onSectionChange((Entity)object);
      }

      // $FF: synthetic method
      public void onTrackingEnd(final Object object) {
         this.onTrackingEnd((Entity)object);
      }

      // $FF: synthetic method
      public void onTrackingStart(final Object object) {
         this.onTrackingStart((Entity)object);
      }

      // $FF: synthetic method
      public void onTickingStart(final Object object) {
         this.onTickingStart((Entity)object);
      }

      // $FF: synthetic method
      public void onDestroyed(final Object object) {
         this.onDestroyed((Entity)object);
      }

      // $FF: synthetic method
      public void onCreated(final Object object) {
         this.onCreated((Entity)object);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ClientLevelData implements WritableLevelData {
      private final boolean hardcore;
      private final GameRules gameRules;
      private final boolean isFlat;
      private BlockPos spawnPos;
      private float spawnAngle;
      private long gameTime;
      private long dayTime;
      private boolean raining;
      private Difficulty difficulty;
      private boolean difficultyLocked;

      public ClientLevelData(Difficulty difficulty, boolean bl, boolean bl2) {
         this.difficulty = difficulty;
         this.hardcore = bl;
         this.isFlat = bl2;
         this.gameRules = new GameRules();
      }

      public BlockPos getSpawnPos() {
         return this.spawnPos;
      }

      public float getSpawnAngle() {
         return this.spawnAngle;
      }

      public long getGameTime() {
         return this.gameTime;
      }

      public long getDayTime() {
         return this.dayTime;
      }

      public void setGameTime(long l) {
         this.gameTime = l;
      }

      public void setDayTime(long l) {
         this.dayTime = l;
      }

      public void setSpawn(BlockPos blockPos, float f) {
         this.spawnPos = blockPos.immutable();
         this.spawnAngle = f;
      }

      public boolean isThundering() {
         return false;
      }

      public boolean isRaining() {
         return this.raining;
      }

      public void setRaining(boolean bl) {
         this.raining = bl;
      }

      public boolean isHardcore() {
         return this.hardcore;
      }

      public GameRules getGameRules() {
         return this.gameRules;
      }

      public Difficulty getDifficulty() {
         return this.difficulty;
      }

      public boolean isDifficultyLocked() {
         return this.difficultyLocked;
      }

      public void fillCrashReportCategory(CrashReportCategory crashReportCategory, LevelHeightAccessor levelHeightAccessor) {
         super.fillCrashReportCategory(crashReportCategory, levelHeightAccessor);
      }

      public void setDifficulty(Difficulty difficulty) {
         this.difficulty = difficulty;
      }

      public void setDifficultyLocked(boolean bl) {
         this.difficultyLocked = bl;
      }

      public double getHorizonHeight(LevelHeightAccessor levelHeightAccessor) {
         return this.isFlat ? (double)levelHeightAccessor.getMinBuildHeight() : 63.0D;
      }

      public float getClearColorScale() {
         return this.isFlat ? 1.0F : 0.03125F;
      }
   }
}
