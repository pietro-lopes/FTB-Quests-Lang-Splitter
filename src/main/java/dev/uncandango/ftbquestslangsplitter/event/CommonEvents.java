package dev.uncandango.ftbquestslangsplitter.event;

import dev.uncandango.ftbquestslangsplitter.FTBQuestsLangSplitter;
import dev.uncandango.ftbquestslangsplitter.command.LangSplitterCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = FTBQuestsLangSplitter.MODID)
public class CommonEvents {
	@SubscribeEvent
	public static void registerClientCommands(RegisterCommandsEvent event) {
		LangSplitterCommands.registerCommonCommands(event.getBuildContext(), event.getDispatcher());
	}
}
