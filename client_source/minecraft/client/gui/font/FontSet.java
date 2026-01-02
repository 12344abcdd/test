package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

@Environment(EnvType.CLIENT)
public class FontSet implements AutoCloseable {
   private static final RandomSource RANDOM = RandomSource.create();
   private static final float LARGE_FORWARD_ADVANCE = 32.0F;
   private final TextureManager textureManager;
   private final ResourceLocation name;
   private BakedGlyph missingGlyph;
   private BakedGlyph whiteGlyph;
   private List<GlyphProvider.Conditional> allProviders = List.of();
   private List<GlyphProvider> activeProviders = List.of();
   private final CodepointMap<BakedGlyph> glyphs = new CodepointMap((i) -> {
      return new BakedGlyph[i];
   }, (i) -> {
      return new BakedGlyph[i][];
   });
   private final CodepointMap<FontSet.GlyphInfoFilter> glyphInfos = new CodepointMap((i) -> {
      return new FontSet.GlyphInfoFilter[i];
   }, (i) -> {
      return new FontSet.GlyphInfoFilter[i][];
   });
   private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap();
   private final List<FontTexture> textures = Lists.newArrayList();

   public FontSet(TextureManager textureManager, ResourceLocation resourceLocation) {
      this.textureManager = textureManager;
      this.name = resourceLocation;
   }

   public void reload(List<GlyphProvider.Conditional> list, Set<FontOption> set) {
      this.allProviders = list;
      this.reload(set);
   }

   public void reload(Set<FontOption> set) {
      this.activeProviders = List.of();
      this.resetTextures();
      this.activeProviders = this.selectProviders(this.allProviders, set);
   }

   private void resetTextures() {
      this.closeTextures();
      this.glyphs.clear();
      this.glyphInfos.clear();
      this.glyphsByWidth.clear();
      this.missingGlyph = SpecialGlyphs.MISSING.bake(this::stitch);
      this.whiteGlyph = SpecialGlyphs.WHITE.bake(this::stitch);
   }

