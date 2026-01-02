package com.example.mixin.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Frustum.class)
public class FrustumMixin {
    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;
    @Shadow private Vector4f viewVector;

    @Unique
    private final Vector4f[] frustumData = new Vector4f[6];

    @Overwrite
    private void calculateFrustum(Matrix4f projection, Matrix4f modelView) {
        Matrix4f matrix4f = new Matrix4f(modelView).mul(projection);
        matrix4f.transpose();

        this.viewVector = new Vector4f(0.0F, 0.0F, 1.0F, 0.0F);
        matrix4f.transform(this.viewVector);

        getPlane(matrix4f, -1, 0, 0, 0);
        getPlane(matrix4f, 1, 0, 0, 1);
        getPlane(matrix4f, 0, -1, 0, 2);
        getPlane(matrix4f, 0, 1, 0, 3);
        getPlane(matrix4f, 0, 0, -1, 4);
        getPlane(matrix4f, 0, 0, 1, 5);
    }

    @Unique
    private void getPlane(Matrix4f matrix, int x, int y, int z, int idx) {
        Vector4f v = new Vector4f((float)x, (float)y, (float)z, 1.0F);
        matrix.transform(v);
        v.normalize();
        frustumData[idx] = v;
    }

    @Overwrite
    public void prepare(double x, double y, double z) {
        this.camX = x;
        this.camY = y;
        this.camZ = z;
    }

    @Overwrite
    public boolean isVisible(AABB aabb) {
        return this.cubeInFrustum(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    @Overwrite
    private boolean cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        float[] xs = new float[] {
            (float)(minX - this.camX), (float)(maxX - this.camX)
        };
        float[] ys = new float[] {
            (float)(minY - this.camY), (float)(maxY - this.camY)
        };
        float[] zs = new float[] {
            (float)(minZ - this.camZ), (float)(maxZ - this.camZ)
        };

        for (Vector4f plane : frustumData) {
            if (plane == null) return true;
            boolean inside = false;
            for (float x : xs) {
                for (float y : ys) {
                    for (float z : zs) {
                        if (plane.dot(new Vector4f(x, y, z, 1.0F)) > 0.0F) {
                            inside = true;
                            break;
                        }
                    }
                    if (inside) break;
                }
                if (inside) break;
            }
            if (!inside) {
                return false;
            }
        }
        return true;
    }
}