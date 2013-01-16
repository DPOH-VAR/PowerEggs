package me.dpohvar.powereggs;

import me.dpohvar.powernbt.nbt.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

/**
 * 15.01.13 13:55
 * @author DPOH-VAR
 */
public class PowerEggs extends JavaPlugin implements Listener {
    public static PowerEggs plugin; {plugin = this;}

    HashMap<Egg,NBTTagCompound> eggs = new HashMap<Egg, NBTTagCompound>();

    public boolean isDebug() {
        return getConfig().getBoolean("debug");
    }

    public void onEnable(){
        getServer().getPluginManager().registerEvents(this,this);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if (!(event.getAction().equals(Action.RIGHT_CLICK_AIR)||event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) return;
        ItemStack item = event.getPlayer().getItemInHand();
        if(item==null) return;
        // get compound
        NBTContainerItem con = new NBTContainerItem(item);
        NBTTagCompound compound = con.getTag().getCompound("poweregg");
        // check for exists
        if(compound==null) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("powereggs.use")) return;
        // check for permissions
        String permit = compound.getString("permission");
        if(permit!=null){
            if(!player.hasPermission(permit)){
                player.sendMessage(ChatColor.RED+"you have not permissions for this egg");
                return;
            }
        }
        // decreace amount and cancel event
        if(!player.getGameMode().equals(GameMode.CREATIVE)) {
            if (!compound.getBool("infinite")){
                if(item.getAmount()>1) {
                    item.setAmount(item.getAmount()-1);
                } else {
                    player.setItemInHand(null);
                }
            }
        }
        event.setCancelled(true);
        // create new egg
        Egg egg = player.launchProjectile(Egg.class);
        eggs.put(egg,compound);
    }

    @EventHandler
    public void onThrow(PlayerEggThrowEvent event){
        Egg egg = event.getEgg();
        if(!eggs.containsKey(egg)) return;
        LivingEntity player = egg.getShooter();
        Location loc = egg.getLocation();
        event.setHatching(false);
        NBTTagCompound compound = eggs.remove(egg);
        // Explosion
        Float explosion = compound.getFloat("explosion");
        if(explosion!=null) loc.getWorld().createExplosion(loc,explosion);
        // Teleport
        if (compound.getBool("teleport")) {
            Location l = loc.clone();
            l.setPitch(player.getLocation().getPitch());
            l.setYaw(player.getLocation().getYaw());
            player.teleport(l);
        } else {
            int[] array = compound.getIntArray("teleport");
            if(array!=null && array.length>=3){
                Location l = loc.clone();
                l.setPitch(player.getLocation().getPitch());
                l.setYaw(player.getLocation().getYaw());
                l.add(array[0],array[1],array[2]);
                player.teleport(l);
            }
        }
        // Spawn mobs
        NBTTagList spawnList = compound.getList("spawn");
        if (spawnList!=null && spawnList.getSubTypeId()==10){
            NBTTagCompound mobTypeAndData = (NBTTagCompound) spawnList.get((int)(Math.random()*spawnList.size()));
            if(mobTypeAndData!=null){
                String type = mobTypeAndData.getString("Type");
                if(type!=null){
                    EntityType et = EntityType.fromName(type);
                    Entity ent = loc.getWorld().spawnEntity(loc,et);
                    NBTTagCompound mobData = mobTypeAndData.getCompound("Properties");
                    if(mobData!=null){
                        NBTContainerEntity container = new NBTContainerEntity(ent);
                        container.setTag(mobData);
                    }
                }
            }
        }
        // Generate tree
        String tree = compound.getString("tree");
        if(tree!=null) {
            TreeType type = TreeType.valueOf(tree);
            if (type!=null) loc.getWorld().generateTree(loc, type);
        }
        // Grow structures
        NBTTagList structList = compound.getList("structures");
        if (structList!=null && structList.getSubTypeId()==10){
            NBTTagCompound structure = (NBTTagCompound) structList.get((int)(Math.random()*structList.size()));
            boolean phycic = structure.getBool("physic");
            NBTTagList structBlocks = structure.getList("blocks");
            if(structBlocks!=null && structBlocks.getSubTypeId()==10){
                Block block = loc.getBlock();
                for(NBTBase base: structBlocks.asList()){
                    NBTTagCompound c = (NBTTagCompound) base;
                    Integer type = c.getInt("id");
                    if(type==null) continue;
                    byte data = 0;
                    Byte xdata = c.getByte("data");
                    if(xdata!=null) data=xdata;
                    int[] pos = c.getIntArray("pos");
                    if(pos==null||pos.length<3) continue;
                    Block b = block.getRelative(pos[0],pos[1],pos[2]);
                    b.setTypeIdAndData(type,data,phycic);
                    NBTTagCompound mobData = c.getCompound("Properties");
                    if(mobData!=null){
                        NBTContainerBlock container = new NBTContainerBlock(b);
                        container.setTag(mobData);
                    }
                }
            }
        }
        //play sound
        String sound = compound.getString("sound");
        if(sound!=null) {
            Sound type = Sound.valueOf(sound);
            if (type!=null) {
                loc.getWorld().playSound(loc,type,1,1);
            }
        }
    }

}
















