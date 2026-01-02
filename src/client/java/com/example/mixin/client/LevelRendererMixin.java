package com.example.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow private net.minecraft.client.multiplayer.ClientLevel level;
    @Shadow private net.minecraft.client.Minecraft minecraft;
    @Shadow private net.minecraft.client.renderer.entity.EntityRenderDispatcher entityRenderDispatcher;
    @Shadow private net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    @Shadow private Frustum capturedFrustum;
    @Shadow private Frustum cullingFrustum;
    @Shadow private Vector3d frustumPos;
    @Shadow private boolean captureFrustum;

    @Inject(at = @At("HEAD"), method = "renderLevel")
    private void onRenderLevelStart(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo info) {
        // 在渲染世界开始时执行的自定义逻辑
        System.out.println("开始渲染世界...");
    }

    @Inject(at = @At("TAIL"), method = "renderLevel")
    private void onRenderLevelEnd(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo info) {
        // 在渲染世界结束时执行的自定义逻辑
        System.out.println("结束渲染世界...");
    }
}