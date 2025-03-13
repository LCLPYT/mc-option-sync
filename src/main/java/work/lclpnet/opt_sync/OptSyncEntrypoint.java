package work.lclpnet.opt_sync;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.util.ModTranslations;
import work.lclpnet.opt_sync.gui.ConflictsGui;
import work.lclpnet.opt_sync.lib.Constants;
import work.lclpnet.opt_sync.lib.OptionStorage;
import work.lclpnet.opt_sync.lib.OptionSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class OptSyncEntrypoint implements PreLaunchEntrypoint {

	public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);
	private static @Nullable OptionSync optionSync = null;

	@Override
	public void onPreLaunch() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;

		Translations translations = loadTranslations();
		ConflictsGui.Translator translator = createTranslator(translations);

		var ctx = new FabricSyncContext();
		var storage = new OptionStorage(OptionStorage.getDataDir(), ctx, LOGGER);
		var conflictHandler = new ConflictsGui(translator, LOGGER);
		var sync = new OptionSync(storage, ctx, conflictHandler, LOGGER);

		sync.init();
		sync.pullOptions();

		optionSync = sync;
    }

	public static Optional<OptionSync> getOptionSync() {
		return Optional.ofNullable(optionSync);
	}

	private static Translations loadTranslations() {
		var result = ModTranslations.fromAssets(Constants.MOD_ID, LOGGER);
		Translations translations = result.translations();

		result.whenLoaded().thenRun(() -> LOGGER.info("{} translations loaded.", Constants.MOD_ID));

		return translations;
	}

	private static ConflictsGui.Translator createTranslator(Translations translations) {
		String lang = getGameLanguage();

        return (key, args) -> translations.translateText(lang, key, args).getString();
	}

	private static @NotNull String getGameLanguage() {
		try (var reader = Files.newBufferedReader(Path.of("options.txt"))) {
			return reader.lines()
					.map(line -> line.split(":"))
					.filter(split -> split.length == 2)
					.filter(pair -> "lang".equals(pair[0]))
					.map(pair -> pair[1])
					.findAny()
					.orElse("en_us");
		} catch (IOException e) {
            LOGGER.error("Failed to read language from game options", e);
			return "en_us";
        }
	}
}