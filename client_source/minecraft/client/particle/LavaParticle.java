package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class LavaParticle extends TextureSheetParticle {
   LavaParticle(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D);
      this.gravity = 0.75F;
      this.friction = 0.999F;
      this.xd *= 0.800000011920929D;
      this.yd *= 0.800000011920929D;
      this.zd *= 0.800000011920929D;
      this.yd = (double)(this.random.nextFloat() * 0.4F + 0.05F);
      this.quadSize *= this.random.nextFloat() * 2.0F + 0.2F;
      this.lifetime = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public int getLightColor(float f) {
      int i = super.getLightColor(f);
      int j = true;
      int k = i >> 16 & 255;
      return 240 | k << 16;
   }

   public float getQuadSize(float f) {
      float g = ((float)this.age + f) / (float)this.lifetime;
      return this.quadSize * (1.0F - g * g);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float f = (float)this.age / (float)this.lifetime;
         if (this.random.nextFloat() > f) {
            this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }
      }

   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet spriteSet) {
         this.sprite = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         LavaParticle lavaParticle = new LavaParticle(clientLevel, d, e, f);
         lavaParticle.pickSprite(this.sprite);
         return lavaParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
