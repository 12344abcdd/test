package net.minecraft.client.server;

import com.mojang.authlib.GameProfile;
import java.net.SocketAddress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class IntegratedPlayerList extends PlayerList {
   @Nullable
   private CompoundTag playerData;

   public IntegratedPlayerList(IntegratedServer integratedServer, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, PlayerDataStorage playerDataStorage) {
      super(integratedServer, layeredRegistryAccess, playerDataStorage, 8);
      this.setViewDistance(10);
   }

   protected void save(ServerPlayer serverPlayer) {
      if (this.getServer().isSingleplayerOwner(serverPlayer.getGameProfile())) {
         this.playerData = serverPlayer.saveWithoutId(new CompoundTag());
      }

      super.save(serverPlayer);
   }

   public Component canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile) {
      return (Component)(this.getServer().isSingleplayerOwner(gameProfile) && this.getPlayerByName(gameProfile.getName()) != null ? Component.translatable("multiplayer.disconnect.name_taken") : super.canPlayerLogin(socketAddress, gameProfile));
   }

   public IntegratedServer getServer() {
      return (IntegratedServer)super.getServer();
   }

   @Nullable
   public CompoundTag getSingleplayerData() {
      return this.playerData;
   }

   // $FF: synthetic method
   public MinecraftServer getServer() {
      return this.getServer();
   }
}
