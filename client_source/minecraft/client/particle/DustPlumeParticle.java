package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor.ARGB32;

@Environment(EnvType.CLIENT)
public class DustPlumeParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected DustPlumeParticle(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i, float j, SpriteSet spriteSet) {
      super(clientLevel, d, e, f, 0.7F, 0.6F, 0.7F, g, h + 0.15000000596046448D, i, j, spriteSet, 0.5F, 7, 0.5F, false);
      float k = (float)Math.random() * 0.2F;
      this.rCol = (float)ARGB32.red(12235202) / 255.0F - k;
      this.gCol = (float)ARGB32.green(12235202) / 255.0F - k;
      this.bCol = (float)ARGB32.blue(12235202) / 255.0F - k;
   }

   public void tick() {
      this.gravity = 0.88F * this.gravity;
      this.friction = 0.92F * this.friction;
      super.tick();
   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet spriteSet) {
         this.sprites = spriteSet;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         return new DustPlumeParticle(clientLevel, d, e, f, g, h, i, 1.0F, this.sprites);
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
