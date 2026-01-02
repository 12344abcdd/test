package net.minecraft.client.telemetry;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TelemetryPropertyMap {
   final Map<TelemetryProperty<?>, Object> entries;

   TelemetryPropertyMap(Map<TelemetryProperty<?>, Object> map) {
      this.entries = map;
   }

   public static TelemetryPropertyMap.Builder builder() {
      return new TelemetryPropertyMap.Builder();
   }

   public static MapCodec<TelemetryPropertyMap> createCodec(List<TelemetryProperty<?>> list) {
      return new MapCodec<TelemetryPropertyMap>() {
         public <T> RecordBuilder<T> encode(TelemetryPropertyMap telemetryPropertyMap, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
            RecordBuilder<T> recordBuilder2 = recordBuilder;

            TelemetryProperty telemetryProperty;
            for(Iterator var5 = list.iterator(); var5.hasNext(); recordBuilder2 = this.encodeProperty(telemetryPropertyMap, recordBuilder2, telemetryProperty)) {
               telemetryProperty = (TelemetryProperty)var5.next();
            }

            return recordBuilder2;
         }

         private <T, V> RecordBuilder<T> encodeProperty(TelemetryPropertyMap telemetryPropertyMap, RecordBuilder<T> recordBuilder, TelemetryProperty<V> telemetryProperty) {
            V object = telemetryPropertyMap.get(telemetryProperty);
            return object != null ? recordBuilder.add(telemetryProperty.id(), object, telemetryProperty.codec()) : recordBuilder;
         }

         public <T> DataResult<TelemetryPropertyMap> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
            DataResult<TelemetryPropertyMap.Builder> dataResult = DataResult.success(new TelemetryPropertyMap.Builder());

            TelemetryProperty telemetryProperty;
            for(Iterator var4 = list.iterator(); var4.hasNext(); dataResult = this.decodeProperty(dataResult, dynamicOps, mapLike, telemetryProperty)) {
               telemetryProperty = (TelemetryProperty)var4.next();
            }

            return dataResult.map(TelemetryPropertyMap.Builder::build);
         }

         private <T, V> DataResult<TelemetryPropertyMap.Builder> decodeProperty(DataResult<TelemetryPropertyMap.Builder> dataResult, DynamicOps<T> dynamicOps, MapLike<T> mapLike, TelemetryProperty<V> telemetryProperty) {
            T object = mapLike.get(telemetryProperty.id());
            if (object != null) {
               DataResult<V> dataResult2 = telemetryProperty.codec().parse(dynamicOps, object);
               return dataResult.apply2stable((builder, objectx) -> {
                  return builder.put(telemetryProperty, objectx);
               }, dataResult2);
            } else {
               return dataResult;
            }
         }

         public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            Stream var10000 = list.stream().map(TelemetryProperty::id);
            Objects.requireNonNull(dynamicOps);
            return var10000.map(dynamicOps::createString);
         }

         // $FF: synthetic method
         public RecordBuilder encode(final Object object, final DynamicOps dynamicOps, final RecordBuilder recordBuilder) {
            return this.encode((TelemetryPropertyMap)object, dynamicOps, recordBuilder);
         }
      };
   }

   @Nullable
   public <T> T get(TelemetryProperty<T> telemetryProperty) {
      return this.entries.get(telemetryProperty);
   }

   public String toString() {
      return this.entries.toString();
   }

   public Set<TelemetryProperty<?>> propertySet() {
      return this.entries.keySet();
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final Map<TelemetryProperty<?>, Object> entries = new Reference2ObjectOpenHashMap();

      Builder() {
      }

      public <T> TelemetryPropertyMap.Builder put(TelemetryProperty<T> telemetryProperty, T object) {
         this.entries.put(telemetryProperty, object);
         return this;
      }

      public <T> TelemetryPropertyMap.Builder putIfNotNull(TelemetryProperty<T> telemetryProperty, @Nullable T object) {
         if (object != null) {
            this.entries.put(telemetryProperty, object);
         }

         return this;
      }

      public TelemetryPropertyMap.Builder putAll(TelemetryPropertyMap telemetryPropertyMap) {
         this.entries.putAll(telemetryPropertyMap.entries);
         return this;
      }

      public TelemetryPropertyMap build() {
         return new TelemetryPropertyMap(this.entries);
      }
   }
}
