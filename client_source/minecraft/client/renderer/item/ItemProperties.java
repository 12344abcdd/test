package net.minecraft.client.renderer.item;

import com.google.common.collect.Maps;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightBlock;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemProperties {
   private static final Map<ResourceLocation, ItemPropertyFunction> GENERIC_PROPERTIES = Maps.newHashMap();
   private static final ResourceLocation DAMAGED = ResourceLocation.withDefaultNamespace("damaged");
   private static final ResourceLocation DAMAGE = ResourceLocation.withDefaultNamespace("damage");
   private static final ClampedItemPropertyFunction PROPERTY_DAMAGED = (itemStack, clientLevel, livingEntity, i) -> {
      return itemStack.isDamaged() ? 1.0F : 0.0F;
   };
   private static final ClampedItemPropertyFunction PROPERTY_DAMAGE = (itemStack, clientLevel, livingEntity, i) -> {
      return Mth.clamp((float)itemStack.getDamageValue() / (float)itemStack.getMaxDamage(), 0.0F, 1.0F);
   };
   private static final Map<Item, Map<ResourceLocation, ItemPropertyFunction>> PROPERTIES = Maps.newHashMap();

   public static ClampedItemPropertyFunction registerGeneric(ResourceLocation resourceLocation, ClampedItemPropertyFunction clampedItemPropertyFunction) {
      GENERIC_PROPERTIES.put(resourceLocation, clampedItemPropertyFunction);
      return clampedItemPropertyFunction;
   }

   public static void registerCustomModelData(ItemPropertyFunction itemPropertyFunction) {
      GENERIC_PROPERTIES.put(ResourceLocation.withDefaultNamespace("custom_model_data"), itemPropertyFunction);
   }

   public static void register(Item item, ResourceLocation resourceLocation, ClampedItemPropertyFunction clampedItemPropertyFunction) {
      ((Map)PROPERTIES.computeIfAbsent(item, (itemx) -> {
         return Maps.newHashMap();
      })).put(resourceLocation, clampedItemPropertyFunction);
   }

   @Nullable
   public static ItemPropertyFunction getProperty(ItemStack itemStack, ResourceLocation resourceLocation) {
      if (itemStack.getMaxDamage() > 0) {
         if (DAMAGE.equals(resourceLocation)) {
            return PROPERTY_DAMAGE;
         }

         if (DAMAGED.equals(resourceLocation)) {
            return PROPERTY_DAMAGED;
         }
      }

      ItemPropertyFunction itemPropertyFunction = (ItemPropertyFunction)GENERIC_PROPERTIES.get(resourceLocation);
      if (itemPropertyFunction != null) {
         return itemPropertyFunction;
      } else {
         Map<ResourceLocation, ItemPropertyFunction> map = (Map)PROPERTIES.get(itemStack.getItem());
         return map == null ? null : (ItemPropertyFunction)map.get(resourceLocation);
      }
   }

   static {
      registerGeneric(ResourceLocation.withDefaultNamespace("lefthanded"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.getMainArm() != HumanoidArm.RIGHT ? 1.0F : 0.0F;
      });
      registerGeneric(ResourceLocation.withDefaultNamespace("cooldown"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity instanceof Player ? ((Player)livingEntity).getCooldowns().getCooldownPercent(itemStack.getItem(), 0.0F) : 0.0F;
      });
      ClampedItemPropertyFunction clampedItemPropertyFunction = (itemStack, clientLevel, livingEntity, i) -> {
         ArmorTrim armorTrim = (ArmorTrim)itemStack.get(DataComponents.TRIM);
         return armorTrim != null ? ((TrimMaterial)armorTrim.material().value()).itemModelIndex() : Float.NEGATIVE_INFINITY;
      };
      registerGeneric(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, clampedItemPropertyFunction);
      registerCustomModelData((itemStack, clientLevel, livingEntity, i) -> {
         return (float)((CustomModelData)itemStack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT)).value();
      });
      register(Items.BOW, ResourceLocation.withDefaultNamespace("pull"), (itemStack, clientLevel, livingEntity, i) -> {
         if (livingEntity == null) {
            return 0.0F;
         } else {
            return livingEntity.getUseItem() != itemStack ? 0.0F : (float)(itemStack.getUseDuration(livingEntity) - livingEntity.getUseItemRemainingTicks()) / 20.0F;
         }
      });
      register(Items.BRUSH, ResourceLocation.withDefaultNamespace("brushing"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.getUseItem() == itemStack ? (float)(livingEntity.getUseItemRemainingTicks() % 10) / 10.0F : 0.0F;
      });
      register(Items.BOW, ResourceLocation.withDefaultNamespace("pulling"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F;
      });
      register(Items.BUNDLE, ResourceLocation.withDefaultNamespace("filled"), (itemStack, clientLevel, livingEntity, i) -> {
         return BundleItem.getFullnessDisplay(itemStack);
      });
      register(Items.CLOCK, ResourceLocation.withDefaultNamespace("time"), new ClampedItemPropertyFunction() {
         private double rotation;
         private double rota;
         private long lastUpdateTick;

         public float unclampedCall(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
            Entity entity = livingEntity != null ? livingEntity : itemStack.getEntityRepresentation();
            if (entity == null) {
               return 0.0F;
            } else {
               if (clientLevel == null && ((Entity)entity).level() instanceof ClientLevel) {
                  clientLevel = (ClientLevel)((Entity)entity).level();
               }

               if (clientLevel == null) {
                  return 0.0F;
               } else {
                  double d;
                  if (clientLevel.dimensionType().natural()) {
                     d = (double)clientLevel.getTimeOfDay(1.0F);
                  } else {
                     d = Math.random();
                  }

                  d = this.wobble(clientLevel, d);
                  return (float)d;
               }
            }
         }

         private double wobble(Level level, double d) {
            if (level.getGameTime() != this.lastUpdateTick) {
               this.lastUpdateTick = level.getGameTime();
               double e = d - this.rotation;
               e = Mth.positiveModulo(e + 0.5D, 1.0D) - 0.5D;
               this.rota += e * 0.1D;
               this.rota *= 0.9D;
               this.rotation = Mth.positiveModulo(this.rotation + this.rota, 1.0D);
            }

            return this.rotation;
         }
      });
      register(Items.COMPASS, ResourceLocation.withDefaultNamespace("angle"), new CompassItemPropertyFunction((clientLevel, itemStack, entity) -> {
         LodestoneTracker lodestoneTracker = (LodestoneTracker)itemStack.get(DataComponents.LODESTONE_TRACKER);
         return lodestoneTracker != null ? (GlobalPos)lodestoneTracker.target().orElse((Object)null) : CompassItem.getSpawnPosition(clientLevel);
      }));
      register(Items.RECOVERY_COMPASS, ResourceLocation.withDefaultNamespace("angle"), new CompassItemPropertyFunction((clientLevel, itemStack, entity) -> {
         if (entity instanceof Player) {
            Player player = (Player)entity;
            return (GlobalPos)player.getLastDeathLocation().orElse((Object)null);
         } else {
            return null;
         }
      }));
      register(Items.CROSSBOW, ResourceLocation.withDefaultNamespace("pull"), (itemStack, clientLevel, livingEntity, i) -> {
         if (livingEntity == null) {
            return 0.0F;
         } else {
            return CrossbowItem.isCharged(itemStack) ? 0.0F : (float)(itemStack.getUseDuration(livingEntity) - livingEntity.getUseItemRemainingTicks()) / (float)CrossbowItem.getChargeDuration(itemStack, livingEntity);
         }
      });
      register(Items.CROSSBOW, ResourceLocation.withDefaultNamespace("pulling"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack && !CrossbowItem.isCharged(itemStack) ? 1.0F : 0.0F;
      });
      register(Items.CROSSBOW, ResourceLocation.withDefaultNamespace("charged"), (itemStack, clientLevel, livingEntity, i) -> {
         return CrossbowItem.isCharged(itemStack) ? 1.0F : 0.0F;
      });
      register(Items.CROSSBOW, ResourceLocation.withDefaultNamespace("firework"), (itemStack, clientLevel, livingEntity, i) -> {
         ChargedProjectiles chargedProjectiles = (ChargedProjectiles)itemStack.get(DataComponents.CHARGED_PROJECTILES);
         return chargedProjectiles != null && chargedProjectiles.contains(Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
      });
      register(Items.ELYTRA, ResourceLocation.withDefaultNamespace("broken"), (itemStack, clientLevel, livingEntity, i) -> {
         return ElytraItem.isFlyEnabled(itemStack) ? 0.0F : 1.0F;
      });
      register(Items.FISHING_ROD, ResourceLocation.withDefaultNamespace("cast"), (itemStack, clientLevel, livingEntity, i) -> {
         if (livingEntity == null) {
            return 0.0F;
         } else {
            boolean bl = livingEntity.getMainHandItem() == itemStack;
            boolean bl2 = livingEntity.getOffhandItem() == itemStack;
            if (livingEntity.getMainHandItem().getItem() instanceof FishingRodItem) {
               bl2 = false;
            }

            return (bl || bl2) && livingEntity instanceof Player && ((Player)livingEntity).fishing != null ? 1.0F : 0.0F;
         }
      });
      register(Items.SHIELD, ResourceLocation.withDefaultNamespace("blocking"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F;
      });
      register(Items.TRIDENT, ResourceLocation.withDefaultNamespace("throwing"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F;
      });
      register(Items.LIGHT, ResourceLocation.withDefaultNamespace("level"), (itemStack, clientLevel, livingEntity, i) -> {
         BlockItemStateProperties blockItemStateProperties = (BlockItemStateProperties)itemStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
         Integer integer = (Integer)blockItemStateProperties.get(LightBlock.LEVEL);
         return integer != null ? (float)integer / 16.0F : 1.0F;
      });
      register(Items.GOAT_HORN, ResourceLocation.withDefaultNamespace("tooting"), (itemStack, clientLevel, livingEntity, i) -> {
         return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F;
      });
   }
}
