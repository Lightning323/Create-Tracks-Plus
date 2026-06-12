package com.Gr00ze.stickywheels;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(
   value = "stickywheels",
   dist = {Dist.CLIENT}
)
@EventBusSubscriber(
   modid = "stickywheels",
   value = {Dist.CLIENT}
)
public class StickyWheelsModClient {
   public StickyWheelsModClient(ModContainer container) {
      container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
   }

   @SubscribeEvent
   static void onClientSetup(FMLClientSetupEvent event) {
      StickyWheelsMod.LOGGER.info("HELLO FROM CLIENT SETUP");
      StickyWheelsMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
   }
}
