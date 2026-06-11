/*
 * Decompiled with CFR 0.152.
 */
package org.lightning323.createkinetic;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lightning323.createkinetic.client.TrackRenderTuning;
import org.lightning323.createkinetic.client.TrackTuningScreen;
import org.lightning323.createkinetic.content.blocks.sable_track.SableTrackRenderer;
import org.lightning323.createkinetic.content.blocks.wheel_mount.AdjustableWheelMountRenderer;
import org.lightning323.createkinetic.content.items.SuspensionKeyItem;
import org.lightning323.createkinetic.index.TracksBlockEntityTypes;
import org.lightning323.createkinetic.index.TracksItems;
import org.lightning323.createkinetic.index.TracksPartialModels;
import org.lightning323.createkinetic.index.TracksSpriteShifts;
import org.lightning323.createkinetic.network.RequestOpenTuningPayload;

@Mod(value="tracks", dist={Dist.CLIENT})
public class TracksClient {
    private static final KeyMapping OPEN_TUNING = new KeyMapping("key.tracks.open_tuning", InputConstants.Type.KEYSYM, 74, "key.categories.tracks");

    public TracksClient(IEventBus modBus) {
        modBus.addListener(TracksClient::clientSetup);
        modBus.addListener(TracksClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, TracksClient::clientTick);
        TracksClient.init();
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TrackRenderTuning.load(Minecraft.getInstance().gameDirectory.toPath().resolve("config/tracks-render-tuning.txt"));
            BlockEntityRenderers.register((BlockEntityType)((BlockEntityType) OffroadBlockEntityTypes.WHEEL_MOUNT.get()), AdjustableWheelMountRenderer::new);
            BlockEntityRenderers.register((BlockEntityType)((BlockEntityType) TracksBlockEntityTypes.SABLE_TRACK.get()), SableTrackRenderer::new);
        });
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TUNING);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean bl = TracksClient.holdingSuspensionKey = minecraft.player != null && (minecraft.player.getMainHandItem().is(TracksItems.SUSPENSION_KEY.asItem()) || minecraft.player.getOffhandItem().is(TracksItems.SUSPENSION_KEY.asItem()));
        if (minecraft.player != null && TracksClient.holdingSuspensionKey) {
            ItemStack key = minecraft.player.getMainHandItem().is(TracksItems.SUSPENSION_KEY.asItem()) ? minecraft.player.getMainHandItem() : minecraft.player.getOffhandItem();
            SuspensionKeyItem.TuningMode mode = SuspensionKeyItem.getMode(key);
            TracksClient.holdingSuspensionKeyInPositionMode = mode == SuspensionKeyItem.TuningMode.POSITION;
            TracksClient.holdingSuspensionKeyInAllPositionMode = mode == SuspensionKeyItem.TuningMode.ALL_POSITION;
            TracksClient.holdingSuspensionKeyInResetMode = mode == SuspensionKeyItem.TuningMode.RESET;
        } else {
            TracksClient.holdingSuspensionKeyInPositionMode = false;
            TracksClient.holdingSuspensionKeyInAllPositionMode = false;
            TracksClient.holdingSuspensionKeyInResetMode = false;
        }
        while (OPEN_TUNING.consumeClick()) {
            if (minecraft.player == null) continue;
            PacketDistributor.sendToServer((CustomPacketPayload)new RequestOpenTuningPayload(), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    public static void openTuningScreen() {
        Minecraft.getInstance().setScreen((Screen)new TrackTuningScreen());
    }


    public static boolean holdingSuspensionKey = false;
    public static boolean holdingSuspensionKeyInPositionMode = false;
    public static boolean holdingSuspensionKeyInAllPositionMode = false;
    public static boolean holdingSuspensionKeyInResetMode = false;

    public static void init() {
        TracksPartialModels.init();
        TracksSpriteShifts.init();
    }
}

