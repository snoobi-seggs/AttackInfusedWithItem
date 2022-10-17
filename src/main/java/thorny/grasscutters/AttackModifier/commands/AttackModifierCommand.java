package thorny.grasscutters.AttackModifier.commands;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.game.avatar.Avatar;
//import emu.grasscutter.game.entity.EntityGadget; //unused for now, since vehicles i rather
import emu.grasscutter.game.entity.EntityItem;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.EntityVehicle;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.net.proto.VisionTypeOuterClass.VisionType; // removed coz idk why this is needed if remove entity alr removed it
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketSceneEntityDisappearNotify; // removed coz alr sent in removeentity, no use sending twice
import emu.grasscutter.utils.Position;
import emu.grasscutter.command.Command.TargetRequirement;
import emu.grasscutter.data.excels.MonsterData;
import emu.grasscutter.data.excels.AvatarSkillDepotData;
import emu.grasscutter.data.excels.GadgetData;
import emu.grasscutter.data.excels.ItemData;
import emu.grasscutter.data.GameData;

import java.util.ArrayList;
import java.util.List;

//alteri seggs
import thorny.grasscutters.AttackModifier.AttackModifier;
import thorny.grasscutters.AttackModifier.utils.*;


// Command usage
@Command(label = "attack", aliases = {"at","am","snoospawn"}, usage = "[IdOfObject/none/clear/config] [radiusSpawnFromSelf] [height] [count] [spreadRationToRadius] [rotXupwards] [rotYHorizontalFromSELF] [rotZlastrot]\n\nID of object is the id of the object in the handbook\nCould ALSO be:\n[none] to remove current attacks\n[clear] to remove all active things\n[reload] to reload config\n\nradiusSpawnFromSelf is the distance between u and the object spawned when atking\nheight is the distance above u the gadget is spawned\nrotX change the vertical rotation of object\nrotY changed the horizontal rotation from where u are facing [180 will shoot behind u always]\nrotZ is the 3rd cross vector rotation to rot y and x", targetRequirement = TargetRequirement.NONE)
public class AttackModifierCommand implements CommandHandler {
    private static final Config config = AttackModifier.getInstance().config.getConfig();
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
    //static List<EntityGadget> activeGadgets = new ArrayList<>(); // Current gadgets
    //static List<EntityGadget> removeGadgets = new ArrayList<>(); // To be removed gadgets
    static List<EntityVehicle> activeVehicles = new ArrayList<>(); // Current gadgets
    static List<EntityVehicle> removeVehicles = new ArrayList<>(); // To be removed gadgets
    static List<EntityItem> activeItems = new ArrayList<>();
    static List<EntityItem> removeItems = new ArrayList<>();
    static Boolean toAdd = false; //this to add is for skills from config.

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
                    rotXFromArgs = Float.parseFloat(args.get(5));
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
                    if (args.get(0).toLowerCase().equals("none") || args.get(0).toLowerCase().equals("off"))  {
                        gadgetId = 0;
                        itemId = 0;
                        monsterId = 0;
                        toAdd = false;
                        CommandHandler.sendMessage(targetPlayer, "infusion has been reset, use /at reload to add back config at patterns");
                        return;
                    } else if (args.get(0).toLowerCase().equals("clear") || args.get(0).toLowerCase().equals("remove")) {
                        activeVehicles.forEach(entity -> targetPlayer.getScene().killEntity(entity, 0));
                        activeItems.forEach(entity -> targetPlayer.getScene().killEntity(entity, 0));
                        return;
                    } else if (args.get(0).toLowerCase().equals("reload") || args.get(0).toLowerCase().equals("config")) {
                        AttackModifier.getInstance().reloadConfig();
                        CommandHandler.sendMessage(sender, "reloaded config omg seggs");
                        toAdd = true;
                        return;
                    }
                // parsed if not none
                    thing = Integer.parseInt(args.get(0));
                    toAdd = false;
                } catch (Exception e) {
                    CommandHandler.sendMessage(targetPlayer, "the input for itemId/gadgetId/monsterId is invalid");
                }
                break;
            default:
                CommandHandler.sendMessage(targetPlayer, "[IdOfObject/none/clear/config] [radiusSpawnFromSelf] [height] [count] [spreadRatioToRadius] [rotXupwards] [rotYHorizontalFromSELF] [rotZlastrot]\n\nID of object is the id of the object in the handbook\nCould ALSO be:\n[none] to remove current attacks\n[clear] to remove all active things\n[reload] to reload config\n\nradiusSpawnFromSelf is the distance between u and the object spawned when atking\nheight is the distance above u the gadget is spawned\nrotX change the vertical rotation of object\nrotY changed the horizontal rotation from where u are facing [180 will shoot behind u always]\nrotZ is the 3rd cross vector rotation to rot y and x");
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
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nGadgetID : " + gadgetId + "\n\nWITH SETTINGS:\nRadius from self : " + radius + "\nHeight : " + height + "\nCount : " + count + "\nSpread" + spread + "\nRotX,RotY,RotZ = " + List.of(rotX,rotY,rotZ).toString());
        } else if (itemId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nItemID : " + itemId + "\n\nWITH SETTINGS:\nRadius from self : " + radius + "\nHeight : " + height + "\nCount : " + count + "\nSpread" + spread);
        } else if (monsterId != 0) {
            CommandHandler.sendMessage(targetPlayer, "Future attacks are now infused with:" + "\n\nMonsterID : " + monsterId  + "\n\nWITH SETTINGS:\nRadius from self : " + radius + "\nHeight : " + height + "\nCount : " + count + "\nSpread" + spread);
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

        if (toAdd) {   // does not overwrite current gadget id frm args if to add is false since u already specified a id to overwrite u stupid shit fuck u and ur comments fuck
            Avatar activeAvatar = session.getPlayer().getTeamManager().getCurrentAvatarEntity().getAvatar();
            AvatarSkillDepotData skillDepot = activeAvatar.getSkillDepot();
            int avatarId = activeAvatar.getAvatarId();
            int addedAttack = 0;
            int usedAttack = -1;
            //check skill type used
            if (skillId == (skillDepot.getSkills().get(0))) {
                usedAttack = 0;
            } else if (skillId == (skillDepot.getSkills().get(1))) {
                usedAttack = 1;
            } else if (skillId == (skillDepot.getEnergySkill())) {
                usedAttack = 2;
            }
            //wtf is this alteri :|
            switch (avatarId) {
                default -> usedAttack = -1;
                case 10000002 -> { // ayaya
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.ayakaIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.ayakaIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.ayakaIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000003 -> { // Jean
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.jeanIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.jeanIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.jeanIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000005 -> { // Traveler Male elementless
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.travelerMaleNoElementIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.travelerMaleNoElementIds.skill.elementalSkill; // Elemental
                        case 2 -> addedAttack = config.travelerMaleNoElementIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000006 -> { // lisa
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.lisaIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.lisaIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.lisaIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000007 -> { // Traveler female elementless
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.travelerFemaleNoElementIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.travelerFemaleNoElementIds.skill.elementalSkill; // Elemental
                        case 2 -> addedAttack = config.travelerFemaleNoElementIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000014 -> { // Barbara
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.barbaraIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.barbaraIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.barbaraIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000015 -> { // kaeya
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kaeyaIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.kaeyaIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.kaeyaIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000016 -> { // Diluc
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.dilucIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.dilucIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.dilucIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000020 -> { // razor
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.razorIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.razorIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.razorIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000021 -> { // amber
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.amberIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.amberIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.amberIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000022 -> { // venti
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.ventiIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.ventiIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.ventiIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000023 -> { // xiangling
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.xianglingIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.xianglingIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.xianglingIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000024 -> { // beidou
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.beidouIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.beidouIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.beidouIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000025 -> { // xingqiu
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.xingqiuIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.xingqiuIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.xingqiuIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000026 -> { // xiao
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.xiaoIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.xiaoIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.xiaoIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000027 -> { // ningguang
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.ningguangIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.ningguangIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.ningguangIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000029 -> { // klee
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kleeIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.kleeIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.kleeIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000030 -> { // zhongli
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.zhongliIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.zhongliIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.zhongliIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000031 -> { // fischl
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.fischlIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.fischlIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.fischlIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000032 -> { // bennett
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.bennettIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.bennettIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.bennettIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000033 -> { // tartaglia
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.tartagliaIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.tartagliaIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.tartagliaIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000034 -> { // noelle
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.noelleIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.noelleIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.noelleIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000035 -> { // qiqi
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.qiqiIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.qiqiIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.qiqiIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000036 -> { // chongyun
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.chongyunIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.chongyunIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.chongyunIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000037 -> { // ganyu
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.ganyuIds.skill.normalAtk; // Normal attack
                        case 1 -> addedAttack = config.ganyuIds.skill.elementalSkill; // Elemental skill
                        case 2 -> addedAttack = config.ganyuIds.skill.elementalBurst; // Burst
                    }
                }
                case 10000038 -> { // albedo
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.albedoIds.skill.normalAtk;
                        case 1 -> addedAttack = config.albedoIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.albedoIds.skill.elementalBurst;
                    }
                }
                case 10000039 -> { // diona
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.albedoIds.skill.normalAtk;
                        case 1 -> addedAttack = config.albedoIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.albedoIds.skill.elementalBurst;
                    }
                }
                case 10000041 -> { // mona
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.monaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.monaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.monaIds.skill.elementalBurst;
                    }
                }
                case 10000042 -> { // keqing
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.keqingIds.skill.normalAtk;
                        case 1 -> addedAttack = config.keqingIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.keqingIds.skill.elementalBurst;
                    }
                }
                case 10000043 -> { // sucrose
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.sucroseIds.skill.normalAtk;
                        case 1 -> addedAttack = config.sucroseIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.sucroseIds.skill.elementalBurst;
                    }
                }
                case 10000044 -> { // xinyan
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.xinyanIds.skill.normalAtk;
                        case 1 -> addedAttack = config.xinyanIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.xinyanIds.skill.elementalBurst;
                    }
                }
                case 10000045 -> { // rosaria
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.rosariaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.rosariaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.rosariaIds.skill.elementalBurst;
                    }
                }
                case 10000046 -> { // hutao
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.hutaoIds.skill.normalAtk;
                        case 1 -> addedAttack = config.hutaoIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.hutaoIds.skill.elementalBurst;
                    }
                }
                case 10000047 -> { // kazuha
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kazuhaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.kazuhaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.kazuhaIds.skill.elementalBurst;
                    }
                }
                case 10000048 -> { // yanfei
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.yanfeiIds.skill.normalAtk;
                        case 1 -> addedAttack = config.yanfeiIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.yanfeiIds.skill.elementalBurst;
                    }
                }
                case 10000049 -> { // yoimiya
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.yoimiyaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.yoimiyaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.yoimiyaIds.skill.elementalBurst;
                    }
                }
                case 10000050 -> { // tohma
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.thomaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.thomaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.thomaIds.skill.elementalBurst;
                    }
                }
                case 10000051 -> { // eula
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.eulaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.eulaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.eulaIds.skill.elementalBurst;
                    }
                }
                case 10000052 -> { // raiden
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.raidenshogunIds.skill.normalAtk;
                        case 1 -> addedAttack = config.raidenshogunIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.raidenshogunIds.skill.elementalBurst;
                    }
                }
                case 10000053 -> { // sayu
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.sayuIds.skill.normalAtk;
                        case 1 -> addedAttack = config.sayuIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.sayuIds.skill.elementalBurst;
                    }
                }
                case 10000054 -> { // kokomi
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kokomiIds.skill.normalAtk;
                        case 1 -> addedAttack = config.kokomiIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.kokomiIds.skill.elementalBurst;
                    }
                }
                case 10000055 -> { // gorou
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.gorouIds.skill.normalAtk;
                        case 1 -> addedAttack = config.gorouIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.gorouIds.skill.elementalBurst;
                    }
                }
                case 10000056 -> { // sara
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kujousaraIds.skill.normalAtk;
                        case 1 -> addedAttack = config.kujousaraIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.kujousaraIds.skill.elementalBurst;
                    }
                }
                case 10000057 -> { // itto
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kujousaraIds.skill.normalAtk;
                        case 1 -> addedAttack = config.kujousaraIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.kujousaraIds.skill.elementalBurst;
                    }
                }
                case 10000058 -> { // yae miko
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kujousaraIds.skill.normalAtk;
                        case 1 -> addedAttack = config.kujousaraIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.kujousaraIds.skill.elementalBurst;
                    }
                }
                case 10000059 -> { // heizou
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.heizouIds.skill.normalAtk;
                        case 1 -> addedAttack = config.heizouIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.heizouIds.skill.elementalBurst;
                    }
                }
                case 10000060 -> { // yelan
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.yelanIds.skill.normalAtk;
                        case 1 -> addedAttack = config.yelanIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.yelanIds.skill.elementalBurst;
                    }
                }
                case 10000062 -> { // aloy
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.aloyIds.skill.normalAtk;
                        case 1 -> addedAttack = config.aloyIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.aloyIds.skill.elementalBurst;
                    }
                }
                case 10000063 -> { // shenhe
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.shenheIds.skill.normalAtk;
                        case 1 -> addedAttack = config.shenheIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.shenheIds.skill.elementalBurst;
                    }
                }
                case 10000064 -> { // yunjin
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.yunjinIds.skill.normalAtk;
                        case 1 -> addedAttack = config.yunjinIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.yunjinIds.skill.elementalBurst;
                    }
                }
                case 10000065 -> { // shinobu
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.kukishinobuIds.skill.normalAtk;
                        case 1 -> addedAttack = config.kukishinobuIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.kukishinobuIds.skill.elementalBurst;
                    }
                }
                case 10000066 -> { // ayato
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.ayatoIds.skill.normalAtk;
                        case 1 -> addedAttack = config.ayatoIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.ayatoIds.skill.elementalBurst;
                    }
                }
                case 10000067 -> { // collei
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.colleiIds.skill.normalAtk;
                        case 1 -> addedAttack = config.colleiIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.colleiIds.skill.elementalBurst;
                    }
                }
                case 10000068 -> { // dori
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.doriIds.skill.normalAtk;
                        case 1 -> addedAttack = config.doriIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.doriIds.skill.elementalBurst;
                    }
                }
                case 10000069 -> { // tighnari
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.tighnariIds.skill.normalAtk;
                        case 1 -> addedAttack = config.tighnariIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.tighnariIds.skill.elementalBurst;
                    }
                }
                case 10000070 -> { // nilou
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.nilouIds.skill.normalAtk;
                        case 1 -> addedAttack = config.nilouIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.nilouIds.skill.elementalBurst;
                    }
                }
                case 10000071 -> { // cyno
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.cynoIds.skill.normalAtk;
                        case 1 -> addedAttack = config.cynoIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.cynoIds.skill.elementalBurst;
                    }
                }
                case 10000072 -> { // candace
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.candaceIds.skill.normalAtk;
                        case 1 -> addedAttack = config.candaceIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.candaceIds.skill.elementalBurst;
                    }
                }
                case 10000073 -> { // nahida
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.nahidaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.nahidaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.nahidaIds.skill.elementalBurst;
                    }
                }
                case 10000074 -> { // layla
                    switch (usedAttack) {
                        default -> addedAttack = 0;
                        case 0 -> addedAttack = config.laylaIds.skill.normalAtk;
                        case 1 -> addedAttack = config.laylaIds.skill.elementalSkill;
                        case 2 -> addedAttack = config.laylaIds.skill.elementalBurst;
                    }
                }
            }
            //sort if item mons or gadget
            if (GameData.getGadgetDataMap().get(addedAttack) != null) {
                addedAttackGadgetId = addedAttack;
            } else if (GameData.getItemDataMap().get(addedAttack) != null) {
                addedAttackItemId = addedAttack;
            } else if (GameData.getMonsterDataMap().get(addedAttack) != null) {
                addedAttackMonsterId = addedAttack;
            }

            //placeholder add other stats take from last cast.
            addedAttackRotX = rotX;
            addedAttackRotY = rotY;
            addedAttackRotZ = rotZ;
            addedAttackHeight = height;
            addedAttackRadius = radius;
            addedAttackCount = count;
            addedAttackSpread = spread;
        }

        if (!toAdd) {
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
                Position staggeredPosition = new AttackModifierCommand().GetRandomPositionInCircle(originalTargetRefererence,addedAttackSpread);
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
                Position staggeredPosition = new AttackModifierCommand().GetRandomPositionInCircle(originalTargetRefererence,addedAttackSpread);
                EntityItem attItem = new EntityItem(scene, session.getPlayer(), GameData.getItemDataMap().get(addedAttackItemId), staggeredPosition, 1);
                int currTime = (int)(System.currentTimeMillis() - 1665393100);
                attItem.setGroupId(currTime);
                activeItems.add(attItem);
                scene.addEntity(attItem);
            }
        }
        if (addedAttackMonsterId != 0) {
            target.addY(1f);
            Position originalTargetRefererence = new Position(target);
            for (int i = 1 ; i <= addedAttackCount  ; i++) {
                Position staggeredPosition = new AttackModifierCommand().GetRandomPositionInCircle(originalTargetRefererence,addedAttackSpread);
                EntityMonster attMonster = new EntityMonster(scene,GameData.getMonsterDataMap().get(addedAttackMonsterId), staggeredPosition, 90); 
                scene.addEntity(attMonster);
            }
        }

        // Remove all gadgets when list not empty
        if (!activeVehicles.isEmpty()) {
            for (EntityVehicle gadget : activeVehicles) {
                // When gadgets have lived for 10 sec
                if((int)(System.currentTimeMillis() - 1665393100) > (gadget.getGroupId()+10000)){
                    // Add to removal list
                    removeVehicles.add(gadget);
                    // Remove entity
                    scene.removeEntity(gadget);//, VisionType.VISION_TYPE_REMOVE);
                    //scene.broadcastPacket(new PacketSceneEntityDisappearNotify(gadget, VisionType.VISION_TYPE_REMOVE));
                }
            }
            // Remove gadgets and clean list
            activeVehicles.removeAll(removeVehicles);
            removeVehicles.clear();
        }

        if (!activeItems.isEmpty()) {
            for (EntityItem item : activeItems) {
                // When gadgets have lived for 10 sec
                if((int)(System.currentTimeMillis() - 1665393100) > (item.getGroupId()+10000)){
                    // Add to removal list
                    removeItems.add(item);
                    // Remove entity
                    scene.removeEntity(item);//, VisionType.VISION_TYPE_REMOVE);
                    //scene.broadcastPacket(new PacketSceneEntityDisappearNotify(gadget, VisionType.VISION_TYPE_REMOVE));
                }
            }
            // Remove items and clean list
            activeItems.removeAll(removeItems);
            removeItems.clear();
        }
    }
}
