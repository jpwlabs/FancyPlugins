package com.fancyinnovations.fancydialogs.storage;

import com.fancyinnovations.fancydialogs.FancyDialogsPlugin;
import com.fancyinnovations.fancydialogs.api.data.DialogData;
import de.oliver.jpw.logging.properties.ThrowableProperty;
import de.oliver.jdb.JDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonDialogStorage implements DialogStorage {

    private final JDB jdb;

    public JsonDialogStorage() {
        this.jdb = new JDB("plugins/FancyDialogs/data/dialogs");
    }

    @Override
    public void save(DialogData dialog) {
        try {
            jdb.set(dialog.id(), dialog);
        } catch (IOException e) {
            FancyDialogsPlugin.get().getFancyLogger().error("Failed to save dialog " + dialog.id(), ThrowableProperty.of(e));
        }
    }

    @Override
    public void saveBatch(Collection<DialogData> dialogs) {
        for (DialogData dialog : dialogs) {
            save(dialog);
        }
    }

    @Override
    public void delete(DialogData dialog) {
        jdb.delete(dialog.id());
    }

    @Override
    public Collection<DialogData> loadAll() {
        List<DialogData> dialogs = new ArrayList<>();

        try {
            dialogs = jdb.getAll("", DialogData.class);
        } catch (IOException e) {
            FancyDialogsPlugin.get().getFancyLogger().error("Failed to load all dialogs", ThrowableProperty.of(e));
        }

        return dialogs;
    }
}
