package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SectionRenderDispatcher {
   private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
   private final PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
   private final Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
   private int highPriorityQuota = 2;
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   final SectionBufferBuilderPack fixedBuffers;
   private final SectionBufferBuilderPool bufferPool;
   private volatile int toBatchCount;
   private volatile boolean closed;
   private final ProcessorMailbox<Runnable> mailbox;
   private final Executor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   private Vec3 camera;
   final SectionCompiler sectionCompiler;

   public SectionRenderDispatcher(ClientLevel clientLevel, LevelRenderer levelRenderer, Executor executor, RenderBuffers renderBuffers, BlockRenderDispatcher blockRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
      this.camera = Vec3.ZERO;
      this.level = clientLevel;
      this.renderer = levelRenderer;
      this.fixedBuffers = renderBuffers.fixedBufferPack();
      this.bufferPool = renderBuffers.sectionBufferPool();
      this.executor = executor;
      this.mailbox = ProcessorMailbox.create(executor, "Section Renderer");
      this.mailbox.tell(this::runTask);
      this.sectionCompiler = new SectionCompiler(blockRenderDispatcher, blockEntityRenderDispatcher);
   }

   public void setLevel(ClientLevel clientLevel) {
      this.level = clientLevel;
   }

   private void runTask() {
      if (!this.closed && !this.bufferPool.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask compileTask = this.pollTask();
         if (compileTask != null) {
            SectionBufferBuilderPack sectionBufferBuilderPack = (SectionBufferBuilderPack)Objects.requireNonNull(this.bufferPool.acquire());
            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
            CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName(compileTask.name(), () -> {
               return compileTask.doTask(sectionBufferBuilderPack);
            }), this.executor).thenCompose((completableFuture) -> {
               return completableFuture;
            }).whenComplete((sectionTaskResult, throwable) -> {
               if (throwable != null) {
                  Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwable, "Batching sections"));
               } else {
                  this.mailbox.tell(() -> {
                     if (sectionTaskResult == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                        sectionBufferBuilderPack.clearAll();
                     } else {
                        sectionBufferBuilderPack.discardAll();
                     }

                     this.bufferPool.release(sectionBufferBuilderPack);
                     this.runTask();
                  });
               }
            });
         }
      }
   }

   @Nullable
   private SectionRenderDispatcher.RenderSection.CompileTask pollTask() {
      SectionRenderDispatcher.RenderSection.CompileTask compileTask;
      if (this.highPriorityQuota <= 0) {
         compileTask = (SectionRenderDispatcher.RenderSection.CompileTask)this.toBatchLowPriority.poll();
         if (compileTask != null) {
            this.highPriorityQuota = 2;
            return compileTask;
         }
      }

      compileTask = (SectionRenderDispatcher.RenderSection.CompileTask)this.toBatchHighPriority.poll();
      if (compileTask != null) {
         --this.highPriorityQuota;
         return compileTask;
      } else {
         this.highPriorityQuota = 2;
         return (SectionRenderDispatcher.RenderSection.CompileTask)this.toBatchLowPriority.poll();
      }
   }

   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
   }

   public int getToBatchCount() {
      return this.toBatchCount;
   }

   public int getToUpload() {
      return this.toUpload.size();
   }

   public int getFreeBufferCount() {
      return this.bufferPool.getFreeBufferCount();
   }

   public void setCamera(Vec3 vec3) {
      this.camera = vec3;
   }

   public Vec3 getCameraPosition() {
      return this.camera;
   }

   public void uploadAllPendingUploads() {
      Runnable runnable;
      while((runnable = (Runnable)this.toUpload.poll()) != null) {
         runnable.run();
      }

   }

   public void rebuildSectionSync(SectionRenderDispatcher.RenderSection renderSection, RenderRegionCache renderRegionCache) {
      renderSection.compileSync(renderRegionCache);
   }

   public void blockUntilClear() {
      this.clearBatchQueue();
   }

   public void schedule(SectionRenderDispatcher.RenderSection.CompileTask compileTask) {
      if (!this.closed) {
         this.mailbox.tell(() -> {
            if (!this.closed) {
               if (compileTask.isHighPriority) {
                  this.toBatchHighPriority.offer(compileTask);
               } else {
                  this.toBatchLowPriority.offer(compileTask);
               }

               this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
               this.runTask();
            }
         });
      }
   }

   public CompletableFuture<Void> uploadSectionLayer(MeshData meshData, VertexBuffer vertexBuffer) {
      if (this.closed) {
         return CompletableFuture.completedFuture((Object)null);
      } else {
         Runnable var10000 = () -> {
            if (vertexBuffer.isInvalid()) {
               meshData.close();
            } else {
               vertexBuffer.bind();
               vertexBuffer.upload(meshData);
               VertexBuffer.unbind();
            }
         };
         Queue var10001 = this.toUpload;
         Objects.requireNonNull(var10001);
         return CompletableFuture.runAsync(var10000, var10001::add);
      }
   }

   public CompletableFuture<Void> uploadSectionIndexBuffer(ByteBufferBuilder.Result result, VertexBuffer vertexBuffer) {
      if (this.closed) {
         return CompletableFuture.completedFuture((Object)null);
      } else {
         Runnable var10000 = () -> {
            if (vertexBuffer.isInvalid()) {
               result.close();
            } else {
               vertexBuffer.bind();
               vertexBuffer.uploadIndexBuffer(result);
               VertexBuffer.unbind();
            }
         };
         Queue var10001 = this.toUpload;
         Objects.requireNonNull(var10001);
         return CompletableFuture.runAsync(var10000, var10001::add);
      }
   }

   private void clearBatchQueue() {
      SectionRenderDispatcher.RenderSection.CompileTask compileTask;
      while(!this.toBatchHighPriority.isEmpty()) {
         compileTask = (SectionRenderDispatcher.RenderSection.CompileTask)this.toBatchHighPriority.poll();
         if (compileTask != null) {
            compileTask.cancel();
         }
      }

      while(!this.toBatchLowPriority.isEmpty()) {
         compileTask = (SectionRenderDispatcher.RenderSection.CompileTask)this.toBatchLowPriority.poll();
         if (compileTask != null) {
            compileTask.cancel();
         }
      }

      this.toBatchCount = 0;
   }

   public boolean isQueueEmpty() {
      return this.toBatchCount == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.closed = true;
      this.clearBatchQueue();
      this.uploadAllPendingUploads();
   }

   @Environment(EnvType.CLIENT)
   public class RenderSection {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled;
      private final AtomicInteger initialCompilationCancelCount;
      @Nullable
      private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
      @Nullable
      private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
      private final Set<BlockEntity> globalBlockEntities;
      private final Map<RenderType, VertexBuffer> buffers;
      private AABB bb;
      private boolean dirty;
      final MutableBlockPos origin;
      private final MutableBlockPos[] relativeOrigins;
      private boolean playerChanged;

      public RenderSection(final int i, final int j, final int k, final int l) {
         this.compiled = new AtomicReference(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
         this.initialCompilationCancelCount = new AtomicInteger(0);
         this.globalBlockEntities = Sets.newHashSet();
         this.buffers = (Map)RenderType.chunkBufferLayers().stream().collect(Collectors.toMap((renderType) -> {
            return renderType;
         }, (renderType) -> {
            return new VertexBuffer(VertexBuffer.Usage.STATIC);
         }));
         this.dirty = true;
         this.origin = new MutableBlockPos(-1, -1, -1);
         this.relativeOrigins = (MutableBlockPos[])Util.make(new MutableBlockPos[6], (mutableBlockPoss) -> {
            for(int i = 0; i < mutableBlockPoss.length; ++i) {
               mutableBlockPoss[i] = new MutableBlockPos();
            }

         });
         this.index = i;
         this.setOrigin(j, k, l);
      }

      private boolean doesChunkExistAt(BlockPos blockPos) {
         return SectionRenderDispatcher.this.level.getChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()), ChunkStatus.FULL, false) != null;
      }

      public boolean hasAllNeighbors() {
         int i = true;
         if (!(this.getDistToPlayerSqr() > 576.0D)) {
            return true;
         } else {
            return this.doesChunkExistAt(this.relativeOrigins[Direction.WEST.ordinal()]) && this.doesChunkExistAt(this.relativeOrigins[Direction.NORTH.ordinal()]) && this.doesChunkExistAt(this.relativeOrigins[Direction.EAST.ordinal()]) && this.doesChunkExistAt(this.relativeOrigins[Direction.SOUTH.ordinal()]);
         }
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      public VertexBuffer getBuffer(RenderType renderType) {
         return (VertexBuffer)this.buffers.get(renderType);
      }

      public void setOrigin(int i, int j, int k) {
         this.reset();
         this.origin.set(i, j, k);
         this.bb = new AABB((double)i, (double)j, (double)k, (double)(i + 16), (double)(j + 16), (double)(k + 16));
         Direction[] var4 = Direction.values();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Direction direction = var4[var6];
            this.relativeOrigins[direction.ordinal()].set(this.origin).move(direction, 16);
         }

      }

      protected double getDistToPlayerSqr() {
         Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
         double d = this.bb.minX + 8.0D - camera.getPosition().x;
         double e = this.bb.minY + 8.0D - camera.getPosition().y;
         double f = this.bb.minZ + 8.0D - camera.getPosition().z;
         return d * d + e * e + f * f;
      }

      public SectionRenderDispatcher.CompiledSection getCompiled() {
         return (SectionRenderDispatcher.CompiledSection)this.compiled.get();
      }

      private void reset() {
         this.cancelTasks();
         this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
         this.dirty = true;
      }

      public void releaseBuffers() {
         this.reset();
         this.buffers.values().forEach(VertexBuffer::close);
      }

      public BlockPos getOrigin() {
         return this.origin;
      }

      public void setDirty(boolean bl) {
         boolean bl2 = this.dirty;
         this.dirty = true;
         this.playerChanged = bl | (bl2 && this.playerChanged);
      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public BlockPos getRelativeOrigin(Direction direction) {
         return this.relativeOrigins[direction.ordinal()];
      }

      public boolean resortTransparency(RenderType renderType, SectionRenderDispatcher sectionRenderDispatcher) {
         SectionRenderDispatcher.CompiledSection compiledSection = this.getCompiled();
         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
         }

         if (!compiledSection.hasBlocks.contains(renderType)) {
            return false;
         } else {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(this.getDistToPlayerSqr(), compiledSection);
            sectionRenderDispatcher.schedule(this.lastResortTransparencyTask);
            return true;
         }
      }

      protected boolean cancelTasks() {
         boolean bl = false;
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
            bl = true;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }

         return bl;
      }

      public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache renderRegionCache) {
         boolean bl = this.cancelTasks();
         RenderChunkRegion renderChunkRegion = renderRegionCache.createRegion(SectionRenderDispatcher.this.level, SectionPos.of(this.origin));
         boolean bl2 = this.compiled.get() == SectionRenderDispatcher.CompiledSection.UNCOMPILED;
         if (bl2 && bl) {
            this.initialCompilationCancelCount.incrementAndGet();
         }

         this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(this.getDistToPlayerSqr(), renderChunkRegion, !bl2 || this.initialCompilationCancelCount.get() > 2);
         return this.lastRebuildTask;
      }

      public void rebuildSectionAsync(SectionRenderDispatcher sectionRenderDispatcher, RenderRegionCache renderRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask compileTask = this.createCompileTask(renderRegionCache);
         sectionRenderDispatcher.schedule(compileTask);
      }

      void updateGlobalBlockEntities(Collection<BlockEntity> collection) {
         Set<BlockEntity> set = Sets.newHashSet(collection);
         HashSet set2;
         synchronized(this.globalBlockEntities) {
            set2 = Sets.newHashSet(this.globalBlockEntities);
            set.removeAll(this.globalBlockEntities);
            set2.removeAll(collection);
            this.globalBlockEntities.clear();
            this.globalBlockEntities.addAll(collection);
         }

         SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set2, set);
      }

      public void compileSync(RenderRegionCache renderRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask compileTask = this.createCompileTask(renderRegionCache);
         compileTask.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      public boolean isAxisAlignedWith(int i, int j, int k) {
         BlockPos blockPos = this.getOrigin();
         return i == SectionPos.blockToSectionCoord(blockPos.getX()) || k == SectionPos.blockToSectionCoord(blockPos.getZ()) || j == SectionPos.blockToSectionCoord(blockPos.getY());
      }

      void setCompiled(SectionRenderDispatcher.CompiledSection compiledSection) {
         this.compiled.set(compiledSection);
         this.initialCompilationCancelCount.set(0);
         SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
      }

      VertexSorting createVertexSorting() {
         Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
         return VertexSorting.byDistance((float)(vec3.x - (double)this.origin.getX()), (float)(vec3.y - (double)this.origin.getY()), (float)(vec3.z - (double)this.origin.getZ()));
      }

      @Environment(EnvType.CLIENT)
      private class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         private final SectionRenderDispatcher.CompiledSection compiledSection;

         public ResortTransparencyTask(final double d, final SectionRenderDispatcher.CompiledSection compiledSection) {
            super(RenderSection.this, d, true);
            this.compiledSection = compiledSection;
         }

         protected String name() {
            return "rend_chk_sort";
         }

         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack sectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               MeshData.SortState sortState = this.compiledSection.transparencyState;
               if (sortState != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                  VertexSorting vertexSorting = RenderSection.this.createVertexSorting();
                  ByteBufferBuilder.Result result = sortState.buildSortedIndexBuffer(sectionBufferBuilderPack.buffer(RenderType.translucent()), vertexSorting);
                  if (result == null) {
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else if (this.isCancelled.get()) {
                     result.close();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completableFuture = SectionRenderDispatcher.this.uploadSectionIndexBuffer(result, RenderSection.this.getBuffer(RenderType.translucent())).thenApply((void_) -> {
                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                     });
                     return completableFuture.handle((sectionTaskResult, throwable) -> {
                        if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwable, "Rendering section"));
                        }

                        return this.isCancelled.get() ? SectionRenderDispatcher.SectionTaskResult.CANCELLED : SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                     });
                  }
               } else {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               }
            }
         }

         public void cancel() {
            this.isCancelled.set(true);
         }
      }

      @Environment(EnvType.CLIENT)
      private abstract class CompileTask implements Comparable<SectionRenderDispatcher.RenderSection.CompileTask> {
         protected final double distAtCreation;
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final boolean isHighPriority;

         public CompileTask(final SectionRenderDispatcher.RenderSection renderSection, final double d, final boolean bl) {
            this.distAtCreation = d;
            this.isHighPriority = bl;
         }

         public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack sectionBufferBuilderPack);

         public abstract void cancel();

         protected abstract String name();

         public int compareTo(SectionRenderDispatcher.RenderSection.CompileTask compileTask) {
            return Doubles.compare(this.distAtCreation, compileTask.distAtCreation);
         }

         // $FF: synthetic method
         public int compareTo(final Object object) {
            return this.compareTo((SectionRenderDispatcher.RenderSection.CompileTask)object);
         }
      }

      @Environment(EnvType.CLIENT)
      private class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         @Nullable
         protected RenderChunkRegion region;

         public RebuildTask(final double d, @Nullable final RenderChunkRegion renderChunkRegion, final boolean bl) {
            super(RenderSection.this, d, bl);
            this.region = renderChunkRegion;
         }

         protected String name() {
            return "rend_chk_rebuild";
         }

         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack sectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.cancel();
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               RenderChunkRegion renderChunkRegion = this.region;
               this.region = null;
               if (renderChunkRegion == null) {
                  RenderSection.this.setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL);
               } else {
                  SectionPos sectionPos = SectionPos.of(RenderSection.this.origin);
                  SectionCompiler.Results results = SectionRenderDispatcher.this.sectionCompiler.compile(sectionPos, renderChunkRegion, RenderSection.this.createVertexSorting(), sectionBufferBuilderPack);
                  RenderSection.this.updateGlobalBlockEntities(results.globalBlockEntities);
                  if (this.isCancelled.get()) {
                     results.release();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     SectionRenderDispatcher.CompiledSection compiledSection = new SectionRenderDispatcher.CompiledSection();
                     compiledSection.visibilitySet = results.visibilitySet;
                     compiledSection.renderableBlockEntities.addAll(results.blockEntities);
                     compiledSection.transparencyState = results.transparencyState;
                     List<CompletableFuture<Void>> list = new ArrayList(results.renderedLayers.size());
                     results.renderedLayers.forEach((renderType, meshData) -> {
                        list.add(SectionRenderDispatcher.this.uploadSectionLayer(meshData, RenderSection.this.getBuffer(renderType)));
                        compiledSection.hasBlocks.add(renderType);
                     });
                     return Util.sequenceFailFast(list).handle((listx, throwable) -> {
                        if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwable, "Rendering section"));
                        }

                        if (this.isCancelled.get()) {
                           return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                        } else {
                           RenderSection.this.setCompiled(compiledSection);
                           return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                        }
                     });
                  }
               }
            }
         }

         public void cancel() {
            this.region = null;
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderSection.this.setDirty(false);
            }

         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum SectionTaskResult {
      SUCCESSFUL,
      CANCELLED;

      // $FF: synthetic method
      private static SectionRenderDispatcher.SectionTaskResult[] $values() {
         return new SectionRenderDispatcher.SectionTaskResult[]{SUCCESSFUL, CANCELLED};
      }
   }

   @Environment(EnvType.CLIENT)
   public static class CompiledSection {
      public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
         public boolean facesCanSeeEachother(Direction direction, Direction direction2) {
            return false;
         }
      };
      public static final SectionRenderDispatcher.CompiledSection EMPTY = new SectionRenderDispatcher.CompiledSection() {
         public boolean facesCanSeeEachother(Direction direction, Direction direction2) {
            return true;
         }
      };
      final Set<RenderType> hasBlocks = new ObjectArraySet(RenderType.chunkBufferLayers().size());
      final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
      VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      MeshData.SortState transparencyState;

      public boolean hasNoRenderableLayers() {
         return this.hasBlocks.isEmpty();
      }

      public boolean isEmpty(RenderType renderType) {
         return !this.hasBlocks.contains(renderType);
      }

      public List<BlockEntity> getRenderableBlockEntities() {
         return this.renderableBlockEntities;
      }

      public boolean facesCanSeeEachother(Direction direction, Direction direction2) {
         return this.visibilitySet.visibilityBetween(direction, direction2);
      }
   }
}
