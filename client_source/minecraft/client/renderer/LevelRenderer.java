package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner.FlameParticle;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity.Client;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int SECTION_SIZE = 16;
   public static final int HALF_SECTION_SIZE = 8;
   private static final float SKY_DISC_RADIUS = 512.0F;
   private static final int MIN_FOG_DISTANCE = 32;
   private static final int RAIN_RADIUS = 10;
   private static final int RAIN_DIAMETER = 21;
   private static final int TRANSPARENT_SORT_COUNT = 15;
   private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
   private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
   protected static final ResourceLocation CLOUDS_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
   private static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
   private static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
   private static final ResourceLocation RAIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
   private static final ResourceLocation SNOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
   public static final Direction[] DIRECTIONS = Direction.values();
   private final Minecraft minecraft;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   @Nullable
   private ClientLevel level;
   private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
   private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList(10000);
   private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
   @Nullable
   private ViewArea viewArea;
   @Nullable
   private VertexBuffer starBuffer;
   @Nullable
   private VertexBuffer skyBuffer;
   @Nullable
   private VertexBuffer darkBuffer;
   private boolean generateClouds = true;
   @Nullable
   private VertexBuffer cloudBuffer;
   private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
   private int ticks;
   private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap();
   private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap();
   private final Map<BlockPos, SoundInstance> playingJukeboxSongs = Maps.newHashMap();
   @Nullable
   private RenderTarget entityTarget;
   @Nullable
   private PostChain entityEffect;
   @Nullable
   private RenderTarget translucentTarget;
   @Nullable
   private RenderTarget itemEntityTarget;
   @Nullable
   private RenderTarget particlesTarget;
   @Nullable
   private RenderTarget weatherTarget;
   @Nullable
   private RenderTarget cloudsTarget;
   @Nullable
   private PostChain transparencyChain;
   private int lastCameraSectionX = Integer.MIN_VALUE;
   private int lastCameraSectionY = Integer.MIN_VALUE;
   private int lastCameraSectionZ = Integer.MIN_VALUE;
   private double prevCamX = Double.MIN_VALUE;
   private double prevCamY = Double.MIN_VALUE;
   private double prevCamZ = Double.MIN_VALUE;
   private double prevCamRotX = Double.MIN_VALUE;
   private double prevCamRotY = Double.MIN_VALUE;
   private int prevCloudX = Integer.MIN_VALUE;
   private int prevCloudY = Integer.MIN_VALUE;
   private int prevCloudZ = Integer.MIN_VALUE;
   private Vec3 prevCloudColor;
   @Nullable
   private CloudStatus prevCloudsType;
   @Nullable
   private SectionRenderDispatcher sectionRenderDispatcher;
   private int lastViewDistance;
   private int renderedEntities;
   private int culledEntities;
   private Frustum cullingFrustum;
   private boolean captureFrustum;
   @Nullable
   private Frustum capturedFrustum;
   private final Vector4f[] frustumPoints;
   private final Vector3d frustumPos;
   private double xTransparentOld;
   private double yTransparentOld;
   private double zTransparentOld;
   private int rainSoundTime;
   private final float[] rainSizeX;
   private final float[] rainSizeZ;

   public LevelRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers) {
      this.prevCloudColor = Vec3.ZERO;
      this.lastViewDistance = -1;
      this.frustumPoints = new Vector4f[8];
      this.frustumPos = new Vector3d(0.0D, 0.0D, 0.0D);
      this.rainSizeX = new float[1024];
      this.rainSizeZ = new float[1024];
      this.minecraft = minecraft;
      this.entityRenderDispatcher = entityRenderDispatcher;
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
      this.renderBuffers = renderBuffers;

      for(int i = 0; i < 32; ++i) {
         for(int j = 0; j < 32; ++j) {
            float f = (float)(j - 16);
            float g = (float)(i - 16);
            float h = Mth.sqrt(f * f + g * g);
            this.rainSizeX[i << 5 | j] = -g / h;
            this.rainSizeZ[i << 5 | j] = f / h;
         }
      }

      this.createStars();
      this.createLightSky();
      this.createDarkSky();
   }

   private void renderSnowAndRain(LightTexture lightTexture, float f, double d, double e, double g) {
      float h = this.minecraft.level.getRainLevel(f);
      if (!(h <= 0.0F)) {
         lightTexture.turnOnLightLayer();
         Level level = this.minecraft.level;
         int i = Mth.floor(d);
         int j = Mth.floor(e);
         int k = Mth.floor(g);
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferBuilder = null;
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         int l = 5;
         if (Minecraft.useFancyGraphics()) {
            l = 10;
         }

         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         int m = -1;
         float n = (float)this.ticks + f;
         RenderSystem.setShader(GameRenderer::getParticleShader);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for(int o = k - l; o <= k + l; ++o) {
            for(int p = i - l; p <= i + l; ++p) {
               int q = (o - k + 16) * 32 + p - i + 16;
               double r = (double)this.rainSizeX[q] * 0.5D;
               double s = (double)this.rainSizeZ[q] * 0.5D;
               mutableBlockPos.set((double)p, e, (double)o);
               Biome biome = (Biome)level.getBiome(mutableBlockPos).value();
               if (biome.hasPrecipitation()) {
                  int t = level.getHeight(Types.MOTION_BLOCKING, p, o);
                  int u = j - l;
                  int v = j + l;
                  if (u < t) {
                     u = t;
                  }

                  if (v < t) {
                     v = t;
                  }

                  int w = t;
                  if (t < j) {
                     w = j;
                  }

                  if (u != v) {
                     RandomSource randomSource = RandomSource.create((long)(p * p * 3121 + p * 45238971 ^ o * o * 418711 + o * 13761));
                     mutableBlockPos.set(p, u, o);
                     Precipitation precipitation = biome.getPrecipitationAt(mutableBlockPos);
                     float z;
                     double ac;
                     int ag;
                     if (precipitation == Precipitation.RAIN) {
                        if (m != 0) {
                           if (m >= 0) {
                              BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
                           }

                           m = 0;
                           RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                           bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        int x = this.ticks & 131071;
                        int y = p * p * 3121 + p * 45238971 + o * o * 418711 + o * 13761 & 255;
                        z = 3.0F + randomSource.nextFloat();
                        float aa = -((float)(x + y) + f) / 32.0F * z;
                        float ab = aa % 32.0F;
                        ac = (double)p + 0.5D - d;
                        double ad = (double)o + 0.5D - g;
                        float ae = (float)Math.sqrt(ac * ac + ad * ad) / (float)l;
                        float af = ((1.0F - ae * ae) * 0.5F + 0.5F) * h;
                        mutableBlockPos.set(p, w, o);
                        ag = getLightColor(level, mutableBlockPos);
                        bufferBuilder.addVertex((float)((double)p - d - r + 0.5D), (float)((double)v - e), (float)((double)o - g - s + 0.5D)).setUv(0.0F, (float)u * 0.25F + ab).setColor(1.0F, 1.0F, 1.0F, af).setLight(ag);
                        bufferBuilder.addVertex((float)((double)p - d + r + 0.5D), (float)((double)v - e), (float)((double)o - g + s + 0.5D)).setUv(1.0F, (float)u * 0.25F + ab).setColor(1.0F, 1.0F, 1.0F, af).setLight(ag);
                        bufferBuilder.addVertex((float)((double)p - d + r + 0.5D), (float)((double)u - e), (float)((double)o - g + s + 0.5D)).setUv(1.0F, (float)v * 0.25F + ab).setColor(1.0F, 1.0F, 1.0F, af).setLight(ag);
                        bufferBuilder.addVertex((float)((double)p - d - r + 0.5D), (float)((double)u - e), (float)((double)o - g - s + 0.5D)).setUv(0.0F, (float)v * 0.25F + ab).setColor(1.0F, 1.0F, 1.0F, af).setLight(ag);
                     } else if (precipitation == Precipitation.SNOW) {
                        if (m != 1) {
                           if (m >= 0) {
                              BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
                           }

                           m = 1;
                           RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                           bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        float ah = -((float)(this.ticks & 511) + f) / 512.0F;
                        float ai = (float)(randomSource.nextDouble() + (double)n * 0.01D * (double)((float)randomSource.nextGaussian()));
                        z = (float)(randomSource.nextDouble() + (double)(n * (float)randomSource.nextGaussian()) * 0.001D);
                        double aj = (double)p + 0.5D - d;
                        ac = (double)o + 0.5D - g;
                        float ak = (float)Math.sqrt(aj * aj + ac * ac) / (float)l;
                        float al = ((1.0F - ak * ak) * 0.3F + 0.5F) * h;
                        mutableBlockPos.set(p, w, o);
                        int am = getLightColor(level, mutableBlockPos);
                        int an = am >> 16 & '\uffff';
                        ag = am & '\uffff';
                        int ao = (an * 3 + 240) / 4;
                        int ap = (ag * 3 + 240) / 4;
                        bufferBuilder.addVertex((float)((double)p - d - r + 0.5D), (float)((double)v - e), (float)((double)o - g - s + 0.5D)).setUv(0.0F + ai, (float)u * 0.25F + ah + z).setColor(1.0F, 1.0F, 1.0F, al).setUv2(ap, ao);
                        bufferBuilder.addVertex((float)((double)p - d + r + 0.5D), (float)((double)v - e), (float)((double)o - g + s + 0.5D)).setUv(1.0F + ai, (float)u * 0.25F + ah + z).setColor(1.0F, 1.0F, 1.0F, al).setUv2(ap, ao);
                        bufferBuilder.addVertex((float)((double)p - d + r + 0.5D), (float)((double)u - e), (float)((double)o - g + s + 0.5D)).setUv(1.0F + ai, (float)v * 0.25F + ah + z).setColor(1.0F, 1.0F, 1.0F, al).setUv2(ap, ao);
                        bufferBuilder.addVertex((float)((double)p - d - r + 0.5D), (float)((double)u - e), (float)((double)o - g - s + 0.5D)).setUv(0.0F + ai, (float)v * 0.25F + ah + z).setColor(1.0F, 1.0F, 1.0F, al).setUv2(ap, ao);
                     }
                  }
               }
            }
         }

         if (m >= 0) {
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         lightTexture.turnOffLightLayer();
      }
   }

   public void tickRain(Camera camera) {
      float f = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
      if (!(f <= 0.0F)) {
         RandomSource randomSource = RandomSource.create((long)this.ticks * 312987231L);
         LevelReader levelReader = this.minecraft.level;
         BlockPos blockPos = BlockPos.containing(camera.getPosition());
         BlockPos blockPos2 = null;
         int i = (int)(100.0F * f * f) / (this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

         for(int j = 0; j < i; ++j) {
            int k = randomSource.nextInt(21) - 10;
            int l = randomSource.nextInt(21) - 10;
            BlockPos blockPos3 = levelReader.getHeightmapPos(Types.MOTION_BLOCKING, blockPos.offset(k, 0, l));
            if (blockPos3.getY() > levelReader.getMinBuildHeight() && blockPos3.getY() <= blockPos.getY() + 10 && blockPos3.getY() >= blockPos.getY() - 10) {
               Biome biome = (Biome)levelReader.getBiome(blockPos3).value();
               if (biome.getPrecipitationAt(blockPos3) == Precipitation.RAIN) {
                  blockPos2 = blockPos3.below();
                  if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
                     break;
                  }

                  double d = randomSource.nextDouble();
                  double e = randomSource.nextDouble();
                  BlockState blockState = levelReader.getBlockState(blockPos2);
                  FluidState fluidState = levelReader.getFluidState(blockPos2);
                  VoxelShape voxelShape = blockState.getCollisionShape(levelReader, blockPos2);
                  double g = voxelShape.max(Axis.Y, d, e);
                  double h = (double)fluidState.getHeight(levelReader, blockPos2);
                  double m = Math.max(g, h);
                  ParticleOptions particleOptions = !fluidState.is(FluidTags.LAVA) && !blockState.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockState) ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
                  this.minecraft.level.addParticle(particleOptions, (double)blockPos2.getX() + d, (double)blockPos2.getY() + m, (double)blockPos2.getZ() + e, 0.0D, 0.0D, 0.0D);
               }
            }
         }

         if (blockPos2 != null && randomSource.nextInt(3) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            if (blockPos2.getY() > blockPos.getY() + 1 && levelReader.getHeightmapPos(Types.MOTION_BLOCKING, blockPos).getY() > Mth.floor((float)blockPos.getY())) {
               this.minecraft.level.playLocalSound(blockPos2, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
            } else {
               this.minecraft.level.playLocalSound(blockPos2, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
            }
         }

      }
   }

   public void close() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.close();
      }

   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      this.initOutline();
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      }

   }

   public void initOutline() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace("shaders/post/entity_outline.json");

      try {
         this.entityEffect = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourceLocation);
         this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         this.entityTarget = this.entityEffect.getTempTarget("final");
      } catch (IOException var3) {
         LOGGER.warn("Failed to load shader: {}", resourceLocation, var3);
         this.entityEffect = null;
         this.entityTarget = null;
      } catch (JsonSyntaxException var4) {
         LOGGER.warn("Failed to parse shader: {}", resourceLocation, var4);
         this.entityEffect = null;
         this.entityTarget = null;
      }

   }

   private void initTransparency() {
      this.deinitTransparency();
      ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace("shaders/post/transparency.json");

      try {
         PostChain postChain = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourceLocation);
         postChain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         RenderTarget renderTarget = postChain.getTempTarget("translucent");
         RenderTarget renderTarget2 = postChain.getTempTarget("itemEntity");
         RenderTarget renderTarget3 = postChain.getTempTarget("particles");
         RenderTarget renderTarget4 = postChain.getTempTarget("weather");
         RenderTarget renderTarget5 = postChain.getTempTarget("clouds");
         this.transparencyChain = postChain;
         this.translucentTarget = renderTarget;
         this.itemEntityTarget = renderTarget2;
         this.particlesTarget = renderTarget3;
         this.weatherTarget = renderTarget4;
         this.cloudsTarget = renderTarget5;
      } catch (Exception var8) {
         String string = var8 instanceof JsonSyntaxException ? "parse" : "load";
         String string2 = "Failed to " + string + " shader: " + String.valueOf(resourceLocation);
         LevelRenderer.TransparencyShaderException transparencyShaderException = new LevelRenderer.TransparencyShaderException(string2, var8);
         if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1) {
            Component component = (Component)this.minecraft.getResourceManager().listPacks().findFirst().map((packResources) -> {
               return Component.literal(packResources.packId());
            }).orElse((Object)null);
            this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
            this.minecraft.clearResourcePacksOnError(transparencyShaderException, component, (Minecraft.GameLoadCookie)null);
         } else {
            this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
            this.minecraft.options.save();
            LOGGER.error(LogUtils.FATAL_MARKER, string2, transparencyShaderException);
            this.minecraft.emergencySaveAndCrash(new CrashReport(string2, transparencyShaderException));
         }
      }

   }

   private void deinitTransparency() {
      if (this.transparencyChain != null) {
         this.transparencyChain.close();
         this.translucentTarget.destroyBuffers();
         this.itemEntityTarget.destroyBuffers();
         this.particlesTarget.destroyBuffers();
         this.weatherTarget.destroyBuffers();
         this.cloudsTarget.destroyBuffers();
         this.transparencyChain = null;
         this.translucentTarget = null;
         this.itemEntityTarget = null;
         this.particlesTarget = null;
         this.weatherTarget = null;
         this.cloudsTarget = null;
      }

   }

   public void doEntityOutline() {
      if (this.shouldShowEntityOutlines()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
         this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }

   }

   protected boolean shouldShowEntityOutlines() {
      return !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null;
   }

   private void createDarkSky() {
      if (this.darkBuffer != null) {
         this.darkBuffer.close();
      }

      this.darkBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
      this.darkBuffer.bind();
      this.darkBuffer.upload(buildSkyDisc(Tesselator.getInstance(), -16.0F));
      VertexBuffer.unbind();
   }

   private void createLightSky() {
      if (this.skyBuffer != null) {
         this.skyBuffer.close();
      }

      this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
      this.skyBuffer.bind();
      this.skyBuffer.upload(buildSkyDisc(Tesselator.getInstance(), 16.0F));
      VertexBuffer.unbind();
   }

   private static MeshData buildSkyDisc(Tesselator tesselator, float f) {
      float g = Math.signum(f) * 512.0F;
      float h = 512.0F;
      BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
      bufferBuilder.addVertex(0.0F, f, 0.0F);

      for(int i = -180; i <= 180; i += 45) {
         bufferBuilder.addVertex(g * Mth.cos((float)i * 0.017453292F), f, 512.0F * Mth.sin((float)i * 0.017453292F));
      }

      return bufferBuilder.buildOrThrow();
   }

   private void createStars() {
      if (this.starBuffer != null) {
         this.starBuffer.close();
      }

      this.starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
      this.starBuffer.bind();
      this.starBuffer.upload(this.drawStars(Tesselator.getInstance()));
      VertexBuffer.unbind();
   }

   private MeshData drawStars(Tesselator tesselator) {
      RandomSource randomSource = RandomSource.create(10842L);
      int i = true;
      float f = 100.0F;
      BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

      for(int j = 0; j < 1500; ++j) {
         float g = randomSource.nextFloat() * 2.0F - 1.0F;
         float h = randomSource.nextFloat() * 2.0F - 1.0F;
         float k = randomSource.nextFloat() * 2.0F - 1.0F;
         float l = 0.15F + randomSource.nextFloat() * 0.1F;
         float m = Mth.lengthSquared(g, h, k);
         if (!(m <= 0.010000001F) && !(m >= 1.0F)) {
            Vector3f vector3f = (new Vector3f(g, h, k)).normalize(100.0F);
            float n = (float)(randomSource.nextDouble() * 3.1415927410125732D * 2.0D);
            Quaternionf quaternionf = (new Quaternionf()).rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector3f).rotateZ(n);
            bufferBuilder.addVertex(vector3f.add((new Vector3f(l, -l, 0.0F)).rotate(quaternionf)));
            bufferBuilder.addVertex(vector3f.add((new Vector3f(l, l, 0.0F)).rotate(quaternionf)));
            bufferBuilder.addVertex(vector3f.add((new Vector3f(-l, l, 0.0F)).rotate(quaternionf)));
            bufferBuilder.addVertex(vector3f.add((new Vector3f(-l, -l, 0.0F)).rotate(quaternionf)));
         }
      }

      return bufferBuilder.buildOrThrow();
   }

   public void setLevel(@Nullable ClientLevel clientLevel) {
      this.lastCameraSectionX = Integer.MIN_VALUE;
      this.lastCameraSectionY = Integer.MIN_VALUE;
      this.lastCameraSectionZ = Integer.MIN_VALUE;
      this.entityRenderDispatcher.setLevel(clientLevel);
      this.level = clientLevel;
      if (clientLevel != null) {
         this.allChanged();
      } else {
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
         }

         if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.dispose();
         }

         this.sectionRenderDispatcher = null;
         this.globalBlockEntities.clear();
         this.sectionOcclusionGraph.waitAndReset((ViewArea)null);
         this.visibleSections.clear();
      }

   }

   public void graphicsChanged() {
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      } else {
         this.deinitTransparency();
      }

   }

   public void allChanged() {
      if (this.level != null) {
         this.graphicsChanged();
         this.level.clearTintCaches();
         if (this.sectionRenderDispatcher == null) {
            this.sectionRenderDispatcher = new SectionRenderDispatcher(this.level, this, Util.backgroundExecutor(), this.renderBuffers, this.minecraft.getBlockRenderer(), this.minecraft.getBlockEntityRenderDispatcher());
         } else {
            this.sectionRenderDispatcher.setLevel(this.level);
         }

         this.generateClouds = true;
         ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
         this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
         }

         this.sectionRenderDispatcher.blockUntilClear();
         synchronized(this.globalBlockEntities) {
            this.globalBlockEntities.clear();
         }

         this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
         this.sectionOcclusionGraph.waitAndReset(this.viewArea);
         this.visibleSections.clear();
         Entity entity = this.minecraft.getCameraEntity();
         if (entity != null) {
            this.viewArea.repositionCamera(entity.getX(), entity.getZ());
         }

      }
   }

   public void resize(int i, int j) {
      this.needsUpdate();
      if (this.entityEffect != null) {
         this.entityEffect.resize(i, j);
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.resize(i, j);
      }

   }

   public String getSectionStatistics() {
      int i = this.viewArea.sections.length;
      int j = this.countRenderedSections();
      return String.format(Locale.ROOT, "C: %d/%d %sD: %d, %s", j, i, this.minecraft.smartCull ? "(s) " : "", this.lastViewDistance, this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats());
   }

   public SectionRenderDispatcher getSectionRenderDispatcher() {
      return this.sectionRenderDispatcher;
   }

   public double getTotalSections() {
      return (double)this.viewArea.sections.length;
   }

   public double getLastViewDistance() {
      return (double)this.lastViewDistance;
   }

   public int countRenderedSections() {
      int i = 0;
      ObjectListIterator var2 = this.visibleSections.iterator();

      while(var2.hasNext()) {
         SectionRenderDispatcher.RenderSection renderSection = (SectionRenderDispatcher.RenderSection)var2.next();
         if (!renderSection.getCompiled().hasNoRenderableLayers()) {
            ++i;
         }
      }

      return i;
   }

   public String getEntityStatistics() {
      int var10000 = this.renderedEntities;
      return "E: " + var10000 + "/" + this.level.getEntityCount() + ", B: " + this.culledEntities + ", SD: " + this.level.getServerSimulationDistance();
   }

   private void setupRender(Camera camera, Frustum frustum, boolean bl, boolean bl2) {
      Vec3 vec3 = camera.getPosition();
      if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
         this.allChanged();
      }

      this.level.getProfiler().push("camera");
      double d = this.minecraft.player.getX();
      double e = this.minecraft.player.getY();
      double f = this.minecraft.player.getZ();
      int i = SectionPos.posToSectionCoord(d);
      int j = SectionPos.posToSectionCoord(e);
      int k = SectionPos.posToSectionCoord(f);
      if (this.lastCameraSectionX != i || this.lastCameraSectionY != j || this.lastCameraSectionZ != k) {
         this.lastCameraSectionX = i;
         this.lastCameraSectionY = j;
         this.lastCameraSectionZ = k;
         this.viewArea.repositionCamera(d, f);
      }

      this.sectionRenderDispatcher.setCamera(vec3);
      this.level.getProfiler().popPush("cull");
      this.minecraft.getProfiler().popPush("culling");
      BlockPos blockPos = camera.getBlockPosition();
      double g = Math.floor(vec3.x / 8.0D);
      double h = Math.floor(vec3.y / 8.0D);
      double l = Math.floor(vec3.z / 8.0D);
      if (g != this.prevCamX || h != this.prevCamY || l != this.prevCamZ) {
         this.sectionOcclusionGraph.invalidate();
      }

      this.prevCamX = g;
      this.prevCamY = h;
      this.prevCamZ = l;
      this.minecraft.getProfiler().popPush("update");
      if (!bl) {
         boolean bl3 = this.minecraft.smartCull;
         if (bl2 && this.level.getBlockState(blockPos).isSolidRender(this.level, blockPos)) {
            bl3 = false;
         }

         Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * (Double)this.minecraft.options.entityDistanceScaling().get());
         this.minecraft.getProfiler().push("section_occlusion_graph");
         this.sectionOcclusionGraph.update(bl3, camera, frustum, this.visibleSections);
         this.minecraft.getProfiler().pop();
         double m = Math.floor((double)(camera.getXRot() / 2.0F));
         double n = Math.floor((double)(camera.getYRot() / 2.0F));
         if (this.sectionOcclusionGraph.consumeFrustumUpdate() || m != this.prevCamRotX || n != this.prevCamRotY) {
            this.applyFrustum(offsetFrustum(frustum));
            this.prevCamRotX = m;
            this.prevCamRotY = n;
         }
      }

      this.minecraft.getProfiler().pop();
   }

   public static Frustum offsetFrustum(Frustum frustum) {
      return (new Frustum(frustum)).offsetToFullyIncludeCameraCube(8);
   }

   private void applyFrustum(Frustum frustum) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
      } else {
         this.minecraft.getProfiler().push("apply_frustum");
         this.visibleSections.clear();
         this.sectionOcclusionGraph.addSectionsInFrustum(frustum, this.visibleSections);
         this.minecraft.getProfiler().pop();
      }
   }

   public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection renderSection) {
      this.sectionOcclusionGraph.onSectionCompiled(renderSection);
   }

   private void captureFrustum(Matrix4f matrix4f, Matrix4f matrix4f2, double d, double e, double f, Frustum frustum) {
      this.capturedFrustum = frustum;
      Matrix4f matrix4f3 = new Matrix4f(matrix4f2);
      matrix4f3.mul(matrix4f);
      matrix4f3.invert();
      this.frustumPos.x = d;
      this.frustumPos.y = e;
      this.frustumPos.z = f;
      this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

      for(int i = 0; i < 8; ++i) {
         matrix4f3.transform(this.frustumPoints[i]);
         this.frustumPoints[i].div(this.frustumPoints[i].w());
      }

   }

   public void prepareCullFrustum(Vec3 vec3, Matrix4f matrix4f, Matrix4f matrix4f2) {
      this.cullingFrustum = new Frustum(matrix4f, matrix4f2);
      this.cullingFrustum.prepare(vec3.x(), vec3.y(), vec3.z());
   }

   public void renderLevel(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2) {
      TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
      float f = deltaTracker.getGameTimeDeltaPartialTick(false);
      RenderSystem.setShaderGameTime(this.level.getGameTime(), f);
      this.blockEntityRenderDispatcher.prepare(this.level, camera, this.minecraft.hitResult);
      this.entityRenderDispatcher.prepare(this.level, camera, this.minecraft.crosshairPickEntity);
      ProfilerFiller profilerFiller = this.level.getProfiler();
      profilerFiller.popPush("light_update_queue");
      this.level.pollLightUpdates();
      profilerFiller.popPush("light_updates");
      this.level.getChunkSource().getLightEngine().runLightUpdates();
      Vec3 vec3 = camera.getPosition();
      double d = vec3.x();
      double e = vec3.y();
      double g = vec3.z();
      profilerFiller.popPush("culling");
      boolean bl2 = this.capturedFrustum != null;
      Frustum frustum;
      if (bl2) {
         frustum = this.capturedFrustum;
         frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
      } else {
         frustum = this.cullingFrustum;
      }

      this.minecraft.getProfiler().popPush("captureFrustum");
      if (this.captureFrustum) {
         this.captureFrustum(matrix4f, matrix4f2, vec3.x, vec3.y, vec3.z, bl2 ? new Frustum(matrix4f, matrix4f2) : frustum);
         this.captureFrustum = false;
      }

      profilerFiller.popPush("clear");
      FogRenderer.setupColor(camera, f, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), gameRenderer.getDarkenWorldAmount(f));
      FogRenderer.levelFogColor();
      RenderSystem.clear(16640, Minecraft.ON_OSX);
      float h = gameRenderer.getRenderDistance();
      boolean bl3 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d), Mth.floor(e)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
      profilerFiller.popPush("sky");
      RenderSystem.setShader(GameRenderer::getPositionShader);
      this.renderSky(matrix4f, matrix4f2, f, camera, bl3, () -> {
         FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, h, bl3, f);
      });
      profilerFiller.popPush("fog");
      FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(h, 32.0F), bl3, f);
      profilerFiller.popPush("terrain_setup");
      this.setupRender(camera, frustum, bl2, this.minecraft.player.isSpectator());
      profilerFiller.popPush("compile_sections");
      this.compileSections(camera);
      profilerFiller.popPush("terrain");
      this.renderSectionLayer(RenderType.solid(), d, e, g, matrix4f, matrix4f2);
      this.renderSectionLayer(RenderType.cutoutMipped(), d, e, g, matrix4f, matrix4f2);
      this.renderSectionLayer(RenderType.cutout(), d, e, g, matrix4f, matrix4f2);
      if (this.level.effects().constantAmbientLight()) {
         Lighting.setupNetherLevel();
      } else {
         Lighting.setupLevel();
      }

      profilerFiller.popPush("entities");
      this.renderedEntities = 0;
      this.culledEntities = 0;
      if (this.itemEntityTarget != null) {
         this.itemEntityTarget.clear(Minecraft.ON_OSX);
         this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      if (this.weatherTarget != null) {
         this.weatherTarget.clear(Minecraft.ON_OSX);
      }

      if (this.shouldShowEntityOutlines()) {
         this.entityTarget.clear(Minecraft.ON_OSX);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
      matrix4fStack.pushMatrix();
      matrix4fStack.mul(matrix4f);
      RenderSystem.applyModelViewMatrix();
      boolean bl4 = false;
      PoseStack poseStack = new PoseStack();
      MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
      Iterator var26 = this.level.entitiesForRendering().iterator();

      while(true) {
         Entity entity;
         do {
            BlockPos blockPos;
            do {
               do {
                  do {
                     if (!var26.hasNext()) {
                        bufferSource.endLastBatch();
                        this.checkPoseStack(poseStack);
                        bufferSource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
                        bufferSource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
                        bufferSource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                        bufferSource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
                        profilerFiller.popPush("blockentities");
                        ObjectListIterator var40 = this.visibleSections.iterator();

                        while(true) {
                           List list;
                           do {
                              if (!var40.hasNext()) {
                                 synchronized(this.globalBlockEntities) {
                                    Iterator var44 = this.globalBlockEntities.iterator();

                                    while(true) {
                                       if (!var44.hasNext()) {
                                          break;
                                       }

                                       BlockEntity blockEntity2 = (BlockEntity)var44.next();
                                       BlockPos blockPos3 = blockEntity2.getBlockPos();
                                       poseStack.pushPose();
                                       poseStack.translate((double)blockPos3.getX() - d, (double)blockPos3.getY() - e, (double)blockPos3.getZ() - g);
                                       this.blockEntityRenderDispatcher.render(blockEntity2, f, poseStack, bufferSource);
                                       poseStack.popPose();
                                    }
                                 }

                                 this.checkPoseStack(poseStack);
                                 bufferSource.endBatch(RenderType.solid());
                                 bufferSource.endBatch(RenderType.endPortal());
                                 bufferSource.endBatch(RenderType.endGateway());
                                 bufferSource.endBatch(Sheets.solidBlockSheet());
                                 bufferSource.endBatch(Sheets.cutoutBlockSheet());
                                 bufferSource.endBatch(Sheets.bedSheet());
                                 bufferSource.endBatch(Sheets.shulkerBoxSheet());
                                 bufferSource.endBatch(Sheets.signSheet());
                                 bufferSource.endBatch(Sheets.hangingSignSheet());
                                 bufferSource.endBatch(Sheets.chestSheet());
                                 this.renderBuffers.outlineBufferSource().endOutlineBatch();
                                 if (bl4) {
                                    this.entityEffect.process(deltaTracker.getGameTimeDeltaTicks());
                                    this.minecraft.getMainRenderTarget().bindWrite(false);
                                 }

                                 profilerFiller.popPush("destroyProgress");
                                 ObjectIterator var41 = this.destructionProgress.long2ObjectEntrySet().iterator();

                                 while(var41.hasNext()) {
                                    Entry<SortedSet<BlockDestructionProgress>> entry = (Entry)var41.next();
                                    blockPos = BlockPos.of(entry.getLongKey());
                                    double l = (double)blockPos.getX() - d;
                                    double m = (double)blockPos.getY() - e;
                                    double n = (double)blockPos.getZ() - g;
                                    if (!(l * l + m * m + n * n > 1024.0D)) {
                                       SortedSet<BlockDestructionProgress> sortedSet2 = (SortedSet)entry.getValue();
                                       if (sortedSet2 != null && !sortedSet2.isEmpty()) {
                                          int o = ((BlockDestructionProgress)sortedSet2.last()).getProgress();
                                          poseStack.pushPose();
                                          poseStack.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - g);
                                          PoseStack.Pose pose2 = poseStack.last();
                                          VertexConsumer vertexConsumer2 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(o)), pose2, 1.0F);
                                          this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(blockPos), blockPos, this.level, poseStack, vertexConsumer2);
                                          poseStack.popPose();
                                       }
                                    }
                                 }

                                 this.checkPoseStack(poseStack);
                                 HitResult hitResult = this.minecraft.hitResult;
                                 if (bl && hitResult != null && hitResult.getType() == Type.BLOCK) {
                                    profilerFiller.popPush("outline");
                                    BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
                                    BlockState blockState = this.level.getBlockState(blockPos4);
                                    if (!blockState.isAir() && this.level.getWorldBorder().isWithinBounds(blockPos4)) {
                                       VertexConsumer vertexConsumer3 = bufferSource.getBuffer(RenderType.lines());
                                       this.renderHitOutline(poseStack, vertexConsumer3, camera.getEntity(), d, e, g, blockPos4, blockState);
                                    }
                                 }

                                 this.minecraft.debugRenderer.render(poseStack, bufferSource, d, e, g);
                                 bufferSource.endLastBatch();
                                 bufferSource.endBatch(Sheets.translucentCullBlockSheet());
                                 bufferSource.endBatch(Sheets.bannerSheet());
                                 bufferSource.endBatch(Sheets.shieldSheet());
                                 bufferSource.endBatch(RenderType.armorEntityGlint());
                                 bufferSource.endBatch(RenderType.glint());
                                 bufferSource.endBatch(RenderType.glintTranslucent());
                                 bufferSource.endBatch(RenderType.entityGlint());
                                 bufferSource.endBatch(RenderType.entityGlintDirect());
                                 bufferSource.endBatch(RenderType.waterMask());
                                 this.renderBuffers.crumblingBufferSource().endBatch();
                                 if (this.transparencyChain != null) {
                                    bufferSource.endBatch(RenderType.lines());
                                    bufferSource.endBatch();
                                    this.translucentTarget.clear(Minecraft.ON_OSX);
                                    this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
                                    profilerFiller.popPush("translucent");
                                    this.renderSectionLayer(RenderType.translucent(), d, e, g, matrix4f, matrix4f2);
                                    profilerFiller.popPush("string");
                                    this.renderSectionLayer(RenderType.tripwire(), d, e, g, matrix4f, matrix4f2);
                                    this.particlesTarget.clear(Minecraft.ON_OSX);
                                    this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
                                    RenderStateShard.PARTICLES_TARGET.setupRenderState();
                                    profilerFiller.popPush("particles");
                                    this.minecraft.particleEngine.render(lightTexture, camera, f);
                                    RenderStateShard.PARTICLES_TARGET.clearRenderState();
                                 } else {
                                    profilerFiller.popPush("translucent");
                                    if (this.translucentTarget != null) {
                                       this.translucentTarget.clear(Minecraft.ON_OSX);
                                    }

                                    this.renderSectionLayer(RenderType.translucent(), d, e, g, matrix4f, matrix4f2);
                                    bufferSource.endBatch(RenderType.lines());
                                    bufferSource.endBatch();
                                    profilerFiller.popPush("string");
                                    this.renderSectionLayer(RenderType.tripwire(), d, e, g, matrix4f, matrix4f2);
                                    profilerFiller.popPush("particles");
                                    this.minecraft.particleEngine.render(lightTexture, camera, f);
                                 }

                                 if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
                                    if (this.transparencyChain != null) {
                                       this.cloudsTarget.clear(Minecraft.ON_OSX);
                                    }

                                    profilerFiller.popPush("clouds");
                                    this.renderClouds(poseStack, matrix4f, matrix4f2, f, d, e, g);
                                 }

                                 if (this.transparencyChain != null) {
                                    RenderStateShard.WEATHER_TARGET.setupRenderState();
                                    profilerFiller.popPush("weather");
                                    this.renderSnowAndRain(lightTexture, f, d, e, g);
                                    this.renderWorldBorder(camera);
                                    RenderStateShard.WEATHER_TARGET.clearRenderState();
                                    this.transparencyChain.process(deltaTracker.getGameTimeDeltaTicks());
                                    this.minecraft.getMainRenderTarget().bindWrite(false);
                                 } else {
                                    RenderSystem.depthMask(false);
                                    profilerFiller.popPush("weather");
                                    this.renderSnowAndRain(lightTexture, f, d, e, g);
                                    this.renderWorldBorder(camera);
                                    RenderSystem.depthMask(true);
                                 }

                                 this.renderDebug(poseStack, bufferSource, camera);
                                 bufferSource.endLastBatch();
                                 matrix4fStack.popMatrix();
                                 RenderSystem.applyModelViewMatrix();
                                 RenderSystem.depthMask(true);
                                 RenderSystem.disableBlend();
                                 FogRenderer.setupNoFog();
                                 return;
                              }

                              SectionRenderDispatcher.RenderSection renderSection = (SectionRenderDispatcher.RenderSection)var40.next();
                              list = renderSection.getCompiled().getRenderableBlockEntities();
                           } while(list.isEmpty());

                           Iterator var50 = list.iterator();

                           while(var50.hasNext()) {
                              BlockEntity blockEntity = (BlockEntity)var50.next();
                              BlockPos blockPos2 = blockEntity.getBlockPos();
                              MultiBufferSource multiBufferSource2 = bufferSource;
                              poseStack.pushPose();
                              poseStack.translate((double)blockPos2.getX() - d, (double)blockPos2.getY() - e, (double)blockPos2.getZ() - g);
                              SortedSet<BlockDestructionProgress> sortedSet = (SortedSet)this.destructionProgress.get(blockPos2.asLong());
                              if (sortedSet != null && !sortedSet.isEmpty()) {
                                 int k = ((BlockDestructionProgress)sortedSet.last()).getProgress();
                                 if (k >= 0) {
                                    PoseStack.Pose pose = poseStack.last();
                                    VertexConsumer vertexConsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(k)), pose, 1.0F);
                                    multiBufferSource2 = (renderType) -> {
                                       VertexConsumer vertexConsumer2 = bufferSource.getBuffer(renderType);
                                       return renderType.affectsCrumbling() ? VertexMultiConsumer.create(vertexConsumer, vertexConsumer2) : vertexConsumer2;
                                    };
                                 }
                              }

                              this.blockEntityRenderDispatcher.render(blockEntity, f, poseStack, (MultiBufferSource)multiBufferSource2);
                              poseStack.popPose();
                           }
                        }
                     }

                     entity = (Entity)var26.next();
                  } while(!this.entityRenderDispatcher.shouldRender(entity, frustum, d, e, g) && !entity.hasIndirectPassenger(this.minecraft.player));

                  blockPos = entity.blockPosition();
               } while(!this.level.isOutsideBuildHeight(blockPos.getY()) && !this.isSectionCompiled(blockPos));
            } while(entity == camera.getEntity() && !camera.isDetached() && (!(camera.getEntity() instanceof LivingEntity) || !((LivingEntity)camera.getEntity()).isSleeping()));
         } while(entity instanceof LocalPlayer && camera.getEntity() != entity);

         ++this.renderedEntities;
         if (entity.tickCount == 0) {
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();
         }

         Object multiBufferSource;
         if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity)) {
            bl4 = true;
            OutlineBufferSource outlineBufferSource = this.renderBuffers.outlineBufferSource();
            multiBufferSource = outlineBufferSource;
            int i = entity.getTeamColor();
            outlineBufferSource.setColor(ARGB32.red(i), ARGB32.green(i), ARGB32.blue(i), 255);
         } else {
            multiBufferSource = bufferSource;
         }

         float j = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
         this.renderEntity(entity, d, e, g, j, poseStack, (MultiBufferSource)multiBufferSource);
      }
   }

   private void checkPoseStack(PoseStack poseStack) {
      if (!poseStack.clear()) {
         throw new IllegalStateException("Pose stack not empty");
      }
   }

   private void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource) {
      double h = Mth.lerp((double)g, entity.xOld, entity.getX());
      double i = Mth.lerp((double)g, entity.yOld, entity.getY());
      double j = Mth.lerp((double)g, entity.zOld, entity.getZ());
      float k = Mth.lerp(g, entity.yRotO, entity.getYRot());
      this.entityRenderDispatcher.render(entity, h - d, i - e, j - f, k, g, poseStack, multiBufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, g));
   }

   private void renderSectionLayer(RenderType renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2) {
      RenderSystem.assertOnRenderThread();
      renderType.setupRenderState();
      if (renderType == RenderType.translucent()) {
         this.minecraft.getProfiler().push("translucent_sort");
         double g = d - this.xTransparentOld;
         double h = e - this.yTransparentOld;
         double i = f - this.zTransparentOld;
         if (g * g + h * h + i * i > 1.0D) {
            int j = SectionPos.posToSectionCoord(d);
            int k = SectionPos.posToSectionCoord(e);
            int l = SectionPos.posToSectionCoord(f);
            boolean bl = j != SectionPos.posToSectionCoord(this.xTransparentOld) || l != SectionPos.posToSectionCoord(this.zTransparentOld) || k != SectionPos.posToSectionCoord(this.yTransparentOld);
            this.xTransparentOld = d;
            this.yTransparentOld = e;
            this.zTransparentOld = f;
            int m = 0;
            ObjectListIterator var21 = this.visibleSections.iterator();

            label76:
            while(true) {
               SectionRenderDispatcher.RenderSection renderSection;
               do {
                  do {
                     if (!var21.hasNext()) {
                        break label76;
                     }

                     renderSection = (SectionRenderDispatcher.RenderSection)var21.next();
                  } while(m >= 15);
               } while(!bl && !renderSection.isAxisAlignedWith(j, k, l));

               if (renderSection.resortTransparency(renderType, this.sectionRenderDispatcher)) {
                  ++m;
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }

      this.minecraft.getProfiler().push("filterempty");
      this.minecraft.getProfiler().popPush(() -> {
         return "render_" + String.valueOf(renderType);
      });
      boolean bl2 = renderType != RenderType.translucent();
      ObjectListIterator<SectionRenderDispatcher.RenderSection> objectListIterator = this.visibleSections.listIterator(bl2 ? 0 : this.visibleSections.size());
      ShaderInstance shaderInstance = RenderSystem.getShader();
      shaderInstance.setDefaultUniforms(VertexFormat.Mode.QUADS, matrix4f, matrix4f2, this.minecraft.getWindow());
      shaderInstance.apply();
      Uniform uniform = shaderInstance.CHUNK_OFFSET;

      while(true) {
         if (bl2) {
            if (!objectListIterator.hasNext()) {
               break;
            }
         } else if (!objectListIterator.hasPrevious()) {
            break;
         }

         SectionRenderDispatcher.RenderSection renderSection2 = bl2 ? (SectionRenderDispatcher.RenderSection)objectListIterator.next() : (SectionRenderDispatcher.RenderSection)objectListIterator.previous();
         if (!renderSection2.getCompiled().isEmpty(renderType)) {
            VertexBuffer vertexBuffer = renderSection2.getBuffer(renderType);
            BlockPos blockPos = renderSection2.getOrigin();
            if (uniform != null) {
               uniform.set((float)((double)blockPos.getX() - d), (float)((double)blockPos.getY() - e), (float)((double)blockPos.getZ() - f));
               uniform.upload();
            }

            vertexBuffer.bind();
            vertexBuffer.draw();
         }
      }

      if (uniform != null) {
         uniform.set(0.0F, 0.0F, 0.0F);
      }

      shaderInstance.clear();
      VertexBuffer.unbind();
      this.minecraft.getProfiler().pop();
      renderType.clearRenderState();
   }

   private void renderDebug(PoseStack poseStack, MultiBufferSource multiBufferSource, Camera camera) {
      if (this.minecraft.sectionPath || this.minecraft.sectionVisibility) {
         double d = camera.getPosition().x();
         double e = camera.getPosition().y();
         double f = camera.getPosition().z();
         ObjectListIterator var10 = this.visibleSections.iterator();

         label75:
         while(true) {
            SectionRenderDispatcher.RenderSection renderSection;
            SectionOcclusionGraph.Node node;
            do {
               if (!var10.hasNext()) {
                  break label75;
               }

               renderSection = (SectionRenderDispatcher.RenderSection)var10.next();
               node = this.sectionOcclusionGraph.getNode(renderSection);
            } while(node == null);

            BlockPos blockPos = renderSection.getOrigin();
            poseStack.pushPose();
            poseStack.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
            Matrix4f matrix4f = poseStack.last().pose();
            VertexConsumer vertexConsumer;
            int i;
            int k;
            int l;
            if (this.minecraft.sectionPath) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.lines());
               i = node.step == 0 ? 0 : Mth.hsvToRgb((float)node.step / 50.0F, 0.9F, 0.9F);
               int j = i >> 16 & 255;
               k = i >> 8 & 255;
               l = i & 255;

               for(int m = 0; m < DIRECTIONS.length; ++m) {
                  if (node.hasSourceDirection(m)) {
                     Direction direction = DIRECTIONS[m];
                     vertexConsumer.addVertex(matrix4f, 8.0F, 8.0F, 8.0F).setColor(j, k, l, 255).setNormal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ());
                     vertexConsumer.addVertex(matrix4f, (float)(8 - 16 * direction.getStepX()), (float)(8 - 16 * direction.getStepY()), (float)(8 - 16 * direction.getStepZ())).setColor(j, k, l, 255).setNormal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ());
                  }
               }
            }

            if (this.minecraft.sectionVisibility && !renderSection.getCompiled().hasNoRenderableLayers()) {
               vertexConsumer = multiBufferSource.getBuffer(RenderType.lines());
               i = 0;
               Direction[] var28 = DIRECTIONS;
               k = var28.length;
               l = 0;

               while(true) {
                  if (l >= k) {
                     if (i > 0) {
                        VertexConsumer vertexConsumer2 = multiBufferSource.getBuffer(RenderType.debugQuads());
                        float g = 0.5F;
                        float h = 0.2F;
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        vertexConsumer2.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                     }
                     break;
                  }

                  Direction direction2 = var28[l];
                  Direction[] var33 = DIRECTIONS;
                  int var22 = var33.length;

                  for(int var23 = 0; var23 < var22; ++var23) {
                     Direction direction3 = var33[var23];
                     boolean bl = renderSection.getCompiled().facesCanSeeEachother(direction2, direction3);
                     if (!bl) {
                        ++i;
                        vertexConsumer.addVertex(matrix4f, (float)(8 + 8 * direction2.getStepX()), (float)(8 + 8 * direction2.getStepY()), (float)(8 + 8 * direction2.getStepZ())).setColor(255, 0, 0, 255).setNormal((float)direction2.getStepX(), (float)direction2.getStepY(), (float)direction2.getStepZ());
                        vertexConsumer.addVertex(matrix4f, (float)(8 + 8 * direction3.getStepX()), (float)(8 + 8 * direction3.getStepY()), (float)(8 + 8 * direction3.getStepZ())).setColor(255, 0, 0, 255).setNormal((float)direction3.getStepX(), (float)direction3.getStepY(), (float)direction3.getStepZ());
                     }
                  }

                  ++l;
               }
            }

            poseStack.popPose();
         }
      }

      if (this.capturedFrustum != null) {
         poseStack.pushPose();
         poseStack.translate((float)(this.frustumPos.x - camera.getPosition().x), (float)(this.frustumPos.y - camera.getPosition().y), (float)(this.frustumPos.z - camera.getPosition().z));
         Matrix4f matrix4f2 = poseStack.last().pose();
         VertexConsumer vertexConsumer3 = multiBufferSource.getBuffer(RenderType.debugQuads());
         this.addFrustumQuad(vertexConsumer3, matrix4f2, 0, 1, 2, 3, 0, 1, 1);
         this.addFrustumQuad(vertexConsumer3, matrix4f2, 4, 5, 6, 7, 1, 0, 0);
         this.addFrustumQuad(vertexConsumer3, matrix4f2, 0, 1, 5, 4, 1, 1, 0);
         this.addFrustumQuad(vertexConsumer3, matrix4f2, 2, 3, 7, 6, 0, 0, 1);
         this.addFrustumQuad(vertexConsumer3, matrix4f2, 0, 4, 7, 3, 0, 1, 0);
         this.addFrustumQuad(vertexConsumer3, matrix4f2, 1, 5, 6, 2, 1, 0, 1);
         VertexConsumer vertexConsumer4 = multiBufferSource.getBuffer(RenderType.lines());
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 0);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 1);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 1);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 2);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 2);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 3);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 3);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 0);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 4);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 5);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 5);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 6);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 6);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 7);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 7);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 4);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 0);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 4);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 1);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 5);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 2);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 6);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 3);
         this.addFrustumVertex(vertexConsumer4, matrix4f2, 7);
         poseStack.popPose();
      }

   }

   private void addFrustumVertex(VertexConsumer vertexConsumer, Matrix4f matrix4f, int i) {
      vertexConsumer.addVertex(matrix4f, this.frustumPoints[i].x(), this.frustumPoints[i].y(), this.frustumPoints[i].z()).setColor(-16777216).setNormal(0.0F, 0.0F, -1.0F);
   }

   private void addFrustumQuad(VertexConsumer vertexConsumer, Matrix4f matrix4f, int i, int j, int k, int l, int m, int n, int o) {
      float f = 0.25F;
      vertexConsumer.addVertex(matrix4f, this.frustumPoints[i].x(), this.frustumPoints[i].y(), this.frustumPoints[i].z()).setColor((float)m, (float)n, (float)o, 0.25F);
      vertexConsumer.addVertex(matrix4f, this.frustumPoints[j].x(), this.frustumPoints[j].y(), this.frustumPoints[j].z()).setColor((float)m, (float)n, (float)o, 0.25F);
      vertexConsumer.addVertex(matrix4f, this.frustumPoints[k].x(), this.frustumPoints[k].y(), this.frustumPoints[k].z()).setColor((float)m, (float)n, (float)o, 0.25F);
      vertexConsumer.addVertex(matrix4f, this.frustumPoints[l].x(), this.frustumPoints[l].y(), this.frustumPoints[l].z()).setColor((float)m, (float)n, (float)o, 0.25F);
   }

   public void captureFrustum() {
      this.captureFrustum = true;
   }

   public void killFrustum() {
      this.capturedFrustum = null;
   }

   public void tick() {
      if (this.level.tickRateManager().runsNormally()) {
         ++this.ticks;
      }

      if (this.ticks % 20 == 0) {
         ObjectIterator iterator = this.destroyingBlocks.values().iterator();

         while(iterator.hasNext()) {
            BlockDestructionProgress blockDestructionProgress = (BlockDestructionProgress)iterator.next();
            int i = blockDestructionProgress.getUpdatedRenderTick();
            if (this.ticks - i > 400) {
               iterator.remove();
               this.removeProgress(blockDestructionProgress);
            }
         }

      }
   }

   private void removeProgress(BlockDestructionProgress blockDestructionProgress) {
      long l = blockDestructionProgress.getPos().asLong();
      Set<BlockDestructionProgress> set = (Set)this.destructionProgress.get(l);
      set.remove(blockDestructionProgress);
      if (set.isEmpty()) {
         this.destructionProgress.remove(l);
      }

   }

   private void renderEndSky(PoseStack poseStack) {
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
      Tesselator tesselator = Tesselator.getInstance();

      for(int i = 0; i < 6; ++i) {
         poseStack.pushPose();
         if (i == 1) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
         }

         if (i == 2) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
         }

         if (i == 3) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
         }

         if (i == 4) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
         }

         if (i == 5) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
         }

         Matrix4f matrix4f = poseStack.last().pose();
         BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(-14145496);
         bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(-14145496);
         bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(-14145496);
         bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(-14145496);
         BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
         poseStack.popPose();
      }

      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }

   public void renderSky(Matrix4f matrix4f, Matrix4f matrix4f2, float f, Camera camera, boolean bl, Runnable runnable) {
      runnable.run();
      if (!bl) {
         FogType fogType = camera.getFluidInCamera();
         if (fogType != FogType.POWDER_SNOW && fogType != FogType.LAVA && !this.doesMobEffectBlockSky(camera)) {
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(matrix4f);
            if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
               this.renderEndSky(poseStack);
            } else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
               Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), f);
               float g = (float)vec3.x;
               float h = (float)vec3.y;
               float i = (float)vec3.z;
               FogRenderer.levelFogColor();
               Tesselator tesselator = Tesselator.getInstance();
               RenderSystem.depthMask(false);
               RenderSystem.setShaderColor(g, h, i, 1.0F);
               ShaderInstance shaderInstance = RenderSystem.getShader();
               this.skyBuffer.bind();
               this.skyBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderInstance);
               VertexBuffer.unbind();
               RenderSystem.enableBlend();
               float[] fs = this.level.effects().getSunriseColor(this.level.getTimeOfDay(f), f);
               float j;
               float l;
               float p;
               float q;
               float r;
               if (fs != null) {
                  RenderSystem.setShader(GameRenderer::getPositionColorShader);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  poseStack.pushPose();
                  poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                  j = Mth.sin(this.level.getSunAngle(f)) < 0.0F ? 180.0F : 0.0F;
                  poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(j));
                  poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
                  float k = fs[0];
                  l = fs[1];
                  float m = fs[2];
                  Matrix4f matrix4f3 = poseStack.last().pose();
                  BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                  bufferBuilder.addVertex(matrix4f3, 0.0F, 100.0F, 0.0F).setColor(k, l, m, fs[3]);
                  int n = true;

                  for(int o = 0; o <= 16; ++o) {
                     p = (float)o * 6.2831855F / 16.0F;
                     q = Mth.sin(p);
                     r = Mth.cos(p);
                     bufferBuilder.addVertex(matrix4f3, q * 120.0F, r * 120.0F, -r * 40.0F * fs[3]).setColor(fs[0], fs[1], fs[2], 0.0F);
                  }

                  BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
                  poseStack.popPose();
               }

               RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
               poseStack.pushPose();
               j = 1.0F - this.level.getRainLevel(f);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, j);
               poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
               poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(this.level.getTimeOfDay(f) * 360.0F));
               Matrix4f matrix4f4 = poseStack.last().pose();
               l = 30.0F;
               RenderSystem.setShader(GameRenderer::getPositionTexShader);
               RenderSystem.setShaderTexture(0, SUN_LOCATION);
               BufferBuilder bufferBuilder2 = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
               bufferBuilder2.addVertex(matrix4f4, -l, 100.0F, -l).setUv(0.0F, 0.0F);
               bufferBuilder2.addVertex(matrix4f4, l, 100.0F, -l).setUv(1.0F, 0.0F);
               bufferBuilder2.addVertex(matrix4f4, l, 100.0F, l).setUv(1.0F, 1.0F);
               bufferBuilder2.addVertex(matrix4f4, -l, 100.0F, l).setUv(0.0F, 1.0F);
               BufferUploader.drawWithShader(bufferBuilder2.buildOrThrow());
               l = 20.0F;
               RenderSystem.setShaderTexture(0, MOON_LOCATION);
               int s = this.level.getMoonPhase();
               int t = s % 4;
               int n = s / 4 % 2;
               float u = (float)(t + 0) / 4.0F;
               p = (float)(n + 0) / 2.0F;
               q = (float)(t + 1) / 4.0F;
               r = (float)(n + 1) / 2.0F;
               bufferBuilder2 = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
               bufferBuilder2.addVertex(matrix4f4, -l, -100.0F, l).setUv(q, r);
               bufferBuilder2.addVertex(matrix4f4, l, -100.0F, l).setUv(u, r);
               bufferBuilder2.addVertex(matrix4f4, l, -100.0F, -l).setUv(u, p);
               bufferBuilder2.addVertex(matrix4f4, -l, -100.0F, -l).setUv(q, p);
               BufferUploader.drawWithShader(bufferBuilder2.buildOrThrow());
               float v = this.level.getStarBrightness(f) * j;
               if (v > 0.0F) {
                  RenderSystem.setShaderColor(v, v, v, v);
                  FogRenderer.setupNoFog();
                  this.starBuffer.bind();
                  this.starBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, GameRenderer.getPositionShader());
                  VertexBuffer.unbind();
                  runnable.run();
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.disableBlend();
               RenderSystem.defaultBlendFunc();
               poseStack.popPose();
               RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
               double d = this.minecraft.player.getEyePosition(f).y - this.level.getLevelData().getHorizonHeight(this.level);
               if (d < 0.0D) {
                  poseStack.pushPose();
                  poseStack.translate(0.0F, 12.0F, 0.0F);
                  this.darkBuffer.bind();
                  this.darkBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderInstance);
                  VertexBuffer.unbind();
                  poseStack.popPose();
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.depthMask(true);
            }
         }
      }
   }

   private boolean doesMobEffectBlockSky(Camera camera) {
      Entity var3 = camera.getEntity();
      if (!(var3 instanceof LivingEntity)) {
         return false;
      } else {
         LivingEntity livingEntity = (LivingEntity)var3;
         return livingEntity.hasEffect(MobEffects.BLINDNESS) || livingEntity.hasEffect(MobEffects.DARKNESS);
      }
   }

   public void renderClouds(PoseStack poseStack, Matrix4f matrix4f, Matrix4f matrix4f2, float f, double d, double e, double g) {
      float h = this.level.effects().getCloudHeight();
      if (!Float.isNaN(h)) {
         float i = 12.0F;
         float j = 4.0F;
         double k = 2.0E-4D;
         double l = (double)(((float)this.ticks + f) * 0.03F);
         double m = (d + l) / 12.0D;
         double n = (double)(h - (float)e + 0.33F);
         double o = g / 12.0D + 0.33000001311302185D;
         m -= (double)(Mth.floor(m / 2048.0D) * 2048);
         o -= (double)(Mth.floor(o / 2048.0D) * 2048);
         float p = (float)(m - (double)Mth.floor(m));
         float q = (float)(n / 4.0D - (double)Mth.floor(n / 4.0D)) * 4.0F;
         float r = (float)(o - (double)Mth.floor(o));
         Vec3 vec3 = this.level.getCloudColor(f);
         int s = (int)Math.floor(m);
         int t = (int)Math.floor(n / 4.0D);
         int u = (int)Math.floor(o);
         if (s != this.prevCloudX || t != this.prevCloudY || u != this.prevCloudZ || this.minecraft.options.getCloudsType() != this.prevCloudsType || this.prevCloudColor.distanceToSqr(vec3) > 2.0E-4D) {
            this.prevCloudX = s;
            this.prevCloudY = t;
            this.prevCloudZ = u;
            this.prevCloudColor = vec3;
            this.prevCloudsType = this.minecraft.options.getCloudsType();
            this.generateClouds = true;
         }

         if (this.generateClouds) {
            this.generateClouds = false;
            if (this.cloudBuffer != null) {
               this.cloudBuffer.close();
            }

            this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.cloudBuffer.bind();
            this.cloudBuffer.upload(this.buildClouds(Tesselator.getInstance(), m, n, o, vec3));
            VertexBuffer.unbind();
         }

         FogRenderer.levelFogColor();
         poseStack.pushPose();
         poseStack.mulPose(matrix4f);
         poseStack.scale(12.0F, 1.0F, 12.0F);
         poseStack.translate(-p, q, -r);
         if (this.cloudBuffer != null) {
            this.cloudBuffer.bind();
            int v = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

            for(int w = v; w < 2; ++w) {
               RenderType renderType = w == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
               renderType.setupRenderState();
               ShaderInstance shaderInstance = RenderSystem.getShader();
               this.cloudBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderInstance);
               renderType.clearRenderState();
            }

            VertexBuffer.unbind();
         }

         poseStack.popPose();
      }
   }

   private MeshData buildClouds(Tesselator tesselator, double d, double e, double f, Vec3 vec3) {
      float g = 4.0F;
      float h = 0.00390625F;
      int i = true;
      int j = true;
      float k = 9.765625E-4F;
      float l = (float)Mth.floor(d) * 0.00390625F;
      float m = (float)Mth.floor(f) * 0.00390625F;
      float n = (float)vec3.x;
      float o = (float)vec3.y;
      float p = (float)vec3.z;
      float q = n * 0.9F;
      float r = o * 0.9F;
      float s = p * 0.9F;
      float t = n * 0.7F;
      float u = o * 0.7F;
      float v = p * 0.7F;
      float w = n * 0.8F;
      float x = o * 0.8F;
      float y = p * 0.8F;
      BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
      float z = (float)Math.floor(e / 4.0D) * 4.0F;
      if (this.prevCloudsType == CloudStatus.FANCY) {
         for(int aa = -3; aa <= 4; ++aa) {
            for(int ab = -3; ab <= 4; ++ab) {
               float ac = (float)(aa * 8);
               float ad = (float)(ab * 8);
               if (z > -5.0F) {
                  bufferBuilder.addVertex(ac + 0.0F, z + 0.0F, ad + 8.0F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(t, u, v, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
                  bufferBuilder.addVertex(ac + 8.0F, z + 0.0F, ad + 8.0F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(t, u, v, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
                  bufferBuilder.addVertex(ac + 8.0F, z + 0.0F, ad + 0.0F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(t, u, v, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
                  bufferBuilder.addVertex(ac + 0.0F, z + 0.0F, ad + 0.0F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(t, u, v, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
               }

               if (z <= 5.0F) {
                  bufferBuilder.addVertex(ac + 0.0F, z + 4.0F - 9.765625E-4F, ad + 8.0F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, 1.0F, 0.0F);
                  bufferBuilder.addVertex(ac + 8.0F, z + 4.0F - 9.765625E-4F, ad + 8.0F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, 1.0F, 0.0F);
                  bufferBuilder.addVertex(ac + 8.0F, z + 4.0F - 9.765625E-4F, ad + 0.0F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, 1.0F, 0.0F);
                  bufferBuilder.addVertex(ac + 0.0F, z + 4.0F - 9.765625E-4F, ad + 0.0F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, 1.0F, 0.0F);
               }

               int ae;
               if (aa > -1) {
                  for(ae = 0; ae < 8; ++ae) {
                     bufferBuilder.addVertex(ac + (float)ae + 0.0F, z + 0.0F, ad + 8.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(-1.0F, 0.0F, 0.0F);
                     bufferBuilder.addVertex(ac + (float)ae + 0.0F, z + 4.0F, ad + 8.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(-1.0F, 0.0F, 0.0F);
                     bufferBuilder.addVertex(ac + (float)ae + 0.0F, z + 4.0F, ad + 0.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(-1.0F, 0.0F, 0.0F);
                     bufferBuilder.addVertex(ac + (float)ae + 0.0F, z + 0.0F, ad + 0.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(-1.0F, 0.0F, 0.0F);
                  }
               }

               if (aa <= 1) {
                  for(ae = 0; ae < 8; ++ae) {
                     bufferBuilder.addVertex(ac + (float)ae + 1.0F - 9.765625E-4F, z + 0.0F, ad + 8.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(1.0F, 0.0F, 0.0F);
                     bufferBuilder.addVertex(ac + (float)ae + 1.0F - 9.765625E-4F, z + 4.0F, ad + 8.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 8.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(1.0F, 0.0F, 0.0F);
                     bufferBuilder.addVertex(ac + (float)ae + 1.0F - 9.765625E-4F, z + 4.0F, ad + 0.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(1.0F, 0.0F, 0.0F);
                     bufferBuilder.addVertex(ac + (float)ae + 1.0F - 9.765625E-4F, z + 0.0F, ad + 0.0F).setUv((ac + (float)ae + 0.5F) * 0.00390625F + l, (ad + 0.0F) * 0.00390625F + m).setColor(q, r, s, 0.8F).setNormal(1.0F, 0.0F, 0.0F);
                  }
               }

               if (ab > -1) {
                  for(ae = 0; ae < 8; ++ae) {
                     bufferBuilder.addVertex(ac + 0.0F, z + 4.0F, ad + (float)ae + 0.0F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, -1.0F);
                     bufferBuilder.addVertex(ac + 8.0F, z + 4.0F, ad + (float)ae + 0.0F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, -1.0F);
                     bufferBuilder.addVertex(ac + 8.0F, z + 0.0F, ad + (float)ae + 0.0F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, -1.0F);
                     bufferBuilder.addVertex(ac + 0.0F, z + 0.0F, ad + (float)ae + 0.0F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, -1.0F);
                  }
               }

               if (ab <= 1) {
                  for(ae = 0; ae < 8; ++ae) {
                     bufferBuilder.addVertex(ac + 0.0F, z + 4.0F, ad + (float)ae + 1.0F - 9.765625E-4F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, 1.0F);
                     bufferBuilder.addVertex(ac + 8.0F, z + 4.0F, ad + (float)ae + 1.0F - 9.765625E-4F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, 1.0F);
                     bufferBuilder.addVertex(ac + 8.0F, z + 0.0F, ad + (float)ae + 1.0F - 9.765625E-4F).setUv((ac + 8.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, 1.0F);
                     bufferBuilder.addVertex(ac + 0.0F, z + 0.0F, ad + (float)ae + 1.0F - 9.765625E-4F).setUv((ac + 0.0F) * 0.00390625F + l, (ad + (float)ae + 0.5F) * 0.00390625F + m).setColor(w, x, y, 0.8F).setNormal(0.0F, 0.0F, 1.0F);
                  }
               }
            }
         }
      } else {
         int aa = true;
         int ab = true;

         for(int af = -32; af < 32; af += 32) {
            for(int ag = -32; ag < 32; ag += 32) {
               bufferBuilder.addVertex((float)(af + 0), z, (float)(ag + 32)).setUv((float)(af + 0) * 0.00390625F + l, (float)(ag + 32) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
               bufferBuilder.addVertex((float)(af + 32), z, (float)(ag + 32)).setUv((float)(af + 32) * 0.00390625F + l, (float)(ag + 32) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
               bufferBuilder.addVertex((float)(af + 32), z, (float)(ag + 0)).setUv((float)(af + 32) * 0.00390625F + l, (float)(ag + 0) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
               bufferBuilder.addVertex((float)(af + 0), z, (float)(ag + 0)).setUv((float)(af + 0) * 0.00390625F + l, (float)(ag + 0) * 0.00390625F + m).setColor(n, o, p, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
            }
         }
      }

      return bufferBuilder.buildOrThrow();
   }

   private void compileSections(Camera camera) {
      this.minecraft.getProfiler().push("populate_sections_to_compile");
      LevelLightEngine levelLightEngine = this.level.getLightEngine();
      RenderRegionCache renderRegionCache = new RenderRegionCache();
      BlockPos blockPos = camera.getBlockPosition();
      List<SectionRenderDispatcher.RenderSection> list = Lists.newArrayList();
      ObjectListIterator var6 = this.visibleSections.iterator();

      while(true) {
         SectionRenderDispatcher.RenderSection renderSection;
         SectionPos sectionPos;
         do {
            do {
               if (!var6.hasNext()) {
                  this.minecraft.getProfiler().popPush("upload");
                  this.sectionRenderDispatcher.uploadAllPendingUploads();
                  this.minecraft.getProfiler().popPush("schedule_async_compile");
                  Iterator var11 = list.iterator();

                  while(var11.hasNext()) {
                     renderSection = (SectionRenderDispatcher.RenderSection)var11.next();
                     renderSection.rebuildSectionAsync(this.sectionRenderDispatcher, renderRegionCache);
                     renderSection.setNotDirty();
                  }

                  this.minecraft.getProfiler().pop();
                  return;
               }

               renderSection = (SectionRenderDispatcher.RenderSection)var6.next();
               sectionPos = SectionPos.of(renderSection.getOrigin());
            } while(!renderSection.isDirty());
         } while(!levelLightEngine.lightOnInSection(sectionPos));

         boolean bl = false;
         if (this.minecraft.options.prioritizeChunkUpdates().get() != PrioritizeChunkUpdates.NEARBY) {
            if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
               bl = renderSection.isDirtyFromPlayer();
            }
         } else {
            BlockPos blockPos2 = renderSection.getOrigin().offset(8, 8, 8);
            bl = blockPos2.distSqr(blockPos) < 768.0D || renderSection.isDirtyFromPlayer();
         }

         if (bl) {
            this.minecraft.getProfiler().push("build_near_sync");
            this.sectionRenderDispatcher.rebuildSectionSync(renderSection, renderRegionCache);
            renderSection.setNotDirty();
            this.minecraft.getProfiler().pop();
         } else {
            list.add(renderSection);
         }
      }
   }

   private void renderWorldBorder(Camera camera) {
      WorldBorder worldBorder = this.level.getWorldBorder();
      double d = (double)(this.minecraft.options.getEffectiveRenderDistance() * 16);
      if (!(camera.getPosition().x < worldBorder.getMaxX() - d) || !(camera.getPosition().x > worldBorder.getMinX() + d) || !(camera.getPosition().z < worldBorder.getMaxZ() - d) || !(camera.getPosition().z > worldBorder.getMinZ() + d)) {
         double e = 1.0D - worldBorder.getDistanceToBorder(camera.getPosition().x, camera.getPosition().z) / d;
         e = Math.pow(e, 4.0D);
         e = Mth.clamp(e, 0.0D, 1.0D);
         double f = camera.getPosition().x;
         double g = camera.getPosition().z;
         double h = (double)this.minecraft.gameRenderer.getDepthFar();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
         RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         int i = worldBorder.getStatus().getColor();
         float j = (float)(i >> 16 & 255) / 255.0F;
         float k = (float)(i >> 8 & 255) / 255.0F;
         float l = (float)(i & 255) / 255.0F;
         RenderSystem.setShaderColor(j, k, l, (float)e);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.polygonOffset(-3.0F, -3.0F);
         RenderSystem.enablePolygonOffset();
         RenderSystem.disableCull();
         float m = (float)(Util.getMillis() % 3000L) / 3000.0F;
         float n = (float)(-Mth.frac(camera.getPosition().y * 0.5D));
         float o = n + (float)h;
         BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         double p = Math.max((double)Mth.floor(g - d), worldBorder.getMinZ());
         double q = Math.min((double)Mth.ceil(g + d), worldBorder.getMaxZ());
         float r = (float)(Mth.floor(p) & 1) * 0.5F;
         float s;
         double t;
         double u;
         float v;
         if (f > worldBorder.getMaxX() - d) {
            s = r;

            for(t = p; t < q; s += 0.5F) {
               u = Math.min(1.0D, q - t);
               v = (float)u * 0.5F;
               bufferBuilder.addVertex((float)(worldBorder.getMaxX() - f), (float)(-h), (float)(t - g)).setUv(m - s, m + o);
               bufferBuilder.addVertex((float)(worldBorder.getMaxX() - f), (float)(-h), (float)(t + u - g)).setUv(m - (v + s), m + o);
               bufferBuilder.addVertex((float)(worldBorder.getMaxX() - f), (float)h, (float)(t + u - g)).setUv(m - (v + s), m + n);
               bufferBuilder.addVertex((float)(worldBorder.getMaxX() - f), (float)h, (float)(t - g)).setUv(m - s, m + n);
               ++t;
            }
         }

         if (f < worldBorder.getMinX() + d) {
            s = r;

            for(t = p; t < q; s += 0.5F) {
               u = Math.min(1.0D, q - t);
               v = (float)u * 0.5F;
               bufferBuilder.addVertex((float)(worldBorder.getMinX() - f), (float)(-h), (float)(t - g)).setUv(m + s, m + o);
               bufferBuilder.addVertex((float)(worldBorder.getMinX() - f), (float)(-h), (float)(t + u - g)).setUv(m + v + s, m + o);
               bufferBuilder.addVertex((float)(worldBorder.getMinX() - f), (float)h, (float)(t + u - g)).setUv(m + v + s, m + n);
               bufferBuilder.addVertex((float)(worldBorder.getMinX() - f), (float)h, (float)(t - g)).setUv(m + s, m + n);
               ++t;
            }
         }

         p = Math.max((double)Mth.floor(f - d), worldBorder.getMinX());
         q = Math.min((double)Mth.ceil(f + d), worldBorder.getMaxX());
         r = (float)(Mth.floor(p) & 1) * 0.5F;
         if (g > worldBorder.getMaxZ() - d) {
            s = r;

            for(t = p; t < q; s += 0.5F) {
               u = Math.min(1.0D, q - t);
               v = (float)u * 0.5F;
               bufferBuilder.addVertex((float)(t - f), (float)(-h), (float)(worldBorder.getMaxZ() - g)).setUv(m + s, m + o);
               bufferBuilder.addVertex((float)(t + u - f), (float)(-h), (float)(worldBorder.getMaxZ() - g)).setUv(m + v + s, m + o);
               bufferBuilder.addVertex((float)(t + u - f), (float)h, (float)(worldBorder.getMaxZ() - g)).setUv(m + v + s, m + n);
               bufferBuilder.addVertex((float)(t - f), (float)h, (float)(worldBorder.getMaxZ() - g)).setUv(m + s, m + n);
               ++t;
            }
         }

         if (g < worldBorder.getMinZ() + d) {
            s = r;

            for(t = p; t < q; s += 0.5F) {
               u = Math.min(1.0D, q - t);
               v = (float)u * 0.5F;
               bufferBuilder.addVertex((float)(t - f), (float)(-h), (float)(worldBorder.getMinZ() - g)).setUv(m - s, m + o);
               bufferBuilder.addVertex((float)(t + u - f), (float)(-h), (float)(worldBorder.getMinZ() - g)).setUv(m - (v + s), m + o);
               bufferBuilder.addVertex((float)(t + u - f), (float)h, (float)(worldBorder.getMinZ() - g)).setUv(m - (v + s), m + n);
               bufferBuilder.addVertex((float)(t - f), (float)h, (float)(worldBorder.getMinZ() - g)).setUv(m - s, m + n);
               ++t;
            }
         }

         MeshData meshData = bufferBuilder.build();
         if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
         }

         RenderSystem.enableCull();
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.depthMask(true);
      }
   }

   private void renderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
      renderShape(poseStack, vertexConsumer, blockState.getShape(this.level, blockPos, CollisionContext.of(entity)), (double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f, 0.0F, 0.0F, 0.0F, 0.4F);
   }

   private static Vec3 mixColor(float f) {
      float g = 5.99999F;
      int i = (int)(Mth.clamp(f, 0.0F, 1.0F) * 5.99999F);
      float h = f * 5.99999F - (float)i;
      Vec3 var10000;
      switch(i) {
      case 0:
         var10000 = new Vec3(1.0D, (double)h, 0.0D);
         break;
      case 1:
         var10000 = new Vec3((double)(1.0F - h), 1.0D, 0.0D);
         break;
      case 2:
         var10000 = new Vec3(0.0D, 1.0D, (double)h);
         break;
      case 3:
         var10000 = new Vec3(0.0D, 1.0D - (double)h, 1.0D);
         break;
      case 4:
         var10000 = new Vec3((double)h, 0.0D, 1.0D);
         break;
      case 5:
         var10000 = new Vec3(1.0D, 0.0D, 1.0D - (double)h);
         break;
      default:
         throw new IllegalStateException("Unexpected value: " + i);
      }

      return var10000;
   }

   private static Vec3 shiftHue(float f, float g, float h, float i) {
      Vec3 vec3 = mixColor(i).scale((double)f);
      Vec3 vec32 = mixColor((i + 0.33333334F) % 1.0F).scale((double)g);
      Vec3 vec33 = mixColor((i + 0.6666667F) % 1.0F).scale((double)h);
      Vec3 vec34 = vec3.add(vec32).add(vec33);
      double d = Math.max(Math.max(1.0D, vec34.x), Math.max(vec34.y, vec34.z));
      return new Vec3(vec34.x / d, vec34.y / d, vec34.z / d);
   }

   public static void renderVoxelShape(PoseStack poseStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j, boolean bl) {
      List<AABB> list = voxelShape.toAabbs();
      if (!list.isEmpty()) {
         int k = bl ? list.size() : list.size() * 8;
         renderShape(poseStack, vertexConsumer, Shapes.create((AABB)list.get(0)), d, e, f, g, h, i, j);

         for(int l = 1; l < list.size(); ++l) {
            AABB aABB = (AABB)list.get(l);
            float m = (float)l / (float)k;
            Vec3 vec3 = shiftHue(g, h, i, m);
            renderShape(poseStack, vertexConsumer, Shapes.create(aABB), d, e, f, (float)vec3.x, (float)vec3.y, (float)vec3.z, j);
         }

      }
   }

   private static void renderShape(PoseStack poseStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j) {
      PoseStack.Pose pose = poseStack.last();
      voxelShape.forAllEdges((k, l, m, n, o, p) -> {
         float q = (float)(n - k);
         float r = (float)(o - l);
         float s = (float)(p - m);
         float t = Mth.sqrt(q * q + r * r + s * s);
         q /= t;
         r /= t;
         s /= t;
         vertexConsumer.addVertex(pose, (float)(k + d), (float)(l + e), (float)(m + f)).setColor(g, h, i, j).setNormal(pose, q, r, s);
         vertexConsumer.addVertex(pose, (float)(n + d), (float)(o + e), (float)(p + f)).setColor(g, h, i, j).setNormal(pose, q, r, s);
      });
   }

   public static void renderLineBox(VertexConsumer vertexConsumer, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m) {
      renderLineBox(new PoseStack(), vertexConsumer, d, e, f, g, h, i, j, k, l, m, j, k, l);
   }

   public static void renderLineBox(PoseStack poseStack, VertexConsumer vertexConsumer, AABB aABB, float f, float g, float h, float i) {
      renderLineBox(poseStack, vertexConsumer, aABB.minX, aABB.minY, aABB.minZ, aABB.maxX, aABB.maxY, aABB.maxZ, f, g, h, i, f, g, h);
   }

   public static void renderLineBox(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m) {
      renderLineBox(poseStack, vertexConsumer, d, e, f, g, h, i, j, k, l, m, j, k, l);
   }

   public static void renderLineBox(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m, float n, float o, float p) {
      PoseStack.Pose pose = poseStack.last();
      float q = (float)d;
      float r = (float)e;
      float s = (float)f;
      float t = (float)g;
      float u = (float)h;
      float v = (float)i;
      vertexConsumer.addVertex(pose, q, r, s).setColor(j, o, p, m).setNormal(pose, 1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, r, s).setColor(j, o, p, m).setNormal(pose, 1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, r, s).setColor(n, k, p, m).setNormal(pose, 0.0F, 1.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, u, s).setColor(n, k, p, m).setNormal(pose, 0.0F, 1.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, r, s).setColor(n, o, l, m).setNormal(pose, 0.0F, 0.0F, 1.0F);
      vertexConsumer.addVertex(pose, q, r, v).setColor(n, o, l, m).setNormal(pose, 0.0F, 0.0F, 1.0F);
      vertexConsumer.addVertex(pose, t, r, s).setColor(j, k, l, m).setNormal(pose, 0.0F, 1.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, u, s).setColor(j, k, l, m).setNormal(pose, 0.0F, 1.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, u, s).setColor(j, k, l, m).setNormal(pose, -1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, u, s).setColor(j, k, l, m).setNormal(pose, -1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, u, s).setColor(j, k, l, m).setNormal(pose, 0.0F, 0.0F, 1.0F);
      vertexConsumer.addVertex(pose, q, u, v).setColor(j, k, l, m).setNormal(pose, 0.0F, 0.0F, 1.0F);
      vertexConsumer.addVertex(pose, q, u, v).setColor(j, k, l, m).setNormal(pose, 0.0F, -1.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, r, v).setColor(j, k, l, m).setNormal(pose, 0.0F, -1.0F, 0.0F);
      vertexConsumer.addVertex(pose, q, r, v).setColor(j, k, l, m).setNormal(pose, 1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, r, v).setColor(j, k, l, m).setNormal(pose, 1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, r, v).setColor(j, k, l, m).setNormal(pose, 0.0F, 0.0F, -1.0F);
      vertexConsumer.addVertex(pose, t, r, s).setColor(j, k, l, m).setNormal(pose, 0.0F, 0.0F, -1.0F);
      vertexConsumer.addVertex(pose, q, u, v).setColor(j, k, l, m).setNormal(pose, 1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, u, v).setColor(j, k, l, m).setNormal(pose, 1.0F, 0.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, r, v).setColor(j, k, l, m).setNormal(pose, 0.0F, 1.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, u, v).setColor(j, k, l, m).setNormal(pose, 0.0F, 1.0F, 0.0F);
      vertexConsumer.addVertex(pose, t, u, s).setColor(j, k, l, m).setNormal(pose, 0.0F, 0.0F, 1.0F);
      vertexConsumer.addVertex(pose, t, u, v).setColor(j, k, l, m).setNormal(pose, 0.0F, 0.0F, 1.0F);
   }

   public static void addChainedFilledBoxVertices(PoseStack poseStack, VertexConsumer vertexConsumer, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m) {
      addChainedFilledBoxVertices(poseStack, vertexConsumer, (float)d, (float)e, (float)f, (float)g, (float)h, (float)i, j, k, l, m);
   }

   public static void addChainedFilledBoxVertices(PoseStack poseStack, VertexConsumer vertexConsumer, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o) {
      Matrix4f matrix4f = poseStack.last().pose();
      vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, f, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, h).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
      vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
   }

   public static void renderFace(PoseStack poseStack, VertexConsumer vertexConsumer, Direction direction, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o) {
      Matrix4f matrix4f = poseStack.last().pose();
      switch(direction) {
      case DOWN:
         vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, g, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, g, k).setColor(l, m, n, o);
         break;
      case UP:
         vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, j, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, j, h).setColor(l, m, n, o);
         break;
      case NORTH:
         vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, j, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, g, h).setColor(l, m, n, o);
         break;
      case SOUTH:
         vertexConsumer.addVertex(matrix4f, f, g, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, j, k).setColor(l, m, n, o);
         break;
      case WEST:
         vertexConsumer.addVertex(matrix4f, f, g, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, g, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, j, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, f, j, h).setColor(l, m, n, o);
         break;
      case EAST:
         vertexConsumer.addVertex(matrix4f, i, g, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, j, h).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, j, k).setColor(l, m, n, o);
         vertexConsumer.addVertex(matrix4f, i, g, k).setColor(l, m, n, o);
      }

   }

   public void blockChanged(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, BlockState blockState2, int i) {
      this.setBlockDirty(blockPos, (i & 8) != 0);
   }

   private void setBlockDirty(BlockPos blockPos, boolean bl) {
      for(int i = blockPos.getZ() - 1; i <= blockPos.getZ() + 1; ++i) {
         for(int j = blockPos.getX() - 1; j <= blockPos.getX() + 1; ++j) {
            for(int k = blockPos.getY() - 1; k <= blockPos.getY() + 1; ++k) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), bl);
            }
         }
      }

   }

   public void setBlocksDirty(int i, int j, int k, int l, int m, int n) {
      for(int o = k - 1; o <= n + 1; ++o) {
         for(int p = i - 1; p <= l + 1; ++p) {
            for(int q = j - 1; q <= m + 1; ++q) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(p), SectionPos.blockToSectionCoord(q), SectionPos.blockToSectionCoord(o));
            }
         }
      }

   }

   public void setBlockDirty(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
      if (this.minecraft.getModelManager().requiresRender(blockState, blockState2)) {
         this.setBlocksDirty(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
      }

   }

   public void setSectionDirtyWithNeighbors(int i, int j, int k) {
      for(int l = k - 1; l <= k + 1; ++l) {
         for(int m = i - 1; m <= i + 1; ++m) {
            for(int n = j - 1; n <= j + 1; ++n) {
               this.setSectionDirty(m, n, l);
            }
         }
      }

   }

   public void setSectionDirty(int i, int j, int k) {
      this.setSectionDirty(i, j, k, false);
   }

   private void setSectionDirty(int i, int j, int k, boolean bl) {
      this.viewArea.setDirty(i, j, k, bl);
   }

   public void playJukeboxSong(Holder<JukeboxSong> holder, BlockPos blockPos) {
      if (this.level != null) {
         this.stopJukeboxSong(blockPos);
         JukeboxSong jukeboxSong = (JukeboxSong)holder.value();
         SoundEvent soundEvent = (SoundEvent)jukeboxSong.soundEvent().value();
         SoundInstance soundInstance = SimpleSoundInstance.forJukeboxSong(soundEvent, Vec3.atCenterOf(blockPos));
         this.playingJukeboxSongs.put(blockPos, soundInstance);
         this.minecraft.getSoundManager().play(soundInstance);
         this.minecraft.gui.setNowPlaying(jukeboxSong.description());
         this.notifyNearbyEntities(this.level, blockPos, true);
      }
   }

   private void stopJukeboxSong(BlockPos blockPos) {
      SoundInstance soundInstance = (SoundInstance)this.playingJukeboxSongs.remove(blockPos);
      if (soundInstance != null) {
         this.minecraft.getSoundManager().stop(soundInstance);
      }

   }

   public void stopJukeboxSongAndNotifyNearby(BlockPos blockPos) {
      this.stopJukeboxSong(blockPos);
      if (this.level != null) {
         this.notifyNearbyEntities(this.level, blockPos, false);
      }

   }

   private void notifyNearbyEntities(Level level, BlockPos blockPos, boolean bl) {
      List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, (new AABB(blockPos)).inflate(3.0D));
      Iterator var5 = list.iterator();

      while(var5.hasNext()) {
         LivingEntity livingEntity = (LivingEntity)var5.next();
         livingEntity.setRecordPlayingNearby(blockPos, bl);
      }

   }

   public void addParticle(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
      this.addParticle(particleOptions, bl, false, d, e, f, g, h, i);
   }

   public void addParticle(ParticleOptions particleOptions, boolean bl, boolean bl2, double d, double e, double f, double g, double h, double i) {
      try {
         this.addParticleInternal(particleOptions, bl, bl2, d, e, f, g, h, i);
      } catch (Throwable var19) {
         CrashReport crashReport = CrashReport.forThrowable(var19, "Exception while adding particle");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being added");
         crashReportCategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(particleOptions.getType()));
         crashReportCategory.setDetail("Parameters", () -> {
            return ParticleTypes.CODEC.encodeStart(this.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), particleOptions).toString();
         });
         crashReportCategory.setDetail("Position", () -> {
            return CrashReportCategory.formatLocation(this.level, d, e, f);
         });
         throw new ReportedException(crashReport);
      }
   }

   private <T extends ParticleOptions> void addParticle(T particleOptions, double d, double e, double f, double g, double h, double i) {
      this.addParticle(particleOptions, particleOptions.getType().getOverrideLimiter(), d, e, f, g, h, i);
   }

   @Nullable
   private Particle addParticleInternal(ParticleOptions particleOptions, boolean bl, double d, double e, double f, double g, double h, double i) {
      return this.addParticleInternal(particleOptions, bl, false, d, e, f, g, h, i);
   }

   @Nullable
   private Particle addParticleInternal(ParticleOptions particleOptions, boolean bl, boolean bl2, double d, double e, double f, double g, double h, double i) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      ParticleStatus particleStatus = this.calculateParticleLevel(bl2);
      if (bl) {
         return this.minecraft.particleEngine.createParticle(particleOptions, d, e, f, g, h, i);
      } else if (camera.getPosition().distanceToSqr(d, e, f) > 1024.0D) {
         return null;
      } else {
         return particleStatus == ParticleStatus.MINIMAL ? null : this.minecraft.particleEngine.createParticle(particleOptions, d, e, f, g, h, i);
      }
   }

   private ParticleStatus calculateParticleLevel(boolean bl) {
      ParticleStatus particleStatus = (ParticleStatus)this.minecraft.options.particles().get();
      if (bl && particleStatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
         particleStatus = ParticleStatus.DECREASED;
      }

      if (particleStatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
         particleStatus = ParticleStatus.MINIMAL;
      }

      return particleStatus;
   }

   public void clear() {
   }

   public void globalLevelEvent(int i, BlockPos blockPos, int j) {
      switch(i) {
      case 1023:
      case 1028:
      case 1038:
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         if (camera.isInitialized()) {
            double d = (double)blockPos.getX() - camera.getPosition().x;
            double e = (double)blockPos.getY() - camera.getPosition().y;
            double f = (double)blockPos.getZ() - camera.getPosition().z;
            double g = Math.sqrt(d * d + e * e + f * f);
            double h = camera.getPosition().x;
            double k = camera.getPosition().y;
            double l = camera.getPosition().z;
            if (g > 0.0D) {
               h += d / g * 2.0D;
               k += e / g * 2.0D;
               l += f / g * 2.0D;
            }

            if (i == 1023) {
               this.level.playLocalSound(h, k, l, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            } else if (i == 1038) {
               this.level.playLocalSound(h, k, l, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            } else {
               this.level.playLocalSound(h, k, l, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
            }
         }
      default:
      }
   }

   public void levelEvent(int i, BlockPos blockPos, int j) {
      RandomSource randomSource = this.level.random;
      int u;
      double v;
      double w;
      double x;
      double r;
      float n;
      int o;
      float ad;
      double g;
      double p;
      double q;
      switch(i) {
      case 1000:
         this.level.playLocalSound(blockPos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1001:
         this.level.playLocalSound(blockPos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
         break;
      case 1002:
         this.level.playLocalSound(blockPos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
         break;
      case 1004:
         this.level.playLocalSound(blockPos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
         break;
      case 1009:
         if (j == 0) {
            this.level.playLocalSound(blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.8F, false);
         } else if (j == 1) {
            this.level.playLocalSound(blockPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.4F, false);
         }
         break;
      case 1010:
         this.level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getHolder(j).ifPresent((reference) -> {
            this.playJukeboxSong(reference, blockPos);
         });
         break;
      case 1011:
         this.stopJukeboxSongAndNotifyNearby(blockPos);
         break;
      case 1015:
         this.level.playLocalSound(blockPos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1016:
         this.level.playLocalSound(blockPos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1017:
         this.level.playLocalSound(blockPos, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1018:
         this.level.playLocalSound(blockPos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1019:
         this.level.playLocalSound(blockPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1020:
         this.level.playLocalSound(blockPos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1021:
         this.level.playLocalSound(blockPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1022:
         this.level.playLocalSound(blockPos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1024:
         this.level.playLocalSound(blockPos, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1025:
         this.level.playLocalSound(blockPos, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1026:
         this.level.playLocalSound(blockPos, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1027:
         this.level.playLocalSound(blockPos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1029:
         this.level.playLocalSound(blockPos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1030:
         this.level.playLocalSound(blockPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1031:
         this.level.playLocalSound(blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1032:
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, randomSource.nextFloat() * 0.4F + 0.8F, 0.25F));
         break;
      case 1033:
         this.level.playLocalSound(blockPos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1034:
         this.level.playLocalSound(blockPos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1035:
         this.level.playLocalSound(blockPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1039:
         this.level.playLocalSound(blockPos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1040:
         this.level.playLocalSound(blockPos, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1041:
         this.level.playLocalSound(blockPos, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1042:
         this.level.playLocalSound(blockPos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1043:
         this.level.playLocalSound(blockPos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1044:
         this.level.playLocalSound(blockPos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1045:
         this.level.playLocalSound(blockPos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1046:
         this.level.playLocalSound(blockPos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1047:
         this.level.playLocalSound(blockPos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1048:
         this.level.playLocalSound(blockPos, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1049:
         this.level.playLocalSound(blockPos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1050:
         this.level.playLocalSound(blockPos, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1051:
         this.level.playLocalSound(blockPos, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F), false);
      case 2010:
         this.shootParticles(j, blockPos, randomSource, ParticleTypes.WHITE_SMOKE);
         break;
      case 1500:
         ComposterBlock.handleFill(this.level, blockPos, j > 0);
         break;
      case 1501:
         this.level.playLocalSound(blockPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.8F, false);

         for(o = 0; o < 8; ++o) {
            this.level.addParticle(ParticleTypes.LARGE_SMOKE, (double)blockPos.getX() + randomSource.nextDouble(), (double)blockPos.getY() + 1.2D, (double)blockPos.getZ() + randomSource.nextDouble(), 0.0D, 0.0D, 0.0D);
         }

         return;
      case 1502:
         this.level.playLocalSound(blockPos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.8F, false);

         for(o = 0; o < 5; ++o) {
            g = (double)blockPos.getX() + randomSource.nextDouble() * 0.6D + 0.2D;
            p = (double)blockPos.getY() + randomSource.nextDouble() * 0.6D + 0.2D;
            q = (double)blockPos.getZ() + randomSource.nextDouble() * 0.6D + 0.2D;
            this.level.addParticle(ParticleTypes.SMOKE, g, p, q, 0.0D, 0.0D, 0.0D);
         }

         return;
      case 1503:
         this.level.playLocalSound(blockPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

         for(o = 0; o < 16; ++o) {
            g = (double)blockPos.getX() + (5.0D + randomSource.nextDouble() * 6.0D) / 16.0D;
            p = (double)blockPos.getY() + 0.8125D;
            q = (double)blockPos.getZ() + (5.0D + randomSource.nextDouble() * 6.0D) / 16.0D;
            this.level.addParticle(ParticleTypes.SMOKE, g, p, q, 0.0D, 0.0D, 0.0D);
         }

         return;
      case 1504:
         PointedDripstoneBlock.spawnDripParticle(this.level, blockPos, this.level.getBlockState(blockPos));
         break;
      case 1505:
         BoneMealItem.addGrowthParticles(this.level, blockPos, j);
         this.level.playLocalSound(blockPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 2000:
         this.shootParticles(j, blockPos, randomSource, ParticleTypes.SMOKE);
         break;
      case 2001:
         BlockState blockState = Block.stateById(j);
         if (!blockState.isAir()) {
            SoundType soundType = blockState.getSoundType();
            this.level.playLocalSound(blockPos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false);
         }

         this.level.addDestroyBlockEffect(blockPos, blockState);
         break;
      case 2002:
      case 2007:
         Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);

         for(int l = 0; l < 8; ++l) {
            this.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)), vec3.x, vec3.y, vec3.z, randomSource.nextGaussian() * 0.15D, randomSource.nextDouble() * 0.2D, randomSource.nextGaussian() * 0.15D);
         }

         float h = (float)(j >> 16 & 255) / 255.0F;
         float m = (float)(j >> 8 & 255) / 255.0F;
         n = (float)(j >> 0 & 255) / 255.0F;
         ParticleOptions particleOptions = i == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

         for(o = 0; o < 100; ++o) {
            g = randomSource.nextDouble() * 4.0D;
            p = randomSource.nextDouble() * 3.141592653589793D * 2.0D;
            q = Math.cos(p) * g;
            r = 0.01D + randomSource.nextDouble() * 0.5D;
            double s = Math.sin(p) * g;
            Particle particle = this.addParticleInternal(particleOptions, particleOptions.getType().getOverrideLimiter(), vec3.x + q * 0.1D, vec3.y + 0.3D, vec3.z + s * 0.1D, q, r, s);
            if (particle != null) {
               float t = 0.75F + randomSource.nextFloat() * 0.25F;
               particle.setColor(h * t, m * t, n * t);
               particle.setPower((float)g);
            }
         }

         this.level.playLocalSound(blockPos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 2003:
         double d = (double)blockPos.getX() + 0.5D;
         double e = (double)blockPos.getY();
         double f = (double)blockPos.getZ() + 0.5D;

         for(int k = 0; k < 8; ++k) {
            this.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)), d, e, f, randomSource.nextGaussian() * 0.15D, randomSource.nextDouble() * 0.2D, randomSource.nextGaussian() * 0.15D);
         }

         for(g = 0.0D; g < 6.283185307179586D; g += 0.15707963267948966D) {
            this.addParticle(ParticleTypes.PORTAL, d + Math.cos(g) * 5.0D, e - 0.4D, f + Math.sin(g) * 5.0D, Math.cos(g) * -5.0D, 0.0D, Math.sin(g) * -5.0D);
            this.addParticle(ParticleTypes.PORTAL, d + Math.cos(g) * 5.0D, e - 0.4D, f + Math.sin(g) * 5.0D, Math.cos(g) * -7.0D, 0.0D, Math.sin(g) * -7.0D);
         }

         return;
      case 2004:
         for(u = 0; u < 20; ++u) {
            v = (double)blockPos.getX() + 0.5D + (randomSource.nextDouble() - 0.5D) * 2.0D;
            w = (double)blockPos.getY() + 0.5D + (randomSource.nextDouble() - 0.5D) * 2.0D;
            x = (double)blockPos.getZ() + 0.5D + (randomSource.nextDouble() - 0.5D) * 2.0D;
            this.level.addParticle(ParticleTypes.SMOKE, v, w, x, 0.0D, 0.0D, 0.0D);
            this.level.addParticle(ParticleTypes.FLAME, v, w, x, 0.0D, 0.0D, 0.0D);
         }

         return;
      case 2006:
         for(o = 0; o < 200; ++o) {
            ad = randomSource.nextFloat() * 4.0F;
            float ai = randomSource.nextFloat() * 6.2831855F;
            p = (double)(Mth.cos(ai) * ad);
            q = 0.01D + randomSource.nextDouble() * 0.5D;
            r = (double)(Mth.sin(ai) * ad);
            Particle particle2 = this.addParticleInternal(ParticleTypes.DRAGON_BREATH, false, (double)blockPos.getX() + p * 0.1D, (double)blockPos.getY() + 0.3D, (double)blockPos.getZ() + r * 0.1D, p, q, r);
            if (particle2 != null) {
               particle2.setPower(ad);
            }
         }

         if (j == 1) {
            this.level.playLocalSound(blockPos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
         }
         break;
      case 2008:
         this.level.addParticle(ParticleTypes.EXPLOSION, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         break;
      case 2009:
         for(o = 0; o < 8; ++o) {
            this.level.addParticle(ParticleTypes.CLOUD, (double)blockPos.getX() + randomSource.nextDouble(), (double)blockPos.getY() + 1.2D, (double)blockPos.getZ() + randomSource.nextDouble(), 0.0D, 0.0D, 0.0D);
         }

         return;
      case 2011:
         ParticleUtils.spawnParticleInBlock(this.level, blockPos, j, ParticleTypes.HAPPY_VILLAGER);
         break;
      case 2012:
         ParticleUtils.spawnParticleInBlock(this.level, blockPos, j, ParticleTypes.HAPPY_VILLAGER);
         break;
      case 2013:
         ParticleUtils.spawnSmashAttackParticles(this.level, blockPos, j);
         break;
      case 3000:
         this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         this.level.playLocalSound(blockPos, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 10.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
         break;
      case 3001:
         this.level.playLocalSound(blockPos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
         break;
      case 3002:
         if (j >= 0 && j < Axis.VALUES.length) {
            ParticleUtils.spawnParticlesAlongAxis(Axis.VALUES[j], this.level, blockPos, 0.125D, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19));
         } else {
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, blockPos, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
         }
         break;
      case 3003:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, blockPos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
         this.level.playLocalSound(blockPos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 3004:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, blockPos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
         break;
      case 3005:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, blockPos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
         break;
      case 3006:
         u = j >> 6;
         float z;
         float ab;
         if (u > 0) {
            if (randomSource.nextFloat() < 0.3F + (float)u * 0.1F) {
               n = 0.15F + 0.02F * (float)u * (float)u * randomSource.nextFloat();
               float y = 0.4F + 0.3F * (float)u * randomSource.nextFloat();
               this.level.playLocalSound(blockPos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, n, y, false);
            }

            byte b = (byte)(j & 63);
            IntProvider intProvider = UniformInt.of(0, u);
            z = 0.005F;
            Supplier<Vec3> supplier = () -> {
               return new Vec3(Mth.nextDouble(randomSource, -0.004999999888241291D, 0.004999999888241291D), Mth.nextDouble(randomSource, -0.004999999888241291D, 0.004999999888241291D), Mth.nextDouble(randomSource, -0.004999999888241291D, 0.004999999888241291D));
            };
            if (b == 0) {
               Direction[] var48 = Direction.values();
               int var13 = var48.length;

               for(int var14 = 0; var14 < var13; ++var14) {
                  Direction direction = var48[var14];
                  float aa = direction == Direction.DOWN ? 3.1415927F : 0.0F;
                  r = direction.getAxis() == Axis.Y ? 0.65D : 0.57D;
                  ParticleUtils.spawnParticlesOnBlockFace(this.level, blockPos, new SculkChargeParticleOptions(aa), intProvider, direction, supplier, r);
               }

               return;
            } else {
               Iterator var50 = MultifaceBlock.unpack(b).iterator();

               while(var50.hasNext()) {
                  Direction direction2 = (Direction)var50.next();
                  ab = direction2 == Direction.UP ? 3.1415927F : 0.0F;
                  q = 0.35D;
                  ParticleUtils.spawnParticlesOnBlockFace(this.level, blockPos, new SculkChargeParticleOptions(ab), intProvider, direction2, supplier, 0.35D);
               }

               return;
            }
         } else {
            this.level.playLocalSound(blockPos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            boolean bl = this.level.getBlockState(blockPos).isCollisionShapeFullBlock(this.level, blockPos);
            int ac = bl ? 40 : 20;
            z = bl ? 0.45F : 0.25F;
            ad = 0.07F;

            for(int ae = 0; ae < ac; ++ae) {
               float af = 2.0F * randomSource.nextFloat() - 1.0F;
               ab = 2.0F * randomSource.nextFloat() - 1.0F;
               float ag = 2.0F * randomSource.nextFloat() - 1.0F;
               this.level.addParticle(ParticleTypes.SCULK_CHARGE_POP, (double)blockPos.getX() + 0.5D + (double)(af * z), (double)blockPos.getY() + 0.5D + (double)(ab * z), (double)blockPos.getZ() + 0.5D + (double)(ag * z), (double)(af * 0.07F), (double)(ab * 0.07F), (double)(ag * 0.07F));
            }

            return;
         }
      case 3007:
         for(int ah = 0; ah < 10; ++ah) {
            this.level.addParticle(new ShriekParticleOption(ah * 5), false, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + SculkShriekerBlock.TOP_Y, (double)blockPos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         }

         BlockState blockState3 = this.level.getBlockState(blockPos);
         boolean bl2 = blockState3.hasProperty(BlockStateProperties.WATERLOGGED) && (Boolean)blockState3.getValue(BlockStateProperties.WATERLOGGED);
         if (!bl2) {
            this.level.playLocalSound((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + SculkShriekerBlock.TOP_Y, (double)blockPos.getZ() + 0.5D, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0F, 0.6F + this.level.random.nextFloat() * 0.4F, false);
         }
         break;
      case 3008:
         BlockState blockState2 = Block.stateById(j);
         Block var31 = blockState2.getBlock();
         if (var31 instanceof BrushableBlock) {
            BrushableBlock brushableBlock = (BrushableBlock)var31;
            this.level.playLocalSound(blockPos, brushableBlock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
         }

         this.level.addDestroyBlockEffect(blockPos, blockState2);
         break;
      case 3009:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, blockPos, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
         break;
      case 3011:
         TrialSpawner.addSpawnParticles(this.level, blockPos, randomSource, FlameParticle.decode(j).particleType);
         break;
      case 3012:
         this.level.playLocalSound(blockPos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         TrialSpawner.addSpawnParticles(this.level, blockPos, randomSource, FlameParticle.decode(j).particleType);
         break;
      case 3013:
         this.level.playLocalSound(blockPos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         TrialSpawner.addDetectPlayerParticles(this.level, blockPos, randomSource, j, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
         break;
      case 3014:
         this.level.playLocalSound(blockPos, SoundEvents.TRIAL_SPAWNER_EJECT_ITEM, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         TrialSpawner.addEjectItemParticles(this.level, blockPos, randomSource);
         break;
      case 3015:
         BlockEntity var30 = this.level.getBlockEntity(blockPos);
         if (var30 instanceof VaultBlockEntity) {
            VaultBlockEntity vaultBlockEntity = (VaultBlockEntity)var30;
            Client.emitActivationParticles(this.level, vaultBlockEntity.getBlockPos(), vaultBlockEntity.getBlockState(), vaultBlockEntity.getSharedData(), j == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
            this.level.playLocalSound(blockPos, SoundEvents.VAULT_ACTIVATE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         }
         break;
      case 3016:
         Client.emitDeactivationParticles(this.level, blockPos, j == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
         this.level.playLocalSound(blockPos, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         break;
      case 3017:
         TrialSpawner.addEjectItemParticles(this.level, blockPos, randomSource);
         break;
      case 3018:
         for(u = 0; u < 10; ++u) {
            v = randomSource.nextGaussian() * 0.02D;
            w = randomSource.nextGaussian() * 0.02D;
            x = randomSource.nextGaussian() * 0.02D;
            this.level.addParticle(ParticleTypes.POOF, (double)blockPos.getX() + randomSource.nextDouble(), (double)blockPos.getY() + randomSource.nextDouble(), (double)blockPos.getZ() + randomSource.nextDouble(), v, w, x);
         }

         this.level.playLocalSound(blockPos, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         break;
      case 3019:
         this.level.playLocalSound(blockPos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         TrialSpawner.addDetectPlayerParticles(this.level, blockPos, randomSource, j, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
         break;
      case 3020:
         this.level.playLocalSound(blockPos, SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE, SoundSource.BLOCKS, j == 0 ? 0.3F : 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         TrialSpawner.addDetectPlayerParticles(this.level, blockPos, randomSource, 0, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
         TrialSpawner.addBecomeOminousParticles(this.level, blockPos, randomSource);
         break;
      case 3021:
         this.level.playLocalSound(blockPos, SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true);
         TrialSpawner.addSpawnParticles(this.level, blockPos, randomSource, FlameParticle.decode(j).particleType);
      }

   }

   public void destroyBlockProgress(int i, BlockPos blockPos, int j) {
      BlockDestructionProgress blockDestructionProgress;
      if (j >= 0 && j < 10) {
         blockDestructionProgress = (BlockDestructionProgress)this.destroyingBlocks.get(i);
         if (blockDestructionProgress != null) {
            this.removeProgress(blockDestructionProgress);
         }

         if (blockDestructionProgress == null || blockDestructionProgress.getPos().getX() != blockPos.getX() || blockDestructionProgress.getPos().getY() != blockPos.getY() || blockDestructionProgress.getPos().getZ() != blockPos.getZ()) {
            blockDestructionProgress = new BlockDestructionProgress(i, blockPos);
            this.destroyingBlocks.put(i, blockDestructionProgress);
         }

         blockDestructionProgress.setProgress(j);
         blockDestructionProgress.updateTick(this.ticks);
         ((SortedSet)this.destructionProgress.computeIfAbsent(blockDestructionProgress.getPos().asLong(), (l) -> {
            return Sets.newTreeSet();
         })).add(blockDestructionProgress);
      } else {
         blockDestructionProgress = (BlockDestructionProgress)this.destroyingBlocks.remove(i);
         if (blockDestructionProgress != null) {
            this.removeProgress(blockDestructionProgress);
         }
      }

   }

   public boolean hasRenderedAllSections() {
      return this.sectionRenderDispatcher.isQueueEmpty();
   }

   public void onChunkLoaded(ChunkPos chunkPos) {
      this.sectionOcclusionGraph.onChunkLoaded(chunkPos);
   }

   public void needsUpdate() {
      this.sectionOcclusionGraph.invalidate();
      this.generateClouds = true;
   }

   public void updateGlobalBlockEntities(Collection<BlockEntity> collection, Collection<BlockEntity> collection2) {
      synchronized(this.globalBlockEntities) {
         this.globalBlockEntities.removeAll(collection);
         this.globalBlockEntities.addAll(collection2);
      }
   }

   public static int getLightColor(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos) {
      return getLightColor(blockAndTintGetter, blockAndTintGetter.getBlockState(blockPos), blockPos);
   }

   public static int getLightColor(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos) {
      if (blockState.emissiveRendering(blockAndTintGetter, blockPos)) {
         return 15728880;
      } else {
         int i = blockAndTintGetter.getBrightness(LightLayer.SKY, blockPos);
         int j = blockAndTintGetter.getBrightness(LightLayer.BLOCK, blockPos);
         int k = blockState.getLightEmission();
         if (j < k) {
            j = k;
         }

         return i << 20 | j << 4;
      }
   }

   public boolean isSectionCompiled(BlockPos blockPos) {
      SectionRenderDispatcher.RenderSection renderSection = this.viewArea.getRenderSectionAt(blockPos);
      return renderSection != null && renderSection.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
   }

   @Nullable
   public RenderTarget entityTarget() {
      return this.entityTarget;
   }

   @Nullable
   public RenderTarget getTranslucentTarget() {
      return this.translucentTarget;
   }

   @Nullable
   public RenderTarget getItemEntityTarget() {
      return this.itemEntityTarget;
   }

   @Nullable
   public RenderTarget getParticlesTarget() {
      return this.particlesTarget;
   }

   @Nullable
   public RenderTarget getWeatherTarget() {
      return this.weatherTarget;
   }

   @Nullable
   public RenderTarget getCloudsTarget() {
      return this.cloudsTarget;
   }

   private void shootParticles(int i, BlockPos blockPos, RandomSource randomSource, SimpleParticleType simpleParticleType) {
      Direction direction = Direction.from3DDataValue(i);
      int j = direction.getStepX();
      int k = direction.getStepY();
      int l = direction.getStepZ();
      double d = (double)blockPos.getX() + (double)j * 0.6D + 0.5D;
      double e = (double)blockPos.getY() + (double)k * 0.6D + 0.5D;
      double f = (double)blockPos.getZ() + (double)l * 0.6D + 0.5D;

      for(int m = 0; m < 10; ++m) {
         double g = randomSource.nextDouble() * 0.2D + 0.01D;
         double h = d + (double)j * 0.01D + (randomSource.nextDouble() - 0.5D) * (double)l * 0.5D;
         double n = e + (double)k * 0.01D + (randomSource.nextDouble() - 0.5D) * (double)k * 0.5D;
         double o = f + (double)l * 0.01D + (randomSource.nextDouble() - 0.5D) * (double)j * 0.5D;
         double p = (double)j * g + randomSource.nextGaussian() * 0.01D;
         double q = (double)k * g + randomSource.nextGaussian() * 0.01D;
         double r = (double)l * g + randomSource.nextGaussian() * 0.01D;
         this.addParticle(simpleParticleType, h, n, o, p, q, r);
      }

   }

   @Environment(EnvType.CLIENT)
   public static class TransparencyShaderException extends RuntimeException {
      public TransparencyShaderException(String string, Throwable throwable) {
         super(string, throwable);
      }
   }
}
