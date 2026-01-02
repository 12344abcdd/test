package net.minecraft.client.animation;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class KeyframeAnimations {
   public static void animate(HierarchicalModel<?> hierarchicalModel, AnimationDefinition animationDefinition, long l, float f, Vector3f vector3f) {
      float g = getElapsedSeconds(animationDefinition, l);
      Iterator var7 = animationDefinition.boneAnimations().entrySet().iterator();

      while(var7.hasNext()) {
         Entry<String, List<AnimationChannel>> entry = (Entry)var7.next();
         Optional<ModelPart> optional = hierarchicalModel.getAnyDescendantWithName((String)entry.getKey());
         List<AnimationChannel> list = (List)entry.getValue();
         optional.ifPresent((modelPart) -> {
            list.forEach((animationChannel) -> {
               Keyframe[] keyframes = animationChannel.keyframes();
               int i = Math.max(0, Mth.binarySearch(0, keyframes.length, (ix) -> {
                  return g <= keyframes[ix].timestamp();
               }) - 1);
               int j = Math.min(keyframes.length - 1, i + 1);
               Keyframe keyframe = keyframes[i];
               Keyframe keyframe2 = keyframes[j];
               float h = g - keyframe.timestamp();
               float k;
               if (j != i) {
                  k = Mth.clamp(h / (keyframe2.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
               } else {
                  k = 0.0F;
               }

               keyframe2.interpolation().apply(vector3f, k, keyframes, i, j, f);
               animationChannel.target().apply(modelPart, vector3f);
            });
         });
      }

   }

   private static float getElapsedSeconds(AnimationDefinition animationDefinition, long l) {
      float f = (float)l / 1000.0F;
      return animationDefinition.looping() ? f % animationDefinition.lengthInSeconds() : f;
   }

   public static Vector3f posVec(float f, float g, float h) {
      return new Vector3f(f, -g, h);
   }

   public static Vector3f degreeVec(float f, float g, float h) {
      return new Vector3f(f * 0.017453292F, g * 0.017453292F, h * 0.017453292F);
   }

   public static Vector3f scaleVec(double d, double e, double f) {
      return new Vector3f((float)(d - 1.0D), (float)(e - 1.0D), (float)(f - 1.0D));
   }
}
