package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class PostPass implements AutoCloseable {
   private final EffectInstance effect;
   public final RenderTarget inTarget;
   public final RenderTarget outTarget;
   private final List<IntSupplier> auxAssets = Lists.newArrayList();
   private final List<String> auxNames = Lists.newArrayList();
   private final List<Integer> auxWidths = Lists.newArrayList();
   private final List<Integer> auxHeights = Lists.newArrayList();
   private Matrix4f shaderOrthoMatrix;
   private final int filterMode;

   public PostPass(ResourceProvider resourceProvider, String string, RenderTarget renderTarget, RenderTarget renderTarget2, boolean bl) throws IOException {
      this.effect = new EffectInstance(resourceProvider, string);
      this.inTarget = renderTarget;
      this.outTarget = renderTarget2;
      this.filterMode = bl ? 9729 : 9728;
   }

   public void close() {
      this.effect.close();
   }

   public final String getName() {
      return this.effect.getName();
   }

   public void addAuxAsset(String string, IntSupplier intSupplier, int i, int j) {
      this.auxNames.add(this.auxNames.size(), string);
      this.auxAssets.add(this.auxAssets.size(), intSupplier);
      this.auxWidths.add(this.auxWidths.size(), i);
      this.auxHeights.add(this.auxHeights.size(), j);
   }

   public void setOrthoMatrix(Matrix4f matrix4f) {
      this.shaderOrthoMatrix = matrix4f;
   }

   public void process(float f) {
      this.inTarget.unbindWrite();
      float g = (float)this.outTarget.width;
      float h = (float)this.outTarget.height;
      RenderSystem.viewport(0, 0, (int)g, (int)h);
      EffectInstance var10000 = this.effect;
      RenderTarget var10002 = this.inTarget;
      Objects.requireNonNull(var10002);
      var10000.setSampler("DiffuseSampler", var10002::getColorTextureId);

      for(int i = 0; i < this.auxAssets.size(); ++i) {
         this.effect.setSampler((String)this.auxNames.get(i), (IntSupplier)this.auxAssets.get(i));
         this.effect.safeGetUniform("AuxSize" + i).set((float)(Integer)this.auxWidths.get(i), (float)(Integer)this.auxHeights.get(i));
      }

      this.effect.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
      this.effect.safeGetUniform("InSize").set((float)this.inTarget.width, (float)this.inTarget.height);
      this.effect.safeGetUniform("OutSize").set(g, h);
      this.effect.safeGetUniform("Time").set(f);
      Minecraft minecraft = Minecraft.getInstance();
      this.effect.safeGetUniform("ScreenSize").set((float)minecraft.getWindow().getWidth(), (float)minecraft.getWindow().getHeight());
      this.effect.apply();
      this.outTarget.clear(Minecraft.ON_OSX);
      this.outTarget.bindWrite(false);
      RenderSystem.depthFunc(519);
      BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.addVertex(0.0F, 0.0F, 500.0F);
      bufferBuilder.addVertex(g, 0.0F, 500.0F);
      bufferBuilder.addVertex(g, h, 500.0F);
      bufferBuilder.addVertex(0.0F, h, 500.0F);
      BufferUploader.draw(bufferBuilder.buildOrThrow());
      RenderSystem.depthFunc(515);
      this.effect.clear();
      this.outTarget.unbindWrite();
      this.inTarget.unbindRead();
      Iterator var6 = this.auxAssets.iterator();

      while(var6.hasNext()) {
         Object object = var6.next();
         if (object instanceof RenderTarget) {
            ((RenderTarget)object).unbindRead();
         }
      }

   }

   public EffectInstance getEffect() {
      return this.effect;
   }

   public int getFilterMode() {
      return this.filterMode;
   }
}
