package com.win.data;

import java.util.Map;

/**
 *
 * @author ourasov
 */
public class Configuration {
    
    private Map<String, Game> games;

    public Map<String, Game> getGames() {
        return games;
    }

    public void setGames(Map<String, Game> games) {
        this.games = games;
    }
    
    public static class Game{
        private int lines;
        private long bet;
        private boolean skipEngine;

        public boolean isSkipEngine() {
            return skipEngine;
        }

        public void setSkipEngine(boolean skipEngine) {
            this.skipEngine = skipEngine;
        }

        public int getLines() {
            return lines;
        }

        public void setLines(int lines) {
            this.lines = lines;
        }

        public long getBet() {
            return bet;
        }

        public void setBet(long bet) {
            this.bet = bet;
        }
    }
}
