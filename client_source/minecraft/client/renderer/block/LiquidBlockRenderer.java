package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class LiquidBlockRenderer {
   private static final float MAX_FLUID_HEIGHT = 0.8888889F;
   private final TextureAtlasSprite[] lavaIcons = new TextureAtlasSprite[2];
   private final TextureAtlasSprite[] waterIcons = new TextureAtlasSprite[2];
   private TextureAtlasSprite waterOverlay;

   protected void setupSprites() {
      this.lavaIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
      this.lavaIcons[1] = ModelBakery.LAVA_FLOW.sprite();
      this.waterIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
      this.waterIcons[1] = ModelBakery.WATER_FLOW.sprite();
      this.waterOverlay = ModelBakery.WATER_OVERLAY.sprite();
   }

   private static boolean isNeighborSameFluid(FluidState fluidState, FluidState fluidState2) {
      return fluidState2.getType().isSame(fluidState.getType());
   }

   private static boolean isFaceOccludedByState(BlockGetter blockGetter, Direction direction, float f, BlockPos blockPos, BlockState blockState) {
      if (blockState.canOcclude()) {
         VoxelShape voxelShape = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)f, 1.0D);
         VoxelShape voxelShape2 = blockState.getOcclusionShape(blockGetter, blockPos);
         return Shapes.blockOccudes(voxelShape, voxelShape2, direction);
      } else {
         return false;
      }
   }

   private static boolean isFaceOccludedByNeighbor(BlockGetter blockGetter, BlockPos blockPos, Direction direction, float f, BlockState blockState) {
      return isFaceOccludedByState(blockGetter, direction, f, blockPos.relative(direction), blockState);
   }

   private static boolean isFaceOccludedBySelf(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction) {
      return isFaceOccludedByState(blockGetter, direction.getOpposite(), 1.0F, blockPos, blockState);
   }

   public static boolean shouldRenderFace(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, FluidState fluidState, BlockState blockState, Direction direction, FluidState fluidState2) {
      return !isFaceOccludedBySelf(blockAndTintGetter, blockPos, blockState, direction) && !isNeighborSameFluid(fluidState, fluidState2);
   }

   public void tesselate(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState) {
      boolean bl = fluidState.is(FluidTags.LAVA);
      TextureAtlasSprite[] textureAtlasSprites = bl ? this.lavaIcons : this.waterIcons;
      int i = bl ? 16777215 : BiomeColors.getAverageWaterColor(blockAndTintGetter, blockPos);
      float f = (float)(i >> 16 & 255) / 255.0F;
      float g = (float)(i >> 8 & 255) / 255.0F;
      float h = (float)(i & 255) / 255.0F;
      BlockState blockState2 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.DOWN));
      FluidState fluidState2 = blockState2.getFluidState();
      BlockState blockState3 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.UP));
      FluidState fluidState3 = blockState3.getFluidState();
      BlockState blockState4 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.NORTH));
      FluidState fluidState4 = blockState4.getFluidState();
      BlockState blockState5 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.SOUTH));
      FluidState fluidState5 = blockState5.getFluidState();
      BlockState blockState6 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.WEST));
      FluidState fluidState6 = blockState6.getFluidState();
      BlockState blockState7 = blockAndTintGetter.getBlockState(blockPos.relative(Direction.EAST));
      FluidState fluidState7 = blockState7.getFluidState();
      boolean bl2 = !isNeighborSameFluid(fluidState, fluidState3);
      boolean bl3 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.DOWN, fluidState2) && !isFaceOccludedByNeighbor(blockAndTintGetter, blockPos, Direction.DOWN, 0.8888889F, blockState2);
      boolean bl4 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.NORTH, fluidState4);
      boolean bl5 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.SOUTH, fluidState5);
      boolean bl6 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.WEST, fluidState6);
      boolean bl7 = shouldRenderFace(blockAndTintGetter, blockPos, fluidState, blockState, Direction.EAST, fluidState7);
      if (bl2 || bl3 || bl7 || bl6 || bl4 || bl5) {
         float j = blockAndTintGetter.getShade(Direction.DOWN, true);
         float k = blockAndTintGetter.getShade(Direction.UP, true);
         float l = blockAndTintGetter.getShade(Direction.NORTH, true);
         float m = blockAndTintGetter.getShade(Direction.WEST, true);
         Fluid fluid = fluidState.getType();
         float n = this.getHeight(blockAndTintGetter, fluid, blockPos, blockState, fluidState);
         float o;
         float p;
         float q;
         float r;
         float s;
         float t;
         float u;
         float v;
         if (n >= 1.0F) {
            o = 1.0F;
            p = 1.0F;
            q = 1.0F;
            r = 1.0F;
         } else {
            s = this.getHeight(blockAndTintGetter, fluid, blockPos.north(), blockState4, fluidState4);
            t = this.getHeight(blockAndTintGetter, fluid, blockPos.south(), blockState5, fluidState5);
            u = this.getHeight(blockAndTintGetter, fluid, blockPos.east(), blockState7, fluidState7);
            v = this.getHeight(blockAndTintGetter, fluid, blockPos.west(), blockState6, fluidState6);
            o = this.calculateAverageHeight(blockAndTintGetter, fluid, n, s, u, blockPos.relative(Direction.NORTH).relative(Direction.EAST));
            p = this.calculateAverageHeight(blockAndTintGetter, fluid, n, s, v, blockPos.relative(Direction.NORTH).relative(Direction.WEST));
            q = this.calculateAverageHeight(blockAndTintGetter, fluid, n, t, u, blockPos.relative(Direction.SOUTH).relative(Direction.EAST));
            r = this.calculateAverageHeight(blockAndTintGetter, fluid, n, t, v, blockPos.relative(Direction.SOUTH).relative(Direction.WEST));
         }

         s = (float)(blockPos.getX() & 15);
         t = (float)(blockPos.getY() & 15);
         u = (float)(blockPos.getZ() & 15);
         v = 0.001F;
         float w = bl3 ? 0.001F : 0.0F;
         float x;
         float z;
         float ab;
         float ad;
         float y;
         float aa;
         float ac;
         float ae;
         float ah;
         float ai;
         float al;
         float am;
         if (bl2 && !isFaceOccludedByNeighbor(blockAndTintGetter, blockPos, Direction.UP, Math.min(Math.min(p, r), Math.min(q, o)), blockState3)) {
            p -= 0.001F;
            r -= 0.001F;
            q -= 0.001F;
            o -= 0.001F;
            Vec3 vec3 = fluidState.getFlow(blockAndTintGetter, blockPos);
            TextureAtlasSprite textureAtlasSprite;
            float af;
            float ag;
            if (vec3.x == 0.0D && vec3.z == 0.0D) {
               textureAtlasSprite = textureAtlasSprites[0];
               x = textureAtlasSprite.getU(0.0F);
               y = textureAtlasSprite.getV(0.0F);
               z = x;
               aa = textureAtlasSprite.getV(1.0F);
               ab = textureAtlasSprite.getU(1.0F);
               ac = aa;
               ad = ab;
               ae = y;
            } else {
               textureAtlasSprite = textureAtlasSprites[1];
               af = (float)Mth.atan2(vec3.z, vec3.x) - 1.5707964F;
               ag = Mth.sin(af) * 0.25F;
               ah = Mth.cos(af) * 0.25F;
               ai = 0.5F;
               x = textureAtlasSprite.getU(0.5F + (-ah - ag));
               y = textureAtlasSprite.getV(0.5F + -ah + ag);
               z = textureAtlasSprite.getU(0.5F + -ah + ag);
               aa = textureAtlasSprite.getV(0.5F + ah + ag);
               ab = textureAtlasSprite.getU(0.5F + ah + ag);
               ac = textureAtlasSprite.getV(0.5F + (ah - ag));
               ad = textureAtlasSprite.getU(0.5F + (ah - ag));
               ae = textureAtlasSprite.getV(0.5F + (-ah - ag));
            }

            float aj = (x + z + ab + ad) / 4.0F;
            af = (y + aa + ac + ae) / 4.0F;
            ag = textureAtlasSprites[0].uvShrinkRatio();
            x = Mth.lerp(ag, x, aj);
            z = Mth.lerp(ag, z, aj);
            ab = Mth.lerp(ag, ab, aj);
            ad = Mth.lerp(ag, ad, aj);
            y = Mth.lerp(ag, y, af);
            aa = Mth.lerp(ag, aa, af);
            ac = Mth.lerp(ag, ac, af);
            ae = Mth.lerp(ag, ae, af);
            int ak = this.getLightColor(blockAndTintGetter, blockPos);
            ai = k * f;
            al = k * g;
            am = k * h;
            this.vertex(vertexConsumer, s + 0.0F, t + p, u + 0.0F, ai, al, am, x, y, ak);
            this.vertex(vertexConsumer, s + 0.0F, t + r, u + 1.0F, ai, al, am, z, aa, ak);
            this.vertex(vertexConsumer, s + 1.0F, t + q, u + 1.0F, ai, al, am, ab, ac, ak);
            this.vertex(vertexConsumer, s + 1.0F, t + o, u + 0.0F, ai, al, am, ad, ae, ak);
            if (fluidState.shouldRenderBackwardUpFace(blockAndTintGetter, blockPos.above())) {
               this.vertex(vertexConsumer, s + 0.0F, t + p, u + 0.0F, ai, al, am, x, y, ak);
               this.vertex(vertexConsumer, s + 1.0F, t + o, u + 0.0F, ai, al, am, ad, ae, ak);
               this.vertex(vertexConsumer, s + 1.0F, t + q, u + 1.0F, ai, al, am, ab, ac, ak);
               this.vertex(vertexConsumer, s + 0.0F, t + r, u + 1.0F, ai, al, am, z, aa, ak);
            }
         }

         if (bl3) {
            x = textureAtlasSprites[0].getU0();
            z = textureAtlasSprites[0].getU1();
            ab = textureAtlasSprites[0].getV0();
            ad = textureAtlasSprites[0].getV1();
            int an = this.getLightColor(blockAndTintGetter, blockPos.below());
            aa = j * f;
            ac = j * g;
            ae = j * h;
            this.vertex(vertexConsumer, s, t + w, u + 1.0F, aa, ac, ae, x, ad, an);
            this.vertex(vertexConsumer, s, t + w, u, aa, ac, ae, x, ab, an);
            this.vertex(vertexConsumer, s + 1.0F, t + w, u, aa, ac, ae, z, ab, an);
            this.vertex(vertexConsumer, s + 1.0F, t + w, u + 1.0F, aa, ac, ae, z, ad, an);
         }

         int ao = this.getLightColor(blockAndTintGetter, blockPos);
         Iterator var67 = Plane.HORIZONTAL.iterator();

         while(true) {
            Direction direction;
            float ap;
            boolean bl8;
            do {
               do {
                  if (!var67.hasNext()) {
                     return;
                  }

                  direction = (Direction)var67.next();
                  switch(direction) {
                  case NORTH:
                     ad = p;
                     y = o;
                     aa = s;
                     ae = s + 1.0F;
                     ac = u + 0.001F;
                     ap = u + 0.001F;
                     bl8 = bl4;
                     break;
                  case SOUTH:
                     ad = q;
                     y = r;
                     aa = s + 1.0F;
                     ae = s;
                     ac = u + 1.0F - 0.001F;
                     ap = u + 1.0F - 0.001F;
                     bl8 = bl5;
                     break;
                  case WEST:
                     ad = r;
                     y = p;
                     aa = s + 0.001F;
                     ae = s + 0.001F;
                     ac = u + 1.0F;
                     ap = u;
                     bl8 = bl6;
                     break;
                  default:
                     ad = o;
                     y = q;
                     aa = s + 1.0F - 0.001F;
                     ae = s + 1.0F - 0.001F;
                     ac = u;
                     ap = u + 1.0F;
                     bl8 = bl7;
                  }
               } while(!bl8);
            } while(isFaceOccludedByNeighbor(blockAndTintGetter, blockPos, direction, Math.max(ad, y), blockAndTintGetter.getBlockState(blockPos.relative(direction))));

            BlockPos blockPos2 = blockPos.relative(direction);
            TextureAtlasSprite textureAtlasSprite2 = textureAtlasSprites[1];
            if (!bl) {
               Block block = blockAndTintGetter.getBlockState(blockPos2).getBlock();
               if (block instanceof HalfTransparentBlock || block instanceof LeavesBlock) {
                  textureAtlasSprite2 = this.waterOverlay;
               }
            }

            ah = textureAtlasSprite2.getU(0.0F);
            ai = textureAtlasSprite2.getU(0.5F);
            al = textureAtlasSprite2.getV((1.0F - ad) * 0.5F);
            am = textureAtlasSprite2.getV((1.0F - y) * 0.5F);
            float aq = textureAtlasSprite2.getV(0.5F);
            float ar = direction.getAxis() == Axis.Z ? l : m;
            float as = k * ar * f;
            float at = k * ar * g;
            float au = k * ar * h;
            this.vertex(vertexConsumer, aa, t + ad, ac, as, at, au, ah, al, ao);
            this.vertex(vertexConsumer, ae, t + y, ap, as, at, au, ai, am, ao);
            this.vertex(vertexConsumer, ae, t + w, ap, as, at, au, ai, aq, ao);
            this.vertex(vertexConsumer, aa, t + w, ac, as, at, au, ah, aq, ao);
            if (textureAtlasSprite2 != this.waterOverlay) {
               this.vertex(vertexConsumer, aa, t + w, ac, as, at, au, ah, aq, ao);
               this.vertex(vertexConsumer, ae, t + w, ap, as, at, au, ai, aq, ao);
               this.vertex(vertexConsumer, ae, t + y, ap, as, at, au, ai, am, ao);
               this.vertex(vertexConsumer, aa, t + ad, ac, as, at, au, ah, al, ao);
            }
         }
      }
   }

   private float calculateAverageHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, float f, float g, float h, BlockPos blockPos) {
      if (!(h >= 1.0F) && !(g >= 1.0F)) {
         float[] fs = new float[2];
         if (h > 0.0F || g > 0.0F) {
            float i = this.getHeight(blockAndTintGetter, fluid, blockPos);
            if (i >= 1.0F) {
               return 1.0F;
            }

            this.addWeightedHeight(fs, i);
         }

         this.addWeightedHeight(fs, f);
         this.addWeightedHeight(fs, h);
         this.addWeightedHeight(fs, g);
         return fs[0] / fs[1];
      } else {
         return 1.0F;
      }
   }

   private void addWeightedHeight(float[] fs, float f) {
      if (f >= 0.8F) {
         fs[0] += f * 10.0F;
         fs[1] += 10.0F;
      } else if (f >= 0.0F) {
         fs[0] += f;
         int var10002 = fs[1]++;
      }

   }

   private float getHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, BlockPos blockPos) {
      BlockState blockState = blockAndTintGetter.getBlockState(blockPos);
      return this.getHeight(blockAndTintGetter, fluid, blockPos, blockState, blockState.getFluidState());
   }

   private float getHeight(BlockAndTintGetter blockAndTintGetter, Fluid fluid, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
      if (fluid.isSame(fluidState.getType())) {
         BlockState blockState2 = blockAndTintGetter.getBlockState(blockPos.above());
         return fluid.isSame(blockState2.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
      } else {
         return !blockState.isSolid() ? 0.0F : -1.0F;
      }
   }

   private void vertex(VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, float k, float l, float m, int n) {
      vertexConsumer.addVertex(f, g, h).setColor(i, j, k, 1.0F).setUv(l, m).setLight(n).setNormal(0.0F, 1.0F, 0.0F);
   }

   private int getLightColor(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos) {
      int i = LevelRenderer.getLightColor(blockAndTintGetter, blockPos);
      int j = LevelRenderer.getLightColor(blockAndTintGetter, blockPos.above());
      int k = i & 255;
      int l = j & 255;
      int m = i >> 16 & 255;
      int n = j >> 16 & 255;
      return (k > l ? k : l) | (m > n ? m : n) << 16;
   }
}
