package net.minecraft.client.server;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.stats.Stats;
import net.minecraft.util.ModCheck;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class IntegratedServer extends MinecraftServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MIN_SIM_DISTANCE = 2;
   private final Minecraft minecraft;
   private boolean paused = true;
   private int publishedPort = -1;
   @Nullable
   private GameType publishedGameType;
   @Nullable
   private LanServerPinger lanPinger;
   @Nullable
   private UUID uuid;
   private int previousSimulationDistance = 0;

   public IntegratedServer(Thread thread, Minecraft minecraft, LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory) {
      super(thread, levelStorageAccess, packRepository, worldStem, minecraft.getProxy(), minecraft.getFixerUpper(), services, chunkProgressListenerFactory);
      this.setSingleplayerProfile(minecraft.getGameProfile());
      this.setDemo(minecraft.isDemo());
      this.setPlayerList(new IntegratedPlayerList(this, this.registries(), this.playerDataStorage));
      this.minecraft = minecraft;
   }

   public boolean initServer() {
      LOGGER.info("Starting integrated minecraft server version {}", SharedConstants.getCurrentVersion().getName());
      this.setUsesAuthentication(true);
      this.setPvpAllowed(true);
      this.setFlightAllowed(true);
      this.initializeKeyPair();
      this.loadLevel();
      GameProfile gameProfile = this.getSingleplayerProfile();
      String string = this.getWorldData().getLevelName();
      this.setMotd(gameProfile != null ? gameProfile.getName() + " - " + string : string);
      return true;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public void tickServer(BooleanSupplier booleanSupplier) {
      boolean bl = this.paused;
      this.paused = Minecraft.getInstance().isPaused();
      ProfilerFiller profilerFiller = this.getProfiler();
      if (!bl && this.paused) {
         profilerFiller.push("autoSave");
         LOGGER.info("Saving and pausing game...");
         this.saveEverything(false, false, false);
         profilerFiller.pop();
      }

      boolean bl2 = Minecraft.getInstance().getConnection() != null;
      if (bl2 && this.paused) {
         this.tickPaused();
      } else {
         if (bl && !this.paused) {
            this.forceTimeSynchronization();
         }

         super.tickServer(booleanSupplier);
         int i = Math.max(2, (Integer)this.minecraft.options.renderDistance().get());
         if (i != this.getPlayerList().getViewDistance()) {
            LOGGER.info("Changing view distance to {}, from {}", i, this.getPlayerList().getViewDistance());
            this.getPlayerList().setViewDistance(i);
         }

         int j = Math.max(2, (Integer)this.minecraft.options.simulationDistance().get());
         if (j != this.previousSimulationDistance) {
            LOGGER.info("Changing simulation distance to {}, from {}", j, this.previousSimulationDistance);
            this.getPlayerList().setSimulationDistance(j);
            this.previousSimulationDistance = j;
         }

      }
   }

   protected LocalSampleLogger getTickTimeLogger() {
      return this.minecraft.getDebugOverlay().getTickTimeLogger();
   }

   public boolean isTickTimeLoggingEnabled() {
      return true;
   }

   private void tickPaused() {
      Iterator var1 = this.getPlayerList().getPlayers().iterator();

      while(var1.hasNext()) {
         ServerPlayer serverPlayer = (ServerPlayer)var1.next();
         serverPlayer.awardStat(Stats.TOTAL_WORLD_TIME);
      }

   }

   public boolean shouldRconBroadcast() {
      return true;
   }

   public boolean shouldInformAdmins() {
      return true;
   }

   public Path getServerDirectory() {
      return this.minecraft.gameDirectory.toPath();
   }

   public boolean isDedicatedServer() {
      return false;
   }

   public int getRateLimitPacketsPerSecond() {
      return 0;
   }

   public boolean isEpollEnabled() {
      return false;
   }

   public void onServerCrash(CrashReport crashReport) {
      this.minecraft.delayCrashRaw(crashReport);
   }

   public SystemReport fillServerSystemReport(SystemReport systemReport) {
      systemReport.setDetail("Type", "Integrated Server (map_client.txt)");
      systemReport.setDetail("Is Modded", () -> {
         return this.getModdedStatus().fullDescription();
      });
      Minecraft var10002 = this.minecraft;
      Objects.requireNonNull(var10002);
      systemReport.setDetail("Launched Version", var10002::getLaunchedVersion);
      return systemReport;
   }

   public ModCheck getModdedStatus() {
      return Minecraft.checkModStatus().merge(super.getModdedStatus());
   }

   public boolean publishServer(@Nullable GameType gameType, boolean bl, int i) {
      try {
         this.minecraft.prepareForMultiplayer();
         this.minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync((optional) -> {
            optional.ifPresent((profileKeyPair) -> {
               ClientPacketListener clientPacketListener = this.minecraft.getConnection();
               if (clientPacketListener != null) {
                  clientPacketListener.setKeyPair(profileKeyPair);
               }

            });
         }, this.minecraft);
         this.getConnection().startTcpServerListener((InetAddress)null, i);
         LOGGER.info("Started serving on {}", i);
         this.publishedPort = i;
         this.lanPinger = new LanServerPinger(this.getMotd(), i.makeConcatWithConstants<invokedynamic>(i));
         this.lanPinger.start();
         this.publishedGameType = gameType;
         this.getPlayerList().setAllowCommandsForAllPlayers(bl);
         int j = this.getProfilePermissions(this.minecraft.player.getGameProfile());
         this.minecraft.player.setPermissionLevel(j);
         Iterator var5 = this.getPlayerList().getPlayers().iterator();

         while(var5.hasNext()) {
            ServerPlayer serverPlayer = (ServerPlayer)var5.next();
            this.getCommands().sendCommands(serverPlayer);
         }

         return true;
      } catch (IOException var7) {
         return false;
      }
   }

   public void stopServer() {
      super.stopServer();
      if (this.lanPinger != null) {
         this.lanPinger.interrupt();
         this.lanPinger = null;
      }

   }

   public void halt(boolean bl) {
      this.executeBlocking(() -> {
         List<ServerPlayer> list = Lists.newArrayList(this.getPlayerList().getPlayers());
         Iterator var2 = list.iterator();

         while(var2.hasNext()) {
            ServerPlayer serverPlayer = (ServerPlayer)var2.next();
            if (!serverPlayer.getUUID().equals(this.uuid)) {
               this.getPlayerList().remove(serverPlayer);
            }
         }

      });
      super.halt(bl);
      if (this.lanPinger != null) {
         this.lanPinger.interrupt();
         this.lanPinger = null;
      }

   }

   public boolean isPublished() {
      return this.publishedPort > -1;
   }

   public int getPort() {
      return this.publishedPort;
   }

   public void setDefaultGameType(GameType gameType) {
      super.setDefaultGameType(gameType);
      this.publishedGameType = null;
   }

   public boolean isCommandBlockEnabled() {
      return true;
   }

   public int getOperatorUserPermissionLevel() {
      return 2;
   }

   public int getFunctionCompilationLevel() {
      return 2;
   }

   public void setUUID(UUID uUID) {
      this.uuid = uUID;
   }

   public boolean isSingleplayerOwner(GameProfile gameProfile) {
      return this.getSingleplayerProfile() != null && gameProfile.getName().equalsIgnoreCase(this.getSingleplayerProfile().getName());
   }

   public int getScaledTrackingDistance(int i) {
      return (int)((Double)this.minecraft.options.entityDistanceScaling().get() * (double)i);
   }

   public boolean forceSynchronousWrites() {
      return this.minecraft.options.syncWrites;
   }

   @Nullable
   public GameType getForcedGameType() {
      return this.isPublished() ? (GameType)MoreObjects.firstNonNull(this.publishedGameType, this.worldData.getGameType()) : null;
   }

   public boolean saveEverything(boolean bl, boolean bl2, boolean bl3) {
      boolean bl4 = super.saveEverything(bl, bl2, bl3);
      this.warnOnLowDiskSpace();
      return bl4;
   }

   private void warnOnLowDiskSpace() {
      if (this.storageSource.checkForLowDiskSpace()) {
         this.minecraft.execute(() -> {
            SystemToast.onLowDiskSpace(this.minecraft);
         });
      }

   }

   public void reportChunkLoadFailure(Throwable throwable, RegionStorageInfo regionStorageInfo, ChunkPos chunkPos) {
      super.reportChunkLoadFailure(throwable, regionStorageInfo, chunkPos);
      this.warnOnLowDiskSpace();
      this.minecraft.execute(() -> {
         SystemToast.onChunkLoadFailure(this.minecraft, chunkPos);
      });
   }

   public void reportChunkSaveFailure(Throwable throwable, RegionStorageInfo regionStorageInfo, ChunkPos chunkPos) {
      super.reportChunkSaveFailure(throwable, regionStorageInfo, chunkPos);
      this.warnOnLowDiskSpace();
      this.minecraft.execute(() -> {
         SystemToast.onChunkSaveFailure(this.minecraft, chunkPos);
      });
   }

   // $FF: synthetic method
   public SampleLogger getTickTimeLogger() {
      return this.getTickTimeLogger();
   }
}
