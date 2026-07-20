package com.onyxclient.commands;

import com.onyxclient.OnyxClient;
import com.onyxclient.gui.HudEditor;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;

public class OnyxCommand extends CommandBase {

    public static void register() {
        ClientCommandHandler.instance.registerCommand(new OnyxCommand());
    }

    @Override
    public String getCommandName() {
        return "hc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hc <reload|apikey|reset|hud>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("Onyx Client v1.0 — /hc reload | apikey | reset | hud"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                OnyxClient.getConfigManager().load();
                OnyxClient.getModuleManager().applyConfig(OnyxClient.getConfigManager().getConfig());
                sender.addChatMessage(new ChatComponentText("§7[Onyx] Config reloaded."));
                break;
            case "apikey":
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§7[Onyx] Usage: /hc apikey <key>"));
                    return;
                }
                OnyxClient.getConfigManager().getConfig().hypixelApiKey = args[1];
                OnyxClient.getConfigManager().save();
                sender.addChatMessage(new ChatComponentText("§7[Onyx] Hypixel API key saved."));
                break;
            case "reset":
                OnyxClient.getConfigManager().getConfig().modules.clear();
                OnyxClient.getConfigManager().getConfig().hud.clear();
                OnyxClient.getConfigManager().save();
                sender.addChatMessage(new ChatComponentText("§7[Onyx] Config reset. Rejoin to apply defaults."));
                break;
            case "hud":
                net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new HudEditor());
                break;
            default:
                sender.addChatMessage(new ChatComponentText("§7[Onyx] Unknown subcommand."));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
