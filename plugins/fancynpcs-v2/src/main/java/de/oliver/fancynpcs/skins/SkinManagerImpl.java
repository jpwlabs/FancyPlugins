package de.oliver.fancynpcs.skins;

import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.skins.SkinData;
import de.oliver.fancynpcs.api.skins.SkinLoadException;
import de.oliver.fancynpcs.api.skins.SkinManager;
import de.oliver.fancynpcs.skins.cache.SkinCache;
import de.oliver.fancynpcs.skins.cache.SkinCacheData;
import de.oliver.fancynpcs.skins.uuidcache.UUIDCache;
import de.oliver.fancynpcs.security.SkinSourcePolicy;

import java.io.File;
import java.util.UUID;

public class SkinManagerImpl implements SkinManager {

    private final String SKINS_DIRECTORY = "plugins/FancyNpcs/skins/";

    private final UUIDCache uuidCache;
    private final SkinCache fileCache;
    private final SkinCache memCache;
    public SkinManagerImpl(
            UUIDCache uuidCache,
            SkinCache fileCache,
            SkinCache memCache
    ) {
        this.uuidCache = uuidCache;
        this.fileCache = fileCache;
        this.memCache = memCache;

        File skinsDir = new File(SKINS_DIRECTORY);
        if (!skinsDir.exists()) {
            skinsDir.mkdirs();
        }
    }

    @Override
    public SkinData getByIdentifier(String identifier, SkinData.SkinVariant variant) throws SkinLoadException {
        if (!SkinSourcePolicy.isApproved(identifier)) {
            throw new SkinLoadException(SkinLoadException.Reason.INVALID_FILE,
                    "(JPW POLICY REJECTED IDENTIFIER = '" + identifier + "')");
        }

        SkinData cached = tryToGetFromCache(identifier, variant);
        if (cached != null) {
            return cached;
        }

        FancyNpcs.getInstance().getFancyLogger().warn(
                "Approved skin '" + identifier + "' is not cached; retaining the safe default skin");
        return new SkinData(identifier, variant);
    }

    @Override
    public SkinData getByUUID(UUID uuid, SkinData.SkinVariant variant) {
        return approvedDefault(variant, "UUID skin lookup");
    }

    @Override
    public SkinData getByUsername(String username, SkinData.SkinVariant variant) throws SkinLoadException {
        return approvedDefault(variant, "username skin lookup");
    }

    @Override
    public SkinData getByURL(String url, SkinData.SkinVariant variant) throws SkinLoadException {
        throw new SkinLoadException(SkinLoadException.Reason.INVALID_URL, "(JPW POLICY REJECTED URL)");
    }

    @Override
    public SkinData getByFile(String filePath, SkinData.SkinVariant variant) throws SkinLoadException {
        return getByIdentifier(filePath, variant);
    }

    private SkinData approvedDefault(SkinData.SkinVariant variant, String rejectedSource) {
        FancyNpcs.getInstance().getFancyLogger().warn(rejectedSource + " disabled by JPW static-skin policy");
        SkinData cached = tryToGetFromCache("default.png", variant);
        return cached != null ? cached : new SkinData("default.png", variant);
    }

    private SkinData tryToGetFromCache(String identifier, SkinData.SkinVariant variant) {
        FancyNpcs.getInstance().getFancyLogger().debug("Trying to get skin from mem cache: " + identifier);

        SkinCacheData data = memCache.getSkin(identifier);
        if (data != null) {
            if (data.skinData().getVariant() != variant) {
                FancyNpcs.getInstance().getFancyLogger().debug("Skin variant does not match: " + identifier);
                return null;
            }

            FancyNpcs.getInstance().getFancyLogger().debug("Found skin from mem cache: " + identifier);
            return data.skinData();
        }

        FancyNpcs.getInstance().getFancyLogger().debug("Trying to get skin from file cache: " + identifier);

        data = fileCache.getSkin(identifier);
        if (data != null) {
            if (data.skinData().getVariant() != variant) {
                FancyNpcs.getInstance().getFancyLogger().debug("Skin variant does not match: " + identifier);
                return null;
            }

            FancyNpcs.getInstance().getFancyLogger().debug("Found skin from file cache: " + identifier);
            memCache.addSkin(data.skinData());
            return data.skinData();
        }

        FancyNpcs.getInstance().getFancyLogger().debug("Skin not found in cache: " + identifier);
        return null;
    }

    public void cacheSkin(SkinData skinData) {
        memCache.addSkin(skinData);
        fileCache.addSkin(skinData);
    }

    public UUIDCache getUuidCache() {
        return uuidCache;
    }

    public SkinCache getFileCache() {
        return fileCache;
    }

    public SkinCache getMemCache() {
        return memCache;
    }
}
