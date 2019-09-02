package club.moddedminecraft.polychat.client;

import club.moddedminecraft.polychat.networking.io.CommandOutputMessage;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @author shadowfacts
 */
public class CommandSender implements ICommandSender {

    private final String command;

    public CommandSender(String command) {
        this.command = command;
    }

    @Override
    public String getName() {
        return "PolyChat";
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    public void sendMessage(ITextComponent component) {
        CommandOutputMessage message = new CommandOutputMessage(this.command, component.getUnformattedText());
        ModClass.sendMessage(message);
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public BlockPos getPosition() {
        return BlockPos.ORIGIN;
    }

    @Override
    public Vec3d getPositionVector() {
        return Vec3d.ZERO;
    }

    @Override
    public World getEntityWorld() {
        return ModClass.server.getWorld(0);
    }

    @Nullable
    @Override
    public Entity getCommandSenderEntity() {
        return null;
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return ModClass.server;
    }

}
