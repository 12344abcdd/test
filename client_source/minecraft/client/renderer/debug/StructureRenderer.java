package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload.PieceInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Environment(EnvType.CLIENT)
public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private final Map<ResourceKey<Level>, Map<String, BoundingBox>> postMainBoxes = Maps.newIdentityHashMap();
   private final Map<ResourceKey<Level>, Map<String, PieceInfo>> postPieces = Maps.newIdentityHashMap();
   private static final int MAX_RENDER_DIST = 500;

   public StructureRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      ResourceKey<Level> resourceKey = this.minecraft.level.dimension();
      BlockPos blockPos = BlockPos.containing(camera.getPosition().x, 0.0D, camera.getPosition().z);
      VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.lines());
      if (this.postMainBoxes.containsKey(resourceKey)) {
         Iterator var13 = ((Map)this.postMainBoxes.get(resourceKey)).values().iterator();

         while(var13.hasNext()) {
            BoundingBox boundingBox = (BoundingBox)var13.next();
            if (blockPos.closerThan(boundingBox.getCenter(), 500.0D)) {
               LevelRenderer.renderLineBox(poseStack, vertexConsumer, (double)boundingBox.minX() - d, (double)boundingBox.minY() - e, (double)boundingBox.minZ() - f, (double)(boundingBox.maxX() + 1) - d, (double)(boundingBox.maxY() + 1) - e, (double)(boundingBox.maxZ() + 1) - f, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }

      Map<String, PieceInfo> map = (Map)this.postPieces.get(resourceKey);
      if (map != null) {
         Iterator var18 = map.values().iterator();

         while(var18.hasNext()) {
            PieceInfo pieceInfo = (PieceInfo)var18.next();
            BoundingBox boundingBox2 = pieceInfo.boundingBox();
            if (blockPos.closerThan(boundingBox2.getCenter(), 500.0D)) {
               if (pieceInfo.isStart()) {
                  LevelRenderer.renderLineBox(poseStack, vertexConsumer, (double)boundingBox2.minX() - d, (double)boundingBox2.minY() - e, (double)boundingBox2.minZ() - f, (double)(boundingBox2.maxX() + 1) - d, (double)(boundingBox2.maxY() + 1) - e, (double)(boundingBox2.maxZ() + 1) - f, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
               } else {
                  LevelRenderer.renderLineBox(poseStack, vertexConsumer, (double)boundingBox2.minX() - d, (double)boundingBox2.minY() - e, (double)boundingBox2.minZ() - f, (double)(boundingBox2.maxX() + 1) - d, (double)(boundingBox2.maxY() + 1) - e, (double)(boundingBox2.maxZ() + 1) - f, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
               }
            }
         }
      }

   }

   public void addBoundingBox(BoundingBox boundingBox, List<PieceInfo> list, ResourceKey<Level> resourceKey) {
      ((Map)this.postMainBoxes.computeIfAbsent(resourceKey, (resourceKeyx) -> {
         return new HashMap();
      })).put(boundingBox.toString(), boundingBox);
      Map<String, PieceInfo> map = (Map)this.postPieces.computeIfAbsent(resourceKey, (resourceKeyx) -> {
         return new HashMap();
      });
      Iterator var5 = list.iterator();

      while(var5.hasNext()) {
         PieceInfo pieceInfo = (PieceInfo)var5.next();
         map.put(pieceInfo.boundingBox().toString(), pieceInfo);
      }

   }

   public void clear() {
      this.postMainBoxes.clear();
      this.postPieces.clear();
   }
}
