package fr.rhaz.sponge.deathpenalty;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;

@Plugin(id = "deathpenalty", name = "DeathPenalty", version = "1.0")
public class DeathPenalty {
	
	private ConfigurationNode config;
	private static DeathPenalty i;
	
	private EventContext context;
	
	@Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    @Inject
    @ConfigDir(sharedRoot = true)
    private Path mainDir;
	private HoconConfigurationLoader cloader;
    
	public EconomyService eco = null;
	
	@Listener
    public void onServerStart(GameStartedServerEvent e) {
		i = this;
		
		context = EventContext.builder().add(EventContextKeys.PLUGIN, i.getContainer()).build();
		
        loadConfig();
        
        Optional<EconomyService> economy = Sponge.getServiceManager().provide(EconomyService.class);
        
        Builder cmdMain = CommandSpec.builder()
			.description(Text.of("Reload DeathPenalty"))
			.permission("deathpenalty.reload")
			.executor(new CommandExecutor() {
				public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException{
					loadConfig();
					src.sendMessage(Text.of(
						TextColors.BLUE, "[DeathPenalty]: ",
						TextColors.GRAY, "Reloaded"
					));
					return CommandResult.success();
				}
			});
        
        Sponge.getCommandManager().register(this, cmdMain.build(), "deathpenalty");
		
		if (economy.isPresent()) 
			eco = economy.get();
		else	
			Sponge.getEventManager().unregisterPluginListeners(this);

	}
	
	public static Logger getLogger() {
	    return i.getContainer().getLogger();
	}
	
	public ConfigurationNode loadConfig() {
		try {
			
			String name = "config.conf";

			URL def = getContainer().getAsset(name).get().getUrl();
			HoconConfigurationLoader defloader = HoconConfigurationLoader.builder().setURL(def).build();
		    ConfigurationNode defaults = defloader.load();
		    
		    File cfile = new File(configDir.toFile(), name);
		    if(!configDir.toFile().exists())
		    	configDir.toFile().mkdirs();
		    if(!cfile.exists())
		    	cfile.createNewFile();
		    
		    cloader = HoconConfigurationLoader.builder().setFile(cfile).build();
			config = cloader.load();
				
			config.mergeValuesFrom(defaults);
			cloader.save(config);
			
			return config;
			
		} catch(IOException e) {
			e.printStackTrace();
		    return null;
		}
	}
	
	public void saveConfig() {
		try {
			cloader.save(config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static DeathPenalty get() {
		return i;
	}
	
	public PluginContainer getContainer() {
		return Sponge.getPluginManager().fromInstance(i).get();
	}
	
	public EventContext getContext() {
		return context;
	}
	
	@Listener
	public void onPlayerInteractBlock(DestructEntityEvent.Death e){
		
		if(!(e.getTargetEntity() instanceof Player))
			return;
		
		Player player = (Player) e.getTargetEntity();
		
		if(player.hasPermission("deathpenalty.bypass"))
			return;
		
		double amount = config.getNode("amount").getDouble(0.0);
		if(amount == 0.0) return;
		
		BigDecimal bamount = BigDecimal.valueOf(amount);
		
		Currency curr = eco.getDefaultCurrency();
		
		Cause cause = e.getCause();
		
		eco.getOrCreateAccount(player.getUniqueId()).get().withdraw(curr, bamount, cause);
	}
	
}
