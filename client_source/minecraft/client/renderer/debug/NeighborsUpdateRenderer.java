package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

@Environment(EnvType.CLIENT)
public class NeighborsUpdateRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private final Map<Long, Map<BlockPos, Integer>> lastUpdate = Maps.newTreeMap(Ordering.natural().reverse());

   NeighborsUpdateRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void addUpdate(long l, BlockPos blockPos) {
      Map<BlockPos, Integer> map = (Map)this.lastUpdate.computeIfAbsent(l, (long_) -> {
         return Maps.newHashMap();
      });
      int i = (Integer)map.getOrDefault(blockPos, 0);
      map.put(blockPos, i + 1);
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      long l = this.minecraft.level.getGameTime();
      int i = true;
      double g = 0.0025D;
      Set<BlockPos> set = Sets.newHashSet();
      Map<BlockPos, Integer> map = Maps.newHashMap();
      VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.lines());
      Iterator iterator = this.lastUpdate.entrySet().iterator();

      while(true) {
         Entry entry;
         while(iterator.hasNext()) {
            entry = (Entry)iterator.next();
            Long long_ = (Long)entry.getKey();
            Map<BlockPos, Integer> map2 = (Map)entry.getValue();
            long m = l - long_;
            if (m > 200L) {
               iterator.remove();
            } else {
               Iterator var23 = map2.entrySet().iterator();

               while(var23.hasNext()) {
                  Entry<BlockPos, Integer> entry2 = (Entry)var23.next();
                  BlockPos blockPos = (BlockPos)entry2.getKey();
                  Integer integer = (Integer)entry2.getValue();
                  if (set.add(blockPos)) {
                     AABB aABB = (new AABB(BlockPos.ZERO)).inflate(0.002D).deflate(0.0025D * (double)m).move((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ()).move(-d, -e, -f);
                     LevelRenderer.renderLineBox(poseStack, vertexConsumer, aABB.minX, aABB.minY, aABB.minZ, aABB.maxX, aABB.maxY, aABB.maxZ, 1.0F, 1.0F, 1.0F, 1.0F);
                     map.put(blockPos, integer);
                  }
               }
            }
         }

         iterator = map.entrySet().iterator();

         while(iterator.hasNext()) {
            entry = (Entry)iterator.next();
            BlockPos blockPos2 = (BlockPos)entry.getKey();
            Integer integer2 = (Integer)entry.getValue();
            DebugRenderer.renderFloatingText(poseStack, multiBufferSource, String.valueOf(integer2), blockPos2.getX(), blockPos2.getY(), blockPos2.getZ(), -1);
         }

         return;
      }
   }
}
