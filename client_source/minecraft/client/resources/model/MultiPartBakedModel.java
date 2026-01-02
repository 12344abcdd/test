package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MultiPartBakedModel implements BakedModel {
   private final List<Pair<Predicate<BlockState>, BakedModel>> selectors;
   protected final boolean hasAmbientOcclusion;
   protected final boolean isGui3d;
   protected final boolean usesBlockLight;
   protected final TextureAtlasSprite particleIcon;
   protected final ItemTransforms transforms;
   protected final ItemOverrides overrides;
   private final Map<BlockState, BitSet> selectorCache = new Reference2ObjectOpenHashMap();

   public MultiPartBakedModel(List<Pair<Predicate<BlockState>, BakedModel>> list) {
      this.selectors = list;
      BakedModel bakedModel = (BakedModel)((Pair)list.iterator().next()).getRight();
      this.hasAmbientOcclusion = bakedModel.useAmbientOcclusion();
      this.isGui3d = bakedModel.isGui3d();
      this.usesBlockLight = bakedModel.usesBlockLight();
      this.particleIcon = bakedModel.getParticleIcon();
      this.transforms = bakedModel.getTransforms();
      this.overrides = bakedModel.getOverrides();
   }

   public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
      if (blockState == null) {
         return Collections.emptyList();
      } else {
         BitSet bitSet = (BitSet)this.selectorCache.get(blockState);
         if (bitSet == null) {
            bitSet = new BitSet();

            for(int i = 0; i < this.selectors.size(); ++i) {
               Pair<Predicate<BlockState>, BakedModel> pair = (Pair)this.selectors.get(i);
               if (((Predicate)pair.getLeft()).test(blockState)) {
                  bitSet.set(i);
               }
            }

            this.selectorCache.put(blockState, bitSet);
         }

         List<BakedQuad> list = Lists.newArrayList();
         long l = randomSource.nextLong();

         for(int j = 0; j < bitSet.length(); ++j) {
            if (bitSet.get(j)) {
               list.addAll(((BakedModel)((Pair)this.selectors.get(j)).getRight()).getQuads(blockState, direction, RandomSource.create(l)));
            }
         }

         return list;
      }
   }

   public boolean useAmbientOcclusion() {
      return this.hasAmbientOcclusion;
   }

   public boolean isGui3d() {
      return this.isGui3d;
   }

   public boolean usesBlockLight() {
      return this.usesBlockLight;
   }

   public boolean isCustomRenderer() {
      return false;
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.particleIcon;
   }

   public ItemTransforms getTransforms() {
      return this.transforms;
   }

   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final List<Pair<Predicate<BlockState>, BakedModel>> selectors = Lists.newArrayList();

      public void add(Predicate<BlockState> predicate, BakedModel bakedModel) {
         this.selectors.add(Pair.of(predicate, bakedModel));
      }

      public BakedModel build() {
         return new MultiPartBakedModel(this.selectors);
      }
   }
}
