package dev.uncandango.ftbquestslangsplitter.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LocaleArgument implements ArgumentType<String> {
	private static final DynamicCommandExceptionType ERROR_INVALID_LOCALE = new DynamicCommandExceptionType((obj) -> Component.translatableEscape("ftbquestslangsplitter.commands.unsupported_locale", obj));

	public static LocaleArgument locale() {
		return new LocaleArgument();
	}

	public static String getLocale(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		var result = reader.readUnquotedString();
		if (Minecraft.getInstance().getLanguageManager().getLanguages().containsKey(result)) {
			return result;
		} else {
			throw ERROR_INVALID_LOCALE.create(result);
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof SharedSuggestionProvider)){
			return Suggestions.empty();
		} else {
			Set<String> suggestion =  Minecraft.getInstance().getLanguageManager().getLanguages().keySet();
			return SharedSuggestionProvider.suggest(suggestion, builder);
		}
	}

	@Override
	public Collection<String> getExamples() {
		return List.of("en_us", "pt_br", "ja_jp", "ru_ru");
	}
}
