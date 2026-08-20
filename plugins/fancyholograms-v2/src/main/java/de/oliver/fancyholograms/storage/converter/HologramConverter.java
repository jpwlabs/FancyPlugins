package de.oliver.fancyholograms.storage.converter;

import de.oliver.fancyholograms.api.data.HologramData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class HologramConverter {

    public abstract @NotNull String getId();

    public abstract boolean canRunConverter();

    /**
     * Returns a list of converted holograms
     *
     * @param spec Configuration of the hologram conversion
     * @return A list of converted holograms.
     */
    protected abstract @NotNull List<HologramData> convertHolograms(@NotNull HologramConversionSession spec);


    /**
     * Returns a list of converted holograms
     *
     * @param spec Configuration of the hologram conversion
     * @return A list of converted holograms.
     */
    public final @NotNull List<HologramData> convert(@NotNull HologramConversionSession spec) {
        List<HologramData> converted = convertHolograms(spec);

        return converted;
    }

    public @NotNull List<String> getConvertableHolograms() {
        return List.of();
    }
}
