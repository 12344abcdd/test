package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.FastColor.ARGB32;

@Environment(EnvType.CLIENT)
public class OutlineBufferSource implements MultiBufferSource {
   private final MultiBufferSource.BufferSource bufferSource;
   private final MultiBufferSource.BufferSource outlineBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
   private int teamR = 255;
   private int teamG = 255;
   private int teamB = 255;
   private int teamA = 255;

   public OutlineBufferSource(MultiBufferSource.BufferSource bufferSource) {
      this.bufferSource = bufferSource;
   }

   public VertexConsumer getBuffer(RenderType renderType) {
      VertexConsumer vertexConsumer;
      if (renderType.isOutline()) {
         vertexConsumer = this.outlineBufferSource.getBuffer(renderType);
         return new OutlineBufferSource.EntityOutlineGenerator(vertexConsumer, this.teamR, this.teamG, this.teamB, this.teamA);
      } else {
         vertexConsumer = this.bufferSource.getBuffer(renderType);
         Optional<RenderType> optional = renderType.outline();
         if (optional.isPresent()) {
            VertexConsumer vertexConsumer2 = this.outlineBufferSource.getBuffer((RenderType)optional.get());
            OutlineBufferSource.EntityOutlineGenerator entityOutlineGenerator = new OutlineBufferSource.EntityOutlineGenerator(vertexConsumer2, this.teamR, this.teamG, this.teamB, this.teamA);
            return VertexMultiConsumer.create(entityOutlineGenerator, vertexConsumer);
         } else {
            return vertexConsumer;
         }
      }
   }

   public void setColor(int i, int j, int k, int l) {
      this.teamR = i;
      this.teamG = j;
      this.teamB = k;
      this.teamA = l;
   }

   public void endOutlineBatch() {
      this.outlineBufferSource.endBatch();
   }

   @Environment(EnvType.CLIENT)
   static record EntityOutlineGenerator(VertexConsumer delegate, int color) implements VertexConsumer {
      public EntityOutlineGenerator(VertexConsumer vertexConsumer, int i, int j, int k, int l) {
         this(vertexConsumer, ARGB32.color(l, i, j, k));
      }

      private EntityOutlineGenerator(VertexConsumer vertexConsumer, int i) {
         this.delegate = vertexConsumer;
         this.color = i;
      }

      public VertexConsumer addVertex(float f, float g, float h) {
         this.delegate.addVertex(f, g, h).setColor(this.color);
         return this;
      }

      public VertexConsumer setColor(int i, int j, int k, int l) {
         return this;
      }

      public VertexConsumer setUv(float f, float g) {
         this.delegate.setUv(f, g);
         return this;
      }

      public VertexConsumer setUv1(int i, int j) {
         return this;
      }

      public VertexConsumer setUv2(int i, int j) {
         return this;
      }

      public VertexConsumer setNormal(float f, float g, float h) {
         return this;
      }

      public VertexConsumer delegate() {
         return this.delegate;
      }

      public int color() {
         return this.color;
      }
   }
}
