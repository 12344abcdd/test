package net.minecraft.client.renderer.chunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RenderChunkRegion implements BlockAndTintGetter {
   public static final int RADIUS = 1;
   public static final int SIZE = 3;
   private final int minChunkX;
   private final int minChunkZ;
   protected final RenderChunk[] chunks;
   protected final Level level;

   RenderChunkRegion(Level level, int i, int j, RenderChunk[] renderChunks) {
      this.level = level;
      this.minChunkX = i;
      this.minChunkZ = j;
      this.chunks = renderChunks;
   }

   public BlockState getBlockState(BlockPos blockPos) {
      return this.getChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ())).getBlockState(blockPos);
   }

   public FluidState getFluidState(BlockPos blockPos) {
      return this.getChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ())).getBlockState(blockPos).getFluidState();
   }

   public float getShade(Direction direction, boolean bl) {
      return this.level.getShade(direction, bl);
   }

   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return this.getChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ())).getBlockEntity(blockPos);
   }

   private RenderChunk getChunk(int i, int j) {
      return this.chunks[index(this.minChunkX, this.minChunkZ, i, j)];
   }

   public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      return this.level.getBlockTint(blockPos, colorResolver);
   }

   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }

   public int getHeight() {
      return this.level.getHeight();
   }

   public static int index(int i, int j, int k, int l) {
      return k - i + (l - j) * 3;
   }
}
