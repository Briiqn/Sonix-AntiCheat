package me.sonix.files;

import me.sonix.Main;
import me.sonix.files.commentedfiles.CommentedFileConfiguration;
import me.sonix.managers.Initializer;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config implements Initializer {

    private static final String[] HEADER = new String[]{
            "+----------------------------------------------------------------------------------------------+",
            "|                                                                                              |",
            "|                                             Sonix                                            |",
            "|                                                                                              |",
            "|                          Website: https://www.youranticheatwebsite.com                       |",
            "|                                                                                              |",
            "|                                         Author: Stelios                                      |",
            "|                                                                                              |",
            "+----------------------------------------------------------------------------------------------+"
    };

    private final Main plugin;
    private CommentedFileConfiguration configuration;
    private static boolean exists;

    public Config(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * @return the config.yml as a CommentedFileConfiguration
     */
    public CommentedFileConfiguration getConfig() {
        return this.configuration;
    }

    @Override
    public void initialize() {

        File configFile = new File(this.plugin.getDataFolder(), "config.yml");

        exists = configFile.exists();

        boolean setHeaderFooter = !configFile.exists();

        boolean changed = setHeaderFooter;

        this.configuration = CommentedFileConfiguration.loadConfiguration(this.plugin, configFile);

        if (setHeaderFooter) this.configuration.addComments(HEADER);

        for (Setting setting : Setting.values()) {

            setting.reset();

            changed |= setting.setIfNotExists(this.configuration);
        }

        if (changed) this.configuration.save();
    }

    @Override
    public void shutdown() {
        for (Setting setting : Setting.values()) setting.reset();
    }

    public enum Setting {
        SERVER_NAME("server_name", "CraftCore", "The server name that will be shown in Player Logs"),

        THEME("theme", "default", "The theme that the anticheat is going to use"),

        TOGGLE_ALERTS_ON_JOIN("toggle_alerts_on_join", true, "Should we enable alerts for admins when they join?"),

        DISABLE_BYPASS_PERMISSION("disable_bypass_permission", false, "Should we disable the bypass permission?", "Disable this for some perfomance gain"),

        TESTSERVER("test_server", false, "Should we enable the Test Server Mode?", "If enabled the Anticheat will disable damage and increase violations for auto ban and will disable auto ban (replace it with autokick)"),

        CHECK_SETTINGS("check_settings", "", "Check Settings"),
        CHECK_SETTINGS_ALERT_CONSOLE("check_settings.alert_console", false, "Should we also send alerts in console?"),
        CHECK_SETTINGS_VIOLATION_RESET_INTERVAL("check_settings.violation_reset_interval", 5, "How often should we clear the player violations? (In minutes)"),

        LOGS("logs", "", "Log Settings"),
        LOGS_ENABLED("logs.enabled", true, "Should we enable logging?"),
        LOGS_TYPE("logs.type", "YAML", "What type of Database should we use for logging?"),
        LOGS_CLEAR_DAYS("logs.clear_days", 5, "Logs older than this value of Days will be cleared");

        private final String key;
        private final Object defaultValue;
        private boolean excluded;
        private final String[] comments;
        private Object value = null;

        Setting(String key, Object defaultValue, String... comments) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comments = comments != null ? comments : new String[0];
        }

        Setting(String key, Object defaultValue, boolean excluded, String... comments) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comments = comments != null ? comments : new String[0];
            this.excluded = excluded;
        }

        /**
         * Gets the setting as a boolean
         *
         * @return The setting as a boolean
         */
        public boolean getBoolean() {
            this.loadValue();
            return (boolean) this.value;
        }

        public String getKey() {
            return this.key;
        }

        /**
         * @return the setting as an int
         */
        public int getInt() {
            this.loadValue();
            return (int) this.getNumber();
        }

        /**
         * @return the setting as a long
         */
        public long getLong() {
            this.loadValue();
            return (long) this.getNumber();
        }

        /**
         * @return the setting as a double
         */
        public double getDouble() {
            this.loadValue();
            return this.getNumber();
        }

        /**
         * @return the setting as a float
         */
        public float getFloat() {
            this.loadValue();
            return (float) this.getNumber();
        }

        /**
         * @return the setting as a String
         */
        public String getString() {
            this.loadValue();
            return String.valueOf(this.value);
        }

        private double getNumber() {
            if (this.value instanceof Integer) {
                return (int) this.value;
            } else if (this.value instanceof Short) {
                return (short) this.value;
            } else if (this.value instanceof Byte) {
                return (byte) this.value;
            } else if (this.value instanceof Float) {
                return (float) this.value;
            }

            return (double) this.value;
        }

        /**
         * @return the setting as a string list
         */
        @SuppressWarnings("unchecked")
        public List<String> getStringList() {
            this.loadValue();
            return (List<String>) this.value;
        }

        private boolean setIfNotExists(CommentedFileConfiguration fileConfiguration) {
            this.loadValue();

            if (exists && this.excluded) return false;

            if (fileConfiguration.get(this.key) == null) {
                List<String> comments = Stream.of(this.comments).collect(Collectors.toList());
                if (this.defaultValue != null) {
                    fileConfiguration.set(this.key, this.defaultValue, comments.toArray(new String[0]));
                } else {
                    fileConfiguration.addComments(comments.toArray(new String[0]));
                }

                return true;
            }

            return false;
        }

        /**
         * Resets the cached value
         */
        public void reset() {
            this.value = null;
        }

        /**
         * @return true if this setting is only a section and doesn't contain an actual value
         */
        public boolean isSection() {
            return this.defaultValue == null;
        }

        /**
         * Loads the value from the config and caches it if it isn't set yet
         */
        private void loadValue() {
            if (this.value != null) return;
            this.value = Main.getInstance().getConfiguration().get(this.key);
        }
    }
}