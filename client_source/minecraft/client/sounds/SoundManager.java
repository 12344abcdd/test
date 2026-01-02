package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Component.SerializerAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import net.minecraft.util.valueproviders.SampledFloat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final ResourceLocation EMPTY_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("empty");
   public static final Sound EMPTY_SOUND;
   public static final ResourceLocation INTENTIONALLY_EMPTY_SOUND_LOCATION;
   public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT;
   public static final Sound INTENTIONALLY_EMPTY_SOUND;
   static final Logger LOGGER;
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON;
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE;
   private final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;
   private final Map<ResourceLocation, Resource> soundCache = new HashMap();

   public SoundManager(Options options) {
      this.soundEngine = new SoundEngine(this, options, ResourceProvider.fromMap(this.soundCache));
   }

   protected SoundManager.Preparations prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      SoundManager.Preparations preparations = new SoundManager.Preparations();
      profilerFiller.startTick();
      profilerFiller.push("list");
      preparations.listResources(resourceManager);
      profilerFiller.pop();

      for(Iterator var4 = resourceManager.getNamespaces().iterator(); var4.hasNext(); profilerFiller.pop()) {
         String string = (String)var4.next();
         profilerFiller.push(string);

         try {
            List<Resource> list = resourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(string, "sounds.json"));

            for(Iterator var7 = list.iterator(); var7.hasNext(); profilerFiller.pop()) {
               Resource resource = (Resource)var7.next();
               profilerFiller.push(resource.sourcePackId());

               try {
                  BufferedReader reader = resource.openAsReader();

                  try {
                     profilerFiller.push("parse");
                     Map<String, SoundEventRegistration> map = (Map)GsonHelper.fromJson(GSON, reader, SOUND_EVENT_REGISTRATION_TYPE);
                     profilerFiller.popPush("register");
                     Iterator var11 = map.entrySet().iterator();

                     while(true) {
                        if (!var11.hasNext()) {
                           profilerFiller.pop();
                           break;
                        }

                        Entry<String, SoundEventRegistration> entry = (Entry)var11.next();
                        preparations.handleRegistration(ResourceLocation.fromNamespaceAndPath(string, (String)entry.getKey()), (SoundEventRegistration)entry.getValue());
                     }
                  } catch (Throwable var14) {
                     if (reader != null) {
                        try {
                           reader.close();
                        } catch (Throwable var13) {
                           var14.addSuppressed(var13);
                        }
                     }

                     throw var14;
                  }

                  if (reader != null) {
                     reader.close();
                  }
               } catch (RuntimeException var15) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", new Object[]{"sounds.json", resource.sourcePackId(), var15});
               }
            }
         } catch (IOException var16) {
         }
      }

      profilerFiller.endTick();
      return preparations;
   }

   protected void apply(SoundManager.Preparations preparations, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      preparations.apply(this.registry, this.soundCache, this.soundEngine);
      Iterator var4;
      ResourceLocation resourceLocation;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         var4 = this.registry.keySet().iterator();

         while(var4.hasNext()) {
            resourceLocation = (ResourceLocation)var4.next();
            WeighedSoundEvents weighedSoundEvents = (WeighedSoundEvents)this.registry.get(resourceLocation);
            if (!ComponentUtils.isTranslationResolvable(weighedSoundEvents.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey(resourceLocation)) {
               LOGGER.error("Missing subtitle {} for sound event: {}", weighedSoundEvents.getSubtitle(), resourceLocation);
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         var4 = this.registry.keySet().iterator();

         while(var4.hasNext()) {
            resourceLocation = (ResourceLocation)var4.next();
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(resourceLocation)) {
               LOGGER.debug("Not having sound event for: {}", resourceLocation);
            }
         }
      }

      this.soundEngine.reload();
   }

   public List<String> getAvailableSoundDevices() {
      return this.soundEngine.getAvailableSoundDevices();
   }

   public ListenerTransform getListenerTransform() {
      return this.soundEngine.getListenerTransform();
   }

   static boolean validateSoundResource(Sound sound, ResourceLocation resourceLocation, ResourceProvider resourceProvider) {
      ResourceLocation resourceLocation2 = sound.getPath();
      if (resourceProvider.getResource(resourceLocation2).isEmpty()) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", resourceLocation2, resourceLocation);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeighedSoundEvents getSoundEvent(ResourceLocation resourceLocation) {
      return (WeighedSoundEvents)this.registry.get(resourceLocation);
   }

   public Collection<ResourceLocation> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance tickableSoundInstance) {
      this.soundEngine.queueTickingSound(tickableSoundInstance);
   }

   public void play(SoundInstance soundInstance) {
      this.soundEngine.play(soundInstance);
   }

   public void playDelayed(SoundInstance soundInstance, int i) {
      this.soundEngine.playDelayed(soundInstance, i);
   }

   public void updateSource(Camera camera) {
      this.soundEngine.updateSource(camera);
   }

   public void pause() {
      this.soundEngine.pause();
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void emergencyShutdown() {
      this.soundEngine.emergencyShutdown();
   }

   public void tick(boolean bl) {
      this.soundEngine.tick(bl);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void updateSourceVolume(SoundSource soundSource, float f) {
      if (soundSource == SoundSource.MASTER && f <= 0.0F) {
         this.stop();
      }

      this.soundEngine.updateCategoryVolume(soundSource, f);
   }

   public void stop(SoundInstance soundInstance) {
      this.soundEngine.stop(soundInstance);
   }

   public boolean isActive(SoundInstance soundInstance) {
      return this.soundEngine.isActive(soundInstance);
   }

   public void addListener(SoundEventListener soundEventListener) {
      this.soundEngine.addEventListener(soundEventListener);
   }

   public void removeListener(SoundEventListener soundEventListener) {
      this.soundEngine.removeEventListener(soundEventListener);
   }

   public void stop(@Nullable ResourceLocation resourceLocation, @Nullable SoundSource soundSource) {
      this.soundEngine.stop(resourceLocation, soundSource);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   public void reload() {
      this.soundEngine.reload();
   }

   // $FF: synthetic method
   protected Object prepare(final ResourceManager resourceManager, final ProfilerFiller profilerFiller) {
      return this.prepare(resourceManager, profilerFiller);
   }

   static {
      EMPTY_SOUND = new Sound(EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
      INTENTIONALLY_EMPTY_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("intentionally_empty");
      INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, (String)null);
      INTENTIONALLY_EMPTY_SOUND = new Sound(INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
      LOGGER = LogUtils.getLogger();
      GSON = (new GsonBuilder()).registerTypeHierarchyAdapter(Component.class, new SerializerAdapter(RegistryAccess.EMPTY)).registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer()).create();
      SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {
      };
   }

   @Environment(EnvType.CLIENT)
   protected static class Preparations {
      final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
      private Map<ResourceLocation, Resource> soundCache = Map.of();

      void listResources(ResourceManager resourceManager) {
         this.soundCache = Sound.SOUND_LISTER.listMatchingResources(resourceManager);
      }

      void handleRegistration(ResourceLocation resourceLocation, SoundEventRegistration soundEventRegistration) {
         WeighedSoundEvents weighedSoundEvents = (WeighedSoundEvents)this.registry.get(resourceLocation);
         boolean bl = weighedSoundEvents == null;
         if (bl || soundEventRegistration.isReplace()) {
            if (!bl) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", resourceLocation);
            }

            weighedSoundEvents = new WeighedSoundEvents(resourceLocation, soundEventRegistration.getSubtitle());
            this.registry.put(resourceLocation, weighedSoundEvents);
         }

         ResourceProvider resourceProvider = ResourceProvider.fromMap(this.soundCache);
         Iterator var6 = soundEventRegistration.getSounds().iterator();

         while(var6.hasNext()) {
            Sound sound = (Sound)var6.next();
            ResourceLocation resourceLocation2 = sound.getLocation();
            Object weighted;
            switch(sound.getType()) {
            case FILE:
               if (!SoundManager.validateSoundResource(sound, resourceLocation, resourceProvider)) {
                  continue;
               }

               weighted = sound;
               break;
            case SOUND_EVENT:
               weighted = new Weighted<Sound>(resourceLocation2, sound) {
                  // $FF: synthetic field
                  final ResourceLocation val$soundLocation;
                  // $FF: synthetic field
                  final Sound val$sound;
                  // $FF: synthetic field
                  final SoundManager.Preparations field_5597;

                  {
                     this.field_5597 = preparations;
                     this.val$soundLocation = resourceLocation;
                     this.val$sound = sound;
                  }

                  public int getWeight() {
                     WeighedSoundEvents weighedSoundEvents = (WeighedSoundEvents)this.field_5597.registry.get(this.val$soundLocation);
                     return weighedSoundEvents == null ? 0 : weighedSoundEvents.getWeight();
                  }

                  public Sound getSound(RandomSource randomSource) {
                     WeighedSoundEvents weighedSoundEvents = (WeighedSoundEvents)this.field_5597.registry.get(this.val$soundLocation);
                     if (weighedSoundEvents == null) {
                        return SoundManager.EMPTY_SOUND;
                     } else {
                        Sound sound = weighedSoundEvents.getSound(randomSource);
                        return new Sound(sound.getLocation(), new MultipliedFloats(new SampledFloat[]{sound.getVolume(), this.val$sound.getVolume()}), new MultipliedFloats(new SampledFloat[]{sound.getPitch(), this.val$sound.getPitch()}), this.val$sound.getWeight(), Sound.Type.FILE, sound.shouldStream() || this.val$sound.shouldStream(), sound.shouldPreload(), sound.getAttenuationDistance());
                     }
                  }

                  public void preloadIfRequired(SoundEngine soundEngine) {
                     WeighedSoundEvents weighedSoundEvents = (WeighedSoundEvents)this.field_5597.registry.get(this.val$soundLocation);
                     if (weighedSoundEvents != null) {
                        weighedSoundEvents.preloadIfRequired(soundEngine);
                     }
                  }

                  // $FF: synthetic method
                  public Object getSound(final RandomSource randomSource) {
                     return this.getSound(randomSource);
                  }
               };
               break;
            default:
               throw new IllegalStateException("Unknown SoundEventRegistration type: " + String.valueOf(sound.getType()));
            }

            weighedSoundEvents.addSound((Weighted)weighted);
         }

      }

      public void apply(Map<ResourceLocation, WeighedSoundEvents> map, Map<ResourceLocation, Resource> map2, SoundEngine soundEngine) {
         map.clear();
         map2.clear();
         map2.putAll(this.soundCache);
         Iterator var4 = this.registry.entrySet().iterator();

         while(var4.hasNext()) {
            Entry<ResourceLocation, WeighedSoundEvents> entry = (Entry)var4.next();
            map.put((ResourceLocation)entry.getKey(), (WeighedSoundEvents)entry.getValue());
            ((WeighedSoundEvents)entry.getValue()).preloadIfRequired(soundEngine);
         }

      }
   }
}
