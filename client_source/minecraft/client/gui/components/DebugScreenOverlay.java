package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.datafixers.DataFixUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debugchart.BandwidthDebugChart;
import net.minecraft.client.gui.components.debugchart.FpsDebugChart;
import net.minecraft.client.gui.components.debugchart.PingDebugChart;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner.SpawnState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugScreenOverlay {
   private static final int COLOR_GREY = 14737632;
   private static final int MARGIN_RIGHT = 2;
   private static final int MARGIN_LEFT = 2;
   private static final int MARGIN_TOP = 2;
   private static final Map<Types, String> HEIGHTMAP_NAMES = (Map)Util.make(new EnumMap(Types.class), (enumMap) -> {
      enumMap.put(Types.WORLD_SURFACE_WG, "SW");
      enumMap.put(Types.WORLD_SURFACE, "S");
      enumMap.put(Types.OCEAN_FLOOR_WG, "OW");
      enumMap.put(Types.OCEAN_FLOOR, "O");
      enumMap.put(Types.MOTION_BLOCKING, "M");
      enumMap.put(Types.MOTION_BLOCKING_NO_LEAVES, "ML");
   });
   private final Minecraft minecraft;
   private final DebugScreenOverlay.AllocationRateCalculator allocationRateCalculator;
   private final Font font;
   private HitResult block;
   private HitResult liquid;
   @Nullable
   private ChunkPos lastPos;
   @Nullable
   private LevelChunk clientChunk;
   @Nullable
   private CompletableFuture<LevelChunk> serverChunk;
   private boolean renderDebug;
   private boolean renderProfilerChart;
   private boolean renderFpsCharts;
   private boolean renderNetworkCharts;
   private final LocalSampleLogger frameTimeLogger = new LocalSampleLogger(1);
   private final LocalSampleLogger tickTimeLogger = new LocalSampleLogger(TpsDebugDimensions.values().length);
   private final LocalSampleLogger pingLogger = new LocalSampleLogger(1);
   private final LocalSampleLogger bandwidthLogger = new LocalSampleLogger(1);
   private final Map<RemoteDebugSampleType, LocalSampleLogger> remoteSupportingLoggers;
   private final FpsDebugChart fpsChart;
   private final TpsDebugChart tpsChart;
   private final PingDebugChart pingChart;
   private final BandwidthDebugChart bandwidthChart;

   public DebugScreenOverlay(Minecraft minecraft) {
      this.remoteSupportingLoggers = Map.of(RemoteDebugSampleType.TICK_TIME, this.tickTimeLogger);
      this.minecraft = minecraft;
      this.allocationRateCalculator = new DebugScreenOverlay.AllocationRateCalculator();
      this.font = minecraft.font;
      this.fpsChart = new FpsDebugChart(this.font, this.frameTimeLogger);
      this.tpsChart = new TpsDebugChart(this.font, this.tickTimeLogger, () -> {
         return minecraft.level.tickRateManager().millisecondsPerTick();
      });
      this.pingChart = new PingDebugChart(this.font, this.pingLogger);
      this.bandwidthChart = new BandwidthDebugChart(this.font, this.bandwidthLogger);
   }

   public void clearChunkCache() {
      this.serverChunk = null;
      this.clientChunk = null;
   }

   public void render(GuiGraphics guiGraphics) {
      this.minecraft.getProfiler().push("debug");
      Entity entity = this.minecraft.getCameraEntity();
      this.block = entity.pick(20.0D, 0.0F, false);
      this.liquid = entity.pick(20.0D, 0.0F, true);
      guiGraphics.drawManaged(() -> {
         this.drawGameInformation(guiGraphics);
         this.drawSystemInformation(guiGraphics);
         int i;
         int j;
         int k;
         if (this.renderFpsCharts) {
            i = guiGraphics.guiWidth();
            j = i / 2;
            this.fpsChart.drawChart(guiGraphics, 0, this.fpsChart.getWidth(j));
            if (this.tickTimeLogger.size() > 0) {
               k = this.tpsChart.getWidth(j);
               this.tpsChart.drawChart(guiGraphics, i - k, k);
            }
         }

         if (this.renderNetworkCharts) {
            i = guiGraphics.guiWidth();
            j = i / 2;
            if (!this.minecraft.isLocalServer()) {
               this.bandwidthChart.drawChart(guiGraphics, 0, this.bandwidthChart.getWidth(j));
            }

            k = this.pingChart.getWidth(j);
            this.pingChart.drawChart(guiGraphics, i - k, k);
         }

      });
      this.minecraft.getProfiler().pop();
   }

   protected void drawGameInformation(GuiGraphics guiGraphics) {
      List<String> list = this.getGameInformation();
      list.add("");
      boolean bl = this.minecraft.getSingleplayerServer() != null;
      String var10001 = this.renderProfilerChart ? "visible" : "hidden";
      list.add("Debug charts: [F3+1] Profiler " + var10001 + "; [F3+2] " + (bl ? "FPS + TPS " : "FPS ") + (this.renderFpsCharts ? "visible" : "hidden") + "; [F3+3] " + (!this.minecraft.isLocalServer() ? "Bandwidth + Ping" : "Ping") + (this.renderNetworkCharts ? " visible" : " hidden"));
      list.add("For help: press F3 + Q");
      this.renderLines(guiGraphics, list, true);
   }

   protected void drawSystemInformation(GuiGraphics guiGraphics) {
      List<String> list = this.getSystemInformation();
      this.renderLines(guiGraphics, list, false);
   }

   private void renderLines(GuiGraphics guiGraphics, List<String> list, boolean bl) {
      Objects.requireNonNull(this.font);
      int i = 9;

      int j;
      String string;
      int k;
      int l;
      int m;
      for(j = 0; j < list.size(); ++j) {
         string = (String)list.get(j);
         if (!Strings.isNullOrEmpty(string)) {
            k = this.font.width(string);
            l = bl ? 2 : guiGraphics.guiWidth() - 2 - k;
            m = 2 + i * j;
            guiGraphics.fill(l - 1, m - 1, l + k + 1, m + i - 1, -1873784752);
         }
      }

      for(j = 0; j < list.size(); ++j) {
         string = (String)list.get(j);
         if (!Strings.isNullOrEmpty(string)) {
            k = this.font.width(string);
            l = bl ? 2 : guiGraphics.guiWidth() - 2 - k;
            m = 2 + i * j;
            guiGraphics.drawString(this.font, string, l, m, 14737632, false);
         }
      }

   }

   protected List<String> getGameInformation() {
      IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
      ClientPacketListener clientPacketListener = this.minecraft.getConnection();
      Connection connection = clientPacketListener.getConnection();
      float f = connection.getAverageSentPackets();
      float g = connection.getAverageReceivedPackets();
      TickRateManager tickRateManager = this.getLevel().tickRateManager();
      String string;
      if (tickRateManager.isSteppingForward()) {
         string = " (frozen - stepping)";
      } else if (tickRateManager.isFrozen()) {
         string = " (frozen)";
      } else {
         string = "";
      }

      String string3;
      if (integratedServer != null) {
         ServerTickRateManager serverTickRateManager = integratedServer.tickRateManager();
         boolean bl = serverTickRateManager.isSprinting();
         if (bl) {
            string = " (sprinting)";
         }

         String string2 = bl ? "-" : String.format(Locale.ROOT, "%.1f", tickRateManager.millisecondsPerTick());
         string3 = String.format(Locale.ROOT, "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx", integratedServer.getCurrentSmoothedTickTime(), string2, string, f, g);
      } else {
         string3 = String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", clientPacketListener.serverBrand(), string, f, g);
      }

      BlockPos blockPos = this.minecraft.getCameraEntity().blockPosition();
      String[] var10000;
      String var10003;
      if (this.minecraft.showOnlyReducedInfo()) {
         var10000 = new String[9];
         var10003 = SharedConstants.getCurrentVersion().getName();
         var10000[0] = "Minecraft " + var10003 + " (" + this.minecraft.getLaunchedVersion() + "/" + ClientBrandRetriever.getClientModName() + ")";
         var10000[1] = this.minecraft.fpsString;
         var10000[2] = string3;
         var10000[3] = this.minecraft.levelRenderer.getSectionStatistics();
         var10000[4] = this.minecraft.levelRenderer.getEntityStatistics();
         var10003 = this.minecraft.particleEngine.countParticles();
         var10000[5] = "P: " + var10003 + ". T: " + this.minecraft.level.getEntityCount();
         var10000[6] = this.minecraft.level.gatherChunkSourceStats();
         var10000[7] = "";
         var10000[8] = String.format(Locale.ROOT, "Chunk-relative: %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
         return Lists.newArrayList(var10000);
      } else {
         Entity entity = this.minecraft.getCameraEntity();
         Direction direction = entity.getDirection();
         String string4;
         switch(direction) {
         case NORTH:
            string4 = "Towards negative Z";
            break;
         case SOUTH:
            string4 = "Towards positive Z";
            break;
         case WEST:
            string4 = "Towards negative X";
            break;
         case EAST:
            string4 = "Towards positive X";
            break;
         default:
            string4 = "Invalid";
         }

         ChunkPos chunkPos = new ChunkPos(blockPos);
         if (!Objects.equals(this.lastPos, chunkPos)) {
            this.lastPos = chunkPos;
            this.clearChunkCache();
         }

         Level level = this.getLevel();
         LongSet longSet = level instanceof ServerLevel ? ((ServerLevel)level).getForcedChunks() : LongSets.EMPTY_SET;
         var10000 = new String[7];
         var10003 = SharedConstants.getCurrentVersion().getName();
         var10000[0] = "Minecraft " + var10003 + " (" + this.minecraft.getLaunchedVersion() + "/" + ClientBrandRetriever.getClientModName() + ("release".equalsIgnoreCase(this.minecraft.getVersionType()) ? "" : "/" + this.minecraft.getVersionType()) + ")";
         var10000[1] = this.minecraft.fpsString;
         var10000[2] = string3;
         var10000[3] = this.minecraft.levelRenderer.getSectionStatistics();
         var10000[4] = this.minecraft.levelRenderer.getEntityStatistics();
         var10003 = this.minecraft.particleEngine.countParticles();
         var10000[5] = "P: " + var10003 + ". T: " + this.minecraft.level.getEntityCount();
         var10000[6] = this.minecraft.level.gatherChunkSourceStats();
         List<String> list = Lists.newArrayList(var10000);
         String string5 = this.getServerChunkStats();
         if (string5 != null) {
            list.add(string5);
         }

         String var10001 = String.valueOf(this.minecraft.level.dimension().location());
         list.add(var10001 + " FC: " + ((LongSet)longSet).size());
         list.add("");
         list.add(String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", this.minecraft.getCameraEntity().getX(), this.minecraft.getCameraEntity().getY(), this.minecraft.getCameraEntity().getZ()));
         list.add(String.format(Locale.ROOT, "Block: %d %d %d [%d %d %d]", blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15));
         list.add(String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]", chunkPos.x, SectionPos.blockToSectionCoord(blockPos.getY()), chunkPos.z, chunkPos.getRegionLocalX(), chunkPos.getRegionLocalZ(), chunkPos.getRegionX(), chunkPos.getRegionZ()));
         list.add(String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", direction, string4, Mth.wrapDegrees(entity.getYRot()), Mth.wrapDegrees(entity.getXRot())));
         LevelChunk levelChunk = this.getClientChunk();
         if (levelChunk.isEmpty()) {
            list.add("Waiting for chunk...");
         } else {
            int i = this.minecraft.level.getChunkSource().getLightEngine().getRawBrightness(blockPos, 0);
            int j = this.minecraft.level.getBrightness(LightLayer.SKY, blockPos);
            int k = this.minecraft.level.getBrightness(LightLayer.BLOCK, blockPos);
            list.add("Client Light: " + i + " (" + j + " sky, " + k + " block)");
            LevelChunk levelChunk2 = this.getServerChunk();
            StringBuilder stringBuilder = new StringBuilder("CH");
            Types[] var24 = Types.values();
            int var25 = var24.length;

            int var26;
            Types types;
            for(var26 = 0; var26 < var25; ++var26) {
               types = var24[var26];
               if (types.sendToClient()) {
                  stringBuilder.append(" ").append((String)HEIGHTMAP_NAMES.get(types)).append(": ").append(levelChunk.getHeight(types, blockPos.getX(), blockPos.getZ()));
               }
            }

            list.add(stringBuilder.toString());
            stringBuilder.setLength(0);
            stringBuilder.append("SH");
            var24 = Types.values();
            var25 = var24.length;

            for(var26 = 0; var26 < var25; ++var26) {
               types = var24[var26];
               if (types.keepAfterWorldgen()) {
                  stringBuilder.append(" ").append((String)HEIGHTMAP_NAMES.get(types)).append(": ");
                  if (levelChunk2 != null) {
                     stringBuilder.append(levelChunk2.getHeight(types, blockPos.getX(), blockPos.getZ()));
                  } else {
                     stringBuilder.append("??");
                  }
               }
            }

            list.add(stringBuilder.toString());
            if (blockPos.getY() >= this.minecraft.level.getMinBuildHeight() && blockPos.getY() < this.minecraft.level.getMaxBuildHeight()) {
               Holder var31 = this.minecraft.level.getBiome(blockPos);
               list.add("Biome: " + printBiome(var31));
               if (levelChunk2 != null) {
                  float h = level.getMoonBrightness();
                  long l = levelChunk2.getInhabitedTime();
                  DifficultyInstance difficultyInstance = new DifficultyInstance(level.getDifficulty(), level.getDayTime(), l, h);
                  list.add(String.format(Locale.ROOT, "Local Difficulty: %.2f // %.2f (Day %d)", difficultyInstance.getEffectiveDifficulty(), difficultyInstance.getSpecialMultiplier(), this.minecraft.level.getDayTime() / 24000L));
               } else {
                  list.add("Local Difficulty: ??");
               }
            }

            if (levelChunk2 != null && levelChunk2.isOldNoiseGeneration()) {
               list.add("Blending: Old");
            }
         }

         ServerLevel serverLevel = this.getServerLevel();
         if (serverLevel != null) {
            ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
            ChunkGenerator chunkGenerator = serverChunkCache.getGenerator();
            RandomState randomState = serverChunkCache.randomState();
            chunkGenerator.addDebugScreenInfo(list, randomState, blockPos);
            Sampler sampler = randomState.sampler();
            BiomeSource biomeSource = chunkGenerator.getBiomeSource();
            biomeSource.addDebugInfo(list, blockPos, sampler);
            SpawnState spawnState = serverChunkCache.getLastSpawnState();
            if (spawnState != null) {
               Object2IntMap<MobCategory> object2IntMap = spawnState.getMobCategoryCounts();
               int m = spawnState.getSpawnableChunkCount();
               list.add("SC: " + m + ", " + (String)Stream.of(MobCategory.values()).map((mobCategory) -> {
                  char var10000 = Character.toUpperCase(mobCategory.getName().charAt(0));
                  return var10000 + ": " + object2IntMap.getInt(mobCategory);
               }).collect(Collectors.joining(", ")));
            } else {
               list.add("SC: N/A");
            }
         }

         PostChain postChain = this.minecraft.gameRenderer.currentEffect();
         if (postChain != null) {
            list.add("Shader: " + postChain.getName());
         }

         var10001 = this.minecraft.getSoundManager().getDebugString();
         list.add(var10001 + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(this.minecraft.player.getCurrentMood() * 100.0F)));
         return list;
      }
   }

   private static String printBiome(Holder<Biome> holder) {
      return (String)holder.unwrap().map((resourceKey) -> {
         return resourceKey.location().toString();
      }, (biome) -> {
         return "[unregistered " + String.valueOf(biome) + "]";
      });
   }

   @Nullable
   private ServerLevel getServerLevel() {
      IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
      return integratedServer != null ? integratedServer.getLevel(this.minecraft.level.dimension()) : null;
   }

   @Nullable
   private String getServerChunkStats() {
      ServerLevel serverLevel = this.getServerLevel();
      return serverLevel != null ? serverLevel.gatherChunkSourceStats() : null;
   }

   private Level getLevel() {
      return (Level)DataFixUtils.orElse(Optional.ofNullable(this.minecraft.getSingleplayerServer()).flatMap((integratedServer) -> {
         return Optional.ofNullable(integratedServer.getLevel(this.minecraft.level.dimension()));
      }), this.minecraft.level);
   }

   @Nullable
   private LevelChunk getServerChunk() {
      if (this.serverChunk == null) {
         ServerLevel serverLevel = this.getServerLevel();
         if (serverLevel == null) {
            return null;
         }

         this.serverChunk = serverLevel.getChunkSource().getChunkFuture(this.lastPos.x, this.lastPos.z, ChunkStatus.FULL, false).thenApply((chunkResult) -> {
            return (LevelChunk)chunkResult.orElse((Object)null);
         });
      }

      return (LevelChunk)this.serverChunk.getNow((Object)null);
   }

   private LevelChunk getClientChunk() {
      if (this.clientChunk == null) {
         this.clientChunk = this.minecraft.level.getChunk(this.lastPos.x, this.lastPos.z);
      }

      return this.clientChunk;
   }

   protected List<String> getSystemInformation() {
      long l = Runtime.getRuntime().maxMemory();
      long m = Runtime.getRuntime().totalMemory();
      long n = Runtime.getRuntime().freeMemory();
      long o = m - n;
      List<String> list = Lists.newArrayList(new String[]{String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")), String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB", o * 100L / l, bytesToMegabytes(o), bytesToMegabytes(l)), String.format(Locale.ROOT, "Allocation rate: %03dMB/s", bytesToMegabytes(this.allocationRateCalculator.bytesAllocatedPerSecond(o))), String.format(Locale.ROOT, "Allocated: %2d%% %03dMB", m * 100L / l, bytesToMegabytes(m)), "", String.format(Locale.ROOT, "CPU: %s", GlUtil.getCpuInfo()), "", String.format(Locale.ROOT, "Display: %dx%d (%s)", Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), GlUtil.getVendor()), GlUtil.getRenderer(), GlUtil.getOpenGLVersion()});
      if (this.minecraft.showOnlyReducedInfo()) {
         return list;
      } else {
         BlockPos blockPos;
         Iterator var12;
         Entry entry;
         Stream var10000;
         String var10001;
         if (this.block.getType() == Type.BLOCK) {
            blockPos = ((BlockHitResult)this.block).getBlockPos();
            BlockState blockState = this.minecraft.level.getBlockState(blockPos);
            list.add("");
            var10001 = String.valueOf(ChatFormatting.UNDERLINE);
            list.add(var10001 + "Targeted Block: " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
            list.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(blockState.getBlock())));
            var12 = blockState.getValues().entrySet().iterator();

            while(var12.hasNext()) {
               entry = (Entry)var12.next();
               list.add(this.getPropertyValueString(entry));
            }

            var10000 = blockState.getTags().map((tagKey) -> {
               return "#" + String.valueOf(tagKey.location());
            });
            Objects.requireNonNull(list);
            var10000.forEach(list::add);
         }

         if (this.liquid.getType() == Type.BLOCK) {
            blockPos = ((BlockHitResult)this.liquid).getBlockPos();
            FluidState fluidState = this.minecraft.level.getFluidState(blockPos);
            list.add("");
            var10001 = String.valueOf(ChatFormatting.UNDERLINE);
            list.add(var10001 + "Targeted Fluid: " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
            list.add(String.valueOf(BuiltInRegistries.FLUID.getKey(fluidState.getType())));
            var12 = fluidState.getValues().entrySet().iterator();

            while(var12.hasNext()) {
               entry = (Entry)var12.next();
               list.add(this.getPropertyValueString(entry));
            }

            var10000 = fluidState.getTags().map((tagKey) -> {
               return "#" + String.valueOf(tagKey.location());
            });
            Objects.requireNonNull(list);
            var10000.forEach(list::add);
         }

         Entity entity = this.minecraft.crosshairPickEntity;
         if (entity != null) {
            list.add("");
            list.add(String.valueOf(ChatFormatting.UNDERLINE) + "Targeted Entity");
            list.add(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
         }

         return list;
      }
   }

   private String getPropertyValueString(Entry<Property<?>, Comparable<?>> entry) {
      Property<?> property = (Property)entry.getKey();
      Comparable<?> comparable = (Comparable)entry.getValue();
      String string = Util.getPropertyName(property, comparable);
      String var10000;
      if (Boolean.TRUE.equals(comparable)) {
         var10000 = String.valueOf(ChatFormatting.GREEN);
         string = var10000 + string;
      } else if (Boolean.FALSE.equals(comparable)) {
         var10000 = String.valueOf(ChatFormatting.RED);
         string = var10000 + string;
      }

      var10000 = property.getName();
      return var10000 + ": " + string;
   }

   private static long bytesToMegabytes(long l) {
      return l / 1024L / 1024L;
   }

   public boolean showDebugScreen() {
      return this.renderDebug && !this.minecraft.options.hideGui;
   }

   public boolean showProfilerChart() {
      return this.showDebugScreen() && this.renderProfilerChart;
   }

   public boolean showNetworkCharts() {
      return this.showDebugScreen() && this.renderNetworkCharts;
   }

   public boolean showFpsCharts() {
      return this.showDebugScreen() && this.renderFpsCharts;
   }

   public void toggleOverlay() {
      this.renderDebug = !this.renderDebug;
   }

   public void toggleNetworkCharts() {
      this.renderNetworkCharts = !this.renderDebug || !this.renderNetworkCharts;
      if (this.renderNetworkCharts) {
         this.renderDebug = true;
         this.renderFpsCharts = false;
      }

   }

   public void toggleFpsCharts() {
      this.renderFpsCharts = !this.renderDebug || !this.renderFpsCharts;
      if (this.renderFpsCharts) {
         this.renderDebug = true;
         this.renderNetworkCharts = false;
      }

   }

   public void toggleProfilerChart() {
      this.renderProfilerChart = !this.renderDebug || !this.renderProfilerChart;
      if (this.renderProfilerChart) {
         this.renderDebug = true;
      }

   }

   public void logFrameDuration(long l) {
      this.frameTimeLogger.logSample(l);
   }

   public LocalSampleLogger getTickTimeLogger() {
      return this.tickTimeLogger;
   }

   public LocalSampleLogger getPingLogger() {
      return this.pingLogger;
   }

   public LocalSampleLogger getBandwidthLogger() {
      return this.bandwidthLogger;
   }

   public void logRemoteSample(long[] ls, RemoteDebugSampleType remoteDebugSampleType) {
      LocalSampleLogger localSampleLogger = (LocalSampleLogger)this.remoteSupportingLoggers.get(remoteDebugSampleType);
      if (localSampleLogger != null) {
         localSampleLogger.logFullSample(ls);
      }

   }

   public void reset() {
      this.renderDebug = false;
      this.tickTimeLogger.reset();
      this.pingLogger.reset();
      this.bandwidthLogger.reset();
   }

   @Environment(EnvType.CLIENT)
   static class AllocationRateCalculator {
      private static final int UPDATE_INTERVAL_MS = 500;
      private static final List<GarbageCollectorMXBean> GC_MBEANS = ManagementFactory.getGarbageCollectorMXBeans();
      private long lastTime = 0L;
      private long lastHeapUsage = -1L;
      private long lastGcCounts = -1L;
      private long lastRate = 0L;

      long bytesAllocatedPerSecond(long l) {
         long m = System.currentTimeMillis();
         if (m - this.lastTime < 500L) {
            return this.lastRate;
         } else {
            long n = gcCounts();
            if (this.lastTime != 0L && n == this.lastGcCounts) {
               double d = (double)TimeUnit.SECONDS.toMillis(1L) / (double)(m - this.lastTime);
               long o = l - this.lastHeapUsage;
               this.lastRate = Math.round((double)o * d);
            }

            this.lastTime = m;
            this.lastHeapUsage = l;
            this.lastGcCounts = n;
            return this.lastRate;
         }
      }

      private static long gcCounts() {
         long l = 0L;

         GarbageCollectorMXBean garbageCollectorMXBean;
         for(Iterator var2 = GC_MBEANS.iterator(); var2.hasNext(); l += garbageCollectorMXBean.getCollectionCount()) {
            garbageCollectorMXBean = (GarbageCollectorMXBean)var2.next();
         }

         return l;
      }
   }
}
