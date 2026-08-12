package edu.ug.nexusb.app;

import edu.ug.nexusb.data.DBLoader;

public final class App {

    public static void main(String[] args) {
        boolean initDb = false;
        for (String arg : args) {
            if (arg.equals("--init-db")) {
                initDb = true;
            }
        }

        if (initDb) {
            DBLoader.run();
            return;
        }

        System.out.println("NexusB Hospital Ops — triage/dispatch engine");
    }

    private App() {
    }
}