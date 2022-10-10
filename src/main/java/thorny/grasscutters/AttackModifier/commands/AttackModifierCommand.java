package thorny.grasscutters.AttackModifier.commands;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.game.entity.EntityGadget;
import emu.grasscutter.game.entity.EntityItem;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.EntityVehicle;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketSceneEntityDisappearNotify;
import emu.grasscutter.utils.Position;
import emu.grasscutter.command.Command.TargetRequirement;
import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.data.excels.GadgetData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.data.GameData;

import java.util.ArrayList;
import java.util.List;

import javax.swing.UIDefaults.ActiveValue;

import com.mchange.v2.c3p0.management.ActiveManagementCoordinator;

// Command usage
@Command(label = "attack", aliases = {"at","am"}, usage = "[IdOfObject] [radiusSpawnFromSelf]", targetRequirement = TargetRequirement.NONE)
public class AttackModifierCommand implements CommandHandler {

    static int gadgetId = 0;
    static int itemId = 0;
    static int monsterId = 0;
    static float radius = 0;
    static List<EntityGadget> activeGadgets = new ArrayList<>(); // Current gadgets
    static List<EntityGadget> removeGadgets = new ArrayList<>(); // To be removed gadgets
    static List<EntityVehicle> activeVehicles = new ArrayList<>(); // Current gadgets
    static List<EntityVehicle> removeVehicles = new ArrayList<>(); // To be removed gadgets

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {

        /*
         * Command usage available to check the gadgets before adding them
         * Just spawns the gadget where the player is standing, given the id
         */
        float radiusFromArgs = 0;
        int thing = 0;
        switch (args.size()) {
            case 2:
                try {
                    radiusFromArgs = Integer.parseInt(args.get(1));
                } catch (Exception e) {
                    CommandHandler.sendMessage(targetPlayer, "the input for radius is invalid");
                }
            case 1:
                // checkNone
                try {
                    if (args.get(0).toLowerCase().equals("none"))  {
                        gadgetId = 0;
                        itemId = 0;
                        monsterId = 0;
                        CommandHandler.sendMessage(targetPlayer, "infusion has been reset");
                        return;
                    }
                // parsed if not none
                    thing = Integer.parseInt(args.get(0));
                } catch (Exception e) {
                    CommandHandler.sendMessage(targetPlayer, "the input for itemId/gadgetId/monsterId is invalid");
                }
        }

        //checks if is weapon or itemid coz some people may want spawn wishes while atking or smth;
        MonsterData monsterData = GameData.getMonsterDataMap().get(thing);
        GadgetData gadgetData = GameData.getGadgetDataMap().get(thing);
        ItemData itemData = GameData.getItemDataMap().get(thing);
        if (monsterData == null && gadgetData == null && itemData == null) {
            CommandHandler.sendMessage(sender, "the id of " + thing + "does not exist");
            return;
        }
        if (gadgetData != null) {
            gadgetId = gadgetData.getId();
            monsterId = 0;
            itemId = 0;
        } else if (itemData != null) {
            itemId = itemData.getId();
            gadgetId = 0;
            monsterId = 0;
        } else if (monsterData != null) {
            monsterId = monsterData.getId();
            gadgetId = 0;
            monsterId = 0;
        }

        radius = radiusFromArgs;

        //modifies last used gadget for future attacks
        if (gadgetId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nGadgetID : " + gadgetId);
        } else if (itemId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nItemID : " + itemId);
        } else if (monsterId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nMonsterID : " + monsterId);
        }
    }

