package dev.uncandango.ftbquestslangsplitter.mixin.accessor;

import dev.ftb.mods.ftbquests.quest.translation.TranslationManager;
import dev.ftb.mods.ftbquests.quest.translation.TranslationTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TranslationManager.class)
public interface TranslationManagerAccessor {
	@Accessor("map")
	Map<String, TranslationTable> langsplitter$getMap();
}
