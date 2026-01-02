package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class FlyTowardsPositionParticle extends TextureSheetParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;
   private final boolean isGlowing;
   private final Particle.LifetimeAlpha lifetimeAlpha;

   FlyTowardsPositionParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
      this(clientLevel, d, e, f, g, h, i, false, Particle.LifetimeAlpha.ALWAYS_OPAQUE);
   }

   FlyTowardsPositionParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, boolean bl, Particle.LifetimeAlpha lifetimeAlpha) {
      super(clientLevel, d, e, f);
      this.isGlowing = bl;
      this.lifetimeAlpha = lifetimeAlpha;
      this.setAlpha(lifetimeAlpha.startAlpha());
      this.xd = g;
      this.yd = h;
      this.zd = i;
      this.xStart = d;
      this.yStart = e;
      this.zStart = f;
      this.xo = d + g;
      this.yo = e + h;
      this.zo = f + i;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      float j = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = 0.9F * j;
      this.gCol = 0.9F * j;
      this.bCol = j;
      this.hasPhysics = false;
      this.lifetime = (int)(Math.random() * 10.0D) + 30;
   }

   public ParticleRenderType getRenderType() {
      return this.lifetimeAlpha.isOpaque() ? ParticleRenderType.PARTICLE_SHEET_OPAQUE : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void move(double d, double e, double f) {
      this.setBoundingBox(this.getBoundingBox().move(d, e, f));
      this.setLocationFromBoundingbox();
   }

   public int getLightColor(float f) {
      if (this.isGlowing) {
         return 240;
      } else {
         int i = super.getLightColor(f);
         float g = (float)this.age / (float)this.lifetime;
         g *= g;
         g *= g;
         int j = i & 255;
         int k = i >> 16 & 255;
         k += (int)(g * 15.0F * 16.0F);
         if (k > 240) {
            k = 240;
         }

         return j | k << 16;
      }
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = (float)this.age / (float)this.lifetime;
         f = 1.0F - f;
         float g = 1.0F - f;
         g *= g;
         g *= g;
         this.x = this.xStart + this.xd * (double)f;
         this.y = this.yStart + this.yd * (double)f - (double)(g * 1.2F);
         this.z = this.zStart + this.zd * (double)f;
      }
   }

   public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
      this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, f));
      super.render(vertexConsumer, camera, f);
   }

   @Environment(EnvType.CLIENT)
   public static class VaultConnectionProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public VaultConnectionProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FlyTowardsPositionParticle flyTowardsPositionParticle = new FlyTowardsPositionParticle(clientLevel, d, e, f, g, h, i, true, new Particle.LifetimeAlpha(0.0F, 0.6F, 0.25F, 1.0F));
         flyTowardsPositionParticle.scale(1.5F);
         flyTowardsPositionParticle.pickSprite(this.sprite);
         return flyTowardsPositionParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public NautilusProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FlyTowardsPositionParticle flyTowardsPositionParticle = new FlyTowardsPositionParticle(clientLevel, d, e, f, g, h, i);
         flyTowardsPositionParticle.pickSprite(this.sprite);
         return flyTowardsPositionParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class EnchantProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EnchantProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FlyTowardsPositionParticle flyTowardsPositionParticle = new FlyTowardsPositionParticle(clientLevel, d, e, f, g, h, i);
         flyTowardsPositionParticle.pickSprite(this.sprite);
         return flyTowardsPositionParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
