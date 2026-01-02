package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.CollisionContext;

@Environment(EnvType.CLIENT)
public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      double g = (double)Util.getNanos();
      if (g - this.lastUpdateTime > 1.0E8D) {
         this.lastUpdateTime = g;
         Entity entity = this.minecraft.gameRenderer.getMainCamera().getEntity();
         this.surroundEntities = ImmutableList.copyOf(entity.level().getEntities(entity, entity.getBoundingBox().inflate(16.0D)));
      }

      Player player = this.minecraft.player;
      if (player != null && player.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(poseStack, multiBufferSource, d, e, f, player, () -> {
            return 0.0D;
         }, 1.0F, 0.0F, 0.0F);
      }

      Iterator var12 = this.surroundEntities.iterator();

      while(var12.hasNext()) {
         Entity entity2 = (Entity)var12.next();
         if (entity2 != player) {
            this.drawHighlights(poseStack, multiBufferSource, d, e, f, entity2, () -> {
               return this.getBias(entity2);
            }, 0.0F, 1.0F, 0.0F);
         }
      }

   }

   private void drawHighlights(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f, Entity entity, DoubleSupplier doubleSupplier, float g, float h, float i) {
      entity.mainSupportingBlockPos.ifPresent((blockPos) -> {
         double j = doubleSupplier.getAsDouble();
         BlockPos blockPos2 = entity.getOnPos();
         this.highlightPosition(blockPos2, poseStack, d, e, f, multiBufferSource, 0.02D + j, g, h, i);
         BlockPos blockPos3 = entity.getOnPosLegacy();
         if (!blockPos3.equals(blockPos2)) {
            this.highlightPosition(blockPos3, poseStack, d, e, f, multiBufferSource, 0.04D + j, 0.0F, 1.0F, 1.0F);
         }

      });
   }

   private double getBias(Entity entity) {
      return 0.02D * (double)(String.valueOf((double)entity.getId() + 0.132453657D).hashCode() % 1000) / 1000.0D;
   }

   private void highlightPosition(BlockPos blockPos, PoseStack poseStack, double d, double e, double f, MultiBufferSource multiBufferSource, double g, float h, float i, float j) {
      double k = (double)blockPos.getX() - d - 2.0D * g;
      double l = (double)blockPos.getY() - e - 2.0D * g;
      double m = (double)blockPos.getZ() - f - 2.0D * g;
      double n = k + 1.0D + 4.0D * g;
      double o = l + 1.0D + 4.0D * g;
      double p = m + 1.0D + 4.0D * g;
      LevelRenderer.renderLineBox(poseStack, multiBufferSource.getBuffer(RenderType.lines()), k, l, m, n, o, p, h, i, j, 0.4F);
      LevelRenderer.renderVoxelShape(poseStack, multiBufferSource.getBuffer(RenderType.lines()), this.minecraft.level.getBlockState(blockPos).getCollisionShape(this.minecraft.level, blockPos, CollisionContext.empty()).move((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ()), -d, -e, -f, h, i, j, 1.0F, false);
   }
}
