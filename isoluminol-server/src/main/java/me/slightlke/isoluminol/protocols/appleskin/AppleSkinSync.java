package me.slightlke.isoluminol.protocols.appleskin;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class AppleSkinSync {

    private static final Identifier SATURATION_CHANNEL = Identifier.fromNamespaceAndPath("appleskin", "saturation_sync");
    private static final Identifier EXHAUSTION_CHANNEL = Identifier.fromNamespaceAndPath("appleskin", "exhaustion_sync");

    private static final Map<ServerPlayer, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean active;

    private AppleSkinSync() {
    }

    public static void init() {
        active = true;
    }

    public static void tick(final ServerPlayer player) {
        if (!active) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        State state = STATES.get(player);

        if (state == null) {
            final FoodData food = player.getFoodData();
            state = new State();
            state.saturation = food.getSaturationLevel();
            state.exhaustion = food.exhaustionLevel;
            STATES.put(player, state);
            send(player, SATURATION_CHANNEL, state.saturation);
            send(player, EXHAUSTION_CHANNEL, state.exhaustion);
            return;
        }

        if (++state.ticks < 2) {
            return;
        }
        state.ticks = 0;

        final FoodData food = player.getFoodData();
        final float saturation = food.getSaturationLevel();
        if (state.saturation != saturation) {
            state.saturation = saturation;
            send(player, SATURATION_CHANNEL, saturation);
        }

        final float exhaustion = food.exhaustionLevel;
        if (Math.abs(state.exhaustion - exhaustion) > 0.2F) {
            state.exhaustion = exhaustion;
            send(player, EXHAUSTION_CHANNEL, exhaustion);
        }
    }

    private static void send(final ServerPlayer player, final Identifier channel, final float value) {
        final byte[] data = new byte[Float.BYTES];
        ByteBuffer.wrap(data).putFloat(value);
        player.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(channel, data)));
    }

    private static final class State {
        private int ticks;
        private float saturation;
        private float exhaustion;
    }
}
