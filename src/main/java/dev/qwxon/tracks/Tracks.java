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
package dev.qwxon.tracks;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.qwxon.tracks.events.TracksCommonEvents;
import dev.qwxon.tracks.index.TracksBlockEntityTypes;
import dev.qwxon.tracks.index.TracksBlocks;
import dev.qwxon.tracks.index.TracksItems;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tracks {
    public static final String MOD_ID = "tracks";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"tracks");
    private static final NonNullSupplier<SimulatedRegistrate> REGISTRATE = NonNullSupplier.lazy(() -> (SimulatedRegistrate)new SimulatedRegistrate(Tracks.path(MOD_ID), MOD_ID).defaultCreativeTab((ResourceKey)null));

    public static void init() {
        Tracks.setTooltips();
        TracksBlocks.init();
        TracksBlockEntityTypes.init();
        TracksItems.init();
        SableEventPlatform.INSTANCE.onPhysicsTick(TracksCommonEvents::physicsTick);
    }

    private static void setTooltips() {
        Tracks.getRegistrate().setTooltipModifierFactory(item -> {
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

