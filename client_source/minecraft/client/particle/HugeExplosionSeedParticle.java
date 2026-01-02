package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class HugeExplosionSeedParticle extends NoRenderParticle {
   HugeExplosionSeedParticle(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D);
      this.lifetime = 8;
   }

   public void tick() {
      for(int i = 0; i < 6; ++i) {
         double d = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0D;
         double e = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0D;
         double f = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0D;
         this.level.addParticle(ParticleTypes.EXPLOSION, d, e, f, (double)((float)this.age / (float)this.lifetime), 0.0D, 0.0D);
      }

      ++this.age;
      if (this.age == this.lifetime) {
         this.remove();
      }

   }

   @Environment(EnvType.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
         return new HugeExplosionSeedParticle(clientLevel, d, e, f);
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
