package com.Gr00ze.stickywheels;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod("stickywheels")
public class StickyWheelsMod {
   public static final String MODID = "stickywheels";
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("stickywheels");
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("stickywheels");

   public StickyWheelsMod(IEventBus modEventBus, ModContainer modContainer) {
      modEventBus.addListener(this::commonSetup);
      NeoForge.EVENT_BUS.register(this);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      LOGGER.info("HELLO FROM COMMON SETUP");
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      LOGGER.info("HELLO from server starting");
   }
}
