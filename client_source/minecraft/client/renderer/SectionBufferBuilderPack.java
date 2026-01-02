package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;

@Environment(EnvType.CLIENT)
public class SectionBufferBuilderPack implements AutoCloseable {
   private static final List<RenderType> RENDER_TYPES = RenderType.chunkBufferLayers();
   public static final int TOTAL_BUFFERS_SIZE;
   private final Map<RenderType, ByteBufferBuilder> buffers;

   public SectionBufferBuilderPack() {
      this.buffers = (Map)Util.make(new Reference2ObjectArrayMap(RENDER_TYPES.size()), (reference2ObjectArrayMap) -> {
         Iterator var1 = RENDER_TYPES.iterator();

         while(var1.hasNext()) {
            RenderType renderType = (RenderType)var1.next();
            reference2ObjectArrayMap.put(renderType, new ByteBufferBuilder(renderType.bufferSize()));
         }

      });
   }

   public ByteBufferBuilder buffer(RenderType renderType) {
      return (ByteBufferBuilder)this.buffers.get(renderType);
   }

   public void clearAll() {
      this.buffers.values().forEach(ByteBufferBuilder::clear);
   }

   public void discardAll() {
      this.buffers.values().forEach(ByteBufferBuilder::discard);
   }

   public void close() {
      this.buffers.values().forEach(ByteBufferBuilder::close);
   }

   static {
      TOTAL_BUFFERS_SIZE = RENDER_TYPES.stream().mapToInt(RenderType::bufferSize).sum();
   }
}
