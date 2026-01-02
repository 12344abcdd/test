package net.minecraft.client.gui.screens.advancements;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
enum AdvancementTabType {
   ABOVE(new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_above_left_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_above_middle_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_above_right_selected")), new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_above_left"), ResourceLocation.withDefaultNamespace("advancements/tab_above_middle"), ResourceLocation.withDefaultNamespace("advancements/tab_above_right")), 28, 32, 8),
   BELOW(new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_below_left_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_below_middle_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_below_right_selected")), new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_below_left"), ResourceLocation.withDefaultNamespace("advancements/tab_below_middle"), ResourceLocation.withDefaultNamespace("advancements/tab_below_right")), 28, 32, 8),
   LEFT(new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_left_top_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_left_middle_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_left_bottom_selected")), new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_left_top"), ResourceLocation.withDefaultNamespace("advancements/tab_left_middle"), ResourceLocation.withDefaultNamespace("advancements/tab_left_bottom")), 32, 28, 5),
   RIGHT(new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_right_top_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_right_middle_selected"), ResourceLocation.withDefaultNamespace("advancements/tab_right_bottom_selected")), new AdvancementTabType.Sprites(ResourceLocation.withDefaultNamespace("advancements/tab_right_top"), ResourceLocation.withDefaultNamespace("advancements/tab_right_middle"), ResourceLocation.withDefaultNamespace("advancements/tab_right_bottom")), 32, 28, 5);

   private final AdvancementTabType.Sprites selectedSprites;
   private final AdvancementTabType.Sprites unselectedSprites;
   private final int width;
   private final int height;
   private final int max;

   private AdvancementTabType(final AdvancementTabType.Sprites sprites, final AdvancementTabType.Sprites sprites2, final int j, final int k, final int l) {
      this.selectedSprites = sprites;
      this.unselectedSprites = sprites2;
      this.width = j;
      this.height = k;
      this.max = l;
   }

   public int getMax() {
      return this.max;
   }

   public void draw(GuiGraphics guiGraphics, int i, int j, boolean bl, int k) {
      AdvancementTabType.Sprites sprites = bl ? this.selectedSprites : this.unselectedSprites;
      ResourceLocation resourceLocation;
      if (k == 0) {
         resourceLocation = sprites.first();
      } else if (k == this.max - 1) {
         resourceLocation = sprites.last();
      } else {
         resourceLocation = sprites.middle();
      }

      guiGraphics.blitSprite(resourceLocation, i + this.getX(k), j + this.getY(k), this.width, this.height);
   }

   public void drawIcon(GuiGraphics guiGraphics, int i, int j, int k, ItemStack itemStack) {
      int l = i + this.getX(k);
      int m = j + this.getY(k);
      switch(this.ordinal()) {
      case 0:
         l += 6;
         m += 9;
         break;
      case 1:
         l += 6;
         m += 6;
         break;
      case 2:
         l += 10;
         m += 5;
         break;
      case 3:
         l += 6;
         m += 5;
      }

      guiGraphics.renderFakeItem(itemStack, l, m);
   }

   public int getX(int i) {
      switch(this.ordinal()) {
      case 0:
         return (this.width + 4) * i;
      case 1:
         return (this.width + 4) * i;
      case 2:
         return -this.width + 4;
      case 3:
         return 248;
      default:
         throw new UnsupportedOperationException("Don't know what this tab type is!" + String.valueOf(this));
      }
   }

   public int getY(int i) {
      switch(this.ordinal()) {
      case 0:
         return -this.height + 4;
      case 1:
         return 136;
      case 2:
         return this.height * i;
      case 3:
         return this.height * i;
      default:
         throw new UnsupportedOperationException("Don't know what this tab type is!" + String.valueOf(this));
      }
   }

   public boolean isMouseOver(int i, int j, int k, double d, double e) {
      int l = i + this.getX(k);
      int m = j + this.getY(k);
      return d > (double)l && d < (double)(l + this.width) && e > (double)m && e < (double)(m + this.height);
   }

   // $FF: synthetic method
   private static AdvancementTabType[] $values() {
      return new AdvancementTabType[]{ABOVE, BELOW, LEFT, RIGHT};
   }

   @Environment(EnvType.CLIENT)
   private static record Sprites(ResourceLocation first, ResourceLocation middle, ResourceLocation last) {
      Sprites(ResourceLocation resourceLocation, ResourceLocation resourceLocation2, ResourceLocation resourceLocation3) {
         this.first = resourceLocation;
         this.middle = resourceLocation2;
         this.last = resourceLocation3;
      }

      public ResourceLocation first() {
         return this.first;
      }

      public ResourceLocation middle() {
         return this.middle;
      }

      public ResourceLocation last() {
         return this.last;
      }
   }
}
