package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChunkDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private final int radius = 12;
   @Nullable
   private ChunkDebugRenderer.ChunkData data;

   public ChunkDebugRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, double d, double e, double f) {
      double g = (double)Util.getNanos();
      if (g - this.lastUpdateTime > 3.0E9D) {
         this.lastUpdateTime = g;
         IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
         if (integratedServer != null) {
            this.data = new ChunkDebugRenderer.ChunkData(this, integratedServer, d, f);
         } else {
            this.data = null;
         }
      }

      if (this.data != null) {
         Map<ChunkPos, String> map = (Map)this.data.serverData.getNow((Object)null);
         double h = this.minecraft.gameRenderer.getMainCamera().getPosition().y * 0.85D;
         Iterator var14 = this.data.clientData.entrySet().iterator();

         while(var14.hasNext()) {
            Entry<ChunkPos, String> entry = (Entry)var14.next();
            ChunkPos chunkPos = (ChunkPos)entry.getKey();
            String string = (String)entry.getValue();
            if (map != null) {
               string = string + (String)map.get(chunkPos);
            }

            String[] strings = string.split("\n");
            int i = 0;
            String[] var20 = strings;
            int var21 = strings.length;

            for(int var22 = 0; var22 < var21; ++var22) {
               String string2 = var20[var22];
               DebugRenderer.renderFloatingText(poseStack, multiBufferSource, string2, (double)SectionPos.sectionToBlockCoord(chunkPos.x, 8), h + (double)i, (double)SectionPos.sectionToBlockCoord(chunkPos.z, 8), -1, 0.15F, true, 0.0F, true);
               i -= 2;
            }
         }
      }

   }

   @Environment(EnvType.CLIENT)
   private final class ChunkData {
      final Map<ChunkPos, String> clientData;
      final CompletableFuture<Map<ChunkPos, String>> serverData;

      ChunkData(final ChunkDebugRenderer chunkDebugRenderer, final IntegratedServer integratedServer, final double d, final double e) {
         ClientLevel clientLevel = chunkDebugRenderer.minecraft.level;
         ResourceKey<Level> resourceKey = clientLevel.dimension();
         int i = SectionPos.posToSectionCoord(d);
         int j = SectionPos.posToSectionCoord(e);
         Builder<ChunkPos, String> builder = ImmutableMap.builder();
         ClientChunkCache clientChunkCache = clientLevel.getChunkSource();

         for(int k = i - 12; k <= i + 12; ++k) {
            for(int l = j - 12; l <= j + 12; ++l) {
               ChunkPos chunkPos = new ChunkPos(k, l);
               String string = "";
               LevelChunk levelChunk = clientChunkCache.getChunk(k, l, false);
               string = string + "Client: ";
               if (levelChunk == null) {
                  string = string + "0n/a\n";
               } else {
                  string = string + (levelChunk.isEmpty() ? " E" : "");
                  string = string + "\n";
               }

               builder.put(chunkPos, string);
            }
         }

         this.clientData = builder.build();
         this.serverData = integratedServer.submit(() -> {
            ServerLevel serverLevel = integratedServer.getLevel(resourceKey);
            if (serverLevel == null) {
               return ImmutableMap.of();
            } else {
               Builder<ChunkPos, String> builder = ImmutableMap.builder();
               ServerChunkCache serverChunkCache = serverLevel.getChunkSource();

               for(int k = i - 12; k <= i + 12; ++k) {
                  for(int l = j - 12; l <= j + 12; ++l) {
                     ChunkPos chunkPos = new ChunkPos(k, l);
                     String var10002 = serverChunkCache.getChunkDebugData(chunkPos);
                     builder.put(chunkPos, "Server: " + var10002);
                  }
               }

               return builder.build();
            }
         });
      }
   }
}
