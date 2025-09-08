package dev.uncandango.ftbquestslangsplitter.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.quest.QuestObjectType;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.translation.TranslationTable;
import dev.uncandango.ftbquestslangsplitter.FTBQuestsLangSplitter;
import dev.uncandango.ftbquestslangsplitter.mixin.accessor.TranslationManagerAccessor;
import dev.uncandango.ftbquestslangsplitter.mixin.accessor.TranslationTableAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LangSplitterCommands {

	public static void registerCommonCommands(CommandBuildContext ctx, CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("langsplitter").requires(LangSplitterCommands::isSSP)
				.then(Commands.literal("split")
					.then(Commands.argument("replace_unmerged", BoolArgumentType.bool())
						.executes(cmd -> splitLangFiles(cmd, null, BoolArgumentType.getBool(cmd, "replace_unmerged")))
						.then(Commands.argument("locale", LocaleArgument.locale())
							.executes(cmd -> splitLangFiles(cmd, LocaleArgument.getLocale(cmd, "locale"), BoolArgumentType.getBool(cmd, "replace_unmerged")))
						)
					)
				)
				.then(Commands.literal("fill_missing_translation")
					.then(Commands.argument("replace_unmerged", BoolArgumentType.bool())
						.then(Commands.argument("locale", LocaleArgument.locale())
							.executes(cmd -> fillMissingTranslation(cmd, LocaleArgument.getLocale(cmd, "locale"), BoolArgumentType.getBool(cmd, "replace_unmerged")))
						)
					)
				)
				.then(Commands.literal("purge_merged")
					.executes(LangSplitterCommands::purgeMerged)
				)
		);
	}

	private static int purgeMerged(CommandContext<CommandSourceStack> cmd) {
		var langFolder = ServerQuestFile.INSTANCE.getFolder().resolve("lang");
		try (Stream<Path> s = Files.walk(langFolder)) {
			s.filter(p -> p.getFileName().toString().endsWith(".snbt_merged"))
				.forEach(p -> {
					try {
						Files.delete(p);
						FTBQuestsLangSplitter.LOGGER.debug("Deleted file {}", langFolder.relativize(p));
					} catch (IOException e) {
						FTBQuestsLangSplitter.LOGGER.error("Error while deleting merged file {}: {}", langFolder.relativize(p), e.getMessage());
					}
				});
		} catch (IOException e) {
			FTBQuestsLangSplitter.LOGGER.error("Error while scanning folder {} for merged files", FMLPaths.CONFIGDIR.get().relativize(langFolder));
			cmd.getSource().sendFailure(Component.literal("Can't scan lang folder " + FMLPaths.CONFIGDIR.get().relativize(langFolder)  + " for merged files"));
		}
		cmd.getSource().sendSuccess(() -> Component.translatable("ftbquestslangsplitter.commands.purged_merged_files"), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int fillMissingTranslation(CommandContext<CommandSourceStack> cmd, String locale, boolean replacesExisting) {
		if (locale.equals("en_us")) {
			cmd.getSource().sendFailure(Component.literal("Lang en_us is supposed to be the main language..."));
			return 0;
		}
		var map = ((TranslationManagerAccessor)ServerQuestFile.INSTANCE.getTranslationManager()).langsplitter$getMap();
		var englishTableMap = ((TranslationTableAccessor)map.computeIfAbsent("en_us",(lang) -> new TranslationTable())).langsplitter$getMap();
		var targetTable = ((TranslationTableAccessor)map.computeIfAbsent(locale,(lang) -> new TranslationTable()));
		var targetTableMap = targetTable.langsplitter$getMap();
		final AtomicInteger count = new AtomicInteger();
		englishTableMap.forEach((id, value) -> {
			if (!targetTableMap.containsKey(id)) {
				count.incrementAndGet();
				targetTableMap.put(id, value);
				FTBQuestsLangSplitter.LOGGER.debug("Added missing entry with id {}", id);
			}
		});
		if (count.get() > 0) {
			targetTable.langsplitter$setSaveNeeded(true);
			cmd.getSource().sendSuccess(() -> Component.translatable("ftbquestslangsplitter.commands.added_missing_lang_entries", count.get(), locale).withStyle(ChatFormatting.GREEN), true);
			return splitLangFiles(cmd, locale, replacesExisting);
		} else {
			cmd.getSource().sendSuccess(() -> Component.translatable("ftbquestslangsplitter.commands.no_missing_entries", locale).withStyle(ChatFormatting.GREEN), true);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static Lazy<Map<String,String>> langsplitter$idToChapter;
	private static final Supplier<Map<String,String>> langsplitter$idToChapterSupplier = () -> {
		Map<String,String> result = new HashMap<>();
		for (var object : ServerQuestFile.INSTANCE.getAllObjects()) {
			switch (object.getObjectType()) {
				case QUEST, TASK, QUEST_LINK -> {
					var chapter = object.getQuestChapter();
					if (chapter == null) continue;
					var fileName = object.getQuestChapter().getFilename();
					result.put(object.getObjectType().getId() + "." + object.getCodeString(), fileName);
				}
			}
		}
		return result;
	};

	private static int splitLangFiles(CommandContext<CommandSourceStack> cmd, @Nullable String langCode, boolean replacesExisting){
		langsplitter$idToChapter = Lazy.of(langsplitter$idToChapterSupplier);
		var map = ((TranslationManagerAccessor)ServerQuestFile.INSTANCE.getTranslationManager()).langsplitter$getMap();
		var langFolder = ServerQuestFile.INSTANCE.getFolder().resolve("lang");
		if (langCode != null && !map.containsKey(langCode)){
			return fillMissingTranslation(cmd, langCode, replacesExisting);
		}
		map.forEach((locale, table) -> {
			if (langCode == null || langCode.equals(locale)) {
				boolean prevSort = SNBT.setShouldSortKeysOnWrite(true);
				Map<Path, CompoundTag> splittedFiles = new HashMap<>();
				((TranslationTableAccessor) table).langsplitter$getMap().forEach((id, value) -> {
					String[] parts = id.split("\\.");
					if (parts.length < 2) {
						FTBQuestsLangSplitter.LOGGER.error("Id {} could not be split into type and code, skipping...", id);
						return;
					}
					String type = parts[0];
					QuestObjectType enumType = QuestObjectType.NAME_MAP.get(type);
					String code = parts[1];
					String key = type + "." + code;
					Path file = resolveFile(langFolder, locale, enumType, key);
					if (file == null) return;
					var tag = splittedFiles.computeIfAbsent(file, (newLangFile) -> new CompoundTag());
					value
						.ifLeft(l -> tag.putString(id, l))
						.ifRight(r -> {
							var list = new ListTag();
							r.forEach(desc -> {
								list.add(StringTag.valueOf(desc));
							});
							tag.put(id, list);
						});
				});
				splittedFiles.forEach((path, tag) -> {
					if (!replacesExisting && Files.exists(path)) {
						cmd.getSource().sendSystemMessage(Component.translatable("ftbquestslangsplitter.commands.file_already_exists", langFolder.relativize(path).toString()).withStyle(ChatFormatting.YELLOW));
					} else {
						SNBT.write(path, tag);
					}
				});
				SNBT.setShouldSortKeysOnWrite(prevSort);
			}
		});
		langsplitter$idToChapter = null;
		if (langCode == null) {
			cmd.getSource().sendSuccess(() -> Component.translatable("ftbquestslangsplitter.commands.all_lang_file_split").withStyle(ChatFormatting.GREEN), true);
		} else {
			cmd.getSource().sendSuccess(() -> Component.translatable("ftbquestslangsplitter.commands.lang_file_split", langCode).withStyle(ChatFormatting.GREEN), true);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Nullable
	private static Path resolveFile(Path baseFolder, String locale, QuestObjectType questType, String questKey) {
		return switch (questType) {
			case QUEST, TASK, QUEST_LINK -> {
				var newFolder = baseFolder.resolve(locale).resolve("chapters");
				if (!createFolder(newFolder)) yield null;
				var chapterFilename = langsplitter$idToChapter.get().get(questKey);
				if (chapterFilename == null) {
					yield null;
				}
				yield newFolder.resolve(chapterFilename + ".snbt");
			}
			default -> {
				var newFolder = baseFolder.resolve(locale);
				if (!createFolder(newFolder)) yield null;
				yield newFolder.resolve(questType.getId() + ".snbt");
			}
		};
	}

	private static boolean createFolder(Path folder){
		if (!Files.exists(folder)) {
			try {
				Files.createDirectories(folder);
			} catch (IOException e) {
				FTBQuestsLangSplitter.LOGGER.error("can't create lang folder {}: {}", folder, e.getMessage());
				return false;
			}
		}
		return true;
	}

	private static boolean isSSP(CommandSourceStack s) {
		//noinspection ConstantValue
		return s.getServer() != null && s.getServer().isSingleplayer();
	}
}
