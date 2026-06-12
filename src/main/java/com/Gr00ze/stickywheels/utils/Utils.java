package com.Gr00ze.stickywheels.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public class Utils {
   public static ItemLike offroadItem(String name) {
      return (ItemLike)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("offroad", name));
   }
}
