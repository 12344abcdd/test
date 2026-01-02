package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class HugeExplosionParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected HugeExplosionParticle(ClientLevel clientLevel, double d, double e, double f, double g, SpriteSet spriteSet) {
      super(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D);
      this.lifetime = 6 + this.random.nextInt(4);
      float h = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = h;
      this.gCol = h;
      this.bCol = h;
      this.quadSize = 2.0F * (1.0F - (float)g * 0.5F);
      this.sprites = spriteSet;
      this.setSpriteFromAge(spriteSet);
   }

   public int getLightColor(float f) {
      return 15728880;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_LIT;
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         return new HugeExplosionParticle(clientLevel, d, e, f, g, this.sprites);
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
