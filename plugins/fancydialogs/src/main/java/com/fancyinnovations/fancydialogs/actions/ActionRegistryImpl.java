package com.fancyinnovations.fancydialogs.actions;

import com.fancyinnovations.fancydialogs.actions.defaultActions.*;
import com.fancyinnovations.fancydialogs.api.DialogAction;
import com.fancyinnovations.fancydialogs.api.DialogActionRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionRegistryImpl implements DialogActionRegistry {

    private final Map<String, DialogAction> actions;

    public ActionRegistryImpl() {
        this.actions = new ConcurrentHashMap<>();

        registerDefaultActions();
    }

    private void registerDefaultActions() {
        registerAction("open_dialog", OpenDialogDialogAction.INSTANCE);
        registerAction("open_random_dialog", OpenRandomDialogDialogAction.INSTANCE);
        registerAction("message", MessageDialogAction.INSTANCE);
    }

    public void registerAction(String actionId, DialogAction action) {
        if (actions.containsKey(actionId)) {
            throw new IllegalArgumentException("Action with ID " + actionId + " is already registered.");
        }

        actions.put(actionId, action);
    }

    public DialogAction getAction(String actionId) {
        return actions.get(actionId);
    }
}