   private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> list, Set<FontOption> set) {
      IntSet intSet = new IntOpenHashSet();
      List<GlyphProvider> list2 = new ArrayList();
      Iterator var5 = list.iterator();

      while(var5.hasNext()) {
         GlyphProvider.Conditional conditional = (GlyphProvider.Conditional)var5.next();
         if (conditional.filter().apply(set)) {
            list2.add(conditional.provider());
            intSet.addAll(conditional.provider().getSupportedGlyphs());
         }
      }

      Set<GlyphProvider> set2 = Sets.newHashSet();
      intSet.forEach((i) -> {
         Iterator var4 = list2.iterator();

         while(var4.hasNext()) {
            GlyphProvider glyphProvider = (GlyphProvider)var4.next();
            GlyphInfo glyphInfo = glyphProvider.getGlyph(i);
            if (glyphInfo != null) {
               set2.add(glyphProvider);
               if (glyphInfo != SpecialGlyphs.MISSING) {
                  ((IntList)this.glyphsByWidth.computeIfAbsent(Mth.ceil(glyphInfo.getAdvance(false)), (ix) -> {
                     return new IntArrayList();
                  })).add(i);
               }
               break;
            }
         }

      });
      Stream var10000 = list2.stream();
      Objects.requireNonNull(set2);
      return var10000.filter(set2::contains).toList();
   }

   public void close() {
      this.closeTextures();
   }

   private void closeTextures() {
      Iterator var1 = this.textures.iterator();

      while(var1.hasNext()) {
         FontTexture fontTexture = (FontTexture)var1.next();
         fontTexture.close();
      }

      this.textures.clear();
   }

   private static boolean hasFishyAdvance(GlyphInfo glyphInfo) {
      float f = glyphInfo.getAdvance(false);
      if (!(f < 0.0F) && !(f > 32.0F)) {
         float g = glyphInfo.getAdvance(true);
         return g < 0.0F || g > 32.0F;
      } else {
         return true;
      }
   }

   private FontSet.GlyphInfoFilter computeGlyphInfo(int i) {
      GlyphInfo glyphInfo = null;
      Iterator var3 = this.activeProviders.iterator();

      while(var3.hasNext()) {
         GlyphProvider glyphProvider = (GlyphProvider)var3.next();
         GlyphInfo glyphInfo2 = glyphProvider.getGlyph(i);
         if (glyphInfo2 != null) {
            if (glyphInfo == null) {
               glyphInfo = glyphInfo2;
            }

            if (!hasFishyAdvance(glyphInfo2)) {
               return new FontSet.GlyphInfoFilter(glyphInfo, glyphInfo2);
            }
         }
      }

      if (glyphInfo != null) {
         return new FontSet.GlyphInfoFilter(glyphInfo, SpecialGlyphs.MISSING);
      } else {
         return FontSet.GlyphInfoFilter.MISSING;
      }
   }

   public GlyphInfo getGlyphInfo(int i, boolean bl) {
      return ((FontSet.GlyphInfoFilter)this.glyphInfos.computeIfAbsent(i, this::computeGlyphInfo)).select(bl);
   }

   private BakedGlyph computeBakedGlyph(int i) {
      Iterator var2 = this.activeProviders.iterator();

      GlyphInfo glyphInfo;
      do {
         if (!var2.hasNext()) {
            return this.missingGlyph;
         }

         GlyphProvider glyphProvider = (GlyphProvider)var2.next();
         glyphInfo = glyphProvider.getGlyph(i);
      } while(glyphInfo == null);

      return glyphInfo.bake(this::stitch);
   }

   public BakedGlyph getGlyph(int i) {
      return (BakedGlyph)this.glyphs.computeIfAbsent(i, this::computeBakedGlyph);
   }

   private BakedGlyph stitch(SheetGlyphInfo sheetGlyphInfo) {
      Iterator var2 = this.textures.iterator();

      BakedGlyph bakedGlyph;
      do {
         if (!var2.hasNext()) {
            ResourceLocation resourceLocation = this.name.withSuffix("/" + this.textures.size());
            boolean bl = sheetGlyphInfo.isColored();
            GlyphRenderTypes glyphRenderTypes = bl ? GlyphRenderTypes.createForColorTexture(resourceLocation) : GlyphRenderTypes.createForIntensityTexture(resourceLocation);
            FontTexture fontTexture2 = new FontTexture(glyphRenderTypes, bl);
            this.textures.add(fontTexture2);
            this.textureManager.register((ResourceLocation)resourceLocation, (AbstractTexture)fontTexture2);
            BakedGlyph bakedGlyph2 = fontTexture2.add(sheetGlyphInfo);
            return bakedGlyph2 == null ? this.missingGlyph : bakedGlyph2;
         }

         FontTexture fontTexture = (FontTexture)var2.next();
         bakedGlyph = fontTexture.add(sheetGlyphInfo);
      } while(bakedGlyph == null);

      return bakedGlyph;
   }

   public BakedGlyph getRandomGlyph(GlyphInfo glyphInfo) {
      IntList intList = (IntList)this.glyphsByWidth.get(Mth.ceil(glyphInfo.getAdvance(false)));
      return intList != null && !intList.isEmpty() ? this.getGlyph(intList.getInt(RANDOM.nextInt(intList.size()))) : this.missingGlyph;
   }

   public ResourceLocation name() {
      return this.name;
   }

   public BakedGlyph whiteGlyph() {
      return this.whiteGlyph;
   }

   @Environment(EnvType.CLIENT)
   private static record GlyphInfoFilter(GlyphInfo glyphInfo, GlyphInfo glyphInfoNotFishy) {
      static final FontSet.GlyphInfoFilter MISSING;

      GlyphInfoFilter(GlyphInfo glyphInfo, GlyphInfo glyphInfo2) {
         this.glyphInfo = glyphInfo;
         this.glyphInfoNotFishy = glyphInfo2;
      }

      GlyphInfo select(boolean bl) {
         return bl ? this.glyphInfoNotFishy : this.glyphInfo;
      }

      public GlyphInfo glyphInfo() {
         return this.glyphInfo;
      }

      public GlyphInfo glyphInfoNotFishy() {
         return this.glyphInfoNotFishy;
      }

      static {
         MISSING = new FontSet.GlyphInfoFilter(SpecialGlyphs.MISSING, SpecialGlyphs.MISSING);
      }
   }
}
