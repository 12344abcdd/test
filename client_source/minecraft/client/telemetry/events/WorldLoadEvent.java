package net.minecraft.client.telemetry.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.TelemetryPropertyMap;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WorldLoadEvent {
   private boolean eventSent;
   @Nullable
   private TelemetryProperty.GameMode gameMode;
   @Nullable
   private String serverBrand;
   @Nullable
   private final String minigameName;

   public WorldLoadEvent(@Nullable String string) {
      this.minigameName = string;
   }

   public void addProperties(TelemetryPropertyMap.Builder builder) {
      if (this.serverBrand != null) {
         builder.put(TelemetryProperty.SERVER_MODDED, !this.serverBrand.equals("vanilla"));
      }

      builder.put(TelemetryProperty.SERVER_TYPE, this.getServerType());
   }

   private TelemetryProperty.ServerType getServerType() {
      ServerData serverData = Minecraft.getInstance().getCurrentServer();
      if (serverData != null && serverData.isRealm()) {
         return TelemetryProperty.ServerType.REALM;
      } else {
         return Minecraft.getInstance().hasSingleplayerServer() ? TelemetryProperty.ServerType.LOCAL : TelemetryProperty.ServerType.OTHER;
      }
   }

   public boolean send(TelemetryEventSender telemetryEventSender) {
      if (!this.eventSent && this.gameMode != null && this.serverBrand != null) {
         this.eventSent = true;
         telemetryEventSender.send(TelemetryEventType.WORLD_LOADED, (builder) -> {
            builder.put(TelemetryProperty.GAME_MODE, this.gameMode);
            if (this.minigameName != null) {
               builder.put(TelemetryProperty.REALMS_MAP_CONTENT, this.minigameName);
            }

         });
         return true;
      } else {
         return false;
      }
   }

   public void setGameMode(GameType gameType, boolean bl) {
      TelemetryProperty.GameMode var10001;
      switch(gameType) {
      case SURVIVAL:
         var10001 = bl ? TelemetryProperty.GameMode.HARDCORE : TelemetryProperty.GameMode.SURVIVAL;
         break;
      case CREATIVE:
         var10001 = TelemetryProperty.GameMode.CREATIVE;
         break;
      case ADVENTURE:
         var10001 = TelemetryProperty.GameMode.ADVENTURE;
         break;
      case SPECTATOR:
         var10001 = TelemetryProperty.GameMode.SPECTATOR;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      this.gameMode = var10001;
   }

   public void setServerBrand(String string) {
      this.serverBrand = string;
   }
}
