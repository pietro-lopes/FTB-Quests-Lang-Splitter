package dev.uncandango.ftbquestslangsplitter.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.integration.PermissionsHelper;
import dev.ftb.mods.ftbquests.quest.translation.TranslationManager;
import dev.ftb.mods.ftbquests.quest.translation.TranslationTable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(TranslationManager.class)
public abstract class TranslationManagerMixinClient {

	private static final AtomicBoolean kjstweaks$updatedAny = new AtomicBoolean();

	@Inject(method = "lambda$saveToNBT$1", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbquests/quest/translation/TranslationTable;setSaveNeeded(Z)V"))
	private static void remindPlayersToSplitRefSet(boolean force, Path langFolder, String locale, TranslationTable table, CallbackInfo ci) {
		kjstweaks$updatedAny.set(true);
	}

	@Inject(method = "saveToNBT", at = @At("RETURN"))
	private void remindPlayersToSplit(Path langFolder, boolean force, CallbackInfo ci) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		if (!kjstweaks$updatedAny.get() || !ClientQuestFile.canClientPlayerEdit()) return;
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server != null && server.isSingleplayer()) {
			var serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
			if (serverPlayer != null && PermissionsHelper.hasEditorPermission(serverPlayer, false)){
				var message = Component.translatable("ftbquestslangsplitter.split.lang_saved_reminder").withStyle(ChatFormatting.GREEN);
				serverPlayer.sendSystemMessage(message);
			}
		}

		kjstweaks$updatedAny.set(false);
	}

}
