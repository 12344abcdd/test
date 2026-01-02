package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display.RenderState;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Display.BlockDisplay.BlockRenderState;
import net.minecraft.world.entity.Display.ItemDisplay.ItemRenderState;
import net.minecraft.world.entity.Display.TextDisplay.Align;
import net.minecraft.world.entity.Display.TextDisplay.CachedInfo;
import net.minecraft.world.entity.Display.TextDisplay.CachedLine;
import net.minecraft.world.entity.Display.TextDisplay.TextRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public abstract class DisplayRenderer<T extends Display, S> extends EntityRenderer<T> {
   private final EntityRenderDispatcher entityRenderDispatcher;

   protected DisplayRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.entityRenderDispatcher = context.getEntityRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(T display) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   public void render(T display, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
      RenderState renderState = display.renderState();
      if (renderState != null) {
         S object = this.getSubState(display);
         if (object != null) {
            float h = display.calculateInterpolationProgress(g);
            this.shadowRadius = renderState.shadowRadius().get(h);
            this.shadowStrength = renderState.shadowStrength().get(h);
            int j = renderState.brightnessOverride();
            int k = j != -1 ? j : i;
            super.render(display, f, g, poseStack, multiBufferSource, k);
            poseStack.pushPose();
            poseStack.mulPose(this.calculateOrientation(renderState, display, g, new Quaternionf()));
            Transformation transformation = (Transformation)renderState.transformation().get(h);
            poseStack.mulPose(transformation.getMatrix());
            this.renderInner(display, object, poseStack, multiBufferSource, k, h);
            poseStack.popPose();
         }
      }
   }

   private Quaternionf calculateOrientation(RenderState renderState, T display, float f, Quaternionf quaternionf) {
      Camera camera = this.entityRenderDispatcher.camera;
      Quaternionf var10000;
      switch(renderState.billboardConstraints()) {
      case FIXED:
         var10000 = quaternionf.rotationYXZ(-0.017453292F * entityYRot(display, f), 0.017453292F * entityXRot(display, f), 0.0F);
         break;
      case HORIZONTAL:
         var10000 = quaternionf.rotationYXZ(-0.017453292F * entityYRot(display, f), 0.017453292F * cameraXRot(camera), 0.0F);
         break;
      case VERTICAL:
         var10000 = quaternionf.rotationYXZ(-0.017453292F * cameraYrot(camera), 0.017453292F * entityXRot(display, f), 0.0F);
         break;
      case CENTER:
         var10000 = quaternionf.rotationYXZ(-0.017453292F * cameraYrot(camera), 0.017453292F * cameraXRot(camera), 0.0F);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static float cameraYrot(Camera camera) {
      return camera.getYRot() - 180.0F;
   }

   private static float cameraXRot(Camera camera) {
      return -camera.getXRot();
   }

   private static <T extends Display> float entityYRot(T display, float f) {
      return Mth.rotLerp(f, display.yRotO, display.getYRot());
   }

   private static <T extends Display> float entityXRot(T display, float f) {
      return Mth.lerp(f, display.xRotO, display.getXRot());
   }

   @Nullable
   protected abstract S getSubState(T display);

   protected abstract void renderInner(T display, S object, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float f);

   @Environment(EnvType.CLIENT)
   public static class TextDisplayRenderer extends DisplayRenderer<TextDisplay, TextRenderState> {
      private final Font font;

      protected TextDisplayRenderer(EntityRendererProvider.Context context) {
         super(context);
         this.font = context.getFont();
      }

      private CachedInfo splitLines(Component component, int i) {
         List<FormattedCharSequence> list = this.font.split(component, i);
         List<CachedLine> list2 = new ArrayList(list.size());
         int j = 0;
         Iterator var6 = list.iterator();

         while(var6.hasNext()) {
            FormattedCharSequence formattedCharSequence = (FormattedCharSequence)var6.next();
            int k = this.font.width(formattedCharSequence);
            j = Math.max(j, k);
            list2.add(new CachedLine(formattedCharSequence, k));
         }

         return new CachedInfo(list2, j);
      }

      @Nullable
      protected TextRenderState getSubState(TextDisplay textDisplay) {
         return textDisplay.textRenderState();
      }

      public void renderInner(TextDisplay textDisplay, TextRenderState textRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float f) {
         byte b = textRenderState.flags();
         boolean bl = (b & 2) != 0;
         boolean bl2 = (b & 4) != 0;
         boolean bl3 = (b & 1) != 0;
         Align align = TextDisplay.getAlign(b);
         byte c = (byte)textRenderState.textOpacity().get(f);
         int j;
         float g;
         if (bl2) {
            g = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            j = (int)(g * 255.0F) << 24;
         } else {
            j = textRenderState.backgroundColor().get(f);
         }

         g = 0.0F;
         Matrix4f matrix4f = poseStack.last().pose();
         matrix4f.rotate(3.1415927F, 0.0F, 1.0F, 0.0F);
         matrix4f.scale(-0.025F, -0.025F, -0.025F);
         CachedInfo cachedInfo = textDisplay.cacheDisplay(this::splitLines);
         Objects.requireNonNull(this.font);
         int k = 9 + 1;
         int l = cachedInfo.width();
         int m = cachedInfo.lines().size() * k;
         matrix4f.translate(1.0F - (float)l / 2.0F, (float)(-m), 0.0F);
         if (j != 0) {
            VertexConsumer vertexConsumer = multiBufferSource.getBuffer(bl ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground());
            vertexConsumer.addVertex(matrix4f, -1.0F, -1.0F, 0.0F).setColor(j).setLight(i);
            vertexConsumer.addVertex(matrix4f, -1.0F, (float)m, 0.0F).setColor(j).setLight(i);
            vertexConsumer.addVertex(matrix4f, (float)l, (float)m, 0.0F).setColor(j).setLight(i);
            vertexConsumer.addVertex(matrix4f, (float)l, -1.0F, 0.0F).setColor(j).setLight(i);
         }

         for(Iterator var23 = cachedInfo.lines().iterator(); var23.hasNext(); g += (float)k) {
            CachedLine cachedLine = (CachedLine)var23.next();
            float var10000;
            switch(align) {
            case LEFT:
               var10000 = 0.0F;
               break;
            case RIGHT:
               var10000 = (float)(l - cachedLine.width());
               break;
            case CENTER:
               var10000 = (float)l / 2.0F - (float)cachedLine.width() / 2.0F;
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
            }

            float h = var10000;
            this.font.drawInBatch((FormattedCharSequence)cachedLine.contents(), h, g, c << 24 | 16777215, bl3, matrix4f, multiBufferSource, bl ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET, 0, i);
         }

      }

      // $FF: synthetic method
      @Nullable
      protected Object getSubState(final Display display) {
         return this.getSubState((TextDisplay)display);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ItemDisplayRenderer extends DisplayRenderer<ItemDisplay, ItemRenderState> {
      private final ItemRenderer itemRenderer;

      protected ItemDisplayRenderer(EntityRendererProvider.Context context) {
         super(context);
         this.itemRenderer = context.getItemRenderer();
      }

      @Nullable
      protected ItemRenderState getSubState(ItemDisplay itemDisplay) {
         return itemDisplay.itemRenderState();
      }

      public void renderInner(ItemDisplay itemDisplay, ItemRenderState itemRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float f) {
         poseStack.mulPose(Axis.YP.rotation(3.1415927F));
         this.itemRenderer.renderStatic(itemRenderState.itemStack(), itemRenderState.itemTransform(), i, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, itemDisplay.level(), itemDisplay.getId());
      }

      // $FF: synthetic method
      @Nullable
      protected Object getSubState(final Display display) {
         return this.getSubState((ItemDisplay)display);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class BlockDisplayRenderer extends DisplayRenderer<BlockDisplay, BlockRenderState> {
      private final BlockRenderDispatcher blockRenderer;

      protected BlockDisplayRenderer(EntityRendererProvider.Context context) {
         super(context);
         this.blockRenderer = context.getBlockRenderDispatcher();
      }

      @Nullable
      protected BlockRenderState getSubState(BlockDisplay blockDisplay) {
         return blockDisplay.blockRenderState();
      }

      public void renderInner(BlockDisplay blockDisplay, BlockRenderState blockRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, float f) {
         this.blockRenderer.renderSingleBlock(blockRenderState.blockState(), poseStack, multiBufferSource, i, OverlayTexture.NO_OVERLAY);
      }

      // $FF: synthetic method
      @Nullable
      protected Object getSubState(final Display display) {
         return this.getSubState((BlockDisplay)display);
      }
   }
}
