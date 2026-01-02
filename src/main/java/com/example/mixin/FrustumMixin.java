package com.yourmodid.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 用于在Fabric 1.21.1下重写Frustum，实现自定义的裁剪判断与数学逻辑。
 */
@Mixin(Frustum.class)
public class FrustumMixin {
    // Shadow字段，调用原版实现/存取
    @Shadow private FrustumIntersection intersection;
    @Shadow private Matrix4f matrix;
    @Shadow private Vector4f viewVector;
    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;

    /**
     * 原Frustum中 isVisible 方法逻辑，
     * 如需魔改可直接写MCP代码或调用cubeInFrustum
     */
    @Overwrite
    public boolean isVisible(AABB box) {
        return this.cubeInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * 实现你在mcp-reborn那一套的判定逻辑，如果有复杂魔改算法，可以原样粘贴、稍加调整参数类型。
     * 推荐优先用 intersection.testAab，便于兼容Fabric新版。如果要魔改逻辑如用frustumData数组，
     * 可以在Mixin中声明私有数组并初始化，然后用同样算法重写。
     */
    @Overwrite
    private boolean cubeInFrustum(double d, double e, double f, double g, double h, double i) {
        float j = (float)(d - this.camX);
        float k = (float)(e - this.camY);
        float l = (float)(f - this.camZ);
        float m = (float)(g - this.camX);
        float n = (float)(h - this.camY);
        float o = (float)(i - this.camZ);
        // 这里可以使用MCP逻辑替换
        // return yourCustomCubeInFrustum(j, k, l, m, n, o); // 示例
        return this.intersection.testAab(j, k, l, m, n, o); // Fabric原版逻辑
    }

    // 如需实现原mcp的frustumData裁剪算法，可以在这里新建私有数据和方法
}
