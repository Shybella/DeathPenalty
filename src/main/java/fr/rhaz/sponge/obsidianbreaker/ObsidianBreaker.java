package fr.rhaz.sponge.obsidianbreaker;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
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
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.TypeTokens;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;

@Plugin(id = "obsidianbreaker", name = "ObsdidianBreaker", version = "1.0")
public class ObsidianBreaker {
	
	private ConfigurationNode config;
	private static ObsidianBreaker i;
	
	private EventContext context;
	
	@Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    @Inject
    @ConfigDir(sharedRoot = true)
    private Path mainDir;
	private HoconConfigurationLoader cloader;
    

	@Listener
    public void onServerStart(GameStartedServerEvent e) {
		i = this;
		
		context = EventContext.builder().add(EventContextKeys.PLUGIN, i.getContainer()).build();
		
        loadConfig();
        
        Builder cmdMain = CommandSpec.builder()
			.description(Text.of("CommandSigns"))
			.permission("csigns.version")
			.executor(new CommandExecutor() {
				public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException{
					src.sendMessage(Text.of(
						TextColors.BLUE, "[CommandSigns]: ",
						TextColors.GRAY, "Version: ",
						TextColors.GOLD, getContainer().getVersion().get()
					));
					return CommandResult.success();
				}
			});
			
		Sponge.getCommandManager().register(this, cmdMain.build(), "cs", "csign", "csigns", "commandsign", "commandsigns");

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
	
	public static ObsidianBreaker get() {
		return i;
	}
	
	public PluginContainer getContainer() {
		return Sponge.getPluginManager().fromInstance(i).get();
	}
	
	public EventContext getContext() {
		return context;
	}
	
	public List<String> getStringList(ConfigurationNode node){
		try {
			return node.getList(TypeTokens.STRING_TOKEN, new ArrayList<>());
		} catch (ObjectMappingException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	public ConfigurationNode node(Location<World> location) {
		return config.getNode(
				"locations", 
				location.getExtent().getName(), 
				String.valueOf(location.getBlockX()), 
				String.valueOf(location.getBlockY()), 
				String.valueOf(location.getBlockZ())
			);
	}
	
	public void clear(ConfigurationNode node) {
		node.setValue(null);
		if(node.getParent().getChildrenMap().isEmpty())
			node.getParent().setValue(null);
		if(node.getParent().getParent().getChildrenMap().isEmpty())
			node.getParent().getParent().setValue(null);
	}
	
	public List<Location<World>> getLocs(Location<World> loc, float f){
		List<Location<World>> locs = new ArrayList<>();
		for (float x = -(f); x <= f; x ++)
			for (float y = -(f); y <= f; y ++)
				for (float z = -(f); z <= f; z ++)
					locs.add(loc.add(x, y, z));
		return locs;
	}
	
	@Listener
	public void onPlayerInteractBlock(ExplosionEvent.Detonate e){
		
		if(!e.getExplosion().shouldBreakBlocks())
			return;
		
		Set<Object> blocks = config.getNode("blocks").getChildrenMap().keySet();
		
		Explosion ex = e.getExplosion();
		
		for(Location<World> loc:getLocs(ex.getLocation(), ex.getRadius())) {
			
			if(e.getAffectedLocations().contains(loc))
				continue;
			
			BlockState b = loc.getBlock();
			
			if(!blocks.contains(b.getType().getName()))
				continue;
			
			int bhealth = config.getNode("blocks", b.getType().getName()).getInt(2);
			
			ConfigurationNode node = node(loc);
			int lhealth = node.getInt(0)+1;
			
			if(lhealth < bhealth) {
				node.setValue(lhealth);
				continue;
			}
			
			clear(node);
			e.getAffectedLocations().add(loc);
			
		}
		
		saveConfig();
	}
	
	@Listener
	public void onPlayerHitBlock(InteractBlockEvent.Primary e, @First Player player){
		
		if (!e.getTargetBlock().getLocation().isPresent())
			return;
		
		Location<World> loc = e.getTargetBlock().getLocation().get();
		
		BlockState b = loc.getBlock();
		
		ConfigurationNode node = node(loc);
		
		Set<Object> blocks = config.getNode("blocks").getChildrenMap().keySet();
		
		if(!blocks.contains(b.getType().getName()))
			return;
		
		clear(node);
		saveConfig();
		
	}
	
}
