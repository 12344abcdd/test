package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class FlameParticle extends RisingParticle {
   FlameParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
      super(clientLevel, d, e, f, g, h, i);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void move(double d, double e, double f) {
      this.setBoundingBox(this.getBoundingBox().move(d, e, f));
      this.setLocationFromBoundingbox();
   }

   public float getQuadSize(float f) {
      float g = ((float)this.age + f) / (float)this.lifetime;
      return this.quadSize * (1.0F - g * g * 0.5F);
   }

   public int getLightColor(float f) {
      float g = ((float)this.age + f) / (float)this.lifetime;
      g = Mth.clamp(g, 0.0F, 1.0F);
      int i = super.getLightColor(f);
      int j = i & 255;
      int k = i >> 16 & 255;
      j += (int)(g * 15.0F * 16.0F);
      if (j > 240) {
         j = 240;
      }

      return j | k << 16;
   }

   @Environment(EnvType.CLIENT)
   public static class SmallFlameProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SmallFlameProvider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FlameParticle flameParticle = new FlameParticle(clientLevel, d, e, f, g, h, i);
         flameParticle.pickSprite(this.sprite);
         flameParticle.scale(0.5F);
         return flameParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         FlameParticle flameParticle = new FlameParticle(clientLevel, d, e, f, g, h, i);
         flameParticle.pickSprite(this.sprite);
         return flameParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
