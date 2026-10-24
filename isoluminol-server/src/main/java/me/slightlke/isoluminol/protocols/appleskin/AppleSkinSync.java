package me.slightlke.isoluminol.protocols.appleskin;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server half of the AppleSkin protocol.
 *
 * <p>Clients subscribe to {@code appleskin:saturation},
 * {@code appleskin:exhaustion} and {@code appleskin:natural_regeneration}
 * through {@code minecraft:register}.</p>
 */
public final class AppleSkinSync {

    private static final Identifier SATURATION_CHANNEL = Identifier.fromNamespaceAndPath("appleskin", "saturation");
    private static final Identifier EXHAUSTION_CHANNEL = Identifier.fromNamespaceAndPath("appleskin", "exhaustion");
    private static final Identifier NATURAL_REGENERATION_CHANNEL = Identifier.fromNamespaceAndPath("appleskin", "natural_regeneration");

    private static final String SATURATION_CHANNEL_NAME = SATURATION_CHANNEL.toString();
    private static final String EXHAUSTION_CHANNEL_NAME = EXHAUSTION_CHANNEL.toString();
    private static final String NATURAL_REGENERATION_CHANNEL_NAME = NATURAL_REGENERATION_CHANNEL.toString();

    private static final float EXHAUSTION_CHANGE_THRESHOLD = 0.2F;

    // Weak keys: state is discarded when the ServerPlayer instance is collected.
    private static final Map<ServerPlayer, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile boolean active;
    private static int syncTickInterval = 20;

    private AppleSkinSync() {
    }

    public static void init(final int syncTickInterval) {
        if (syncTickInterval < 1) {
            throw new IllegalArgumentException("protocols.appleskin.sync-tick-interval must be >= 1");
        }
        AppleSkinSync.syncTickInterval = syncTickInterval;
        active = true;
    }

    public static void tick(final ServerPlayer player) {
        if (!active) {
            return;
        }
        if (player.tickCount % syncTickInterval != 0) {
            return;
        }

        final Set<String> channels = player.connection.pluginMessagerChannels;
        if (!isSubscribed(channels, SATURATION_CHANNEL_NAME)
            && !isSubscribed(channels, EXHAUSTION_CHANNEL_NAME)
            && !isSubscribed(channels, NATURAL_REGENERATION_CHANNEL_NAME)) {
            return;
        }

        final FoodData food = player.getFoodData();
        State state = STATES.get(player);
        if (state == null) {
            state = new State();
            STATES.put(player, state);
        }

        if (isSubscribed(channels, SATURATION_CHANNEL_NAME)) {
            final float saturation = food.getSaturationLevel();
            if (state.saturation == null || state.saturation.floatValue() != saturation) {
                state.saturation = saturation;
                sendFloat(player, SATURATION_CHANNEL, saturation);
            }
        }

        if (isSubscribed(channels, EXHAUSTION_CHANNEL_NAME)) {
            final float exhaustion = food.exhaustionLevel;
            if (state.exhaustion == null || Math.abs(state.exhaustion - exhaustion) > EXHAUSTION_CHANGE_THRESHOLD) {
                state.exhaustion = exhaustion;
                sendFloat(player, EXHAUSTION_CHANNEL, exhaustion);
            }
        }

        if (isSubscribed(channels, NATURAL_REGENERATION_CHANNEL_NAME)) {
            final boolean naturalRegeneration = player.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
            if (state.naturalRegeneration == null || state.naturalRegeneration != naturalRegeneration) {
                state.naturalRegeneration = naturalRegeneration;
                sendBoolean(player, NATURAL_REGENERATION_CHANNEL, naturalRegeneration);
            }
        }
    }

    private static boolean isSubscribed(final Set<String> channels, final String channel) {
        synchronized (channels) {
            return channels.contains(channel);
        }
    }

    private static void sendFloat(final ServerPlayer player, final Identifier channel, final float value) {
        final byte[] data = new byte[Float.BYTES];
        ByteBuffer.wrap(data).putFloat(value);
        send(player, channel, data);
    }

    private static void sendBoolean(final ServerPlayer player, final Identifier channel, final boolean value) {
        send(player, channel, new byte[]{(byte) (value ? 1 : 0)});
    }

    private static void send(final ServerPlayer player, final Identifier channel, final byte[] data) {
        player.connection.send(new ClientboundCustomPayloadPacket(new DiscardedPayload(channel, data)));
    }

    private static final class State {
        private Float saturation;
        private Float exhaustion;
        private Boolean naturalRegeneration;
    }
}
