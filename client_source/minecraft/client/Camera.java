package net.minecraft.client;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class Camera {
   private static final float DEFAULT_CAMERA_DISTANCE = 4.0F;
   private static final Vector3f FORWARDS = new Vector3f(0.0F, 0.0F, -1.0F);
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
   private boolean initialized;
   private BlockGetter level;
   private Entity entity;
   private Vec3 position;
   private final MutableBlockPos blockPosition;
   private final Vector3f forwards;
   private final Vector3f up;
   private final Vector3f left;
   private float xRot;
   private float yRot;
   private final Quaternionf rotation;
   private boolean detached;
   private float eyeHeight;
   private float eyeHeightOld;
   private float partialTickTime;
   public static final float FOG_DISTANCE_SCALE = 0.083333336F;

   public Camera() {
      this.position = Vec3.ZERO;
      this.blockPosition = new MutableBlockPos();
      this.forwards = new Vector3f(FORWARDS);
      this.up = new Vector3f(UP);
      this.left = new Vector3f(LEFT);
      this.rotation = new Quaternionf();
   }

   public void setup(BlockGetter blockGetter, Entity entity, boolean bl, boolean bl2, float f) {
      this.initialized = true;
      this.level = blockGetter;
      this.entity = entity;
      this.detached = bl;
      this.partialTickTime = f;
      this.setRotation(entity.getViewYRot(f), entity.getViewXRot(f));
      this.setPosition(Mth.lerp((double)f, entity.xo, entity.getX()), Mth.lerp((double)f, entity.yo, entity.getY()) + (double)Mth.lerp(f, this.eyeHeightOld, this.eyeHeight), Mth.lerp((double)f, entity.zo, entity.getZ()));
      if (bl) {
         if (bl2) {
            this.setRotation(this.yRot + 180.0F, -this.xRot);
         }

         float var10000;
         if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            var10000 = livingEntity.getScale();
         } else {
            var10000 = 1.0F;
         }

         float g = var10000;
         this.move(-this.getMaxZoom(4.0F * g), 0.0F, 0.0F);
      } else if (entity instanceof LivingEntity && ((LivingEntity)entity).isSleeping()) {
         Direction direction = ((LivingEntity)entity).getBedOrientation();
         this.setRotation(direction != null ? direction.toYRot() - 180.0F : 0.0F, 0.0F);
         this.move(0.0F, 0.3F, 0.0F);
      }

   }

   public void tick() {
      if (this.entity != null) {
         this.eyeHeightOld = this.eyeHeight;
         this.eyeHeight += (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
      }

   }

   private float getMaxZoom(float f) {
      float g = 0.1F;

      for(int i = 0; i < 8; ++i) {
         float h = (float)((i & 1) * 2 - 1);
         float j = (float)((i >> 1 & 1) * 2 - 1);
         float k = (float)((i >> 2 & 1) * 2 - 1);
         Vec3 vec3 = this.position.add((double)(h * 0.1F), (double)(j * 0.1F), (double)(k * 0.1F));
         Vec3 vec32 = vec3.add((new Vec3(this.forwards)).scale((double)(-f)));
         HitResult hitResult = this.level.clip(new ClipContext(vec3, vec32, Block.VISUAL, Fluid.NONE, this.entity));
         if (hitResult.getType() != Type.MISS) {
            float l = (float)hitResult.getLocation().distanceToSqr(this.position);
            if (l < Mth.square(f)) {
               f = Mth.sqrt(l);
            }
         }
      }

      return f;
   }

   protected void move(float f, float g, float h) {
      Vector3f vector3f = (new Vector3f(h, g, -f)).rotate(this.rotation);
      this.setPosition(new Vec3(this.position.x + (double)vector3f.x, this.position.y + (double)vector3f.y, this.position.z + (double)vector3f.z));
   }

   protected void setRotation(float f, float g) {
      this.xRot = g;
      this.yRot = f;
      this.rotation.rotationYXZ(3.1415927F - f * 0.017453292F, -g * 0.017453292F, 0.0F);
      FORWARDS.rotate(this.rotation, this.forwards);
      UP.rotate(this.rotation, this.up);
      LEFT.rotate(this.rotation, this.left);
   }

   protected void setPosition(double d, double e, double f) {
      this.setPosition(new Vec3(d, e, f));
   }

   protected void setPosition(Vec3 vec3) {
      this.position = vec3;
      this.blockPosition.set(vec3.x, vec3.y, vec3.z);
   }

   public Vec3 getPosition() {
      return this.position;
   }

   public BlockPos getBlockPosition() {
      return this.blockPosition;
   }

   public float getXRot() {
      return this.xRot;
   }

   public float getYRot() {
      return this.yRot;
   }

   public Quaternionf rotation() {
      return this.rotation;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public boolean isDetached() {
      return this.detached;
   }

   public Camera.NearPlane getNearPlane() {
      Minecraft minecraft = Minecraft.getInstance();
      double d = (double)minecraft.getWindow().getWidth() / (double)minecraft.getWindow().getHeight();
      double e = Math.tan((double)((float)(Integer)minecraft.options.fov().get() * 0.017453292F) / 2.0D) * 0.05000000074505806D;
      double f = e * d;
      Vec3 vec3 = (new Vec3(this.forwards)).scale(0.05000000074505806D);
      Vec3 vec32 = (new Vec3(this.left)).scale(f);
      Vec3 vec33 = (new Vec3(this.up)).scale(e);
      return new Camera.NearPlane(vec3, vec32, vec33);
   }

   public FogType getFluidInCamera() {
      if (!this.initialized) {
         return FogType.NONE;
      } else {
         FluidState fluidState = this.level.getFluidState(this.blockPosition);
         if (fluidState.is(FluidTags.WATER) && this.position.y < (double)((float)this.blockPosition.getY() + fluidState.getHeight(this.level, this.blockPosition))) {
            return FogType.WATER;
         } else {
            Camera.NearPlane nearPlane = this.getNearPlane();
            List<Vec3> list = Arrays.asList(nearPlane.forward, nearPlane.getTopLeft(), nearPlane.getTopRight(), nearPlane.getBottomLeft(), nearPlane.getBottomRight());
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
               Vec3 vec3 = (Vec3)var4.next();
               Vec3 vec32 = this.position.add(vec3);
               BlockPos blockPos = BlockPos.containing(vec32);
               FluidState fluidState2 = this.level.getFluidState(blockPos);
               if (fluidState2.is(FluidTags.LAVA)) {
                  if (vec32.y <= (double)(fluidState2.getHeight(this.level, blockPos) + (float)blockPos.getY())) {
                     return FogType.LAVA;
                  }
               } else {
                  BlockState blockState = this.level.getBlockState(blockPos);
                  if (blockState.is(Blocks.POWDER_SNOW)) {
                     return FogType.POWDER_SNOW;
                  }
               }
            }

            return FogType.NONE;
         }
      }
   }

   public final Vector3f getLookVector() {
      return this.forwards;
   }

   public final Vector3f getUpVector() {
      return this.up;
   }

   public final Vector3f getLeftVector() {
      return this.left;
   }

   public void reset() {
      this.level = null;
      this.entity = null;
      this.initialized = false;
   }

   public float getPartialTickTime() {
      return this.partialTickTime;
   }

   @Environment(EnvType.CLIENT)
   public static class NearPlane {
      final Vec3 forward;
      private final Vec3 left;
      private final Vec3 up;

      NearPlane(Vec3 vec3, Vec3 vec32, Vec3 vec33) {
         this.forward = vec3;
         this.left = vec32;
         this.up = vec33;
      }

      public Vec3 getTopLeft() {
         return this.forward.add(this.up).add(this.left);
      }

      public Vec3 getTopRight() {
         return this.forward.add(this.up).subtract(this.left);
      }

      public Vec3 getBottomLeft() {
         return this.forward.subtract(this.up).add(this.left);
      }

      public Vec3 getBottomRight() {
         return this.forward.subtract(this.up).subtract(this.left);
      }

      public Vec3 getPointOnPlane(float f, float g) {
         return this.forward.add(this.up.scale((double)g)).subtract(this.left.scale((double)f));
      }
   }
}
