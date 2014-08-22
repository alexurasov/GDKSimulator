package com.win.rtpmonitor;

import com.google.gson.Gson;
import com.win.data.Configuration;
import com.win.slots.api.SpinData;
import com.win.slots.api.machine.EngineAPI;
import com.win.slots.api.machine.SpinResult;
import com.win.slots.gdk.GDK;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ourasov
 */
public class Monitor {

    private static final Logger log = LoggerFactory.getLogger(Monitor.class);
    private GDK gdk;
    private Configuration spinParams;
    private EngineAPI engine;
    private int linesPlayed = 1;
    private long betPerLine = 1;
    private int spinCount = 100000;
    private boolean skipEngine = false; //skip current engine from simulation

    public void setSpinCount(int spinCount) {
        this.spinCount = spinCount;
    }

    public Monitor() {
        gdk = GDK.getInstance();
        this.gdkSanityCheck();
        initializeSpinParamsConfiguration();
    }

    public void startEngineVerification() {
        Set<String> supportedEngines = gdk.getSupportedEngines();
        for (String engineName : supportedEngines) {
            Configuration.Game params = spinParams.getGames().get(engineName);
            if (params != null) {
                this.betPerLine = params.getBet();
                this.linesPlayed = params.getLines();
                this.skipEngine = params.isSkipEngine();
            } else {
                this.betPerLine = 1;
                this.linesPlayed = 1;
                this.skipEngine = false;
            }
            if (!this.skipEngine) {
                verifyEngine(engineName);
            }
        }
        System.exit(0);
    }

    public void startEngineVerification(String engineName, int times) {
        Configuration.Game params = spinParams.getGames().get(engineName);
        if (params != null) {
            this.betPerLine = params.getBet();
            this.linesPlayed = params.getLines();
        } else {
            this.betPerLine = 1;
            this.linesPlayed = 1;
        }
        for (int i = 0; i < times; i++) {
            verifyEngine(engineName);
        }
        System.exit(0);
    }

    public void startEngineVerification(String engineName, int lines, long bet) {
        this.betPerLine = bet;
        this.linesPlayed = lines;
        verifyEngine(engineName);
        System.exit(0);
    }

    private void verifyEngine(String engineName) {

        Date startDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        System.out.println(String.format("%s Start simulation: %s [lines = %d, bet = %d, %d spins]", dateFormat.format(startDate), engineName, this.linesPlayed, this.betPerLine, this.spinCount));
        engine = gdk.getEngine(engineName);
        makeCurrentEngineSnapshot();
        long winAmount = 0;
        long betAmount = 0;
        for (int i = 0; i < spinCount; i++) {
            SpinData spinData = new SpinData(linesPlayed, betPerLine, false, "testUser", 1, 1, 1, 1);
            try {
                SpinResult spinResult = engine.spin(spinData);
                winAmount += spinResult.getBalance();
                betAmount += spinResult.getSpinData().getTotalBetAmount();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Trying to continue simulation...");
            }
        }
        float rtp = (float) winAmount / betAmount;
        logResult(rtp);
        Date endDate = new Date();
        System.out.println(String.format("%s Simulation finished: %s. RTP = %f", dateFormat.format(endDate), engineName, rtp));
    }

    private void initializeSpinParamsConfiguration() {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        try {
            URI gdkConfigPath = this.getClass().getResource("/configuration.json").toURI();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(gdkConfigPath.getPath()));

            while (bufferedReader.ready()) {
                String readLine = bufferedReader.readLine();
                sb.append(readLine);
            }
            bufferedReader.close();
        } catch (Exception e) {
        }
        spinParams = gson.fromJson(sb.toString(), Configuration.class);
    }

    private void makeCurrentEngineSnapshot() {
        Gson gson = new Gson();
        String toJson = gson.toJson(this.engine);
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        //get current date time with Date()
        Date date = new Date();
        boolean engineChanged = isEngineChanged(toJson);

        if (engineChanged) {
            System.err.println("Engine is changed!");
            String filename = "gdkSnapshots/" + this.engine.getID() + dateFormat.format(date) + ".json";
            try {
                FileWriter writer = new FileWriter(filename);
                writer.write(toJson);
                writer.close();
            } catch (IOException e) {
                log.error(String.format("Error during engine snapshot creation! %s", e.getMessage()));
            }
        }
    }

    private boolean isEngineChanged(String currentSnapshot) {
        File myDir = new File("gdkSnapshots/");

        FilenameFilter engineFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(engine.getID()) && name.endsWith(".json");
            }
        };

        File[] listFiles = myDir.listFiles(engineFilter);

        try {
            if (listFiles.length > 0) {
                File previous = listFiles[listFiles.length - 1];
                BufferedReader bufferedReader = new BufferedReader(new FileReader(previous));
                StringBuilder sb = new StringBuilder();
                while (bufferedReader.ready()) {
                    String readLine = bufferedReader.readLine();
                    sb.append(readLine);
                }
                bufferedReader.close();
                if (sb.toString().equals(currentSnapshot)) {
                    return false;
                }
            }
        } catch (IOException e) {
            log.error(String.format("Error during engine snapshot reading! %s", e.getMessage()));
        }
        return true;
    }

    private void logResult(float rtp) {
        try {
            FileWriter writer = new FileWriter("rtp.out", true);
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            String jarVersion = this.gdk.getJARVersion();
            String lastChecksum = this.gdk.getLastChecksum();
            writer.write(String.format("%s %s \t(lines=%d, bet=%d) \tRTP: %f. %d spins\n",
                    dateFormat.format(date), engine.getID(), this.linesPlayed, this.betPerLine, rtp, this.spinCount));
            writer.write(String.format("%s %s [jarVersion:%s, checksum:%s]\n", dateFormat.format(date), engine.getID(), jarVersion, lastChecksum));
            writer.close();
        } catch (IOException e) {
            log.error(String.format("Error during saving RTP: %s", e.getMessage()));
        }
    }

    private void gdkSanityCheck() {
        if (gdk == null) {
            System.err.println("GDK was not initialized!");
            log.error("GDK was not initialized!");
            System.exit(1);
        }
        if (gdk.getSupportedEngines().isEmpty()) {
            System.err.println("Engines was not initialized! Please check GDK admin page!");
            log.error("Engines was not initialized Please check GDK admin page!");
            System.exit(1);
        }
    }
}
