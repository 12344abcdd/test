package net.minecraft.client.gui.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public record GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset) {
   public GlyphRenderTypes(RenderType renderType, RenderType renderType2, RenderType renderType3) {
      this.normal = renderType;
      this.seeThrough = renderType2;
      this.polygonOffset = renderType3;
   }

   public static GlyphRenderTypes createForIntensityTexture(ResourceLocation resourceLocation) {
      return new GlyphRenderTypes(RenderType.textIntensity(resourceLocation), RenderType.textIntensitySeeThrough(resourceLocation), RenderType.textIntensityPolygonOffset(resourceLocation));
   }

   public static GlyphRenderTypes createForColorTexture(ResourceLocation resourceLocation) {
      return new GlyphRenderTypes(RenderType.text(resourceLocation), RenderType.textSeeThrough(resourceLocation), RenderType.textPolygonOffset(resourceLocation));
   }

   public RenderType select(Font.DisplayMode displayMode) {
      RenderType var10000;
      switch(displayMode) {
      case NORMAL:
         var10000 = this.normal;
         break;
      case SEE_THROUGH:
         var10000 = this.seeThrough;
         break;
      case POLYGON_OFFSET:
         var10000 = this.polygonOffset;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public RenderType normal() {
      return this.normal;
   }

   public RenderType seeThrough() {
      return this.seeThrough;
   }

   public RenderType polygonOffset() {
      return this.polygonOffset;
   }
}
