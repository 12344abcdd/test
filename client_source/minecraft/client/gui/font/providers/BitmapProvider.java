package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class BitmapProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private final NativeImage image;
   private final CodepointMap<BitmapProvider.Glyph> glyphs;

   BitmapProvider(NativeImage nativeImage, CodepointMap<BitmapProvider.Glyph> codepointMap) {
      this.image = nativeImage;
      this.glyphs = codepointMap;
   }

   public void close() {
      this.image.close();
   }

   @Nullable
   public GlyphInfo getGlyph(int i) {
      return (GlyphInfo)this.glyphs.get(i);
   }

   public IntSet getSupportedGlyphs() {
      return IntSets.unmodifiable(this.glyphs.keySet());
   }

   @Environment(EnvType.CLIENT)
   private static record Glyph(float scale, NativeImage image, int offsetX, int offsetY, int width, int height, int advance, int ascent) implements GlyphInfo {
      final float scale;
      final NativeImage image;
      final int offsetX;
      final int offsetY;
      final int width;
      final int height;
      final int ascent;

      Glyph(float f, NativeImage nativeImage, int i, int j, int k, int l, int m, int n) {
         this.scale = f;
         this.image = nativeImage;
         this.offsetX = i;
         this.offsetY = j;
         this.width = k;
         this.height = l;
         this.advance = m;
         this.ascent = n;
      }

      public float getAdvance() {
         return (float)this.advance;
      }

      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
         return (BakedGlyph)function.apply(new SheetGlyphInfo() {
            // $FF: synthetic field
            final BitmapProvider.Glyph field_37903;

            {
               this.field_37903 = glyph;
            }

            public float getOversample() {
               return 1.0F / this.field_37903.scale;
            }

            public int getPixelWidth() {
               return this.field_37903.width;
            }

            public int getPixelHeight() {
               return this.field_37903.height;
            }

            public float getBearingTop() {
               return (float)this.field_37903.ascent;
            }

            public void upload(int i, int j) {
               this.field_37903.image.upload(0, i, j, this.field_37903.offsetX, this.field_37903.offsetY, this.field_37903.width, this.field_37903.height, false, false);
            }

            public boolean isColored() {
               return this.field_37903.image.format().components() > 1;
            }
         });
      }

      public float scale() {
         return this.scale;
      }

      public NativeImage image() {
         return this.image;
      }

      public int offsetX() {
         return this.offsetX;
      }

      public int offsetY() {
         return this.offsetY;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }

      public int advance() {
         return this.advance;
      }

      public int ascent() {
         return this.ascent;
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Definition(ResourceLocation file, int height, int ascent, int[][] codepointGrid) implements GlyphProviderDefinition {
      private static final Codec<int[][]> CODEPOINT_GRID_CODEC;
      public static final MapCodec<BitmapProvider.Definition> CODEC;

      public Definition(ResourceLocation resourceLocation, int i, int j, int[][] is) {
         this.file = resourceLocation;
         this.height = i;
         this.ascent = j;
         this.codepointGrid = is;
      }

      private static DataResult<int[][]> validateDimensions(int[][] is) {
         int i = is.length;
         if (i == 0) {
            return DataResult.error(() -> {
               return "Expected to find data in codepoint grid";
            });
         } else {
            int[] js = is[0];
            int j = js.length;
            if (j == 0) {
               return DataResult.error(() -> {
                  return "Expected to find data in codepoint grid";
               });
            } else {
               for(int k = 1; k < i; ++k) {
                  int[] ks = is[k];
                  if (ks.length != j) {
                     return DataResult.error(() -> {
                        return "Lines in codepoint grid have to be the same length (found: " + ks.length + " codepoints, expected: " + j + "), pad with \\u0000";
                     });
                  }
               }

               return DataResult.success(is);
            }
         }
      }

      private static DataResult<BitmapProvider.Definition> validate(BitmapProvider.Definition definition) {
         return definition.ascent > definition.height ? DataResult.error(() -> {
            return "Ascent " + definition.ascent + " higher than height " + definition.height;
         }) : DataResult.success(definition);
      }

      public GlyphProviderType type() {
         return GlyphProviderType.BITMAP;
      }

      public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
         return Either.left(this::load);
      }

      private GlyphProvider load(ResourceManager resourceManager) throws IOException {
         ResourceLocation resourceLocation = this.file.withPrefix("textures/");
         InputStream inputStream = resourceManager.open(resourceLocation);

         BitmapProvider var22;
         try {
            NativeImage nativeImage = NativeImage.read(NativeImage.Format.RGBA, inputStream);
            int i = nativeImage.getWidth();
            int j = nativeImage.getHeight();
            int k = i / this.codepointGrid[0].length;
            int l = j / this.codepointGrid.length;
            float f = (float)this.height / (float)l;
            CodepointMap<BitmapProvider.Glyph> codepointMap = new CodepointMap((ix) -> {
               return new BitmapProvider.Glyph[ix];
            }, (ix) -> {
               return new BitmapProvider.Glyph[ix][];
            });
            int m = 0;

            while(true) {
               if (m >= this.codepointGrid.length) {
                  var22 = new BitmapProvider(nativeImage, codepointMap);
                  break;
               }

               int n = 0;
               int[] var13 = this.codepointGrid[m];
               int var14 = var13.length;

               for(int var15 = 0; var15 < var14; ++var15) {
                  int o = var13[var15];
                  int p = n++;
                  if (o != 0) {
                     int q = this.getActualGlyphWidth(nativeImage, k, l, p, m);
                     BitmapProvider.Glyph glyph = (BitmapProvider.Glyph)codepointMap.put(o, new BitmapProvider.Glyph(f, nativeImage, p * k, m * l, k, l, (int)(0.5D + (double)((float)q * f)) + 1, this.ascent));
                     if (glyph != null) {
                        BitmapProvider.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString(o), resourceLocation);
                     }
                  }
               }

               ++m;
            }
         } catch (Throwable var21) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var20) {
                  var21.addSuppressed(var20);
               }
            }

            throw var21;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         return var22;
      }

      private int getActualGlyphWidth(NativeImage nativeImage, int i, int j, int k, int l) {
         int m;
         for(m = i - 1; m >= 0; --m) {
            int n = k * i + m;

            for(int o = 0; o < j; ++o) {
               int p = l * j + o;
               if (nativeImage.getLuminanceOrAlpha(n, p) != 0) {
                  return m + 1;
               }
            }
         }

         return m + 1;
      }

      public ResourceLocation file() {
         return this.file;
      }

      public int height() {
         return this.height;
      }

      public int ascent() {
         return this.ascent;
      }

      public int[][] codepointGrid() {
         return this.codepointGrid;
      }

      static {
         CODEPOINT_GRID_CODEC = Codec.STRING.listOf().xmap((list) -> {
            int i = list.size();
            int[][] is = new int[i][];

            for(int j = 0; j < i; ++j) {
               is[j] = ((String)list.get(j)).codePoints().toArray();
            }

            return is;
         }, (is) -> {
            List<String> list = new ArrayList(is.length);
            int[][] var2 = is;
            int var3 = is.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               int[] js = var2[var4];
               list.add(new String(js, 0, js.length));
            }

            return list;
         }).validate(BitmapProvider.Definition::validateDimensions);
         CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(ResourceLocation.CODEC.fieldOf("file").forGetter(BitmapProvider.Definition::file), Codec.INT.optionalFieldOf("height", 8).forGetter(BitmapProvider.Definition::height), Codec.INT.fieldOf("ascent").forGetter(BitmapProvider.Definition::ascent), CODEPOINT_GRID_CODEC.fieldOf("chars").forGetter(BitmapProvider.Definition::codepointGrid)).apply(instance, BitmapProvider.Definition::new);
         }).validate(BitmapProvider.Definition::validate);
      }
   }
}
