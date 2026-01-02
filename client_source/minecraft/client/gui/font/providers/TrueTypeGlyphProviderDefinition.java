package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

@Environment(EnvType.CLIENT)
public record TrueTypeGlyphProviderDefinition(ResourceLocation location, float size, float oversample, TrueTypeGlyphProviderDefinition.Shift shift, String skip) implements GlyphProviderDefinition {
   private static final Codec<String> SKIP_LIST_CODEC;
   public static final MapCodec<TrueTypeGlyphProviderDefinition> CODEC;

   public TrueTypeGlyphProviderDefinition(ResourceLocation resourceLocation, float f, float g, TrueTypeGlyphProviderDefinition.Shift shift, String string) {
      this.location = resourceLocation;
      this.size = f;
      this.oversample = g;
      this.shift = shift;
      this.skip = string;
   }

   public GlyphProviderType type() {
      return GlyphProviderType.TTF;
   }

   public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
      return Either.left(this::load);
   }

   private GlyphProvider load(ResourceManager resourceManager) throws IOException {
      FT_Face fT_Face = null;
      ByteBuffer byteBuffer = null;

      try {
         InputStream inputStream = resourceManager.open(this.location.withPrefix("font/"));

         TrueTypeGlyphProvider var19;
         try {
            byteBuffer = TextureUtil.readResource(inputStream);
            byteBuffer.flip();
            synchronized(FreeTypeUtil.LIBRARY_LOCK) {
               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
                  FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), byteBuffer, 0L, pointerBuffer), "Initializing font face");
                  fT_Face = FT_Face.create(pointerBuffer.get());
               } catch (Throwable var14) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable var12) {
                        var14.addSuppressed(var12);
                     }
                  }

                  throw var14;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }

               String string = FreeType.FT_Get_Font_Format(fT_Face);
               if (!"TrueType".equals(string)) {
                  throw new IOException("Font is not in TTF format, was " + string);
               }

               FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(fT_Face, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
               var19 = new TrueTypeGlyphProvider(byteBuffer, fT_Face, this.size, this.oversample, this.shift.x, this.shift.y, this.skip);
            }
         } catch (Throwable var16) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var11) {
                  var16.addSuppressed(var11);
               }
            }

            throw var16;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         return var19;
      } catch (Exception var17) {
         synchronized(FreeTypeUtil.LIBRARY_LOCK) {
            if (fT_Face != null) {
               FreeType.FT_Done_Face(fT_Face);
            }
         }

         MemoryUtil.memFree(byteBuffer);
         throw var17;
      }
   }

   public ResourceLocation location() {
      return this.location;
   }

   public float size() {
      return this.size;
   }

   public float oversample() {
      return this.oversample;
   }

   public TrueTypeGlyphProviderDefinition.Shift shift() {
      return this.shift;
   }

   public String skip() {
      return this.skip;
   }

   static {
      SKIP_LIST_CODEC = Codec.withAlternative(Codec.STRING, Codec.STRING.listOf(), (list) -> {
         return String.join("", list);
      });
      CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(ResourceLocation.CODEC.fieldOf("file").forGetter(TrueTypeGlyphProviderDefinition::location), Codec.FLOAT.optionalFieldOf("size", 11.0F).forGetter(TrueTypeGlyphProviderDefinition::size), Codec.FLOAT.optionalFieldOf("oversample", 1.0F).forGetter(TrueTypeGlyphProviderDefinition::oversample), TrueTypeGlyphProviderDefinition.Shift.CODEC.optionalFieldOf("shift", TrueTypeGlyphProviderDefinition.Shift.NONE).forGetter(TrueTypeGlyphProviderDefinition::shift), SKIP_LIST_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeGlyphProviderDefinition::skip)).apply(instance, TrueTypeGlyphProviderDefinition::new);
      });
   }

   @Environment(EnvType.CLIENT)
   public static record Shift(float x, float y) {
      final float x;
      final float y;
      public static final TrueTypeGlyphProviderDefinition.Shift NONE = new TrueTypeGlyphProviderDefinition.Shift(0.0F, 0.0F);
      public static final Codec<TrueTypeGlyphProviderDefinition.Shift> CODEC = Codec.floatRange(-512.0F, 512.0F).listOf().comapFlatMap((list) -> {
         return Util.fixedSize(list, 2).map((listx) -> {
            return new TrueTypeGlyphProviderDefinition.Shift((Float)listx.get(0), (Float)listx.get(1));
         });
      }, (shift) -> {
         return List.of(shift.x, shift.y);
      });

      public Shift(float f, float g) {
         this.x = f;
         this.y = g;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }
   }
}
