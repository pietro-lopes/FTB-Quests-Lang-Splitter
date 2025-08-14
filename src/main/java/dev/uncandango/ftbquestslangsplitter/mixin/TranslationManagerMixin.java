package dev.uncandango.ftbquestslangsplitter.mixin;

import com.mojang.datafixers.util.Either;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.QuestObjectType;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.translation.TranslationManager;
import dev.ftb.mods.ftbquests.quest.translation.TranslationTable;
import dev.uncandango.ftbquestslangsplitter.FTBQuestsLangSplitter;
import dev.uncandango.ftbquestslangsplitter.mixin.accessor.TranslationTableAccessor;
import joptsimple.internal.Strings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mixin(TranslationManager.class)
public abstract class TranslationManagerMixin {

	@Shadow
	@Final
	private Map<String, TranslationTable> map;

	@Shadow
	private static boolean isValidLangFile(Path p) {
		return false;
	}

	@Shadow
	public abstract void saveToNBT(Path langFolder, boolean force);

	/**
	 * @author Uncandango
	 * @reason Implement splitter reader
	 */
	@Overwrite
	public void loadFromNBT(BaseQuestFile file, Path langFolder) {
		map.clear();

		if (!langsplitter$createFolder(langFolder)) return;

		var timeNow = Instant.now().getEpochSecond();
		try (Stream<Path> s = Files.list(langFolder)) {
			s.sorted(Comparator.comparingInt(path -> Files.isDirectory(path) ? 1 : 0)).forEach(path -> {
				if (Files.isDirectory(path) && !path.getFileName().toString().equals("recovery")){
					String locale = path.getFileName().toString().toLowerCase(Locale.ROOT);
					CompoundTag recovery = new CompoundTag();
					try (Stream<Path> lang = Files.list(path).sorted(Comparator.comparingInt(path2 -> Files.isDirectory(path2) ? 1 : 0))){
						lang.forEach(insideLang -> {
							if (Files.isDirectory(insideLang) && insideLang.getFileName().toString().equals("chapters")) {
								try (Stream<Path> chapter = Files.list(insideLang)){
									chapter.forEach(chapterPath -> {
										if (!Files.isDirectory(chapterPath)) {
											langsplitter$writeToOriginalLangFile(path, chapterPath, map, locale, recovery);
										}
									});
								} catch (IOException e) {
									FTBQuestsLangSplitter.LOGGER.error("can't scan lang folder {}: {}", path, e.getMessage());
								}
							} else {
								langsplitter$writeToOriginalLangFile(path, insideLang, map, locale, recovery);
							}
						});
					} catch (IOException e) {
						FTBQuestsLangSplitter.LOGGER.error("can't scan lang folder {}: {}", path, e.getMessage());
					}
					if (!recovery.isEmpty()) {
						Path recoveryFolder = langFolder.resolve("recovery");
						langsplitter$createFolder(recoveryFolder);
						Path recoveredFile = recoveryFolder.resolve(locale + "_" +  timeNow + ".snbt");
						if (!SNBT.write(recoveredFile, recovery)) {
							FTBQuestsLangSplitter.LOGGER.error("Error while writing recovery file for lang {} at {}", locale, recoveredFile);
						}
					}
				} else {
					if (isValidLangFile(path)) {
						CompoundTag langNBT = SNBT.read(path);
						if (langNBT != null) {
							String locale = (path.getFileName().toString().split("\\.", 2))[0].toLowerCase(Locale.ROOT);
							map.put(locale, TranslationTable.fromNBT(langNBT));
						} else {
							FTBQuestsLangSplitter.LOGGER.error("can't read lang file {}", path);
						}
					}
				}
			});
			if (!map.containsKey(file.getFallbackLocale())) {
				map.put(file.getFallbackLocale(), new TranslationTable());
			}
			FTBQuestsLangSplitter.LOGGER.info("loaded translation tables for {} language(s)", map.size());
		} catch (IOException e) {
			FTBQuestsLangSplitter.LOGGER.error("can't scan lang folder {}: {}", langFolder, e.getMessage());
		}
		this.saveToNBT(langFolder, false);
	}

