package net.minecraft.client.resources.metadata.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record FrameSize(int width, int height) {
   public FrameSize(int i, int j) {
      this.width = i;
      this.height = j;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }
}
