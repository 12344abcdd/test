package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

@Environment(EnvType.CLIENT)
public class WaterDropParticle extends TextureSheetParticle {
   protected WaterDropParticle(ClientLevel clientLevel, double d, double e, double f) {
      super(clientLevel, d, e, f, 0.0D, 0.0D, 0.0D);
      this.xd *= 0.30000001192092896D;
      this.yd = Math.random() * 0.20000000298023224D + 0.10000000149011612D;
      this.zd *= 0.30000001192092896D;
      this.setSize(0.01F, 0.01F);
      this.gravity = 0.06F;
      this.lifetime = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.yd -= (double)this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.9800000190734863D;
         this.yd *= 0.9800000190734863D;
         this.zd *= 0.9800000190734863D;
         if (this.onGround) {
            if (Math.random() < 0.5D) {
               this.remove();
            }

            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
         }

         BlockPos blockPos = BlockPos.containing(this.x, this.y, this.z);
         double d = Math.max(this.level.getBlockState(blockPos).getCollisionShape(this.level, blockPos).max(Axis.Y, this.x - (double)blockPos.getX(), this.z - (double)blockPos.getZ()), (double)this.level.getFluidState(blockPos).getHeight(this.level, blockPos));
         if (d > 0.0D && this.y < (double)blockPos.getY() + d) {
            this.remove();
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
         WaterDropParticle waterDropParticle = new WaterDropParticle(clientLevel, d, e, f);
         waterDropParticle.pickSprite(this.sprite);
         return waterDropParticle;
      }

      // $FF: synthetic method
      public Particle createParticle(final ParticleOptions particleOptions, final ClientLevel clientLevel, final double d, final double e, final double f, final double g, final double h, final double i) {
         return this.createParticle((SimpleParticleType)particleOptions, clientLevel, d, e, f, g, h, i);
      }
   }
}
