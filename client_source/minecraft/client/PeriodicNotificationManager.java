package net.minecraft.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import java.io.BufferedReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class PeriodicNotificationManager extends SimplePreparableReloadListener<Map<String, List<PeriodicNotificationManager.Notification>>> implements AutoCloseable {
   private static final Codec<Map<String, List<PeriodicNotificationManager.Notification>>> CODEC;
   private static final Logger LOGGER;
   private final ResourceLocation notifications;
   private final Object2BooleanFunction<String> selector;
   @Nullable
   private Timer timer;
   @Nullable
   private PeriodicNotificationManager.NotificationTask notificationTask;

   public PeriodicNotificationManager(ResourceLocation resourceLocation, Object2BooleanFunction<String> object2BooleanFunction) {
      this.notifications = resourceLocation;
      this.selector = object2BooleanFunction;
   }

   protected Map<String, List<PeriodicNotificationManager.Notification>> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      try {
         BufferedReader reader = resourceManager.openAsReader(this.notifications);

         Map var4;
         try {
            var4 = (Map)CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).result().orElseThrow();
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

         return var4;
      } catch (Exception var8) {
         LOGGER.warn("Failed to load {}", this.notifications, var8);
         return ImmutableMap.of();
      }
   }

   protected void apply(Map<String, List<PeriodicNotificationManager.Notification>> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      List<PeriodicNotificationManager.Notification> list = (List)map.entrySet().stream().filter((entry) -> {
         return (Boolean)this.selector.apply((String)entry.getKey());
      }).map(Entry::getValue).flatMap(Collection::stream).collect(Collectors.toList());
      if (list.isEmpty()) {
         this.stopTimer();
      } else if (list.stream().anyMatch((notification) -> {
         return notification.period == 0L;
      })) {
         Util.logAndPauseIfInIde("A periodic notification in " + String.valueOf(this.notifications) + " has a period of zero minutes");
         this.stopTimer();
      } else {
         long l = this.calculateInitialDelay(list);
         long m = this.calculateOptimalPeriod(list, l);
         if (this.timer == null) {
            this.timer = new Timer();
         }

         if (this.notificationTask == null) {
            this.notificationTask = new PeriodicNotificationManager.NotificationTask(list, l, m);
         } else {
            this.notificationTask = this.notificationTask.reset(list, m);
         }

         this.timer.scheduleAtFixedRate(this.notificationTask, TimeUnit.MINUTES.toMillis(l), TimeUnit.MINUTES.toMillis(m));
      }
   }

   public void close() {
      this.stopTimer();
   }

   private void stopTimer() {
      if (this.timer != null) {
         this.timer.cancel();
      }

   }

   private long calculateOptimalPeriod(List<PeriodicNotificationManager.Notification> list, long l) {
      return list.stream().mapToLong((notification) -> {
         long m = notification.delay - l;
         return LongMath.gcd(m, notification.period);
      }).reduce(LongMath::gcd).orElseThrow(() -> {
         return new IllegalStateException("Empty notifications from: " + String.valueOf(this.notifications));
      });
   }

   private long calculateInitialDelay(List<PeriodicNotificationManager.Notification> list) {
      return list.stream().mapToLong((notification) -> {
         return notification.delay;
      }).min().orElse(0L);
   }

   // $FF: synthetic method
   protected Object prepare(final ResourceManager resourceManager, final ProfilerFiller profilerFiller) {
      return this.prepare(resourceManager, profilerFiller);
   }

   static {
      CODEC = Codec.unboundedMap(Codec.STRING, RecordCodecBuilder.create((instance) -> {
         return instance.group(Codec.LONG.optionalFieldOf("delay", 0L).forGetter(PeriodicNotificationManager.Notification::delay), Codec.LONG.fieldOf("period").forGetter(PeriodicNotificationManager.Notification::period), Codec.STRING.fieldOf("title").forGetter(PeriodicNotificationManager.Notification::title), Codec.STRING.fieldOf("message").forGetter(PeriodicNotificationManager.Notification::message)).apply(instance, PeriodicNotificationManager.Notification::new);
      }).listOf());
      LOGGER = LogUtils.getLogger();
   }

   @Environment(EnvType.CLIENT)
   static class NotificationTask extends TimerTask {
      private final Minecraft minecraft = Minecraft.getInstance();
      private final List<PeriodicNotificationManager.Notification> notifications;
      private final long period;
      private final AtomicLong elapsed;

      public NotificationTask(List<PeriodicNotificationManager.Notification> list, long l, long m) {
         this.notifications = list;
         this.period = m;
         this.elapsed = new AtomicLong(l);
      }

      public PeriodicNotificationManager.NotificationTask reset(List<PeriodicNotificationManager.Notification> list, long l) {
         this.cancel();
         return new PeriodicNotificationManager.NotificationTask(list, this.elapsed.get(), l);
      }

      public void run() {
         long l = this.elapsed.getAndAdd(this.period);
         long m = this.elapsed.get();
         Iterator var5 = this.notifications.iterator();

         while(var5.hasNext()) {
            PeriodicNotificationManager.Notification notification = (PeriodicNotificationManager.Notification)var5.next();
            if (l >= notification.delay) {
               long n = l / notification.period;
               long o = m / notification.period;
               if (n != o) {
                  this.minecraft.execute(() -> {
                     SystemToast.add(Minecraft.getInstance().getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.translatable(notification.title, new Object[]{n}), Component.translatable(notification.message, new Object[]{n}));
                  });
                  return;
               }
            }
         }

      }
   }

   @Environment(EnvType.CLIENT)
   public static record Notification(long delay, long period, String title, String message) {
      final long delay;
      final long period;
      final String title;
      final String message;

      public Notification(final long l, final long m, final String string, final String string2) {
         this.delay = l != 0L ? l : m;
         this.period = m;
         this.title = string;
         this.message = string2;
      }

      public long delay() {
         return this.delay;
      }

      public long period() {
         return this.period;
      }

      public String title() {
         return this.title;
      }

      public String message() {
         return this.message;
      }
   }
}
