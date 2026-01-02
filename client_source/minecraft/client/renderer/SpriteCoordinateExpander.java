package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Environment(EnvType.CLIENT)
public class SpriteCoordinateExpander implements VertexConsumer {
   private final VertexConsumer delegate;
   private final TextureAtlasSprite sprite;

   public SpriteCoordinateExpander(VertexConsumer vertexConsumer, TextureAtlasSprite textureAtlasSprite) {
      this.delegate = vertexConsumer;
      this.sprite = textureAtlasSprite;
   }

   public VertexConsumer addVertex(float f, float g, float h) {
      return this.delegate.addVertex(f, g, h);
   }

   public VertexConsumer setColor(int i, int j, int k, int l) {
      return this.delegate.setColor(i, j, k, l);
   }

   public VertexConsumer setUv(float f, float g) {
      return this.delegate.setUv(this.sprite.getU(f), this.sprite.getV(g));
   }

   public VertexConsumer setUv1(int i, int j) {
      return this.delegate.setUv1(i, j);
   }

   public VertexConsumer setUv2(int i, int j) {
      return this.delegate.setUv2(i, j);
   }

   public VertexConsumer setNormal(float f, float g, float h) {
      return this.delegate.setNormal(f, g, h);
   }

   public void addVertex(float f, float g, float h, int i, float j, float k, int l, int m, float n, float o, float p) {
      this.delegate.addVertex(f, g, h, i, this.sprite.getU(j), this.sprite.getV(k), l, m, n, o, p);
   }
}
