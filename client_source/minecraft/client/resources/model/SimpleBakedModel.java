package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SimpleBakedModel implements BakedModel {
   protected final List<BakedQuad> unculledFaces;
   protected final Map<Direction, List<BakedQuad>> culledFaces;
   protected final boolean hasAmbientOcclusion;
   protected final boolean isGui3d;
   protected final boolean usesBlockLight;
   protected final TextureAtlasSprite particleIcon;
   protected final ItemTransforms transforms;
   protected final ItemOverrides overrides;

   public SimpleBakedModel(List<BakedQuad> list, Map<Direction, List<BakedQuad>> map, boolean bl, boolean bl2, boolean bl3, TextureAtlasSprite textureAtlasSprite, ItemTransforms itemTransforms, ItemOverrides itemOverrides) {
      this.unculledFaces = list;
      this.culledFaces = map;
      this.hasAmbientOcclusion = bl;
      this.isGui3d = bl3;
      this.usesBlockLight = bl2;
      this.particleIcon = textureAtlasSprite;
      this.transforms = itemTransforms;
      this.overrides = itemOverrides;
   }

   public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
      return direction == null ? this.unculledFaces : (List)this.culledFaces.get(direction);
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
      private final List<BakedQuad> unculledFaces;
      private final Map<Direction, List<BakedQuad>> culledFaces;
      private final ItemOverrides overrides;
      private final boolean hasAmbientOcclusion;
      private TextureAtlasSprite particleIcon;
      private final boolean usesBlockLight;
      private final boolean isGui3d;
      private final ItemTransforms transforms;

      public Builder(BlockModel blockModel, ItemOverrides itemOverrides, boolean bl) {
         this(blockModel.hasAmbientOcclusion(), blockModel.getGuiLight().lightLikeBlock(), bl, blockModel.getTransforms(), itemOverrides);
      }

      private Builder(boolean bl, boolean bl2, boolean bl3, ItemTransforms itemTransforms, ItemOverrides itemOverrides) {
         this.unculledFaces = Lists.newArrayList();
         this.culledFaces = Maps.newEnumMap(Direction.class);
         Direction[] var6 = Direction.values();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Direction direction = var6[var8];
            this.culledFaces.put(direction, Lists.newArrayList());
         }

         this.overrides = itemOverrides;
         this.hasAmbientOcclusion = bl;
         this.usesBlockLight = bl2;
         this.isGui3d = bl3;
         this.transforms = itemTransforms;
      }

      public SimpleBakedModel.Builder addCulledFace(Direction direction, BakedQuad bakedQuad) {
         ((List)this.culledFaces.get(direction)).add(bakedQuad);
         return this;
      }

      public SimpleBakedModel.Builder addUnculledFace(BakedQuad bakedQuad) {
         this.unculledFaces.add(bakedQuad);
         return this;
      }

      public SimpleBakedModel.Builder particle(TextureAtlasSprite textureAtlasSprite) {
         this.particleIcon = textureAtlasSprite;
         return this;
      }

      public SimpleBakedModel.Builder item() {
         return this;
      }

      public BakedModel build() {
         if (this.particleIcon == null) {
            throw new RuntimeException("Missing particle!");
         } else {
            return new SimpleBakedModel(this.unculledFaces, this.culledFaces, this.hasAmbientOcclusion, this.usesBlockLight, this.isGui3d, this.particleIcon, this.transforms, this.overrides);
         }
      }
   }
}
