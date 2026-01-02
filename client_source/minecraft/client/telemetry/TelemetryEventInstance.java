package net.minecraft.client.telemetry;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record TelemetryEventInstance(TelemetryEventType type, TelemetryPropertyMap properties) {
   public static final Codec<TelemetryEventInstance> CODEC;

   public TelemetryEventInstance(TelemetryEventType telemetryEventType, TelemetryPropertyMap telemetryPropertyMap) {
      telemetryPropertyMap.propertySet().forEach((telemetryProperty) -> {
         if (!telemetryEventType.contains(telemetryProperty)) {
            String var10002 = telemetryProperty.id();
            throw new IllegalArgumentException("Property '" + var10002 + "' not expected for event: '" + telemetryEventType.id() + "'");
         }
      });
      this.type = telemetryEventType;
      this.properties = telemetryPropertyMap;
   }

   public TelemetryEvent export(TelemetrySession telemetrySession) {
      return this.type.export(telemetrySession, this.properties);
   }

   public TelemetryEventType type() {
      return this.type;
   }

   public TelemetryPropertyMap properties() {
      return this.properties;
   }

   static {
      CODEC = TelemetryEventType.CODEC.dispatchStable(TelemetryEventInstance::type, TelemetryEventType::codec);
   }
}
