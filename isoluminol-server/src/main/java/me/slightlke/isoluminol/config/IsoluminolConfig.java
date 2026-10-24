package me.slightlke.isoluminol.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class IsoluminolConfig {

    @Setting("optimizations")
    private Optimizations optimizations = new Optimizations();

    @Setting("protocols")
    private Protocols protocols = new Protocols();

    public Optimizations optimizations() {
        return this.optimizations;
    }

    public Protocols protocols() {
        return this.protocols;
    }

    @ConfigSerializable
    public static final class Optimizations {

        @Setting("simd")
        private Simd simd = new Simd();

        public Simd simd() {
            return this.simd;
        }
    }

    @ConfigSerializable
    public static final class Protocols {

        @Setting("appleskin")
        private AppleSkin appleskin = new AppleSkin();

        public AppleSkin appleskin() {
            return this.appleskin;
        }
    }

    @ConfigSerializable
    public static final class Simd {

        @Setting("enabled")
        @Comment("Enable SIMD acceleration for map color conversion. Requires --add-modules=jdk.incubator.vector.")
        private boolean enabled = true;

        public boolean enabled() {
            return this.enabled;
        }
    }

    @ConfigSerializable
    public static final class AppleSkin {

        @Setting("enabled")
        @Comment("Sync saturation and exhaustion to clients for AppleSkin.")
        private boolean enabled = true;

        @Setting("sync-tick-interval")
        @Comment("How often to check and sync saturation/exhaustion, in ticks.")
        private int syncTickInterval = 20;

        public boolean enabled() {
            return this.enabled;
        }

        public int syncTickInterval() {
            return this.syncTickInterval;
        }
    }
}
