package com.example.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * 完全自定义Frustum，可用于替换Fabric官方的裁剪算法。
 */
@Mixin(Frustum.class)
public class FrustumMixin {
    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;
    @Shadow private Vector4f viewVector;

    @Unique
    private final Vector4f[] frustumData = new Vector4f[6];

    /**
     * 计算裁剪面。该方法需在构造或变换时调用。
     * 这里重写 Fabric 原版构造逻辑，把 mcp-reborn 的数据结构迁移过来。
     */
    @Overwrite
    private void calculateFrustum(Matrix4f projection, Matrix4f modelView) {
        // 复制两个矩阵并合并
        Matrix4f matrix4f = new Matrix4f(modelView).mul(projection);
        matrix4f.transpose();

        // 构造视角向量
        this.viewVector = new Vector4f(0.0F, 0.0F, 1.0F, 0.0F);
        matrix4f.transform(this.viewVector);

        // 依次计算六个面（这里和 MCP 版保持一致）
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

    /**
     * 必须重写 prepare 方法（以保证摄像机坐标同步），Fabric 1.21.1默认实现与MCP版相同。
     */
    @Overwrite
    public void prepare(double x, double y, double z) {
        this.camX = x;
        this.camY = y;
        this.camZ = z;
    }

    /**
     * isVisible方法直接调用 cubeInFrustum，为保持fabric接口兼容性。
     */
    @Overwrite
    public boolean isVisible(AABB aabb) {
        return this.cubeInFrustum(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    /**
     * 完整替换 cubeInFrustum，使用 MCP/Forge算法逐平面逐顶点裁剪。
     */
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
        // 判断所有6个裁剪面
        for (Vector4f plane : frustumData) {
            boolean inside = false;
            for (float x : xs) {
                for (float y : ys) {
                    for (float z : zs) {
                        // 只要有一个顶点在平面内就跳过该面
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
                // 有一个裁剪面外所有顶点都不在平面内，则整个包围盒不可见
                return false;
            }
        }
        return true;
    }
}
