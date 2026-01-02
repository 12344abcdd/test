package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

@Environment(EnvType.CLIENT)
public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int LISTENER_RENDER_DIST = 32;
   private static final float BOX_HEIGHT = 1.0F;
   private final List<GameEventListenerRenderer.TrackedGameEvent> trackedGameEvents = Lists.newArrayList();
   private final List<GameEventListenerRenderer.TrackedListener> trackedListeners = Lists.newArrayList();

   public GameEventListenerRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      Level level = this.minecraft.level;
      if (level == null) {
         this.trackedGameEvents.clear();
         this.trackedListeners.clear();
      } else {
         Vec3 vec3 = new Vec3(d, 0.0D, f);
         this.trackedGameEvents.removeIf(GameEventListenerRenderer.TrackedGameEvent::isExpired);
         this.trackedListeners.removeIf((trackedListenerx) -> {
            return trackedListenerx.isExpired(level, vec3);
         });
         VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.lines());
         Iterator var12 = this.trackedListeners.iterator();

         while(var12.hasNext()) {
            GameEventListenerRenderer.TrackedListener trackedListener = (GameEventListenerRenderer.TrackedListener)var12.next();
            trackedListener.getPosition(level).ifPresent((vec3x) -> {
               double g = vec3x.x() - (double)trackedListener.getListenerRadius();
               double h = vec3x.y() - (double)trackedListener.getListenerRadius();
               double i = vec3x.z() - (double)trackedListener.getListenerRadius();
               double j = vec3x.x() + (double)trackedListener.getListenerRadius();
               double k = vec3x.y() + (double)trackedListener.getListenerRadius();
               double l = vec3x.z() + (double)trackedListener.getListenerRadius();
               LevelRenderer.renderVoxelShape(poseStack, vertexConsumer, Shapes.create(new AABB(g, h, i, j, k, l)), -d, -e, -f, 1.0F, 1.0F, 0.0F, 0.35F, true);
            });
         }

         VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(RenderType.debugFilledBox());
         Iterator var31 = this.trackedListeners.iterator();

         GameEventListenerRenderer.TrackedListener trackedListener2;
         while(var31.hasNext()) {
            trackedListener2 = (GameEventListenerRenderer.TrackedListener)var31.next();
            trackedListener2.getPosition(level).ifPresent((vec3x) -> {
               LevelRenderer.addChainedFilledBoxVertices(poseStack, vertexConsumer2, vec3x.x() - 0.25D - d, vec3x.y() - e, vec3x.z() - 0.25D - f, vec3x.x() + 0.25D - d, vec3x.y() - e + 1.0D, vec3x.z() + 0.25D - f, 1.0F, 1.0F, 0.0F, 0.35F);
            });
         }

         var31 = this.trackedListeners.iterator();

         while(var31.hasNext()) {
            trackedListener2 = (GameEventListenerRenderer.TrackedListener)var31.next();
            trackedListener2.getPosition(level).ifPresent((vec3x) -> {
               DebugRenderer.renderFloatingText(poseStack, multiBufferSource, "Listener Origin", vec3x.x(), vec3x.y() + 1.7999999523162842D, vec3x.z(), -1, 0.025F);
               DebugRenderer.renderFloatingText(poseStack, multiBufferSource, BlockPos.containing(vec3x).toString(), vec3x.x(), vec3x.y() + 1.5D, vec3x.z(), -6959665, 0.025F);
            });
         }

         var31 = this.trackedGameEvents.iterator();

         while(var31.hasNext()) {
            GameEventListenerRenderer.TrackedGameEvent trackedGameEvent = (GameEventListenerRenderer.TrackedGameEvent)var31.next();
            Vec3 vec32 = trackedGameEvent.position;
            double g = 0.20000000298023224D;
            double h = vec32.x - 0.20000000298023224D;
            double i = vec32.y - 0.20000000298023224D;
            double j = vec32.z - 0.20000000298023224D;
            double k = vec32.x + 0.20000000298023224D;
            double l = vec32.y + 0.20000000298023224D + 0.5D;
            double m = vec32.z + 0.20000000298023224D;
            renderFilledBox(poseStack, multiBufferSource, new AABB(h, i, j, k, l, m), 1.0F, 1.0F, 1.0F, 0.2F);
            DebugRenderer.renderFloatingText(poseStack, multiBufferSource, trackedGameEvent.gameEvent.location().toString(), vec32.x, vec32.y + 0.8500000238418579D, vec32.z, -7564911, 0.0075F);
         }

      }
   }

   private static void renderFilledBox(PoseStack poseStack, MultiBufferSource multiBufferSource, AABB aABB, float f, float g, float h, float i) {
      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      if (camera.isInitialized()) {
         Vec3 vec3 = camera.getPosition().reverse();
         DebugRenderer.renderFilledBox(poseStack, multiBufferSource, aABB.move(vec3), f, g, h, i);
      }
   }

   public void trackGameEvent(ResourceKey<GameEvent> resourceKey, Vec3 vec3) {
      this.trackedGameEvents.add(new GameEventListenerRenderer.TrackedGameEvent(Util.getMillis(), resourceKey, vec3));
   }

   public void trackListener(PositionSource positionSource, int i) {
      this.trackedListeners.add(new GameEventListenerRenderer.TrackedListener(positionSource, i));
   }

   @Environment(EnvType.CLIENT)
   private static class TrackedListener implements GameEventListener {
      public final PositionSource listenerSource;
      public final int listenerRange;

      public TrackedListener(PositionSource positionSource, int i) {
         this.listenerSource = positionSource;
         this.listenerRange = i;
      }

      public boolean isExpired(Level level, Vec3 vec3) {
         return this.listenerSource.getPosition(level).filter((vec32) -> {
            return vec32.distanceToSqr(vec3) <= 1024.0D;
         }).isPresent();
      }

      public Optional<Vec3> getPosition(Level level) {
         return this.listenerSource.getPosition(level);
      }

      public PositionSource getListenerSource() {
         return this.listenerSource;
      }

      public int getListenerRadius() {
         return this.listenerRange;
      }

      public boolean handleGameEvent(ServerLevel serverLevel, Holder<GameEvent> holder, Context context, Vec3 vec3) {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   private static record TrackedGameEvent(long timeStamp, ResourceKey<GameEvent> gameEvent, Vec3 position) {
      final ResourceKey<GameEvent> gameEvent;
      final Vec3 position;

      TrackedGameEvent(long l, ResourceKey<GameEvent> resourceKey, Vec3 vec3) {
         this.timeStamp = l;
         this.gameEvent = resourceKey;
         this.position = vec3;
      }

      public boolean isExpired() {
         return Util.getMillis() - this.timeStamp > 3000L;
      }

      public long timeStamp() {
         return this.timeStamp;
      }

      public ResourceKey<GameEvent> gameEvent() {
         return this.gameEvent;
      }

      public Vec3 position() {
         return this.position;
      }
   }
}
