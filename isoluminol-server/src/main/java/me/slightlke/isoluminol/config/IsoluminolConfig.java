package me.slightlke.isoluminol.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class IsoluminolConfig {

    public Isoluminol isoluminol = new Isoluminol();

    @ConfigSerializable
    public static class Isoluminol {
        public String tip_en = "No configurable options yet. This is a placeholder and does not need to be modified.";
        public String tip_zh = "暂时没有设置可配置项目，此为占位符，不必修改";
    }
}
