package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.util.FastColor.ARGB32;

@Environment(EnvType.CLIENT)
public class WhiteAshParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteAshParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, float j, SpriteSet spriteSet) {
      super(clientLevel, d, e, f, 0.1F, -0.1F, 0.1F, g, h, i, j, spriteSet, 0.0F, 20, 0.0125F, false);
      this.rCol = (float)ARGB32.red(12235202) / 255.0F;
      this.gCol = (float)ARGB32.green(12235202) / 255.0F;
      this.bCol = (float)ARGB32.blue(12235202) / 255.0F;
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         RandomSource randomSource = clientLevel.random;
         double j = (double)randomSource.nextFloat() * -1.9D * (double)randomSource.nextFloat() * 0.1D;
         double k = (double)randomSource.nextFloat() * -0.5D * (double)randomSource.nextFloat() * 0.1D * 5.0D;
         double l = (double)randomSource.nextFloat() * -1.9D * (double)randomSource.nextFloat() * 0.1D;
         return new WhiteAshParticle(clientLevel, d, e, f, j, k, l, 1.0F, this.sprites);
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
