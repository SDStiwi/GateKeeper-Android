package com.sdstiwi.gatekeeper;

import android.content.Context;

public class SyncManager {
    private static SyncManager instance;

    private SyncManager() {}

    public static SyncManager getInstance() {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) instance = new SyncManager();
            }
        }
        return instance;
    }

    public void syncAllData(Context context) {
        // Portfolio demo: keep the app offline-first so a clone/build works
        // without Firebase credentials. The original Firebase sync helpers remain
        // in the project for future cloud integration.
        if (DemoConfig.DEMO_MODE) return;

        // Cloud sync can be enabled here in a real deployment.
    }
}
