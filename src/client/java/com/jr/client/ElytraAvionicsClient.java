package com.jr.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ElytraAvionicsClient implements ClientModInitializer {
	public static final String MOD_ID = "elytraavionics";

	public static SoundEvent CALLOUT_5;
	public static SoundEvent CALLOUT_10;
	public static SoundEvent CALLOUT_20;
	public static SoundEvent CALLOUT_30;
	public static SoundEvent CALLOUT_40;
	public static SoundEvent CALLOUT_50;
	public static SoundEvent CALLOUT_60;
	public static SoundEvent CALLOUT_70;
	public static SoundEvent CALLOUT_100;
	public static SoundEvent MASTER_WARN;

	@Override
	public void onInitializeClient() {
		ElytraHUD.register();

		CALLOUT_5   = registerSound("callout_5");
		CALLOUT_10  = registerSound("callout_10");
		CALLOUT_20  = registerSound("callout_20");
		CALLOUT_30  = registerSound("callout_30");
		CALLOUT_40  = registerSound("callout_40");
		CALLOUT_50  = registerSound("callout_50");
		CALLOUT_60  = registerSound("callout_60");
		CALLOUT_70  = registerSound("callout_70");
		CALLOUT_100 = registerSound("callout_100");
		MASTER_WARN = registerSound("master_warn");
	}

	private static SoundEvent registerSound(String name) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createFixedRangeEvent(id, 16.0f));
	}
}