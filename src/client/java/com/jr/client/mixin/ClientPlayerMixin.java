package com.jr.client.mixin;

import com.jr.client.ElytraAvionicsClient;
import com.jr.client.ElytraHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;


@Mixin(LocalPlayer.class)
public class ClientPlayerMixin {

	private final Set<Integer> playedCallouts = new HashSet<>();
	private static final int[] CALLOUT_HEIGHTS = {100, 70, 60, 50, 40, 30, 20, 10, 5};
	private SimpleSoundInstance masterWarnInstance = null;
	private int pullUpCooldown = 0;

	@Inject(at = @At("TAIL"), method = "tick")
	private void init(CallbackInfo info) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		Level world = player.level();
		ElytraHUD.groundHeight = getHeightToGround(player, world);
		ElytraHUD.verticalSpeed = Math.round(player.getDeltaMovement().y * 10.0) / 10.0;
		Vec3 velocity = player.getDeltaMovement();
		double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20;
		ElytraHUD.groundSpeed = Math.round(speed * 10.0) / 10.0;
		if (player.isFallFlying() && isElytraLow(player)) {
			if (masterWarnInstance == null || !Minecraft.getInstance().getSoundManager().isActive(masterWarnInstance)) {
				masterWarnInstance = new SimpleSoundInstance(
						Identifier.fromNamespaceAndPath("elytraavionics", "master_warn"),
						SoundSource.PLAYERS, 1.0f, 1.0f, RandomSource.create(), true, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
				Minecraft.getInstance().getSoundManager().play(masterWarnInstance);
			}
		} else if (!isElytraLow(player) || !player.isFallFlying()) {
			if (masterWarnInstance != null) {
				Minecraft.getInstance().getSoundManager().stop(masterWarnInstance);
				masterWarnInstance = null;
			}
		}
		if (player.isFallFlying() && isTerrainAhead(player, world)) {
			ElytraHUD.pullUpActive = true;
			if (pullUpCooldown <= 0) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ElytraAvionicsClient.PULL_UP, 1.0f, 4.0f));
				pullUpCooldown = 60;
			} else {
				pullUpCooldown--;
			}
		} else {
			ElytraHUD.pullUpActive = false;
			pullUpCooldown = 0;
		}
		if (player.isFallFlying() && isPlayerSinking(player)){
			int currentHeight = getHeightToGround(player, world);
			for (int height : CALLOUT_HEIGHTS) {
				if (!playedCallouts.contains(height) && currentHeight <= height && currentHeight > height - 10) {
					playedCallouts.add(height);
					SoundEvent sound = getCalloutSound(height);
					if (sound != null) {
						Minecraft.getInstance().getSoundManager().play(
								SimpleSoundInstance.forUI(sound, 1.0f, 2.0f)
						);
					}
				}
			}
		} else if (player.isFallFlying() && !isPlayerSinking(player)){
			playedCallouts.clear();
		} else {
			playedCallouts.clear();
		}
	}

	private SoundEvent getCalloutSound(int height) {
		return switch (height) {
			case 5   -> ElytraAvionicsClient.CALLOUT_5;
			case 10  -> ElytraAvionicsClient.CALLOUT_10;
			case 20  -> ElytraAvionicsClient.CALLOUT_20;
			case 30  -> ElytraAvionicsClient.CALLOUT_30;
			case 40  -> ElytraAvionicsClient.CALLOUT_40;
			case 50  -> ElytraAvionicsClient.CALLOUT_50;
			case 60  -> ElytraAvionicsClient.CALLOUT_60;
			case 70  -> ElytraAvionicsClient.CALLOUT_70;
			case 100 -> ElytraAvionicsClient.CALLOUT_100;
			default  -> null;
		};
	}

	private boolean isPlayerSinking(Player player){
		return 0 > player.getDeltaMovement().y;
	}

	private int getHeightToGround(LocalPlayer localPlayer, Level world){
		BlockPos position = localPlayer.blockPosition();
		int height = 0;
		for (int i = 1; i <= 150; i++) {
			BlockPos checkPos = position.below(i);
			if (!world.getBlockState(checkPos).isAir()) {
				height = i;
				break;
			}
		}
		return height;
	}

	private boolean isTerrainAhead(LocalPlayer player, Level world) {
		Vec3 velocity = player.getDeltaMovement();
		double speed = velocity.length();
		if (speed < 0.1) return false;
		double lookAhead = Math.min(Math.max(speed * 60, 8), 50);
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(velocity.normalize().scale(lookAhead));
		ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
		BlockHitResult hit = world.clip(context);
		return hit.getType() != HitResult.Type.MISS;
	}

	private boolean isElytraLow(Player player) {
		boolean isLow = false;
		ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!elytra.is(Items.ELYTRA)){
			return false;
		}
		int maxDurability = elytra.getMaxDamage();
		int currentDamage = elytra.getDamageValue();
		int remainingDurability = maxDurability - currentDamage;
		if (remainingDurability < 10){
			isLow = true;
		}
		return isLow;
	}


}