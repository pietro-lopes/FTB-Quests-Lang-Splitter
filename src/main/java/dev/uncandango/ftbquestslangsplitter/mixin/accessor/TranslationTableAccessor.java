package dev.uncandango.ftbquestslangsplitter.mixin.accessor;

import com.mojang.datafixers.util.Either;
import dev.ftb.mods.ftbquests.quest.translation.TranslationTable;
import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(TranslationTable.class)
public interface TranslationTableAccessor {
	@Invoker("isSaveNeeded")
	boolean langsplitter$isSaveNeeded();

	@Invoker("setSaveNeeded")
	void langsplitter$setSaveNeeded(boolean saveNeeded);

	@Invoker("listOfStr")
	static ListTag langsplitter$listOfStr(List<String> list){
		throw new AssertionError();
	};

	@Accessor("map")
	Map<String, Either<String, List<String>>> langsplitter$getMap();
}
