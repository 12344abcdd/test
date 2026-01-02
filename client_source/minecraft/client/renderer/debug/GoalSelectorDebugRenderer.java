package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload.DebugGoal;

@Environment(EnvType.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private final Minecraft minecraft;
   private final Int2ObjectMap<GoalSelectorDebugRenderer.EntityGoalInfo> goalSelectors = new Int2ObjectOpenHashMap();

   public void clear() {
      this.goalSelectors.clear();
   }

   public void addGoalSelector(int i, BlockPos blockPos, List<DebugGoal> list) {
      this.goalSelectors.put(i, new GoalSelectorDebugRenderer.EntityGoalInfo(blockPos, list));
   }

   public void removeGoalSelector(int i) {
      this.goalSelectors.remove(i);
   }

   public GoalSelectorDebugRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      BlockPos blockPos = BlockPos.containing(camera.getPosition().x, 0.0D, camera.getPosition().z);
      ObjectIterator var11 = this.goalSelectors.values().iterator();

      while(true) {
         GoalSelectorDebugRenderer.EntityGoalInfo entityGoalInfo;
         BlockPos blockPos2;
         do {
            if (!var11.hasNext()) {
               return;
            }

            entityGoalInfo = (GoalSelectorDebugRenderer.EntityGoalInfo)var11.next();
            blockPos2 = entityGoalInfo.entityPos;
         } while(!blockPos.closerThan(blockPos2, 160.0D));

         for(int i = 0; i < entityGoalInfo.goals.size(); ++i) {
            DebugGoal debugGoal = (DebugGoal)entityGoalInfo.goals.get(i);
            double g = (double)blockPos2.getX() + 0.5D;
            double h = (double)blockPos2.getY() + 2.0D + (double)i * 0.25D;
            double j = (double)blockPos2.getZ() + 0.5D;
            int k = debugGoal.isRunning() ? -16711936 : -3355444;
            DebugRenderer.renderFloatingText(poseStack, multiBufferSource, debugGoal.name(), g, h, j, k);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static record EntityGoalInfo(BlockPos entityPos, List<DebugGoal> goals) {
      final BlockPos entityPos;
      final List<DebugGoal> goals;

      EntityGoalInfo(BlockPos blockPos, List<DebugGoal> list) {
         this.entityPos = blockPos;
         this.goals = list;
      }

      public BlockPos entityPos() {
         return this.entityPos;
      }

      public List<DebugGoal> goals() {
         return this.goals;
      }
   }
}
