package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum SpecialGlyphs implements GlyphInfo {
   WHITE(() -> {
      return generate(5, 8, (i, j) -> {
         return -1;
      });
   }),
   MISSING(() -> {
      int i = true;
      int j = true;
      return generate(5, 8, (ix, jx) -> {
         boolean bl = ix == 0 || ix + 1 == 5 || jx == 0 || jx + 1 == 8;
         return bl ? -1 : 0;
      });
   });

   final NativeImage image;

   private static NativeImage generate(int i, int j, SpecialGlyphs.PixelProvider pixelProvider) {
      NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, i, j, false);

      for(int k = 0; k < j; ++k) {
         for(int l = 0; l < i; ++l) {
            nativeImage.setPixelRGBA(l, k, pixelProvider.getColor(l, k));
         }
      }

      nativeImage.untrack();
      return nativeImage;
   }

   private SpecialGlyphs(final Supplier<NativeImage> supplier) {
      this.image = (NativeImage)supplier.get();
   }

   public float getAdvance() {
      return (float)(this.image.getWidth() + 1);
   }

   public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
      return (BakedGlyph)function.apply(new SheetGlyphInfo() {
         public int getPixelWidth() {
            return SpecialGlyphs.this.image.getWidth();
         }

         public int getPixelHeight() {
            return SpecialGlyphs.this.image.getHeight();
         }

         public float getOversample() {
            return 1.0F;
         }

         public void upload(int i, int j) {
            SpecialGlyphs.this.image.upload(0, i, j, false);
         }

         public boolean isColored() {
            return true;
         }
      });
   }

   // $FF: synthetic method
   private static SpecialGlyphs[] $values() {
      return new SpecialGlyphs[]{WHITE, MISSING};
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   private interface PixelProvider {
      int getColor(int i, int j);
   }
}
