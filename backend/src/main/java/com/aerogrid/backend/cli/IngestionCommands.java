package com.aerogrid.backend.cli;

import com.aerogrid.backend.ingestion.DataIngestionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent // <--- Això li diu a Spring que aquí hi ha comandes de consola
@RequiredArgsConstructor
public class IngestionCommands {

    private final DataIngestionFacade ingestionFacade;

    /**
     * Command to perform backfill.
     * Usage from console: backfill --days 30
     * Or simply: backfill 30
     */
    @ShellMethod(key = "backfill", value = "Downloads historical data (days back).")
    public String backfill(@ShellOption(defaultValue = "7", help = "Number of days back") int days) {

        // Run logic (in a separate thread to avoid blocking the console)
        new Thread(() -> ingestionFacade.triggerBackfill(days)).start();

        return "Backfill process started for the last " + days + " days";
    }

    /**
     * Command to force ingestion right now.
     * Usage: run-ingestion
     */
    @ShellMethod(key = "run-ingestion", value = "Executes data ingestion immediately.")
    public String runIngestion() {
        new Thread(ingestionFacade::runAllImports).start();
        return "Immediate ingestion started.";
    }
}