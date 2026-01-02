package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@Environment(EnvType.CLIENT)
public abstract class RenderStateShard {
   private static final float VIEW_SCALE_Z_EPSILON = 0.99975586F;
   public static final double MAX_ENCHANTMENT_GLINT_SPEED_MILLIS = 8.0D;
   protected final String name;
   private final Runnable setupState;
   private final Runnable clearState;
   public static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("no_transparency", () -> {
      RenderSystem.disableBlend();
   }, () -> {
   });
   public static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("additive_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   public static final RenderStateShard.TransparencyStateShard LIGHTNING_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("lightning_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   public static final RenderStateShard.TransparencyStateShard GLINT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("glint_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   public static final RenderStateShard.TransparencyStateShard CRUMBLING_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("crumbling_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   public static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   public static final RenderStateShard.ShaderStateShard NO_SHADER = new RenderStateShard.ShaderStateShard();
   public static final RenderStateShard.ShaderStateShard POSITION_COLOR_LIGHTMAP_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader);
   public static final RenderStateShard.ShaderStateShard POSITION_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionShader);
   public static final RenderStateShard.ShaderStateShard POSITION_TEX_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexShader);
   public static final RenderStateShard.ShaderStateShard POSITION_COLOR_TEX_LIGHTMAP_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader);
   public static final RenderStateShard.ShaderStateShard POSITION_COLOR_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_SOLID_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeSolidShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_MIPPED_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCutoutMippedShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCutoutShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTranslucentShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTranslucentMovingBlockShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorCutoutNoCullShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SOLID_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntitySolidShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutNoCullShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutNoCullZOffsetShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeItemEntityTranslucentCullShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentCullShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntitySmoothCutoutShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_BEACON_BEAM_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeBeaconBeamShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_DECAL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityDecalShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_NO_OUTLINE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityNoOutlineShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SHADOW_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityShadowShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_ALPHA_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityAlphaShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_EYES_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEyesShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENERGY_SWIRL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEnergySwirlShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_LEASH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLeashShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_WATER_MASK_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeWaterMaskShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_OUTLINE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeOutlineShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorEntityGlintShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGlintTranslucentShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGlintShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityGlintShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityGlintDirectShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_CRUMBLING_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCrumblingShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextBackgroundShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextIntensityShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextSeeThroughShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextBackgroundSeeThroughShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextIntensitySeeThroughShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_LIGHTNING_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLightningShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_TRIPWIRE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTripwireShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_END_PORTAL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEndPortalShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_END_GATEWAY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEndGatewayShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_CLOUDS_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCloudsShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_LINES_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_OVERLAY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiOverlayShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiTextHighlightShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiGhostRecipeOverlayShader);
   public static final RenderStateShard.ShaderStateShard RENDERTYPE_BREEZE_WIND_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeBreezeWindShader);
   public static final RenderStateShard.TextureStateShard BLOCK_SHEET_MIPPED;
   public static final RenderStateShard.TextureStateShard BLOCK_SHEET;
   public static final RenderStateShard.EmptyTextureStateShard NO_TEXTURE;
   public static final RenderStateShard.TexturingStateShard DEFAULT_TEXTURING;
   public static final RenderStateShard.TexturingStateShard GLINT_TEXTURING;
   public static final RenderStateShard.TexturingStateShard ENTITY_GLINT_TEXTURING;
   public static final RenderStateShard.LightmapStateShard LIGHTMAP;
   public static final RenderStateShard.LightmapStateShard NO_LIGHTMAP;
   public static final RenderStateShard.OverlayStateShard OVERLAY;
   public static final RenderStateShard.OverlayStateShard NO_OVERLAY;
   public static final RenderStateShard.CullStateShard CULL;
   public static final RenderStateShard.CullStateShard NO_CULL;
   public static final RenderStateShard.DepthTestStateShard NO_DEPTH_TEST;
   public static final RenderStateShard.DepthTestStateShard EQUAL_DEPTH_TEST;
   public static final RenderStateShard.DepthTestStateShard LEQUAL_DEPTH_TEST;
   public static final RenderStateShard.DepthTestStateShard GREATER_DEPTH_TEST;
   public static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE;
   public static final RenderStateShard.WriteMaskStateShard COLOR_WRITE;
   public static final RenderStateShard.WriteMaskStateShard DEPTH_WRITE;
   public static final RenderStateShard.LayeringStateShard NO_LAYERING;
   public static final RenderStateShard.LayeringStateShard POLYGON_OFFSET_LAYERING;
   public static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING;
   public static final RenderStateShard.OutputStateShard MAIN_TARGET;
   public static final RenderStateShard.OutputStateShard OUTLINE_TARGET;
   public static final RenderStateShard.OutputStateShard TRANSLUCENT_TARGET;
   public static final RenderStateShard.OutputStateShard PARTICLES_TARGET;
   public static final RenderStateShard.OutputStateShard WEATHER_TARGET;
   public static final RenderStateShard.OutputStateShard CLOUDS_TARGET;
   public static final RenderStateShard.OutputStateShard ITEM_ENTITY_TARGET;
   public static final RenderStateShard.LineStateShard DEFAULT_LINE;
   public static final RenderStateShard.ColorLogicStateShard NO_COLOR_LOGIC;
   public static final RenderStateShard.ColorLogicStateShard OR_REVERSE_COLOR_LOGIC;

   public RenderStateShard(String string, Runnable runnable, Runnable runnable2) {
      this.name = string;
      this.setupState = runnable;
      this.clearState = runnable2;
   }

   public void setupRenderState() {
      this.setupState.run();
   }

   public void clearRenderState() {
      this.clearState.run();
   }

   public String toString() {
      return this.name;
   }

   private static void setupGlintTexturing(float f) {
      long l = (long)((double)Util.getMillis() * (Double)Minecraft.getInstance().options.glintSpeed().get() * 8.0D);
      float g = (float)(l % 110000L) / 110000.0F;
      float h = (float)(l % 30000L) / 30000.0F;
      Matrix4f matrix4f = (new Matrix4f()).translation(-g, h, 0.0F);
      matrix4f.rotateZ(0.17453292F).scale(f);
      RenderSystem.setTextureMatrix(matrix4f);
   }

   static {
      BLOCK_SHEET_MIPPED = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, true);
      BLOCK_SHEET = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false);
      NO_TEXTURE = new RenderStateShard.EmptyTextureStateShard();
      DEFAULT_TEXTURING = new RenderStateShard.TexturingStateShard("default_texturing", () -> {
      }, () -> {
      });
      GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("glint_texturing", () -> {
         setupGlintTexturing(8.0F);
      }, () -> {
         RenderSystem.resetTextureMatrix();
      });
      ENTITY_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
         setupGlintTexturing(0.16F);
      }, () -> {
         RenderSystem.resetTextureMatrix();
      });
      LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
      NO_LIGHTMAP = new RenderStateShard.LightmapStateShard(false);
      OVERLAY = new RenderStateShard.OverlayStateShard(true);
      NO_OVERLAY = new RenderStateShard.OverlayStateShard(false);
      CULL = new RenderStateShard.CullStateShard(true);
      NO_CULL = new RenderStateShard.CullStateShard(false);
      NO_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("always", 519);
      EQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("==", 514);
      LEQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("<=", 515);
      GREATER_DEPTH_TEST = new RenderStateShard.DepthTestStateShard(">", 516);
      COLOR_DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(true, true);
      COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);
      DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(false, true);
      NO_LAYERING = new RenderStateShard.LayeringStateShard("no_layering", () -> {
      }, () -> {
      });
      POLYGON_OFFSET_LAYERING = new RenderStateShard.LayeringStateShard("polygon_offset_layering", () -> {
         RenderSystem.polygonOffset(-1.0F, -10.0F);
         RenderSystem.enablePolygonOffset();
      }, () -> {
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
      });
      VIEW_OFFSET_Z_LAYERING = new RenderStateShard.LayeringStateShard("view_offset_z_layering", () -> {
         Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
         matrix4fStack.pushMatrix();
         matrix4fStack.scale(0.99975586F, 0.99975586F, 0.99975586F);
         RenderSystem.applyModelViewMatrix();
      }, () -> {
         Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
         matrix4fStack.popMatrix();
         RenderSystem.applyModelViewMatrix();
      });
      MAIN_TARGET = new RenderStateShard.OutputStateShard("main_target", () -> {
      }, () -> {
      });
      OUTLINE_TARGET = new RenderStateShard.OutputStateShard("outline_target", () -> {
         Minecraft.getInstance().levelRenderer.entityTarget().bindWrite(false);
      }, () -> {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      });
      TRANSLUCENT_TARGET = new RenderStateShard.OutputStateShard("translucent_target", () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().levelRenderer.getTranslucentTarget().bindWrite(false);
         }

      }, () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
         }

      });
      PARTICLES_TARGET = new RenderStateShard.OutputStateShard("particles_target", () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
         }

      }, () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
         }

      });
      WEATHER_TARGET = new RenderStateShard.OutputStateShard("weather_target", () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().levelRenderer.getWeatherTarget().bindWrite(false);
         }

      }, () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
         }

      });
      CLOUDS_TARGET = new RenderStateShard.OutputStateShard("clouds_target", () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().levelRenderer.getCloudsTarget().bindWrite(false);
         }

      }, () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
         }

      });
      ITEM_ENTITY_TARGET = new RenderStateShard.OutputStateShard("item_entity_target", () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().levelRenderer.getItemEntityTarget().bindWrite(false);
         }

      }, () -> {
         if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
         }

      });
      DEFAULT_LINE = new RenderStateShard.LineStateShard(OptionalDouble.of(1.0D));
      NO_COLOR_LOGIC = new RenderStateShard.ColorLogicStateShard("no_color_logic", () -> {
         RenderSystem.disableColorLogicOp();
      }, () -> {
      });
      OR_REVERSE_COLOR_LOGIC = new RenderStateShard.ColorLogicStateShard("or_reverse", () -> {
         RenderSystem.enableColorLogicOp();
         RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
      }, () -> {
         RenderSystem.disableColorLogicOp();
      });
   }

   @Environment(EnvType.CLIENT)
   public static class TransparencyStateShard extends RenderStateShard {
      public TransparencyStateShard(String string, Runnable runnable, Runnable runnable2) {
         super(string, runnable, runnable2);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ShaderStateShard extends RenderStateShard {
      private final Optional<Supplier<ShaderInstance>> shader;

      public ShaderStateShard(Supplier<ShaderInstance> supplier) {
         super("shader", () -> {
            RenderSystem.setShader(supplier);
         }, () -> {
         });
         this.shader = Optional.of(supplier);
      }

      public ShaderStateShard() {
         super("shader", () -> {
            RenderSystem.setShader(() -> {
               return null;
            });
         }, () -> {
         });
         this.shader = Optional.empty();
      }

      public String toString() {
         String var10000 = this.name;
         return var10000 + "[" + String.valueOf(this.shader) + "]";
      }
   }

   @Environment(EnvType.CLIENT)
   public static class TextureStateShard extends RenderStateShard.EmptyTextureStateShard {
      private final Optional<ResourceLocation> texture;
      private final boolean blur;
      private final boolean mipmap;

      public TextureStateShard(ResourceLocation resourceLocation, boolean bl, boolean bl2) {
         super(() -> {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.getTexture(resourceLocation).setFilter(bl, bl2);
            RenderSystem.setShaderTexture(0, resourceLocation);
         }, () -> {
         });
         this.texture = Optional.of(resourceLocation);
         this.blur = bl;
         this.mipmap = bl2;
      }

      public String toString() {
         String var10000 = this.name;
         return var10000 + "[" + String.valueOf(this.texture) + "(blur=" + this.blur + ", mipmap=" + this.mipmap + ")]";
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return this.texture;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class EmptyTextureStateShard extends RenderStateShard {
      public EmptyTextureStateShard(Runnable runnable, Runnable runnable2) {
         super("texture", runnable, runnable2);
      }

      EmptyTextureStateShard() {
         super("texture", () -> {
         }, () -> {
         });
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return Optional.empty();
      }
   }

   @Environment(EnvType.CLIENT)
   public static class TexturingStateShard extends RenderStateShard {
      public TexturingStateShard(String string, Runnable runnable, Runnable runnable2) {
         super(string, runnable, runnable2);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class LightmapStateShard extends RenderStateShard.BooleanStateShard {
      public LightmapStateShard(boolean bl) {
         super("lightmap", () -> {
            if (bl) {
               Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            }

         }, () -> {
            if (bl) {
               Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
            }

         }, bl);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class OverlayStateShard extends RenderStateShard.BooleanStateShard {
      public OverlayStateShard(boolean bl) {
         super("overlay", () -> {
            if (bl) {
               Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            }

         }, () -> {
            if (bl) {
               Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor();
            }

         }, bl);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class CullStateShard extends RenderStateShard.BooleanStateShard {
      public CullStateShard(boolean bl) {
         super("cull", () -> {
            if (!bl) {
               RenderSystem.disableCull();
            }

         }, () -> {
            if (!bl) {
               RenderSystem.enableCull();
            }

         }, bl);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class DepthTestStateShard extends RenderStateShard {
      private final String functionName;

      public DepthTestStateShard(String string, int i) {
         super("depth_test", () -> {
            if (i != 519) {
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(i);
            }

         }, () -> {
            if (i != 519) {
               RenderSystem.disableDepthTest();
               RenderSystem.depthFunc(515);
            }

         });
         this.functionName = string;
      }

      public String toString() {
         return this.name + "[" + this.functionName + "]";
      }
   }

   @Environment(EnvType.CLIENT)
   public static class WriteMaskStateShard extends RenderStateShard {
      private final boolean writeColor;
      private final boolean writeDepth;

      public WriteMaskStateShard(boolean bl, boolean bl2) {
         super("write_mask_state", () -> {
            if (!bl2) {
               RenderSystem.depthMask(bl2);
            }

            if (!bl) {
               RenderSystem.colorMask(bl, bl, bl, bl);
            }

         }, () -> {
            if (!bl2) {
               RenderSystem.depthMask(true);
            }

            if (!bl) {
               RenderSystem.colorMask(true, true, true, true);
            }

         });
         this.writeColor = bl;
         this.writeDepth = bl2;
      }

      public String toString() {
         return this.name + "[writeColor=" + this.writeColor + ", writeDepth=" + this.writeDepth + "]";
      }
   }

   @Environment(EnvType.CLIENT)
   public static class LayeringStateShard extends RenderStateShard {
      public LayeringStateShard(String string, Runnable runnable, Runnable runnable2) {
         super(string, runnable, runnable2);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class OutputStateShard extends RenderStateShard {
      public OutputStateShard(String string, Runnable runnable, Runnable runnable2) {
         super(string, runnable, runnable2);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class LineStateShard extends RenderStateShard {
      private final OptionalDouble width;

      public LineStateShard(OptionalDouble optionalDouble) {
         super("line_width", () -> {
            if (!Objects.equals(optionalDouble, OptionalDouble.of(1.0D))) {
               if (optionalDouble.isPresent()) {
                  RenderSystem.lineWidth((float)optionalDouble.getAsDouble());
               } else {
                  RenderSystem.lineWidth(Math.max(2.5F, (float)Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F));
               }
            }

         }, () -> {
            if (!Objects.equals(optionalDouble, OptionalDouble.of(1.0D))) {
               RenderSystem.lineWidth(1.0F);
            }

         });
         this.width = optionalDouble;
      }

      public String toString() {
         String var10000 = this.name;
         return var10000 + "[" + String.valueOf(this.width.isPresent() ? this.width.getAsDouble() : "window_scale") + "]";
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ColorLogicStateShard extends RenderStateShard {
      public ColorLogicStateShard(String string, Runnable runnable, Runnable runnable2) {
         super(string, runnable, runnable2);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class BooleanStateShard extends RenderStateShard {
      private final boolean enabled;

      public BooleanStateShard(String string, Runnable runnable, Runnable runnable2, boolean bl) {
         super(string, runnable, runnable2);
         this.enabled = bl;
      }

      public String toString() {
         return this.name + "[" + this.enabled + "]";
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class OffsetTexturingStateShard extends RenderStateShard.TexturingStateShard {
      public OffsetTexturingStateShard(float f, float g) {
         super("offset_texturing", () -> {
            RenderSystem.setTextureMatrix((new Matrix4f()).translation(f, g, 0.0F));
         }, () -> {
            RenderSystem.resetTextureMatrix();
         });
      }
   }

   @Environment(EnvType.CLIENT)
   protected static class MultiTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
      private final Optional<ResourceLocation> cutoutTexture;

      MultiTextureStateShard(ImmutableList<Triple<ResourceLocation, Boolean, Boolean>> immutableList) {
         super(() -> {
            int i = 0;
            UnmodifiableIterator var2 = immutableList.iterator();

            while(var2.hasNext()) {
               Triple<ResourceLocation, Boolean, Boolean> triple = (Triple)var2.next();
               TextureManager textureManager = Minecraft.getInstance().getTextureManager();
               textureManager.getTexture((ResourceLocation)triple.getLeft()).setFilter((Boolean)triple.getMiddle(), (Boolean)triple.getRight());
               RenderSystem.setShaderTexture(i++, (ResourceLocation)triple.getLeft());
            }

         }, () -> {
         });
         this.cutoutTexture = immutableList.stream().findFirst().map(Triple::getLeft);
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return this.cutoutTexture;
      }

      public static RenderStateShard.MultiTextureStateShard.Builder builder() {
         return new RenderStateShard.MultiTextureStateShard.Builder();
      }

      @Environment(EnvType.CLIENT)
      public static final class Builder {
         private final com.google.common.collect.ImmutableList.Builder<Triple<ResourceLocation, Boolean, Boolean>> builder = new com.google.common.collect.ImmutableList.Builder();

         public RenderStateShard.MultiTextureStateShard.Builder add(ResourceLocation resourceLocation, boolean bl, boolean bl2) {
            this.builder.add(Triple.of(resourceLocation, bl, bl2));
            return this;
         }

         public RenderStateShard.MultiTextureStateShard build() {
            return new RenderStateShard.MultiTextureStateShard(this.builder.build());
         }
      }
   }
}
