package com.airavat.astralab.ui;

import javafx.application.Application;

public final class AstraLabLauncher {
    private AstraLabLauncher() {
    }

    public static void main(String[] args) {
        AstraLabApp.registerOpenFileHandlers();
        Application.launch(AstraLabApp.class, args);
    }
}
