package net.minecraft.client.renderer.block.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction.Axis;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public record BlockElementRotation(Vector3f origin, Axis axis, float angle, boolean rescale) {
   public BlockElementRotation(Vector3f vector3f, Axis axis, float f, boolean bl) {
      this.origin = vector3f;
      this.axis = axis;
      this.angle = f;
      this.rescale = bl;
   }

   public Vector3f origin() {
      return this.origin;
   }

   public Axis axis() {
      return this.axis;
   }

   public float angle() {
      return this.angle;
   }

   public boolean rescale() {
      return this.rescale;
   }
}
