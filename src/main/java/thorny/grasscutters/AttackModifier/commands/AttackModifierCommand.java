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


// Command usage
@Command(label = "attack", aliases = {"at","am","snoospawn"}, usage = "[IdOfObject] [radiusSpawnFromSelf] [height] [count] [spreadRationToRadius] [rotXupwards] [rotYHorizontalFromSELF] [rotZlastrot]\n\nID of object is the id of the object in the handbook\nradiusSpawnFromSelf is the distance between u and the object spawned when atking\nheight is the distance above u the gadget is spawned\nrotX change the vertical rotation of object\nrotY changed the horizontal rotation from where u are facing [180 will shoot behind u always]\nrotZ is the 3rd cross vector rotation to rot y and x", targetRequirement = TargetRequirement.NONE)
public class AttackModifierCommand implements CommandHandler {

    static int gadgetId = 0;
    static int itemId = 0;
    static int monsterId = 0;
    static float radius = 0;
    static float rotX = 0;
    static float rotY = 0;
    static float rotZ = 0;
    static float height = 0;
    static int count = 1;
    static float spread = 0;
    static List<EntityGadget> activeGadgets = new ArrayList<>(); // Current gadgets
    static List<EntityGadget> removeGadgets = new ArrayList<>(); // To be removed gadgets
    static List<EntityVehicle> activeVehicles = new ArrayList<>(); // Current gadgets
    static List<EntityVehicle> removeVehicles = new ArrayList<>(); // To be removed gadgets

