package net.minecraft.client.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class LevelLoadStatusManager {
   private final LocalPlayer player;
   private final ClientLevel level;
   private final LevelRenderer levelRenderer;
   private LevelLoadStatusManager.Status status;

   public LevelLoadStatusManager(LocalPlayer localPlayer, ClientLevel clientLevel, LevelRenderer levelRenderer) {
      this.status = LevelLoadStatusManager.Status.WAITING_FOR_SERVER;
      this.player = localPlayer;
      this.level = clientLevel;
      this.levelRenderer = levelRenderer;
   }

   public void tick() {
      switch(this.status.ordinal()) {
      case 1:
         BlockPos blockPos = this.player.blockPosition();
         boolean bl = this.level.isOutsideBuildHeight(blockPos.getY());
         if (bl || this.levelRenderer.isSectionCompiled(blockPos) || this.player.isSpectator() || !this.player.isAlive()) {
            this.status = LevelLoadStatusManager.Status.LEVEL_READY;
         }
      case 0:
      case 2:
      default:
      }
   }

   public boolean levelReady() {
      return this.status == LevelLoadStatusManager.Status.LEVEL_READY;
   }

   public void loadingPacketsReceived() {
      if (this.status == LevelLoadStatusManager.Status.WAITING_FOR_SERVER) {
         this.status = LevelLoadStatusManager.Status.WAITING_FOR_PLAYER_CHUNK;
      }

   }

   @Environment(EnvType.CLIENT)
   private static enum Status {
      WAITING_FOR_SERVER,
      WAITING_FOR_PLAYER_CHUNK,
      LEVEL_READY;

      // $FF: synthetic method
      private static LevelLoadStatusManager.Status[] $values() {
         return new LevelLoadStatusManager.Status[]{WAITING_FOR_SERVER, WAITING_FOR_PLAYER_CHUNK, LEVEL_READY};
      }
   }
}
