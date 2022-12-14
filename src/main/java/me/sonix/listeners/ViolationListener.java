package me.sonix.listeners;

import me.sonix.Main;
import me.sonix.api.events.AnticheatViolationEvent;
import me.sonix.enums.MsgType;
import me.sonix.files.Config;
import me.sonix.managers.logs.PlayerLog;
import me.sonix.managers.profile.Profile;
import me.sonix.tasks.TickTask;
import me.sonix.utils.JsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * A violation listener that we'll use for our alerts by using our custom event.
 */
public class ViolationListener implements Listener {

    private final Main plugin;

    public ViolationListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onViolation(AnticheatViolationEvent e) {

        this.plugin.getAlertManager().getAlertExecutor().execute(() -> {

            final Player p = e.getPlayer();

            if (p == null || !p.isOnline()) return;

            Profile profile = this.plugin.getProfileManager().getProfile(p);

            if (profile == null) return;

            final String tps = String.valueOf(TickTask.getTPS());

            final String checkType = e.getType();

            final String checkName = e.getCheck();

            final String check = (checkType.isEmpty() ? checkName : checkName + " (" + checkType + ")")
                    + (e.isExperimental() ? " (Experimental)" : "");

            final String description = e.getDescription();

            final String information = e.getInformation();

            final String playerName = p.getName();

            final int vl = e.getVl();


            this.plugin.getLogManager().addLogToQueue(new PlayerLog(
                    Config.Setting.SERVER_NAME.getString(),
                    playerName,
                    p.getUniqueId().toString(),
                    check,
                    information
            ));

            //We're sending the alerts by using the server chat packet, Making this much more efficient.
            alerts:
            {

                final String hoverMessage = MsgType.ALERT_HOVER.getMessage()
                        .replace("%description%", description)
                        .replace("%information%", information)
                        .replace("%tps%", tps)
                        .replace("%ping%", String.valueOf(profile.getPing()));

                final String alertMessage = MsgType.ALERT_MESSAGE.getMessage()
                        .replace("%player%", playerName)
                        .replace("%check%", check)
                        .replace("%vl%", String.valueOf(vl))
                        .replace("%tps%", tps)
                        .replace("%ping%", String.valueOf(profile.getPing()));

                JsonBuilder jsonBuilder = new JsonBuilder(alertMessage)
                        .setHoverEvent(JsonBuilder.HoverEventType.SHOW_TEXT, hoverMessage)
                        .setClickEvent(JsonBuilder.ClickEventType.RUN_COMMAND, "/tp " + playerName)
                        .buildText();

                jsonBuilder.sendMessage(this.plugin.getAlertManager().getPlayersWithAlerts());

                if (!Config.Setting.CHECK_SETTINGS_ALERT_CONSOLE.getBoolean()) break alerts;

                Bukkit.getConsoleSender().sendMessage(alertMessage);
            }
        });
    }
}