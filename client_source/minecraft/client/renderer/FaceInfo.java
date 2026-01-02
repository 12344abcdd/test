package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
public enum FaceInfo {
   DOWN(new FaceInfo.VertexInfo[]{new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MAX_Z)}),
   UP(new FaceInfo.VertexInfo[]{new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MIN_Z)}),
   NORTH(new FaceInfo.VertexInfo[]{new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MIN_Z)}),
   SOUTH(new FaceInfo.VertexInfo[]{new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z)}),
   WEST(new FaceInfo.VertexInfo[]{new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MIN_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z)}),
   EAST(new FaceInfo.VertexInfo[]{new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MAX_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MIN_Y, FaceInfo.Constants.MIN_Z), new FaceInfo.VertexInfo(FaceInfo.Constants.MAX_X, FaceInfo.Constants.MAX_Y, FaceInfo.Constants.MIN_Z)});

   private static final FaceInfo[] BY_FACING = (FaceInfo[])Util.make(new FaceInfo[6], (faceInfos) -> {
      faceInfos[FaceInfo.Constants.MIN_Y] = DOWN;
      faceInfos[FaceInfo.Constants.MAX_Y] = UP;
      faceInfos[FaceInfo.Constants.MIN_Z] = NORTH;
      faceInfos[FaceInfo.Constants.MAX_Z] = SOUTH;
      faceInfos[FaceInfo.Constants.MIN_X] = WEST;
      faceInfos[FaceInfo.Constants.MAX_X] = EAST;
   });
   private final FaceInfo.VertexInfo[] infos;

   public static FaceInfo fromFacing(Direction direction) {
      return BY_FACING[direction.get3DDataValue()];
   }

   private FaceInfo(final FaceInfo.VertexInfo... vertexInfos) {
      this.infos = vertexInfos;
   }

   public FaceInfo.VertexInfo getVertexInfo(int i) {
      return this.infos[i];
   }

   // $FF: synthetic method
   private static FaceInfo[] $values() {
      return new FaceInfo[]{DOWN, UP, NORTH, SOUTH, WEST, EAST};
   }

   @Environment(EnvType.CLIENT)
   public static class VertexInfo {
      public final int xFace;
      public final int yFace;
      public final int zFace;

      VertexInfo(int i, int j, int k) {
         this.xFace = i;
         this.yFace = j;
         this.zFace = k;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Constants {
      public static final int MAX_Z;
      public static final int MAX_Y;
      public static final int MAX_X;
      public static final int MIN_Z;
      public static final int MIN_Y;
      public static final int MIN_X;

      static {
         MAX_Z = Direction.SOUTH.get3DDataValue();
         MAX_Y = Direction.UP.get3DDataValue();
         MAX_X = Direction.EAST.get3DDataValue();
         MIN_Z = Direction.NORTH.get3DDataValue();
         MIN_Y = Direction.DOWN.get3DDataValue();
         MIN_X = Direction.WEST.get3DDataValue();
      }
   }
}
