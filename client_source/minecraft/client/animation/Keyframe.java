package net.minecraft.client.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public record Keyframe(float timestamp, Vector3f target, AnimationChannel.Interpolation interpolation) {
   public Keyframe(float f, Vector3f vector3f, AnimationChannel.Interpolation interpolation) {
      this.timestamp = f;
      this.target = vector3f;
      this.interpolation = interpolation;
   }

   public float timestamp() {
      return this.timestamp;
   }

   public Vector3f target() {
      return this.target;
   }

   public AnimationChannel.Interpolation interpolation() {
      return this.interpolation;
   }
}
