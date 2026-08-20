package de.oliver.fancynpcs.skins;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.oliver.fancylib.UUIDFetcher;
import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.skins.SkinData;
import de.oliver.fancynpcs.api.skins.SkinGeneratedEvent;
import de.oliver.fancynpcs.api.skins.SkinLoadException;
import de.oliver.fancynpcs.api.skins.SkinManager;
import de.oliver.fancynpcs.skins.cache.SkinCache;
import de.oliver.fancynpcs.skins.cache.SkinCacheData;
import de.oliver.fancynpcs.skins.uuidcache.UUIDCache;
import de.oliver.fancynpcs.security.SkinSourcePolicy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lushplugins.chatcolorhandler.ChatColorHandler;
import org.mineskin.data.Variant;
import org.mineskin.request.GenerateRequest;

import java.io.File;
import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SkinManagerImpl implements SkinManager, Listener {

    public final static ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(
            5,
            new ThreadFactoryBuilder()
                    .setNameFormat("FancyNpcs-Skins-%d")
                    .build()
    );

    private final String SKINS_DIRECTORY = "plugins/FancyNpcs/skins/";

    private final UUIDCache uuidCache;
    private final SkinCache fileCache;
    private final SkinCache memCache;
    private final SkinGenerationQueue mojangQueue;
    private final SkinGenerationQueue mineSkinQueue;

    public SkinManagerImpl(
            UUIDCache uuidCache,
            SkinCache fileCache,
            SkinCache memCache,
            SkinGenerationQueue mojangQueue,
            SkinGenerationQueue mineSkinQueue
    ) {
        this.uuidCache = uuidCache;
        this.fileCache = fileCache;
        this.memCache = memCache;
        this.mojangQueue = mojangQueue;
        this.mineSkinQueue = mineSkinQueue;

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

    @EventHandler
    public void onSkinGenerated(SkinGeneratedEvent event) {
        if (event.getSkin() == null || !event.getSkin().hasTexture()) {
            FancyNpcs.getInstance().getFancyLogger().error("Generated skin has no texture!");
            return;
        }

        for (Npc npc : FancyNpcs.getInstance().getNpcManager().getAllNpcs()) {
            SkinData skin = npc.getData().getSkinData();
            if (skin == null)
                continue;

            String id = skin.getParsedIdentifier();
            if (SkinUtils.isUsername(id)) {
                UUID uuid = uuidCache.getUUID(id);
                if (uuid == null) {
                    uuid = UUIDFetcher.getUUID(id);
                }

                if (uuid != null) {
                    uuidCache.cacheUUID(id, uuid);
                    id = uuid.toString();
                }
            }
            if (id.equals(event.getId())) {
                final SkinData updatedSkin = new SkinData(
                        skin.getIdentifier(),
                        event.getSkin().getVariant(),
                        event.getSkin().getTextureValue(),
                        event.getSkin().getTextureSignature()
                );
                npc.getData().setSkinData(updatedSkin);
                npc.removeForAll();
                npc.spawnForAll();
                FancyNpcs.getInstance().getFancyLogger().info("Updated skin for NPC: " + npc.getData().getName());
            }
        }

        cacheSkin(event.getSkin());
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