	@Unique
	private static void langsplitter$writeToOriginalLangFile(Path localePath,Path filePath, Map<String, TranslationTable> localeMap, String locale, CompoundTag recovery) {
		if (isValidLangFile(filePath)) {
			CompoundTag langNBT = SNBT.read(filePath);
			if (langNBT != null) {
				TranslationTable table = localeMap.computeIfAbsent(locale, (l) -> new TranslationTable());
				var tableMap = ((TranslationTableAccessor)table).langsplitter$getMap();
				final AtomicBoolean merged = new AtomicBoolean();
				langNBT.getAllKeys().forEach(key -> {
					var entry = langNBT.get(key);
					Either<String, List<String>> newValue = null;
					Tag newValueTag = null;

					if (entry instanceof ListTag lt) {
						if (lt.isEmpty()) return;
						newValue = Either.right(lt.stream().map(Tag::getAsString).toList());
						newValueTag = lt;
					}

					if (entry instanceof StringTag st) {
						if (st.getAsString().isEmpty()) return;
						newValue = Either.left(st.getAsString());
						newValueTag = st;
					}

					if (newValue == null) return;
					var replaced = tableMap.put(key, newValue);
					if (replaced == null) {
						((TranslationTableAccessor) table).langsplitter$setSaveNeeded(true);
						merged.set(true);
					}
					if (replaced != null && !newValue.equals(replaced)) {
						FTBQuestsLangSplitter.LOGGER.warn("Key {} had it's value replaced at lang file {}!", key, locale);
						final Either<String, List<String>> finalNewValue = newValue;
						replaced
							.ifLeft(l -> {
								FTBQuestsLangSplitter.LOGGER.warn("Old value: {}", l);
								finalNewValue
									.ifLeft(l2 -> FTBQuestsLangSplitter.LOGGER.warn("New value: {}", l2))
									.ifRight(r2 -> FTBQuestsLangSplitter.LOGGER.warn("New value: {}", r2));
							})
							.ifRight(r -> {
								FTBQuestsLangSplitter.LOGGER.warn("Old value: {}", r);
								finalNewValue
									.ifLeft(l2 -> FTBQuestsLangSplitter.LOGGER.warn("New value: {}", l2))
									.ifRight(r2 -> FTBQuestsLangSplitter.LOGGER.warn("New value: {}", r2));
							});

						var fileName = langsplitter$getRelativePath(localePath, filePath);
						langsplitter$addToRecovery(recovery, fileName, key, replaced, newValueTag);
						((TranslationTableAccessor) table).langsplitter$setSaveNeeded(true);
						merged.set(true);
					}
				});
				if (merged.get()) {
					Path newPath = null;
					try {
						newPath = filePath.getParent().resolve(filePath.getFileName().toString() + "_merged");
						Files.move(filePath, newPath, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						FTBQuestsLangSplitter.LOGGER.error("Error while renaming file from {} to {}", filePath, newPath);
					}
				} else {
					FTBQuestsLangSplitter.LOGGER.debug("File {} not merged because there was no change.", filePath);
				}
			} else {
				FTBQuestsLangSplitter.LOGGER.error("can't read lang file {}", filePath);
			}
		}
	}

	@Unique
	private static void langsplitter$addToRecovery(CompoundTag recovery, String fileName, String key, Either<String, List<String>> replaced, Tag newTag) {
		final CompoundTag entries = recovery.getCompound(fileName);
		if (entries.isEmpty()) {
			recovery.put(fileName, entries);
		}

		CompoundTag idEntry = new CompoundTag();
		entries.put(key, idEntry);
		replaced
			.ifLeft(l -> idEntry.put("old", StringTag.valueOf(l)))
			.ifRight(r -> idEntry.put("old", TranslationTableAccessor.langsplitter$listOfStr(r)));
		idEntry.put("new", newTag);
	}

	@Unique
	private static String langsplitter$getRelativePath(Path root, Path sibling){
		return root.relativize(sibling).toString().replace('\\','/');
//		List<String> relative = new ArrayList<>();
//		Path currentPath = sibling;
//		relative.addFirst(currentPath.getFileName().toString());
//		while (!(currentPath = currentPath.getParent()).equals(root)) {
//			relative.addFirst(currentPath.getFileName().toString());
//		}
//		return Strings.join(relative, "/");
	}

	@Unique
	private static boolean langsplitter$createFolder(Path folder){
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
}
