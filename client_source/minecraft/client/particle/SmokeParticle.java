package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class SmokeParticle extends BaseAshSmokeParticle {
   protected SmokeParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, float j, SpriteSet spriteSet) {
      super(clientLevel, d, e, f, 0.1F, 0.1F, 0.1F, g, h, i, j, spriteSet, 0.3F, 8, -0.1F, true);
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         return new SmokeParticle(clientLevel, d, e, f, g, h, i, 1.0F, this.sprites);
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
