package dev.uncandango.ftbquestslangsplitter.registry;

import dev.uncandango.ftbquestslangsplitter.FTBQuestsLangSplitter;
import dev.uncandango.ftbquestslangsplitter.command.LocaleArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ArgumentTypes {
	private static DeferredRegister<ArgumentTypeInfo<?,?>> ARGUMENT_TYPES = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, FTBQuestsLangSplitter.MODID);

	public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<LocaleArgument>> LOCALE_ARGUMENT = ARGUMENT_TYPES.register(
		"locale",
		() -> ArgumentTypeInfos.registerByClass(LocaleArgument.class, SingletonArgumentInfo.contextFree(LocaleArgument::locale))
	);

	public static void register(IEventBus modEventBus){
		ARGUMENT_TYPES.register(modEventBus);
	};
}