    public static void addAttack(GameSession session, int skillId){
        // Get position
        Scene scene = session.getPlayer().getScene();
        Position pos = new Position(session.getPlayer().getPosition());
        Position rot = new Position(session.getPlayer().getRotation());

        int addedAttackGadgetId = 0; // Default of no gadget
        int addedAttackItemId = 0; // Default of no gadget
        int addedAttackMonsterId = 0; // Default of no gadget
        
        // Currently will only damage the player
        switch (skillId) { // For Raiden
            case 10521: // Basic attack
                addedAttackGadgetId = 42906105;
                addedAttackItemId = 0;
                addedAttackMonsterId = 0;
                break;
            case 10522: // Elemental skill
                addedAttackGadgetId = 42906108;
                addedAttackItemId = 0;
                addedAttackMonsterId = 0;
                break;
            case 10525: // Burst
                addedAttackGadgetId = 42906119;
                addedAttackItemId = 0;
                addedAttackMonsterId = 0;
                break;
            default:
                addedAttackGadgetId = gadgetId;  //omg tracks last inputted gadget
                addedAttackItemId = itemId;
                addedAttackMonsterId = monsterId;
                break;
        }

        // Try to set position in front of player to not get hit
        // Rotation only cares about y coord, and west is 0, going clockise to 360.
        //var radius = Math.sqrt(1 * 0.2 / Math.PI); //distance from self
        Position target = new Position(pos);                     //create temporary position from player position
        double angle = rot.getY();                 //attempts to get y coord to add a few units of radius to pos in that rot direction
        /*double basicAngle = 0.0;
        //String ASTC = "";
        int multSin = 0;
        int multCos = 0;

        if (angle <= 90.0) {
            basicAngle = angle;
            //ASTC = "A";
            multSin = 1;
            multCos = 1;
        } else if (angle > 90.0 && angle <= 180.0) {
            basicAngle = angle - 90.0;
            //ASTC = "S";
            multSin = 1;
            multCos = -1;
        } else if (angle > 180.0 && angle <= 270.0) {
            basicAngle = angle - 180.0;
            //ASTC = "T";
            multSin = -1;
            multCos = -1;
        } else if (angle > 270.0 && angle <= 360.0) {
            basicAngle = angle - 270.0;
            //ASTC = "C";
            multSin = -1;
            multCos = 1;
        }*/

        double r = radius;                         //removed idk sqrt, set to radius from self.
        target.addX((float) (r * Math.sin(Math.PI/180 * angle)));
        target.addZ((float) (r * Math.cos(Math.PI/180 * angle)));
        //target.addX((float) (r * Math.sin(basicAngle) * (multSin)));
        //target.addZ((float) (r * Math.cos(basicAngle) * (multCos)));
        
        // Only spawn on match
        if (addedAttackGadgetId != 0) {
            //EntityGadget attGadget = new EntityGadget(scene, addedAttackGadgetId, target, rot);
            EntityVehicle attGadget = new EntityVehicle(scene, session.getPlayer(), addedAttackGadgetId, 1, target, rot);
            // Silly way to track gadget alive time -_-
            int currTime = (int)(System.currentTimeMillis() - 1665393100);
            attGadget.setGroupId(currTime);
            //activeGadgets.add(attGadget);
            activeVehicles.add(attGadget);
            // Try to make it not hurt self
            scene.addEntity(attGadget);
            attGadget.setFightProperty(2001, 0);
            attGadget.setFightProperty(1, 0);
            scene.addEntity(attGadget);
            //CommandHandler.sendMessage(session.getPlayer(), "DEBUG:\n\nAVATAR POS: " + pos.getX() + " , " + pos.getY() + " , " + pos.getZ() + "\nAVATAR ROT: " + rot.getX() + " , " + rot.getY() + " , " + rot.getZ() + "\nGADGET POS: " + target.getX() + " , " + target.getY() + " , " + target.getZ() + "\nGADGET ROT: " + rot.toString());
        }
        if (addedAttackItemId != 0) {
            target.addY(3f);
            EntityItem attItem = new EntityItem(scene, session.getPlayer(), GameData.getItemDataMap().get(addedAttackItemId), target, 1);
            scene.addEntity(attItem);
        }
        if (addedAttackMonsterId != 0) {
            EntityMonster attMonster = new EntityMonster(scene,GameData.getMonsterDataMap().get(addedAttackMonsterId), target, 90);
            scene.addEntity(attMonster);
        }

        // Remove all gadgets when list not empty
        if(!activeVehicles.isEmpty()){
            for (EntityVehicle gadget : activeVehicles) {
                // When gadgets have lived for 10 sec
                if((int)(System.currentTimeMillis() - 1665393100) > (gadget.getGroupId()+10000)){
                    // Add to removal list
                    removeVehicles.add(gadget);
                    // Remove entity
                    scene.removeEntity(gadget, VisionType.VISION_TYPE_REMOVE);
                    scene.broadcastPacket(new PacketSceneEntityDisappearNotify(gadget, VisionType.VISION_TYPE_REMOVE));
                }
            }
            // Remove gadgets and clean list
            activeGadgets.removeAll(removeGadgets);
            removeGadgets.clear();
        }
    }
}
