package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ARGB32;

@Environment(EnvType.CLIENT)
public class MobAppearanceParticle extends Particle {
   private final Model model;
   private final RenderType renderType;

   MobAppearanceParticle(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f);
      this.renderType = RenderType.entityTranslucent(ElderGuardianRenderer.GUARDIAN_ELDER_LOCATION);
      this.model = new GuardianModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELDER_GUARDIAN));
      this.gravity = 0.0F;
      this.lifetime = 30;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.CUSTOM;
   }

   public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
      float g = ((float)this.age + f) / (float)this.lifetime;
      float h = 0.05F + 0.5F * Mth.sin(g * 3.1415927F);
      int i = ARGB32.colorFromFloat(h, 1.0F, 1.0F, 1.0F);
      PoseStack poseStack = new PoseStack();
      poseStack.mulPose(camera.rotation());
      poseStack.mulPose(Axis.XP.rotationDegrees(150.0F * g - 60.0F));
      poseStack.scale(1.0F, -1.0F, -1.0F);
      poseStack.translate(0.0F, -1.101F, 1.5F);
      MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
      VertexConsumer vertexConsumer2 = bufferSource.getBuffer(this.renderType);
      this.model.renderToBuffer(poseStack, vertexConsumer2, 15728880, OverlayTexture.NO_OVERLAY, i);
      bufferSource.endBatch();
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         return new MobAppearanceParticle(clientLevel, d, e, f);
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
