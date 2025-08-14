package dev.uncandango.ftbquestslangsplitter;

import com.mojang.logging.LogUtils;
import dev.uncandango.ftbquestslangsplitter.registry.ArgumentTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(FTBQuestsLangSplitter.MODID)
public class FTBQuestsLangSplitter {
    public static final String MODID = "ftbquestslangsplitter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBQuestsLangSplitter(IEventBus modEventBus, ModContainer modContainer) {
		ArgumentTypes.register(modEventBus);
    }
}
