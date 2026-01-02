package net.minecraft.client.multiplayer;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public record TransferState(Map<ResourceLocation, byte[]> cookies) {
   public TransferState(Map<ResourceLocation, byte[]> map) {
      this.cookies = map;
   }

   public Map<ResourceLocation, byte[]> cookies() {
      return this.cookies;
   }
}
