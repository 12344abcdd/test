package net.minecraft.client.gui.font.providers;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastBufferedInputStream;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class UnihexProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int GLYPH_HEIGHT = 16;
   private static final int DIGITS_PER_BYTE = 2;
   private static final int DIGITS_FOR_WIDTH_8 = 32;
   private static final int DIGITS_FOR_WIDTH_16 = 64;
   private static final int DIGITS_FOR_WIDTH_24 = 96;
   private static final int DIGITS_FOR_WIDTH_32 = 128;
   private final CodepointMap<UnihexProvider.Glyph> glyphs;

   UnihexProvider(CodepointMap<UnihexProvider.Glyph> codepointMap) {
      this.glyphs = codepointMap;
   }

   @Nullable
   public GlyphInfo getGlyph(int i) {
      return (GlyphInfo)this.glyphs.get(i);
   }

   public IntSet getSupportedGlyphs() {
      return this.glyphs.keySet();
   }

   @VisibleForTesting
   static void unpackBitsToBytes(IntBuffer intBuffer, int i, int j, int k) {
      int l = 32 - j - 1;
      int m = 32 - k - 1;

      for(int n = l; n >= m; --n) {
         if (n < 32 && n >= 0) {
            boolean bl = (i >> n & 1) != 0;
            intBuffer.put(bl ? -1 : 0);
         } else {
            intBuffer.put(0);
         }
      }

   }

   static void unpackBitsToBytes(IntBuffer intBuffer, UnihexProvider.LineData lineData, int i, int j) {
      for(int k = 0; k < 16; ++k) {
         int l = lineData.line(k);
         unpackBitsToBytes(intBuffer, l, i, j);
      }

   }

   @VisibleForTesting
   static void readFromStream(InputStream inputStream, UnihexProvider.ReaderOutput readerOutput) throws IOException {
      int i = 0;
      ByteArrayList byteList = new ByteArrayList(128);

      while(true) {
         boolean bl = copyUntil(inputStream, byteList, 58);
         int j = byteList.size();
         if (j == 0 && !bl) {
            return;
         }

         if (!bl || j != 4 && j != 5 && j != 6) {
            throw new IllegalArgumentException("Invalid entry at line " + i + ": expected 4, 5 or 6 hex digits followed by a colon");
         }

         int k = 0;

         int l;
         for(l = 0; l < j; ++l) {
            k = k << 4 | decodeHex(i, byteList.getByte(l));
         }

         byteList.clear();
         copyUntil(inputStream, byteList, 10);
         l = byteList.size();
         UnihexProvider.LineData var10000;
         switch(l) {
         case 32:
            var10000 = UnihexProvider.ByteContents.read(i, byteList);
            break;
         case 64:
            var10000 = UnihexProvider.ShortContents.read(i, byteList);
            break;
         case 96:
            var10000 = UnihexProvider.IntContents.read24(i, byteList);
            break;
         case 128:
            var10000 = UnihexProvider.IntContents.read32(i, byteList);
            break;
         default:
            throw new IllegalArgumentException("Invalid entry at line " + i + ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line");
         }

         UnihexProvider.LineData lineData = var10000;
         readerOutput.accept(k, lineData);
         ++i;
         byteList.clear();
      }
   }

   static int decodeHex(int i, ByteList byteList, int j) {
      return decodeHex(i, byteList.getByte(j));
   }

   private static int decodeHex(int i, byte b) {
      byte var10000;
      switch(b) {
      case 48:
         var10000 = 0;
         break;
      case 49:
         var10000 = 1;
         break;
      case 50:
         var10000 = 2;
         break;
      case 51:
         var10000 = 3;
         break;
      case 52:
         var10000 = 4;
         break;
      case 53:
         var10000 = 5;
         break;
      case 54:
         var10000 = 6;
         break;
      case 55:
         var10000 = 7;
         break;
      case 56:
         var10000 = 8;
         break;
      case 57:
         var10000 = 9;
         break;
      case 58:
      case 59:
      case 60:
      case 61:
      case 62:
      case 63:
      case 64:
      default:
         throw new IllegalArgumentException("Invalid entry at line " + i + ": expected hex digit, got " + (char)b);
      case 65:
         var10000 = 10;
         break;
      case 66:
         var10000 = 11;
         break;
      case 67:
         var10000 = 12;
         break;
      case 68:
         var10000 = 13;
         break;
      case 69:
         var10000 = 14;
         break;
      case 70:
         var10000 = 15;
      }

      return var10000;
   }

   private static boolean copyUntil(InputStream inputStream, ByteList byteList, int i) throws IOException {
      while(true) {
         int j = inputStream.read();
         if (j == -1) {
            return false;
         }

         if (j == i) {
            return true;
         }

         byteList.add((byte)j);
      }
   }

   @Environment(EnvType.CLIENT)
   public interface LineData {
      int line(int i);

      int bitWidth();

      default int mask() {
         int i = 0;

         for(int j = 0; j < 16; ++j) {
            i |= this.line(j);
         }

         return i;
      }

      default int calculateWidth() {
         int i = this.mask();
         int j = this.bitWidth();
         int k;
         int l;
         if (i == 0) {
            k = 0;
            l = j;
         } else {
            k = Integer.numberOfLeadingZeros(i);
            l = 32 - Integer.numberOfTrailingZeros(i) - 1;
         }

         return UnihexProvider.Dimensions.pack(k, l);
      }
   }

   @Environment(EnvType.CLIENT)
   private static record ByteContents(byte[] contents) implements UnihexProvider.LineData {
      private ByteContents(byte[] bs) {
         this.contents = bs;
      }

      public int line(int i) {
         return this.contents[i] << 24;
      }

      static UnihexProvider.LineData read(int i, ByteList byteList) {
         byte[] bs = new byte[16];
         int j = 0;

         for(int k = 0; k < 16; ++k) {
            int l = UnihexProvider.decodeHex(i, byteList, j++);
            int m = UnihexProvider.decodeHex(i, byteList, j++);
            byte b = (byte)(l << 4 | m);
            bs[k] = b;
         }

         return new UnihexProvider.ByteContents(bs);
      }

      public int bitWidth() {
         return 8;
      }

      public byte[] contents() {
         return this.contents;
      }
   }

   @Environment(EnvType.CLIENT)
   private static record ShortContents(short[] contents) implements UnihexProvider.LineData {
      private ShortContents(short[] ss) {
         this.contents = ss;
      }

      public int line(int i) {
         return this.contents[i] << 16;
      }

      static UnihexProvider.LineData read(int i, ByteList byteList) {
         short[] ss = new short[16];
         int j = 0;

         for(int k = 0; k < 16; ++k) {
            int l = UnihexProvider.decodeHex(i, byteList, j++);
            int m = UnihexProvider.decodeHex(i, byteList, j++);
            int n = UnihexProvider.decodeHex(i, byteList, j++);
            int o = UnihexProvider.decodeHex(i, byteList, j++);
            short s = (short)(l << 12 | m << 8 | n << 4 | o);
            ss[k] = s;
         }

         return new UnihexProvider.ShortContents(ss);
      }

      public int bitWidth() {
         return 16;
      }

      public short[] contents() {
         return this.contents;
      }
   }

   @Environment(EnvType.CLIENT)
   private static record IntContents(int[] contents, int bitWidth) implements UnihexProvider.LineData {
      private static final int SIZE_24 = 24;

      private IntContents(int[] is, int i) {
         this.contents = is;
         this.bitWidth = i;
      }

      public int line(int i) {
         return this.contents[i];
      }

      static UnihexProvider.LineData read24(int i, ByteList byteList) {
         int[] is = new int[16];
         int j = 0;
         int k = 0;

         for(int l = 0; l < 16; ++l) {
            int m = UnihexProvider.decodeHex(i, byteList, k++);
            int n = UnihexProvider.decodeHex(i, byteList, k++);
            int o = UnihexProvider.decodeHex(i, byteList, k++);
            int p = UnihexProvider.decodeHex(i, byteList, k++);
            int q = UnihexProvider.decodeHex(i, byteList, k++);
            int r = UnihexProvider.decodeHex(i, byteList, k++);
            int s = m << 20 | n << 16 | o << 12 | p << 8 | q << 4 | r;
            is[l] = s << 8;
            j |= s;
         }

         return new UnihexProvider.IntContents(is, 24);
      }

      public static UnihexProvider.LineData read32(int i, ByteList byteList) {
         int[] is = new int[16];
         int j = 0;
         int k = 0;

         for(int l = 0; l < 16; ++l) {
            int m = UnihexProvider.decodeHex(i, byteList, k++);
            int n = UnihexProvider.decodeHex(i, byteList, k++);
            int o = UnihexProvider.decodeHex(i, byteList, k++);
            int p = UnihexProvider.decodeHex(i, byteList, k++);
            int q = UnihexProvider.decodeHex(i, byteList, k++);
            int r = UnihexProvider.decodeHex(i, byteList, k++);
            int s = UnihexProvider.decodeHex(i, byteList, k++);
            int t = UnihexProvider.decodeHex(i, byteList, k++);
            int u = m << 28 | n << 24 | o << 20 | p << 16 | q << 12 | r << 8 | s << 4 | t;
            is[l] = u;
            j |= u;
         }

         return new UnihexProvider.IntContents(is, 32);
      }

      public int[] contents() {
         return this.contents;
      }

      public int bitWidth() {
         return this.bitWidth;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface ReaderOutput {
      void accept(int i, UnihexProvider.LineData lineData);
   }

   @Environment(EnvType.CLIENT)
   private static record Glyph(UnihexProvider.LineData contents, int left, int right) implements GlyphInfo {
      final UnihexProvider.LineData contents;
      final int left;
      final int right;

      Glyph(UnihexProvider.LineData lineData, int i, int j) {
         this.contents = lineData;
         this.left = i;
         this.right = j;
      }

      public int width() {
         return this.right - this.left + 1;
      }

      public float getAdvance() {
         return (float)(this.width() / 2 + 1);
      }

      public float getShadowOffset() {
         return 0.5F;
      }

      public float getBoldOffset() {
         return 0.5F;
      }

      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
         return (BakedGlyph)function.apply(new SheetGlyphInfo() {
            // $FF: synthetic field
            final UnihexProvider.Glyph field_37906;

            {
               this.field_37906 = glyph;
            }

            public float getOversample() {
               return 2.0F;
            }

            public int getPixelWidth() {
               return this.field_37906.width();
            }

            public int getPixelHeight() {
               return 16;
            }

            public void upload(int i, int j) {
               IntBuffer intBuffer = MemoryUtil.memAllocInt(this.field_37906.width() * 16);
               UnihexProvider.unpackBitsToBytes(intBuffer, this.field_37906.contents, this.field_37906.left, this.field_37906.right);
               intBuffer.rewind();
               GlStateManager.upload(0, i, j, this.field_37906.width(), 16, NativeImage.Format.RGBA, intBuffer, MemoryUtil::memFree);
            }

            public boolean isColored() {
               return true;
            }
         });
      }

      public UnihexProvider.LineData contents() {
         return this.contents;
      }

      public int left() {
         return this.left;
      }

      public int right() {
         return this.right;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Definition implements GlyphProviderDefinition {
      public static final MapCodec<UnihexProvider.Definition> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(ResourceLocation.CODEC.fieldOf("hex_file").forGetter((definition) -> {
            return definition.hexFile;
         }), UnihexProvider.OverrideRange.CODEC.listOf().fieldOf("size_overrides").forGetter((definition) -> {
            return definition.sizeOverrides;
         })).apply(instance, UnihexProvider.Definition::new);
      });
      private final ResourceLocation hexFile;
      private final List<UnihexProvider.OverrideRange> sizeOverrides;

      private Definition(ResourceLocation resourceLocation, List<UnihexProvider.OverrideRange> list) {
         this.hexFile = resourceLocation;
         this.sizeOverrides = list;
      }

      public GlyphProviderType type() {
         return GlyphProviderType.UNIHEX;
      }

      public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
         return Either.left(this::load);
      }

      private GlyphProvider load(ResourceManager resourceManager) throws IOException {
         InputStream inputStream = resourceManager.open(this.hexFile);

         UnihexProvider var3;
         try {
            var3 = this.loadData(inputStream);
         } catch (Throwable var6) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         return var3;
      }

      private UnihexProvider loadData(InputStream inputStream) throws IOException {
         CodepointMap<UnihexProvider.LineData> codepointMap = new CodepointMap((ix) -> {
            return new UnihexProvider.LineData[ix];
         }, (ix) -> {
            return new UnihexProvider.LineData[ix][];
         });
         Objects.requireNonNull(codepointMap);
         UnihexProvider.ReaderOutput readerOutput = codepointMap::put;
         ZipInputStream zipInputStream = new ZipInputStream(inputStream);

         UnihexProvider var17;
         try {
            ZipEntry zipEntry;
            while((zipEntry = zipInputStream.getNextEntry()) != null) {
               String string = zipEntry.getName();
               if (string.endsWith(".hex")) {
                  UnihexProvider.LOGGER.info("Found {}, loading", string);
                  UnihexProvider.readFromStream(new FastBufferedInputStream(zipInputStream), readerOutput);
               }
            }

            CodepointMap<UnihexProvider.Glyph> codepointMap2 = new CodepointMap((ix) -> {
               return new UnihexProvider.Glyph[ix];
            }, (ix) -> {
               return new UnihexProvider.Glyph[ix][];
            });
            Iterator var7 = this.sizeOverrides.iterator();

            label40:
            while(true) {
               if (var7.hasNext()) {
                  UnihexProvider.OverrideRange overrideRange = (UnihexProvider.OverrideRange)var7.next();
                  int i = overrideRange.from;
                  int j = overrideRange.to;
                  UnihexProvider.Dimensions dimensions = overrideRange.dimensions;
                  int k = i;

                  while(true) {
                     if (k > j) {
                        continue label40;
                     }

                     UnihexProvider.LineData lineData = (UnihexProvider.LineData)codepointMap.remove(k);
                     if (lineData != null) {
                        codepointMap2.put(k, new UnihexProvider.Glyph(lineData, dimensions.left, dimensions.right));
                     }

                     ++k;
                  }
               }

               codepointMap.forEach((ix, lineDatax) -> {
                  int j = lineDatax.calculateWidth();
                  int k = UnihexProvider.Dimensions.left(j);
                  int l = UnihexProvider.Dimensions.right(j);
                  codepointMap2.put(ix, new UnihexProvider.Glyph(lineDatax, k, l));
               });
               var17 = new UnihexProvider(codepointMap2);
               break;
            }
         } catch (Throwable var15) {
            try {
               zipInputStream.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }

            throw var15;
         }

         zipInputStream.close();
         return var17;
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Dimensions(int left, int right) {
      final int left;
      final int right;
      public static final MapCodec<UnihexProvider.Dimensions> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(Codec.INT.fieldOf("left").forGetter(UnihexProvider.Dimensions::left), Codec.INT.fieldOf("right").forGetter(UnihexProvider.Dimensions::right)).apply(instance, UnihexProvider.Dimensions::new);
      });
      public static final Codec<UnihexProvider.Dimensions> CODEC;

      public Dimensions(int i, int j) {
         this.left = i;
         this.right = j;
      }

      public int pack() {
         return pack(this.left, this.right);
      }

      public static int pack(int i, int j) {
         return (i & 255) << 8 | j & 255;
      }

      public static int left(int i) {
         return (byte)(i >> 8);
      }

      public static int right(int i) {
         return (byte)i;
      }

      public int left() {
         return this.left;
      }

      public int right() {
         return this.right;
      }

      static {
         CODEC = MAP_CODEC.codec();
      }
   }

   @Environment(EnvType.CLIENT)
   private static record OverrideRange(int from, int to, UnihexProvider.Dimensions dimensions) {
      final int from;
      final int to;
      final UnihexProvider.Dimensions dimensions;
      private static final Codec<UnihexProvider.OverrideRange> RAW_CODEC = RecordCodecBuilder.create((instance) -> {
         return instance.group(ExtraCodecs.CODEPOINT.fieldOf("from").forGetter(UnihexProvider.OverrideRange::from), ExtraCodecs.CODEPOINT.fieldOf("to").forGetter(UnihexProvider.OverrideRange::to), UnihexProvider.Dimensions.MAP_CODEC.forGetter(UnihexProvider.OverrideRange::dimensions)).apply(instance, UnihexProvider.OverrideRange::new);
      });
      public static final Codec<UnihexProvider.OverrideRange> CODEC;

      private OverrideRange(int i, int j, UnihexProvider.Dimensions dimensions) {
         this.from = i;
         this.to = j;
         this.dimensions = dimensions;
      }

      public int from() {
         return this.from;
      }

      public int to() {
         return this.to;
      }

      public UnihexProvider.Dimensions dimensions() {
         return this.dimensions;
      }

      static {
         CODEC = RAW_CODEC.validate((overrideRange) -> {
            return overrideRange.from >= overrideRange.to ? DataResult.error(() -> {
               return "Invalid range: [" + overrideRange.from + ";" + overrideRange.to + "]";
            }) : DataResult.success(overrideRange);
         });
      }
   }
}
