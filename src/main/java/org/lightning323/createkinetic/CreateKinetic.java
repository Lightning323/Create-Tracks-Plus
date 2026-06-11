/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.foundation.item.ItemDescription$Modifier
 *  com.simibubi.create.foundation.item.KineticStats
 *  com.simibubi.create.foundation.item.TooltipHelper
 *  com.simibubi.create.foundation.item.TooltipModifier
 *  com.tterrag.registrate.util.nullness.NonNullSupplier
 *  dev.ryanhcode.sable.platform.SableEventPlatform
 *  dev.simulated_team.simulated.registrate.SimulatedRegistrate
 *  dev.simulated_team.simulated.util.SimColors
 *  net.createmod.catnip.lang.FontHelper$Palette
 *  net.minecraft.ChatFormatting
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Rarity
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.lightning323.createkinetic;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.lightning323.createkinetic.config.TracksServerConfig;
import org.lightning323.createkinetic.events.TracksCommonEvents;
import org.lightning323.createkinetic.registry.TracksBlockEntityTypes;
import org.lightning323.createkinetic.registry.TracksBlocks;
import org.lightning323.createkinetic.registry.TracksItems;
import org.lightning323.createkinetic.network.OpenTuningScreenPayload;
import org.lightning323.createkinetic.network.RequestOpenTuningPayload;
import org.lightning323.createkinetic.network.SelectTrackTuningModePayload;

import static org.lightning323.createkinetic.CreateKinetic.MOD_ID;

@Mod(value=MOD_ID)
public class CreateKinetic {
    public static final String MOD_ID = "tracks";
    public static final String trackHiddenTag = "tracks_hidden";
    private static final NonNullSupplier<SimulatedRegistrate> REGISTRATE = NonNullSupplier.lazy(() -> (SimulatedRegistrate)new SimulatedRegistrate(CreateKinetic.path(MOD_ID), MOD_ID).defaultCreativeTab((ResourceKey)null));

    public CreateKinetic(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(CreateKinetic::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, (IConfigSpec) TracksServerConfig.SPEC);
        CreateKinetic.init();
        CreateKinetic.getRegistrate().registerEventListeners(modBus);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID).versioned("1.0.0");
        registrar.playToServer(RequestOpenTuningPayload.TYPE, RequestOpenTuningPayload.STREAM_CODEC, RequestOpenTuningPayload::handle);
        registrar.playToServer(SelectTrackTuningModePayload.TYPE, SelectTrackTuningModePayload.STREAM_CODEC, SelectTrackTuningModePayload::handle);
        registrar.playToClient(OpenTuningScreenPayload.TYPE, OpenTuningScreenPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                TracksClient.openTuningScreen();
            }
        }));
    }

    public static void init() {
        CreateKinetic.setTooltips();
        TracksBlocks.init();
        TracksBlockEntityTypes.init();
        TracksItems.init();
        SableEventPlatform.INSTANCE.onPhysicsTick(TracksCommonEvents::physicsTick);
    }

    private static void setTooltips() {
        CreateKinetic.getRegistrate().setTooltipModifierFactory(item -> {
            Rarity rarity = item.getDefaultInstance().getRarity();
            FontHelper.Palette color = FontHelper.Palette.STANDARD_CREATE;
            if (rarity == Rarity.EPIC) {
                color = new FontHelper.Palette(TooltipHelper.styleFromColor((int)SimColors.EPIC_OURPLE), TooltipHelper.styleFromColor((ChatFormatting)rarity.color()));
            }
            return new ItemDescription.Modifier(item, color).andThen(TooltipModifier.mapNull((TooltipModifier)KineticStats.create((Item)item)));
        });
    }

    public static SimulatedRegistrate getRegistrate() {
        return (SimulatedRegistrate)REGISTRATE.get();
    }

    public static ResourceLocation path(String path) {
        return ResourceLocation.tryBuild((String)MOD_ID, (String)path);
    }
}

