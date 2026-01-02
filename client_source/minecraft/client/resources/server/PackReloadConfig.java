package net.minecraft.client.resources.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface PackReloadConfig {
   void scheduleReload(PackReloadConfig.Callbacks callbacks);

   @Environment(EnvType.CLIENT)
   public interface Callbacks {
      void onSuccess();

      void onFailure(boolean bl);

      List<PackReloadConfig.IdAndPath> packsToLoad();
   }

   @Environment(EnvType.CLIENT)
   public static record IdAndPath(UUID id, Path path) {
      public IdAndPath(UUID uUID, Path path) {
         this.id = uUID;
         this.path = path;
      }

      public UUID id() {
         return this.id;
      }

      public Path path() {
         return this.path;
      }
   }
}