    private Position GetRandomPositionInCircle(Position origin, double radius) {
        Position target = origin.clone();
        double angle = Math.random() * 360;
        double r = Math.sqrt(Math.random() * radius * radius);
        target.addX((float) (r * Math.cos(angle))).addZ((float) (r * Math.sin(angle)));
        return target;
    }

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {

        /*
         * Command usage available to check the gadgets before adding them
         * Just spawns the gadget where the player is standing, given the id
         */
        float radiusFromArgs = 0f;
        int thing = 0;
        float heightFromArgs = 0f;
        float rotXFromArgs = 0f;
        float rotYFromArgs = 0f;
        float rotZFromArgs = 0f;
        int countFromArgs = 1;
        float spreadFromArgs = 0f;
        switch (args.size()) {
            case 8:
                try {
                    rotZFromArgs = Float.parseFloat(args.get(7));
                } catch (NumberFormatException e) {
                    CommandHandler.sendMessage(targetPlayer, "the rotation coords for Z is invalid, default to 0");
                }
            case 7:
                try {
                    rotYFromArgs = Float.parseFloat(args.get(6));
                } catch (NumberFormatException e) {
                    CommandHandler.sendMessage(targetPlayer, "the rotation coords for Y is invalid, default to 0");
                }
            case 6:
                try {
                    rotZFromArgs = Float.parseFloat(args.get(5));
                } catch (NumberFormatException e) {
                    CommandHandler.sendMessage(targetPlayer, "the rotation coords for X is invalid, default to 0");
                }
            case 5:
                try {
                    spreadFromArgs = Float.parseFloat(args.get(4));
                } catch (NumberFormatException e) {
                    CommandHandler.sendMessage(targetPlayer, "the spread for gadget spawn is invalid, default to 0");
                }
            case 4:
                try {
                    countFromArgs = Integer.parseInt(args.get(3));
                } catch (NumberFormatException e) {
                    CommandHandler.sendMessage(targetPlayer, "this value of count is invalid, default to 1");
                }
            case 3:
                try {
                    heightFromArgs = Float.parseFloat(args.get(2));
                } catch (NumberFormatException e) {
                    CommandHandler.sendMessage(targetPlayer, "this height from self is an invalid number,default to 0");
                }
            case 2:
                try {
                    radiusFromArgs = Float.parseFloat(args.get(1));
                } catch (Exception e) {
                    CommandHandler.sendMessage(targetPlayer, "the input for radius is invalid, default to 0");
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
                break;
            default:
                CommandHandler.sendMessage(targetPlayer, "[IdOfObject] [radiusSpawnFromSelf] [height] [count] [spreadRatioToRadius] [rotXupwards] [rotYHorizontalFromSELF] [rotZlastrot]\n\nID of object is the id of the object in the handbook\nradiusSpawnFromSelf is the distance between u and the object spawned when atking\nheight is the distance above u the gadget is spawned\nrotX change the vertical rotation of object\nrotY changed the horizontal rotation from where u are facing [180 will shoot behind u always]\nrotZ is the 3rd cross vector rotation to rot y and x");
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
            itemId = 0;
        }

        radius = radiusFromArgs;
        rotX = rotXFromArgs;
        rotY = rotYFromArgs;
        rotZ = rotZFromArgs;
        height = heightFromArgs;
        count = countFromArgs;
        spread = spreadFromArgs;

        //modifies last used gadget for future attacks
        if (gadgetId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nGadgetID : " + gadgetId + "\n\nWITH SETTINGS:\nRadius from self : " + radius + "\nHeight : " + height + "\nCount : " + count + "\nRotX,RotY,RotZ = " + List.of(rotX,rotY,rotZ).toString());
        } else if (itemId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nItemID : " + itemId + "\n\nWITH SETTINGS:\nRadius from self : " + radius + "\nHeight : " + height + "\nCount : " + count);
        } else if (monsterId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nMonsterID : " + monsterId  + "\n\nWITH SETTINGS:\nRadius from self : " + radius + "\nHeight : " + height + "\nCount : " + count);
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
        float addedAttackRotX = 0;
        float addedAttackRotY = 0;
        float addedAttackRotZ = 0;
        float addedAttackHeight = 0;
        float addedAttackRadius = 0;
        int addedAttackCount = 1;
        float addedAttackSpread = 0;
        
        // Currently will only damage the player
        switch (skillId) { // For Raiden
            case 10521: // Basic attack
                addedAttackGadgetId = 42906105;
                addedAttackRadius = 0.3f;
                break;
            case 10522: // Elemental skill
                addedAttackGadgetId = 42906108;
                break;
            case 10525: // Burst
                addedAttackGadgetId = 42906119;
                break;
            default:
                addedAttackGadgetId = gadgetId;  //omg tracks last inputted gadget
                addedAttackItemId = itemId;
                addedAttackMonsterId = monsterId;
                addedAttackRotX = rotX;
                addedAttackRotY = rotY;
                addedAttackRotZ = rotZ;
                addedAttackHeight = height;
                addedAttackRadius = radius;
                addedAttackCount = count;
                addedAttackSpread = spread;
                break;
        }

        // Try to set position in front of player to not get hit
        Position target = new Position(pos);                     //create temporary position from player position
        double angle = rot.getY() + addedAttackRotY;                 //attempts to get y coord to add a few units of radius to pos in that rot direction

        Position addedAttackRotation = new Position(addedAttackRotX,rot.getY(),addedAttackRotZ);
        double r = addedAttackRadius;                         //removed idk sqrt, set to radius from self.
        target.addX((float) (r * Math.sin(Math.PI/180 * angle)));
        target.addY(addedAttackHeight);
        target.addZ((float) (r * Math.cos(Math.PI/180 * angle)));



        // Only spawn on match
        if (addedAttackGadgetId != 0) {
            Position originalTargetRefererence = new Position(target);
            for (int i = 1 ; i <= addedAttackCount  ; i++) {
                Position staggeredPosition = new AttackModifierCommand().GetRandomPositionInCircle(originalTargetRefererence,count * addedAttackSpread);
                EntityVehicle attGadget = new EntityVehicle(scene, session.getPlayer(), addedAttackGadgetId, 1, staggeredPosition, addedAttackRotation);
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
            }
            //CommandHandler.sendMessage(session.getPlayer(), "DEBUG:\n\nAVATAR POS: " + pos.getX() + " , " + pos.getY() + " , " + pos.getZ() + "\nAVATAR ROT: " + rot.getX() + " , " + rot.getY() + " , " + rot.getZ() + "\nGADGET POS: " + target.getX() + " , " + target.getY() + " , " + target.getZ() + "\nGADGET ROT: " + rot.toString());
        }
        if (addedAttackItemId != 0) {
            target.addY(3f);
            Position originalTargetRefererence = new Position(target);
            for (int i = 1 ; i <= addedAttackCount  ; i++) {
                Position staggeredPosition = new AttackModifierCommand().GetRandomPositionInCircle(originalTargetRefererence,count * addedAttackSpread);
                EntityItem attItem = new EntityItem(scene, session.getPlayer(), GameData.getItemDataMap().get(addedAttackItemId), staggeredPosition, 1);
                scene.addEntity(attItem);
            }
        }
        if (addedAttackMonsterId != 0) {
            target.addY(1f);
            Position originalTargetRefererence = new Position(target);
            for (int i = 1 ; i <= addedAttackCount  ; i++) {
                Position staggeredPosition = new AttackModifierCommand().GetRandomPositionInCircle(originalTargetRefererence,count * addedAttackSpread);
                EntityMonster attMonster = new EntityMonster(scene,GameData.getMonsterDataMap().get(addedAttackMonsterId), staggeredPosition, 90); 
                scene.addEntity(attMonster);
            }
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
