package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      Matrix4f matrix4f = poseStack.last().pose();
      BlockGetter blockGetter = this.minecraft.player.level();
      BlockPos blockPos = BlockPos.containing(d, e, f);
      Iterator var12 = BlockPos.betweenClosed(blockPos.offset(-6, -6, -6), blockPos.offset(6, 6, 6)).iterator();

      while(true) {
         BlockPos blockPos2;
         BlockState blockState;
         do {
            if (!var12.hasNext()) {
               return;
            }

            blockPos2 = (BlockPos)var12.next();
            blockState = blockGetter.getBlockState(blockPos2);
         } while(blockState.is(Blocks.AIR));

         VoxelShape voxelShape = blockState.getShape(blockGetter, blockPos2);
         Iterator var16 = voxelShape.toAabbs().iterator();

         while(var16.hasNext()) {
            AABB aABB = (AABB)var16.next();
            AABB aABB2 = aABB.move(blockPos2).inflate(0.002D);
            float g = (float)(aABB2.minX - d);
            float h = (float)(aABB2.minY - e);
            float i = (float)(aABB2.minZ - f);
            float j = (float)(aABB2.maxX - d);
            float k = (float)(aABB2.maxY - e);
            float l = (float)(aABB2.maxZ - f);
            int m = -2130771968;
            VertexConsumer vertexConsumer;
            if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.WEST)) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
               vertexConsumer.addVertex(matrix4f, g, h, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, h, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, k, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, k, l).setColor(-2130771968);
            }

            if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.SOUTH)) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
               vertexConsumer.addVertex(matrix4f, g, k, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, h, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, k, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, h, l).setColor(-2130771968);
            }

            if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.EAST)) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
               vertexConsumer.addVertex(matrix4f, j, h, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, h, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, k, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, k, i).setColor(-2130771968);
            }

            if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.NORTH)) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
               vertexConsumer.addVertex(matrix4f, j, k, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, h, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, k, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, h, i).setColor(-2130771968);
            }

            if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.DOWN)) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
               vertexConsumer.addVertex(matrix4f, g, h, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, h, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, h, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, h, l).setColor(-2130771968);
            }

            if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.UP)) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
               vertexConsumer.addVertex(matrix4f, g, k, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, g, k, l).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, k, i).setColor(-2130771968);
               vertexConsumer.addVertex(matrix4f, j, k, l).setColor(-2130771968);
            }
         }
      }
   }
}
