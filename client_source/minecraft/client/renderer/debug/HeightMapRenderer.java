package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class HeightMapRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CHUNK_DIST = 2;
   private static final float BOX_HEIGHT = 0.09375F;

   public HeightMapRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      LevelAccessor levelAccessor = this.minecraft.level;
      VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.debugFilledBox());
      BlockPos blockPos = BlockPos.containing(d, 0.0D, f);

      for(int i = -2; i <= 2; ++i) {
         for(int j = -2; j <= 2; ++j) {
            ChunkAccess chunkAccess = levelAccessor.getChunk(blockPos.offset(i * 16, 0, j * 16));
            Iterator var15 = chunkAccess.getHeightmaps().iterator();

            while(var15.hasNext()) {
               Entry<Types, Heightmap> entry = (Entry)var15.next();
               Types types = (Types)entry.getKey();
               ChunkPos chunkPos = chunkAccess.getPos();
               Vector3f vector3f = this.getColor(types);

               for(int k = 0; k < 16; ++k) {
                  for(int l = 0; l < 16; ++l) {
                     int m = SectionPos.sectionToBlockCoord(chunkPos.x, k);
                     int n = SectionPos.sectionToBlockCoord(chunkPos.z, l);
                     float g = (float)((double)((float)levelAccessor.getHeight(types, m, n) + (float)types.ordinal() * 0.09375F) - e);
                     LevelRenderer.addChainedFilledBoxVertices(poseStack, vertexConsumer, (double)((float)m + 0.25F) - d, (double)g, (double)((float)n + 0.25F) - f, (double)((float)m + 0.75F) - d, (double)(g + 0.09375F), (double)((float)n + 0.75F) - f, vector3f.x(), vector3f.y(), vector3f.z(), 1.0F);
                  }
               }
            }
         }
      }

   }

   private Vector3f getColor(Types types) {
      Vector3f var10000;
      switch(types) {
      case WORLD_SURFACE_WG:
         var10000 = new Vector3f(1.0F, 1.0F, 0.0F);
         break;
      case OCEAN_FLOOR_WG:
         var10000 = new Vector3f(1.0F, 0.0F, 1.0F);
         break;
      case WORLD_SURFACE:
         var10000 = new Vector3f(0.0F, 0.7F, 0.0F);
         break;
      case OCEAN_FLOOR:
         var10000 = new Vector3f(0.0F, 0.0F, 0.5F);
         break;
      case MOTION_BLOCKING:
         var10000 = new Vector3f(0.0F, 0.3F, 0.3F);
         break;
      case MOTION_BLOCKING_NO_LEAVES:
         var10000 = new Vector3f(0.0F, 0.5F, 0.5F);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
