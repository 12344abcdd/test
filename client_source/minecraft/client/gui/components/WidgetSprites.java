package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public record WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused, ResourceLocation disabledFocused) {
   public WidgetSprites(ResourceLocation resourceLocation, ResourceLocation resourceLocation2) {
      this(resourceLocation, resourceLocation, resourceLocation2, resourceLocation2);
   }

   public WidgetSprites(ResourceLocation resourceLocation, ResourceLocation resourceLocation2, ResourceLocation resourceLocation3) {
      this(resourceLocation, resourceLocation2, resourceLocation3, resourceLocation2);
   }

   public WidgetSprites(ResourceLocation resourceLocation, ResourceLocation resourceLocation2, ResourceLocation resourceLocation3, ResourceLocation resourceLocation4) {
      this.enabled = resourceLocation;
      this.disabled = resourceLocation2;
      this.enabledFocused = resourceLocation3;
      this.disabledFocused = resourceLocation4;
   }

   public ResourceLocation get(boolean bl, boolean bl2) {
      if (bl) {
         return bl2 ? this.enabledFocused : this.enabled;
      } else {
         return bl2 ? this.disabledFocused : this.disabled;
      }
   }

   public ResourceLocation enabled() {
      return this.enabled;
   }

   public ResourceLocation disabled() {
      return this.disabled;
   }

   public ResourceLocation enabledFocused() {
      return this.enabledFocused;
   }

   public ResourceLocation disabledFocused() {
      return this.disabledFocused;
   }
}
