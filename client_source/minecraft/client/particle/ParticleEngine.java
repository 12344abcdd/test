package net.minecraft.client.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ParticleEngine implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private static final ResourceLocation PARTICLES_ATLAS_INFO = ResourceLocation.withDefaultNamespace("particles");
   private static final int MAX_PARTICLES_PER_LAYER = 16384;
   private static final List<ParticleRenderType> RENDER_ORDER;
   protected ClientLevel level;
   private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newIdentityHashMap();
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final TextureManager textureManager;
   private final RandomSource random = RandomSource.create();
   private final Int2ObjectMap<ParticleProvider<?>> providers = new Int2ObjectOpenHashMap();
   private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
   private final Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets = Maps.newHashMap();
   private final TextureAtlas textureAtlas;
   private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap();

   public ParticleEngine(ClientLevel clientLevel, TextureManager textureManager) {
      this.textureAtlas = new TextureAtlas(TextureAtlas.LOCATION_PARTICLES);
      textureManager.register((ResourceLocation)this.textureAtlas.location(), (AbstractTexture)this.textureAtlas);
      this.level = clientLevel;
      this.textureManager = textureManager;
      this.registerProviders();
   }

   private void registerProviders() {
      this.register(ParticleTypes.ANGRY_VILLAGER, (ParticleEngine.SpriteParticleRegistration)(HeartParticle.AngryVillagerProvider::new));
      this.register(ParticleTypes.BLOCK_MARKER, (ParticleProvider)(new BlockMarker.Provider()));
      this.register(ParticleTypes.BLOCK, (ParticleProvider)(new TerrainParticle.Provider()));
      this.register(ParticleTypes.BUBBLE, (ParticleEngine.SpriteParticleRegistration)(BubbleParticle.Provider::new));
      this.register(ParticleTypes.BUBBLE_COLUMN_UP, (ParticleEngine.SpriteParticleRegistration)(BubbleColumnUpParticle.Provider::new));
      this.register(ParticleTypes.BUBBLE_POP, (ParticleEngine.SpriteParticleRegistration)(BubblePopParticle.Provider::new));
      this.register(ParticleTypes.CAMPFIRE_COSY_SMOKE, (ParticleEngine.SpriteParticleRegistration)(CampfireSmokeParticle.CosyProvider::new));
      this.register(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, (ParticleEngine.SpriteParticleRegistration)(CampfireSmokeParticle.SignalProvider::new));
      this.register(ParticleTypes.CLOUD, (ParticleEngine.SpriteParticleRegistration)(PlayerCloudParticle.Provider::new));
      this.register(ParticleTypes.COMPOSTER, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.ComposterFillProvider::new));
      this.register(ParticleTypes.CRIT, (ParticleEngine.SpriteParticleRegistration)(CritParticle.Provider::new));
      this.register(ParticleTypes.CURRENT_DOWN, (ParticleEngine.SpriteParticleRegistration)(WaterCurrentDownParticle.Provider::new));
      this.register(ParticleTypes.DAMAGE_INDICATOR, (ParticleEngine.SpriteParticleRegistration)(CritParticle.DamageIndicatorProvider::new));
      this.register(ParticleTypes.DRAGON_BREATH, (ParticleEngine.SpriteParticleRegistration)(DragonBreathParticle.Provider::new));
      this.register(ParticleTypes.DOLPHIN, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.DolphinSpeedProvider::new));
      this.register(ParticleTypes.DRIPPING_LAVA, (ParticleProvider.Sprite)(DripParticle::createLavaHangParticle));
      this.register(ParticleTypes.FALLING_LAVA, (ParticleProvider.Sprite)(DripParticle::createLavaFallParticle));
      this.register(ParticleTypes.LANDING_LAVA, (ParticleProvider.Sprite)(DripParticle::createLavaLandParticle));
      this.register(ParticleTypes.DRIPPING_WATER, (ParticleProvider.Sprite)(DripParticle::createWaterHangParticle));
      this.register(ParticleTypes.FALLING_WATER, (ParticleProvider.Sprite)(DripParticle::createWaterFallParticle));
      this.register(ParticleTypes.DUST, DustParticle.Provider::new);
      this.register(ParticleTypes.DUST_COLOR_TRANSITION, DustColorTransitionParticle.Provider::new);
      this.register(ParticleTypes.EFFECT, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.ELDER_GUARDIAN, (ParticleProvider)(new MobAppearanceParticle.Provider()));
      this.register(ParticleTypes.ENCHANTED_HIT, (ParticleEngine.SpriteParticleRegistration)(CritParticle.MagicProvider::new));
      this.register(ParticleTypes.ENCHANT, (ParticleEngine.SpriteParticleRegistration)(FlyTowardsPositionParticle.EnchantProvider::new));
      this.register(ParticleTypes.END_ROD, (ParticleEngine.SpriteParticleRegistration)(EndRodParticle.Provider::new));
      this.register(ParticleTypes.ENTITY_EFFECT, SpellParticle.MobEffectProvider::new);
      this.register(ParticleTypes.EXPLOSION_EMITTER, (ParticleProvider)(new HugeExplosionSeedParticle.Provider()));
      this.register(ParticleTypes.EXPLOSION, (ParticleEngine.SpriteParticleRegistration)(HugeExplosionParticle.Provider::new));
      this.register(ParticleTypes.SONIC_BOOM, (ParticleEngine.SpriteParticleRegistration)(SonicBoomParticle.Provider::new));
      this.register(ParticleTypes.FALLING_DUST, FallingDustParticle.Provider::new);
      this.register(ParticleTypes.GUST, (ParticleEngine.SpriteParticleRegistration)(GustParticle.Provider::new));
      this.register(ParticleTypes.SMALL_GUST, (ParticleEngine.SpriteParticleRegistration)(GustParticle.SmallProvider::new));
      this.register(ParticleTypes.GUST_EMITTER_LARGE, (ParticleProvider)(new GustSeedParticle.Provider(3.0D, 7, 0)));
      this.register(ParticleTypes.GUST_EMITTER_SMALL, (ParticleProvider)(new GustSeedParticle.Provider(1.0D, 3, 2)));
      this.register(ParticleTypes.FIREWORK, (ParticleEngine.SpriteParticleRegistration)(FireworkParticles.SparkProvider::new));
      this.register(ParticleTypes.FISHING, (ParticleEngine.SpriteParticleRegistration)(WakeParticle.Provider::new));
      this.register(ParticleTypes.FLAME, (ParticleEngine.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.INFESTED, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.SCULK_SOUL, (ParticleEngine.SpriteParticleRegistration)(SoulParticle.EmissiveProvider::new));
      this.register(ParticleTypes.SCULK_CHARGE, SculkChargeParticle.Provider::new);
      this.register(ParticleTypes.SCULK_CHARGE_POP, (ParticleEngine.SpriteParticleRegistration)(SculkChargePopParticle.Provider::new));
      this.register(ParticleTypes.SOUL, (ParticleEngine.SpriteParticleRegistration)(SoulParticle.Provider::new));
      this.register(ParticleTypes.SOUL_FIRE_FLAME, (ParticleEngine.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.FLASH, (ParticleEngine.SpriteParticleRegistration)(FireworkParticles.FlashProvider::new));
      this.register(ParticleTypes.HAPPY_VILLAGER, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.HappyVillagerProvider::new));
      this.register(ParticleTypes.HEART, (ParticleEngine.SpriteParticleRegistration)(HeartParticle.Provider::new));
      this.register(ParticleTypes.INSTANT_EFFECT, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.InstantProvider::new));
      this.register(ParticleTypes.ITEM, (ParticleProvider)(new BreakingItemParticle.Provider()));
      this.register(ParticleTypes.ITEM_SLIME, (ParticleProvider)(new BreakingItemParticle.SlimeProvider()));
      this.register(ParticleTypes.ITEM_COBWEB, (ParticleProvider)(new BreakingItemParticle.CobwebProvider()));
      this.register(ParticleTypes.ITEM_SNOWBALL, (ParticleProvider)(new BreakingItemParticle.SnowballProvider()));
      this.register(ParticleTypes.LARGE_SMOKE, (ParticleEngine.SpriteParticleRegistration)(LargeSmokeParticle.Provider::new));
      this.register(ParticleTypes.LAVA, (ParticleEngine.SpriteParticleRegistration)(LavaParticle.Provider::new));
      this.register(ParticleTypes.MYCELIUM, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.Provider::new));
      this.register(ParticleTypes.NAUTILUS, (ParticleEngine.SpriteParticleRegistration)(FlyTowardsPositionParticle.NautilusProvider::new));
      this.register(ParticleTypes.NOTE, (ParticleEngine.SpriteParticleRegistration)(NoteParticle.Provider::new));
      this.register(ParticleTypes.POOF, (ParticleEngine.SpriteParticleRegistration)(ExplodeParticle.Provider::new));
      this.register(ParticleTypes.PORTAL, (ParticleEngine.SpriteParticleRegistration)(PortalParticle.Provider::new));
      this.register(ParticleTypes.RAIN, (ParticleEngine.SpriteParticleRegistration)(WaterDropParticle.Provider::new));
      this.register(ParticleTypes.SMOKE, (ParticleEngine.SpriteParticleRegistration)(SmokeParticle.Provider::new));
      this.register(ParticleTypes.WHITE_SMOKE, (ParticleEngine.SpriteParticleRegistration)(WhiteSmokeParticle.Provider::new));
      this.register(ParticleTypes.SNEEZE, (ParticleEngine.SpriteParticleRegistration)(PlayerCloudParticle.SneezeProvider::new));
      this.register(ParticleTypes.SNOWFLAKE, (ParticleEngine.SpriteParticleRegistration)(SnowflakeParticle.Provider::new));
      this.register(ParticleTypes.SPIT, (ParticleEngine.SpriteParticleRegistration)(SpitParticle.Provider::new));
      this.register(ParticleTypes.SWEEP_ATTACK, (ParticleEngine.SpriteParticleRegistration)(AttackSweepParticle.Provider::new));
      this.register(ParticleTypes.TOTEM_OF_UNDYING, (ParticleEngine.SpriteParticleRegistration)(TotemParticle.Provider::new));
      this.register(ParticleTypes.SQUID_INK, (ParticleEngine.SpriteParticleRegistration)(SquidInkParticle.Provider::new));
      this.register(ParticleTypes.UNDERWATER, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.UnderwaterProvider::new));
      this.register(ParticleTypes.SPLASH, (ParticleEngine.SpriteParticleRegistration)(SplashParticle.Provider::new));
      this.register(ParticleTypes.WITCH, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.WitchProvider::new));
      this.register(ParticleTypes.DRIPPING_HONEY, (ParticleProvider.Sprite)(DripParticle::createHoneyHangParticle));
      this.register(ParticleTypes.FALLING_HONEY, (ParticleProvider.Sprite)(DripParticle::createHoneyFallParticle));
      this.register(ParticleTypes.LANDING_HONEY, (ParticleProvider.Sprite)(DripParticle::createHoneyLandParticle));
      this.register(ParticleTypes.FALLING_NECTAR, (ParticleProvider.Sprite)(DripParticle::createNectarFallParticle));
      this.register(ParticleTypes.FALLING_SPORE_BLOSSOM, (ParticleProvider.Sprite)(DripParticle::createSporeBlossomFallParticle));
      this.register(ParticleTypes.SPORE_BLOSSOM_AIR, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.SporeBlossomAirProvider::new));
      this.register(ParticleTypes.ASH, (ParticleEngine.SpriteParticleRegistration)(AshParticle.Provider::new));
      this.register(ParticleTypes.CRIMSON_SPORE, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.CrimsonSporeProvider::new));
      this.register(ParticleTypes.WARPED_SPORE, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.WarpedSporeProvider::new));
      this.register(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, (ParticleProvider.Sprite)(DripParticle::createObsidianTearHangParticle));
      this.register(ParticleTypes.FALLING_OBSIDIAN_TEAR, (ParticleProvider.Sprite)(DripParticle::createObsidianTearFallParticle));
      this.register(ParticleTypes.LANDING_OBSIDIAN_TEAR, (ParticleProvider.Sprite)(DripParticle::createObsidianTearLandParticle));
      this.register(ParticleTypes.REVERSE_PORTAL, (ParticleEngine.SpriteParticleRegistration)(ReversePortalParticle.ReversePortalProvider::new));
      this.register(ParticleTypes.WHITE_ASH, (ParticleEngine.SpriteParticleRegistration)(WhiteAshParticle.Provider::new));
      this.register(ParticleTypes.SMALL_FLAME, (ParticleEngine.SpriteParticleRegistration)(FlameParticle.SmallFlameProvider::new));
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_WATER, (ParticleProvider.Sprite)(DripParticle::createDripstoneWaterHangParticle));
      this.register(ParticleTypes.FALLING_DRIPSTONE_WATER, (ParticleProvider.Sprite)(DripParticle::createDripstoneWaterFallParticle));
      this.register(ParticleTypes.CHERRY_LEAVES, (ParticleEngine.SpriteParticleRegistration)((spriteSet) -> {
         return (simpleParticleType, clientLevel, d, e, f, g, h, i) -> {
            return new CherryParticle(clientLevel, d, e, f, spriteSet);
         };
      }));
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, (ParticleProvider.Sprite)(DripParticle::createDripstoneLavaHangParticle));
      this.register(ParticleTypes.FALLING_DRIPSTONE_LAVA, (ParticleProvider.Sprite)(DripParticle::createDripstoneLavaFallParticle));
      this.register(ParticleTypes.VIBRATION, VibrationSignalParticle.Provider::new);
      this.register(ParticleTypes.GLOW_SQUID_INK, (ParticleEngine.SpriteParticleRegistration)(SquidInkParticle.GlowInkProvider::new));
      this.register(ParticleTypes.GLOW, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.GlowSquidProvider::new));
      this.register(ParticleTypes.WAX_ON, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.WaxOnProvider::new));
      this.register(ParticleTypes.WAX_OFF, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.WaxOffProvider::new));
      this.register(ParticleTypes.ELECTRIC_SPARK, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.ElectricSparkProvider::new));
      this.register(ParticleTypes.SCRAPE, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.ScrapeProvider::new));
      this.register(ParticleTypes.SHRIEK, ShriekParticle.Provider::new);
      this.register(ParticleTypes.EGG_CRACK, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.EggCrackProvider::new));
      this.register(ParticleTypes.DUST_PLUME, (ParticleEngine.SpriteParticleRegistration)(DustPlumeParticle.Provider::new));
      this.register(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, (ParticleEngine.SpriteParticleRegistration)(TrialSpawnerDetectionParticle.Provider::new));
      this.register(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, (ParticleEngine.SpriteParticleRegistration)(TrialSpawnerDetectionParticle.Provider::new));
      this.register(ParticleTypes.VAULT_CONNECTION, (ParticleEngine.SpriteParticleRegistration)(FlyTowardsPositionParticle.VaultConnectionProvider::new));
      this.register(ParticleTypes.DUST_PILLAR, (ParticleProvider)(new TerrainParticle.DustPillarProvider()));
      this.register(ParticleTypes.RAID_OMEN, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.TRIAL_OMEN, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.OMINOUS_SPAWNING, (ParticleEngine.SpriteParticleRegistration)(FlyStraightTowardsParticle.OminousSpawnProvider::new));
   }

   private <T extends ParticleOptions> void register(ParticleType<T> particleType, ParticleProvider<T> particleProvider) {
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(particleType), particleProvider);
   }

   private <T extends ParticleOptions> void register(ParticleType<T> particleType, ParticleProvider.Sprite<T> sprite) {
      this.register(particleType, (spriteSet) -> {
         return (particleOptions, clientLevel, d, e, f, g, h, i) -> {
            TextureSheetParticle textureSheetParticle = sprite.createParticle(particleOptions, clientLevel, d, e, f, g, h, i);
            if (textureSheetParticle != null) {
               textureSheetParticle.pickSprite(spriteSet);
            }

            return textureSheetParticle;
         };
      });
   }

   private <T extends ParticleOptions> void register(ParticleType<T> particleType, ParticleEngine.SpriteParticleRegistration<T> spriteParticleRegistration) {
      ParticleEngine.MutableSpriteSet mutableSpriteSet = new ParticleEngine.MutableSpriteSet();
      this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(particleType), mutableSpriteSet);
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(particleType), spriteParticleRegistration.create(mutableSpriteSet));
   }

   public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
      CompletableFuture<List<ParticleDefinition>> completableFuture = CompletableFuture.supplyAsync(() -> {
         return PARTICLE_LISTER.listMatchingResources(resourceManager);
      }, executor).thenCompose((map) -> {
         List<CompletableFuture<ParticleDefinition>> list = new ArrayList(map.size());
         map.forEach((resourceLocation, resource) -> {
            ResourceLocation resourceLocation2 = PARTICLE_LISTER.fileToId(resourceLocation);
            list.add(CompletableFuture.supplyAsync(() -> {
               @Environment(EnvType.CLIENT)
               record ParticleDefinition(ResourceLocation id, Optional<List<ResourceLocation>> sprites) {
                  ParticleDefinition(ResourceLocation resourceLocation, Optional<List<ResourceLocation>> optional) {
                     this.id = resourceLocation;
                     this.sprites = optional;
                  }

                  public ResourceLocation id() {
                     return this.id;
                  }

                  public Optional<List<ResourceLocation>> sprites() {
                     return this.sprites;
                  }
               }

               return new ParticleDefinition(resourceLocation2, this.loadParticleDescription(resourceLocation2, resource));
            }, executor));
         });
         return Util.sequence(list);
      });
      CompletableFuture<SpriteLoader.Preparations> completableFuture2 = SpriteLoader.create(this.textureAtlas).loadAndStitch(resourceManager, PARTICLES_ATLAS_INFO, 0, executor).thenCompose(SpriteLoader.Preparations::waitForUpload);
      CompletableFuture var10000 = CompletableFuture.allOf(completableFuture2, completableFuture);
      Objects.requireNonNull(preparationBarrier);
      return var10000.thenCompose(preparationBarrier::wait).thenAcceptAsync((void_) -> {
         this.clearParticles();
         profilerFiller2.startTick();
         profilerFiller2.push("upload");
         SpriteLoader.Preparations preparations = (SpriteLoader.Preparations)completableFuture2.join();
         this.textureAtlas.upload(preparations);
         profilerFiller2.popPush("bindSpriteSets");
         Set<ResourceLocation> set = new HashSet();
         TextureAtlasSprite textureAtlasSprite = preparations.missing();
         ((List)completableFuture.join()).forEach((arg) -> {
            Optional<List<ResourceLocation>> optional = arg.sprites();
            if (!optional.isEmpty()) {
               List<TextureAtlasSprite> list = new ArrayList();
               Iterator var7 = ((List)optional.get()).iterator();

               while(var7.hasNext()) {
                  ResourceLocation resourceLocation = (ResourceLocation)var7.next();
                  TextureAtlasSprite textureAtlasSprite2 = (TextureAtlasSprite)preparations.regions().get(resourceLocation);
                  if (textureAtlasSprite2 == null) {
                     set.add(resourceLocation);
                     list.add(textureAtlasSprite);
                  } else {
                     list.add(textureAtlasSprite2);
                  }
               }

               if (list.isEmpty()) {
                  list.add(textureAtlasSprite);
               }

               ((ParticleEngine.MutableSpriteSet)this.spriteSets.get(arg.id())).rebind(list);
            }
         });
         if (!set.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", set.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
         }

         profilerFiller2.pop();
         profilerFiller2.endTick();
      }, executor2);
   }

   public void close() {
      this.textureAtlas.clearTextureData();
   }

   private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation resourceLocation, Resource resource) {
      if (!this.spriteSets.containsKey(resourceLocation)) {
         LOGGER.debug("Redundant texture list for particle: {}", resourceLocation);
         return Optional.empty();
      } else {
         try {
            BufferedReader reader = resource.openAsReader();

            Optional var5;
            try {
               ParticleDescription particleDescription = ParticleDescription.fromJson(GsonHelper.parse(reader));
               var5 = Optional.of(particleDescription.getTextures());
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         } catch (IOException var8) {
            throw new IllegalStateException("Failed to load description for particle " + String.valueOf(resourceLocation), var8);
         }
      }
   }

   public void createTrackingEmitter(Entity entity, ParticleOptions particleOptions) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particleOptions));
   }

   public void createTrackingEmitter(Entity entity, ParticleOptions particleOptions, int i) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particleOptions, i));
   }

   @Nullable
   public Particle createParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
      Particle particle = this.makeParticle(particleOptions, d, e, f, g, h, i);
      if (particle != null) {
         this.add(particle);
         return particle;
      } else {
         return null;
      }
   }

   @Nullable
   private <T extends ParticleOptions> Particle makeParticle(T particleOptions, double d, double e, double f, double g, double h, double i) {
      ParticleProvider<T> particleProvider = (ParticleProvider)this.providers.get(BuiltInRegistries.PARTICLE_TYPE.getId(particleOptions.getType()));
      return particleProvider == null ? null : particleProvider.createParticle(particleOptions, this.level, d, e, f, g, h, i);
   }

   public void add(Particle particle) {
      Optional<ParticleGroup> optional = particle.getParticleGroup();
      if (optional.isPresent()) {
         if (this.hasSpaceInParticleLimit((ParticleGroup)optional.get())) {
            this.particlesToAdd.add(particle);
            this.updateCount((ParticleGroup)optional.get(), 1);
         }
      } else {
         this.particlesToAdd.add(particle);
      }

   }

   public void tick() {
      this.particles.forEach((particleRenderType, queue) -> {
         this.level.getProfiler().push(particleRenderType.toString());
         this.tickParticleList(queue);
         this.level.getProfiler().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         List<TrackingEmitter> list = Lists.newArrayList();
         Iterator var2 = this.trackingEmitters.iterator();

         while(var2.hasNext()) {
            TrackingEmitter trackingEmitter = (TrackingEmitter)var2.next();
            trackingEmitter.tick();
            if (!trackingEmitter.isAlive()) {
               list.add(trackingEmitter);
            }
         }

         this.trackingEmitters.removeAll(list);
      }

      Particle particle;
      if (!this.particlesToAdd.isEmpty()) {
         while((particle = (Particle)this.particlesToAdd.poll()) != null) {
            ((Queue)this.particles.computeIfAbsent(particle.getRenderType(), (particleRenderType) -> {
               return EvictingQueue.create(16384);
            })).add(particle);
         }
      }

   }

   private void tickParticleList(Collection<Particle> collection) {
      if (!collection.isEmpty()) {
         Iterator iterator = collection.iterator();

         while(iterator.hasNext()) {
            Particle particle = (Particle)iterator.next();
            this.tickParticle(particle);
            if (!particle.isAlive()) {
               particle.getParticleGroup().ifPresent((particleGroup) -> {
                  this.updateCount(particleGroup, -1);
               });
               iterator.remove();
            }
         }
      }

   }

   private void updateCount(ParticleGroup particleGroup, int i) {
      this.trackedParticleCounts.addTo(particleGroup, i);
   }

   private void tickParticle(Particle particle) {
      try {
         particle.tick();
      } catch (Throwable var5) {
         CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking Particle");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
         Objects.requireNonNull(particle);
         crashReportCategory.setDetail("Particle", particle::toString);
         ParticleRenderType var10002 = particle.getRenderType();
         Objects.requireNonNull(var10002);
         crashReportCategory.setDetail("Particle Type", var10002::toString);
         throw new ReportedException(crashReport);
      }
   }

   public void render(LightTexture lightTexture, Camera camera, float f) {
      lightTexture.turnOnLightLayer();
      RenderSystem.enableDepthTest();
      Iterator var4 = RENDER_ORDER.iterator();

      while(true) {
         ParticleRenderType particleRenderType;
         Queue queue;
         BufferBuilder bufferBuilder;
         do {
            do {
               do {
                  if (!var4.hasNext()) {
                     RenderSystem.depthMask(true);
                     RenderSystem.disableBlend();
                     lightTexture.turnOffLightLayer();
                     return;
                  }

                  particleRenderType = (ParticleRenderType)var4.next();
                  queue = (Queue)this.particles.get(particleRenderType);
               } while(queue == null);
            } while(queue.isEmpty());

            RenderSystem.setShader(GameRenderer::getParticleShader);
            Tesselator tesselator = Tesselator.getInstance();
            bufferBuilder = particleRenderType.begin(tesselator, this.textureManager);
         } while(bufferBuilder == null);

         Iterator var9 = queue.iterator();

         while(var9.hasNext()) {
            Particle particle = (Particle)var9.next();

            try {
               particle.render(bufferBuilder, camera, f);
            } catch (Throwable var14) {
               CrashReport crashReport = CrashReport.forThrowable(var14, "Rendering Particle");
               CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
               Objects.requireNonNull(particle);
               crashReportCategory.setDetail("Particle", particle::toString);
               Objects.requireNonNull(particleRenderType);
               crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
               throw new ReportedException(crashReport);
            }
         }

         MeshData meshData = bufferBuilder.build();
         if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
         }
      }
   }

   public void setLevel(@Nullable ClientLevel clientLevel) {
      this.level = clientLevel;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public void destroy(BlockPos blockPos, BlockState blockState) {
      if (!blockState.isAir() && blockState.shouldSpawnTerrainParticles()) {
         VoxelShape voxelShape = blockState.getShape(this.level, blockPos);
         double d = 0.25D;
         voxelShape.forAllBoxes((dx, e, f, g, h, i) -> {
            double j = Math.min(1.0D, g - dx);
            double k = Math.min(1.0D, h - e);
            double l = Math.min(1.0D, i - f);
            int m = Math.max(2, Mth.ceil(j / 0.25D));
            int n = Math.max(2, Mth.ceil(k / 0.25D));
            int o = Math.max(2, Mth.ceil(l / 0.25D));

            for(int p = 0; p < m; ++p) {
               for(int q = 0; q < n; ++q) {
                  for(int r = 0; r < o; ++r) {
                     double s = ((double)p + 0.5D) / (double)m;
                     double t = ((double)q + 0.5D) / (double)n;
                     double u = ((double)r + 0.5D) / (double)o;
                     double v = s * j + dx;
                     double w = t * k + e;
                     double x = u * l + f;
                     this.add(new TerrainParticle(this.level, (double)blockPos.getX() + v, (double)blockPos.getY() + w, (double)blockPos.getZ() + x, s - 0.5D, t - 0.5D, u - 0.5D, blockState, blockPos));
                  }
               }
            }

         });
      }
   }

   public void crack(BlockPos blockPos, Direction direction) {
      BlockState blockState = this.level.getBlockState(blockPos);
      if (blockState.getRenderShape() != RenderShape.INVISIBLE && blockState.shouldSpawnTerrainParticles()) {
         int i = blockPos.getX();
         int j = blockPos.getY();
         int k = blockPos.getZ();
         float f = 0.1F;
         AABB aABB = blockState.getShape(this.level, blockPos).bounds();
         double d = (double)i + this.random.nextDouble() * (aABB.maxX - aABB.minX - 0.20000000298023224D) + 0.10000000149011612D + aABB.minX;
         double e = (double)j + this.random.nextDouble() * (aABB.maxY - aABB.minY - 0.20000000298023224D) + 0.10000000149011612D + aABB.minY;
         double g = (double)k + this.random.nextDouble() * (aABB.maxZ - aABB.minZ - 0.20000000298023224D) + 0.10000000149011612D + aABB.minZ;
         if (direction == Direction.DOWN) {
            e = (double)j + aABB.minY - 0.10000000149011612D;
         }

         if (direction == Direction.UP) {
            e = (double)j + aABB.maxY + 0.10000000149011612D;
         }

         if (direction == Direction.NORTH) {
            g = (double)k + aABB.minZ - 0.10000000149011612D;
         }

         if (direction == Direction.SOUTH) {
            g = (double)k + aABB.maxZ + 0.10000000149011612D;
         }

         if (direction == Direction.WEST) {
            d = (double)i + aABB.minX - 0.10000000149011612D;
         }

         if (direction == Direction.EAST) {
            d = (double)i + aABB.maxX + 0.10000000149011612D;
         }

         this.add((new TerrainParticle(this.level, d, e, g, 0.0D, 0.0D, 0.0D, blockState, blockPos)).setPower(0.2F).scale(0.6F));
      }
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(Collection::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleGroup particleGroup) {
      return this.trackedParticleCounts.getInt(particleGroup) < particleGroup.getLimit();
   }

   private void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }

   static {
      RENDER_ORDER = ImmutableList.of(ParticleRenderType.TERRAIN_SHEET, ParticleRenderType.PARTICLE_SHEET_OPAQUE, ParticleRenderType.PARTICLE_SHEET_LIT, ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT, ParticleRenderType.CUSTOM);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface SpriteParticleRegistration<T extends ParticleOptions> {
      ParticleProvider<T> create(SpriteSet spriteSet);
   }

   @Environment(EnvType.CLIENT)
   private static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      MutableSpriteSet() {
      }

      public TextureAtlasSprite get(int i, int j) {
         return (TextureAtlasSprite)this.sprites.get(i * (this.sprites.size() - 1) / j);
      }

      public TextureAtlasSprite get(RandomSource randomSource) {
         return (TextureAtlasSprite)this.sprites.get(randomSource.nextInt(this.sprites.size()));
      }

      public void rebind(List<TextureAtlasSprite> list) {
         this.sprites = ImmutableList.copyOf(list);
      }
   }
}
