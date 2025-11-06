package net.nextfur.fws.bukkit.common.commands;

import net.nextfur.fws.bukkit.FurWatchBukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BanItemCommand {
    public static void execute(CommonCommands data, Player player, String command, String[] args) {
        FurWatchBukkit plugin = data.getPlugin();

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso: /banitem <add|remove> [motivo]");
            return;
        }

        String subCommand = args[0].toLowerCase();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Você precisa estar segurando um item para banir ou desbanir.");
            return;
        }

        String itemKey = itemInHand.getType().getKey().toString();

        List<String> bannedItems = plugin.getBannedItems();

        HashMap<String, Object> apiData = new HashMap<>();
        apiData.put("item", itemKey);
        apiData.put("staff", player.getName());

        switch (subCommand) {
            case "add": {
                if (bannedItems.contains(itemKey)) {
                    player.sendMessage(ChatColor.YELLOW + "Este item já está banido.");
                    return;
                }

                bannedItems.add(itemKey);

                String reason = "Não especificado";
                if (args.length > 1) {
                    reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                }

                apiData.put("action", "add");
                apiData.put("reason", reason);
                plugin.getApi().postAsync("banitem", apiData);

                player.sendMessage(ChatColor.GREEN + "Item " + ChatColor.GOLD + itemKey + ChatColor.GREEN + " foi banido. Motivo: " + reason);
                plugin._getLogger().info("Item " + itemKey + " banido por " + player.getName() + ". Motivo: " + reason);
                break;
            }
            case "remove": {
                if (!bannedItems.contains(itemKey)) {
                    player.sendMessage(ChatColor.YELLOW + "Este item não está na lista de banidos.");
                    return;
                }

                bannedItems.remove(itemKey);

                apiData.put("action", "remove");
                plugin.getApi().postAsync("banitem", apiData);

                player.sendMessage(ChatColor.GREEN + "Item " + ChatColor.GOLD + itemKey + ChatColor.GREEN + " foi desbanido.");
                plugin._getLogger().info("Item " + itemKey + " desbanido por " + player.getName() + ".");
                break;
            }
            default:
                player.sendMessage(ChatColor.RED + "Uso: /banitem <add|remove> [motivo]");
                break;
        }
    }

}
