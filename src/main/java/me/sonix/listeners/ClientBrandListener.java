package me.sonix.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.sonix.Main;
import me.sonix.utils.ChatUtils;
import me.sonix.utils.TaskUtils;
import me.sonix.managers.profile.Profile;
import me.sonix.utils.custom.ExpiringSet;
import me.sonix.wrappers.WrapperPlayClientCustomPayload;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A client listener that we'll use in order to get the profile's client brand.
 */
public class ClientBrandListener extends PacketAdapter {

    private final Main plugin;

    /*
    We need to do this in order to fix edge cases that mostly occur in bungeecord servers
    Where the client brand payload would get sent more than once.
     */
    private final ExpiringSet<UUID> cache = new ExpiringSet<>(5000L);

    public ClientBrandListener(Main plugin) {
        super(plugin, ListenerPriority.MONITOR, PacketType.Play.Client.CUSTOM_PAYLOAD);

        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent e) {
        if (e.isPlayerTemporary() || e.getPlayer() == null) return;

        Player player = e.getPlayer();

        UUID uuid = player.getUniqueId();

        WrapperPlayClientCustomPayload payload = new WrapperPlayClientCustomPayload(e.getPacket());

        /*
        Check if we received a payload from the brand channel
        Or if the player has set his brand recently.
         */
        if (!payload.getChannel().toLowerCase().endsWith("brand") || this.cache.contains(uuid)) return;

        String brand;

        try {

            /*
            Clear any color codes to make sure they're not exploiting this
            And translate the bytes.
             */
            brand = ChatUtils.stripColorCodes(new String(payload.getContents(), StandardCharsets.UTF_8).substring(1));

        } catch (Exception ex) {

            /*
            Cant parse, should never happen unless a client is doing it intentionally.
             */
            return;
        }

        /*
        Add the player's uuid to the cache
         */
        this.cache.add(uuid);

        /*
        Schedule it to run two seconds later to make sure the player profile has been initialized
         */
        TaskUtils.taskLaterAsync(() -> {

            Profile profile = this.plugin.getProfileManager().getProfile(player);

            /*
            Just to make sure.
             */
            if (profile == null || !profile.getClient().equals("Unknown")) return;

            profile.setClient(brand);

        }, 2000L);
    }
}