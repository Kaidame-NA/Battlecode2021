package testplayer;

import battlecode.common.*;

import java.util.*;

public class EnlightenmentCenter extends RobotPlayer{


    static int numberofunits = rc.getRobotCount();
    static int numberofunitsproduced, numberofmuckrakersproduced,
            numberofslanderersproduced, numberofpoliticiansproduced, nukes, attacknuke, attacknukemuck = 0;
    static double buildcooldown;
    static HashSet<Integer> producedUnitIDs = new HashSet<Integer>();
    static boolean attacking;
    static int tgtConviction = 0;
    //static HashSet<MapLocation> neutralAttackedECs = new HashSet<MapLocation>();
    static int[] slanderervals = {949,902,855,810,766,724,683,643,605,568,532,497,463,431,399,368,339,310,
            282,255,228,203,178,154,130,107,85,63,41,21};
    static int peakInfluence = 86;
    static int effectiveTurn = 0;
    static int wavecount = 7;
    static int closestEnemyMuckDist = 9999;
    static int closestEnemyMuckConv = 0;
    static int turnssinceattacked = 0;
    static int[] neutralECTargets = new int[20];
    static int indexOfNeutralTgts = -1;
    static int[] ownFlag = new int[4];
    static int ownFlagNum = 0;
    static int scoutFlag = 0;
    static boolean scoutFlagTurn = false;
    static int[] ifBlockExecutes = new int[4];
    static int[] lastTurnFlag = new int[4];


    static void setup() throws GameActionException {
        turnCount = rc.getRoundNum();
        buildcooldown = Math.ceil(2/rc.sensePassability(rc.getLocation()));
    }

    static void run() throws GameActionException {
        if (turnCount % 2 == 1){
            effectiveTurn ++;
            System.out.println("effectiveTurn " + effectiveTurn);
        }
        //System.out.println("Before copy 1: " + Clock.getBytecodeNum());
        //HashSet<Integer> producedUnitsCopy = (HashSet<Integer>) producedUnitIDs.clone();
        //System.out.println("After copy 2: " + Clock.getBytecodeNum() );

        //System.out.println("Iterator: " + Clock.getBytecodeNum() + ", size of HashSet: " + producedUnitIDs.size() );
        comms();
        System.out.println(tgtConviction);
        //System.out.println(closestEnemyDist);
        //System.out.println("After iteration: " + Clock.getBytecodeNum() );
        spawn();
        //System.out.println("After spawn 4: " + Clock.getBytecodeNum() );
        if (turnCount > 75) {
            bidVote();
        }
        //System.out.println("After bidding 5: " + Clock.getBytecodeNum() );
        closestEnemyMuckDist = 9999;
        closestEnemyMuckConv = 0;
        System.out.println("ifBlockExecutes " + Arrays.toString(ifBlockExecutes));

        lastTurnFlag = ownFlag;
        if (effectiveTurn % 10 == 0) {
            attacknuke = 0;
            attacknukemuck = 0;
            nukes = 0;
        }
    }
    static void comms() throws GameActionException{
        //scoutFlagTurn = !scoutFlagTurn;
        Iterator iterator = producedUnitIDs.iterator();
        int id = -1;
        //for (int i = producedUnitIDs.size(); --i >=0;) {
        while(iterator.hasNext()){
            //System.out.println(Clock.getBytecodeNum());
            id = (int) iterator.next();
            //System.out.println(Clock.getBytecodeNum());

            if(!rc.canGetFlag(id)){
                iterator.remove();
                //System.out.println("removed!");
            }
            else {
                //System.out.println("1: " + Clock.getBytecodeNum());
                //System.out.println("2: " + Clock.getBytecodeNum());
                int unitFlag = rc.getFlag(id);
                if(unitFlag == 0 || unitFlag == ownFlagNum)
                {
                    //System.out.println("continued! bytecode: " + Clock.getBytecodeNum());
                    continue;
                }
                //System.out.println("3: " + Clock.getBytecodeNum());
                int[] flag = decodeFlag(unitFlag);
                //System.out.println("4: " + Clock.getBytecodeNum());
                if (flag[0] == 0 && flag[1] != 0) {
                    MapLocation unitPos = new MapLocation(rc.getLocation().x + flag[1], rc.getLocation().y +flag[2]);
                    int unitDist = unitPos.distanceSquaredTo(rc.getLocation());
                    if (unitDist < closestEnemyMuckDist) {
                        closestEnemyMuckDist = unitDist;
                        closestEnemyMuckConv = flag[3];
                        scoutFlag = unitFlag;
                    }
                    ifBlockExecutes[3]++;
                }
                else if (flag[0] == NEUTRAL_EC_FOUND && (!attacking || (flag[1] == ownFlag[1] && flag[2] == ownFlag[2]))) {
                    tgtConviction = flag[3]; //check for switching attack target in this file
                    rc.setFlag(unitFlag);
                    ownFlag = flag;
                    ownFlagNum = unitFlag;
                    attacking = true;
                    ifBlockExecutes[0]++;
                    //some logic about spawning correct poli size
                } else if (flag[0] == ENEMY_EC_FOUND && (!attacking || (flag[1] == ownFlag[1] && flag[2] == ownFlag[2]))) {
                    rc.setFlag(unitFlag);
                    ownFlag = flag;
                    ownFlagNum = unitFlag;
                    tgtConviction = flag[3];
                    attacking = true;
                    ifBlockExecutes[1]++;
                    //some logic about spawning correct poli size
                } else if (flag[0] == SECURED_EC) {
                    if (ownFlag[1] == flag[1] && ownFlag[2] == flag[2]) {
                        rc.setFlag(unitFlag);
                        ownFlag = flag;
                        ownFlagNum = unitFlag;
                        attacking = false;
                        ifBlockExecutes[2]++;
                    }
                }
                //System.out.println("5: " + Clock.getBytecodeNum());
            }
            //System.out.println("end of loop: " + Clock.getBytecodeNum());
        }
        /*
        if (scoutFlagTurn && scoutFlag != 0) {
            rc.setFlag(scoutFlag);
        }
        scoutFlag = 0;*/
    }

    static void spawn() throws GameActionException {
        boolean spawnSafeSlanderer = safeToSpawnSlanderer();
        Direction spawnDir = getOptimalSpawn();
        Direction spawnDirSland = getOptimalSpawnSlanderer();
        int slandVal = getOptimalSlandererVal();
        RobotType unitType = RobotType.POLITICIAN;
        int poliVal = getOptimalPoliVal();
        int bigPoliVal = getOptimalBigPoliVal();

        if (closestEnemyMuckDist >= 81)
            turnssinceattacked++;

        int conviction = 0;
        /*
        if (rc.getEmpowerFactor(rc.getTeam(), 10) > 2 && rc.getInfluence() > 1000) {
            if (rc.getInfluence() < 1000000) {
                conviction = rc.getInfluence();
            } else {
                conviction = rc.getInfluence() - 100000;
            }
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        //overflow
        else if (rc.getInfluence() >= 100000 && effectiveTurn % 3 == 0) {
            conviction = 29;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        } else if (rc.getInfluence() >= 100000 && (effectiveTurn % 3 == 1 || effectiveTurn % 3 == 2)) {
            conviction = 10000;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

         */

        //defend

        //if i have over 1000 go get those neutrals no matter what.  Otherwise prioritize defense.
/*
        else if (rc.getInfluence() > 200 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            if (rc.getInfluence() > 200 && nukes < 2 && tgtConviction == 255 && rc.getInfluence() >= 567 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
                conviction = 567;
                numberofpoliticiansproduced++;
                numberofunitsproduced++;
                nukes++;
            } else if (rc.getInfluence() > 200 && nukes < 2 && tgtConviction < 255 && rc.getInfluence() >= tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
                conviction = tgtConviction + 11;
                numberofpoliticiansproduced++;
                numberofunitsproduced++;
                nukes++;
            }
            else if (rc.getInfluence() > 200 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
                unitType = RobotType.MUCKRAKER;
                conviction = 1;
                numberofmuckrakersproduced++;
                numberofunitsproduced++;
            }
        }
*/
/*
        else if (closestEnemyMuckDist < 256 && closestEnemyMuckDist >= 81) {
            if ((numberofunitsproduced % 3 == 2 || numberofunitsproduced % 3 == 1) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
                conviction = poliVal;
                numberofunitsproduced++;
                numberofpoliticiansproduced++;
            }
            else if ((numberofunitsproduced % 3 == 0) && spawnSafeSlanderer && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
                unitType = RobotType.SLANDERER;
                spawnDir = spawnDirSland;
                conviction = slandVal;
                numberofunitsproduced++;
                numberofslanderersproduced++;
            }
        }
 */
        if (closestEnemyMuckDist < 81 && closestEnemyMuckConv > 16 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), closestEnemyMuckConv + 11)) {
            conviction = closestEnemyMuckConv + 11;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
            turnssinceattacked = 0;
        }

        else if (closestEnemyMuckDist < 81 && closestEnemyMuckConv > 16) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
            turnssinceattacked = 0;
        }


        else if (closestEnemyMuckDist < 81 && rc.getInfluence() >= poliVal) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
            turnssinceattacked = 0;
        }
        else if (closestEnemyMuckDist < 81) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
            turnssinceattacked = 0;
        }



        //attack

        else if (rc.getInfluence() >1000 && attacknuke <1 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            conviction = rc.getInfluence()/2;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            attacknuke++;
        }

        else if (rc.getInfluence() >1000 && attacknukemuck <1 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = rc.getInfluence()/3;
            numberofmuckrakersproduced++;
            numberofunitsproduced++;
            attacknukemuck++;
        }
/*
        else if ((numberofunitsproduced % 4 == 3) && rc.getInfluence() >= 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            conviction = 400;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
        }
        else if ((numberofunitsproduced % 4 == 3) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal) &&
                rc.getInfluence() < 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }
        else if ((numberofunitsproduced % 4 == 0) && spawnSafeSlanderer && rc.getInfluence() < 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND
                && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }
        else if (rc.getInfluence() < 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
*/
        //neutral
// nuking neutrals - current nuke limit of 2 - nuke limit resets if we take the base (honestly the resets might be better if its done based on turncount like every 20 turns
        // reset, but for now this is ok.  the reason this might be better is because if 2 nukes doesnt kill it, we're in trouble cause then we can't really take it).
        else if (numberofslanderersproduced >= 11 && nukes <2 && tgtConviction == 255 && rc.getInfluence() >= 511 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = 511;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            nukes++;
        }

        else if (numberofslanderersproduced >= 11 && nukes <2 && tgtConviction < 255 && rc.getInfluence() >= tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = tgtConviction + 11;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            nukes++;
        }
// if we're within 50 of making a nuke that can take a Neutral, save for it

        else if (numberofslanderersproduced >= 11 && nukes <2 && tgtConviction == 255 && rc.getInfluence() >= 511 - 75 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofmuckrakersproduced++;
            numberofunitsproduced++;
        }

        else if (numberofslanderersproduced >= 11 && nukes <2 && tgtConviction < 255 && rc.getInfluence() >= tgtConviction + 11 - 50 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofmuckrakersproduced++;
            numberofunitsproduced++;
        }
/*
        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 7 == 3 || numberofunitsproduced % 7 == 2 || numberofunitsproduced % 7 == 5 || numberofunitsproduced % 7 == 6) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal) &&
                rc.getInfluence() < tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }
        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 7 == 0 || numberofunitsproduced % 7 == 4) && spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }
        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 7 == 1) && rc.getInfluence() < tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
*/
        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 3) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, bigPoliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = bigPoliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 1) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 0 || numberofunitsproduced % 4 == 2) && spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 0 || numberofunitsproduced % 4 == 2) && !spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 5 || numberofunitsproduced % 10 == 1) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, bigPoliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = bigPoliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 4  || numberofunitsproduced % 10 == 7 || numberofunitsproduced % 10 == 9) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 0 || numberofunitsproduced % 10 == 3 || numberofunitsproduced % 10 == 6 || numberofunitsproduced % 10 == 8) && spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 0 || numberofunitsproduced % 10 == 3 || numberofunitsproduced % 10 == 6 || numberofunitsproduced % 10 == 8) && !spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 2) && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 2) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, bigPoliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = bigPoliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 3 || numberofunitsproduced % 6 == 5) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 0 || numberofunitsproduced % 6 == 4) && spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 0 || numberofunitsproduced % 6 == 4) && !spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 1) && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && (numberofunitsproduced % 7 == 4) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal) &&
                rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && (numberofunitsproduced % 7 == 0 || numberofunitsproduced % 7 == 2 || numberofunitsproduced % 7 == 5) && spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }
        else if (numberofpoliticiansproduced >= 13 && (numberofunitsproduced % 7 == 0 || numberofunitsproduced % 7 == 2 || numberofunitsproduced % 7 == 5) && !spawnSafeSlanderer && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (numberofpoliticiansproduced >= 13 && (numberofunitsproduced % 7 == 1 || numberofunitsproduced % 7 == 3 ||  numberofunitsproduced % 7 == 6) && rc.getInfluence() < tgtConviction + 11 && ownFlag[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        //build 1
        else if (turnCount == 1 || turnCount == 37) {
            unitType = RobotType.SLANDERER;
            conviction = 130;
            spawnDir = spawnDirSland;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        //early spawning 8 cardinal directions
        else if (turnCount == 3) {
            unitType = RobotType.MUCKRAKER;
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.NORTH, 1)) {
                spawnDir = Direction.NORTH;
            }
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 7) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.EAST, 1)) {
                spawnDir = Direction.EAST;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 11) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.SOUTH, 1)) {
                spawnDir = Direction.SOUTH;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 15 ) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.WEST, 1)) {
                spawnDir = Direction.WEST;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 19) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.NORTHEAST, 1)) {
                spawnDir = Direction.NORTHEAST;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 23) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.SOUTHEAST, 1)) {
                spawnDir = Direction.SOUTHEAST;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 27) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.SOUTHWEST, 1)) {
                spawnDir = Direction.SOUTHWEST;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 31) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.NORTHWEST, 1)) {
                spawnDir = Direction.NORTHWEST;
            }
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount ==35 || turnCount == 39) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (turnCount == 5 || turnCount == 9 || turnCount == 13 || turnCount == 17) {
            unitType = RobotType.SLANDERER;
            conviction = 41;
            spawnDir = spawnDirSland;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (turnCount == 21 || turnCount == 25) {
            unitType = RobotType.SLANDERER;
            conviction = 63;
            spawnDir = spawnDirSland;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (turnCount == 29) {
            unitType = RobotType.SLANDERER;
            conviction = 85;
            spawnDir = spawnDirSland;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }
        else if (turnCount == 33) {
            unitType = RobotType.SLANDERER;
            conviction = 107;
            spawnDir = spawnDirSland;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (turnCount == 41) {
            unitType = RobotType.SLANDERER;
            conviction = 154;
            spawnDir = spawnDirSland;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (numberofslanderersproduced >= 11 && numberofpoliticiansproduced < 14) {
            conviction = 20;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }


/*
        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 7 == 3  || numberofunitsproduced % 7 == 2 || numberofunitsproduced % 7 == 5 || numberofunitsproduced % 7 == 6) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }
        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 7 == 0 || numberofunitsproduced % 7 == 4) && spawnSafeSlanderer && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }
        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 7 == 1) && rc.canBuildRobot(RobotType.MUCKRAKER, spawnDir, 1)) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
*/
        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 3) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, bigPoliVal)) {
            conviction = bigPoliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 1) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 0 || numberofunitsproduced % 4 == 2) && spawnSafeSlanderer && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (rc.getInfluence() >= 1000 && (numberofunitsproduced % 4 == 0 || numberofunitsproduced % 4 == 2) && !spawnSafeSlanderer
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 5 || numberofunitsproduced % 10 == 1) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, bigPoliVal)) {
            conviction = bigPoliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 4  || numberofunitsproduced % 10 == 7 || numberofunitsproduced % 10 == 9) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 500 && ((numberofunitsproduced % 10 == 0 || numberofunitsproduced % 10 == 3 || numberofunitsproduced % 10 == 6 || numberofunitsproduced % 10 == 8) && spawnSafeSlanderer && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal))) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (rc.getInfluence() >= 500 && ((numberofunitsproduced % 10 == 0 || numberofunitsproduced % 10 == 3 || numberofunitsproduced % 10 == 6 || numberofunitsproduced % 10 == 8) && !spawnSafeSlanderer
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal))) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 500 && (numberofunitsproduced % 10 == 2) && rc.canBuildRobot(RobotType.MUCKRAKER, spawnDir, 1)) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
/*
        else if (numberofslanderersproduced == 0 && numberofpoliticiansproduced == 0 && rc.getInfluence() > 40) {
            conviction = 14;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
*/
        else if (rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 2) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, bigPoliVal)) {
            conviction = bigPoliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 3 || numberofunitsproduced % 6 == 5) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 0 || numberofunitsproduced % 6 == 4) && spawnSafeSlanderer && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 0 || numberofunitsproduced % 6 == 4) && !spawnSafeSlanderer
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 100 && (numberofunitsproduced % 6 == 1) && rc.canBuildRobot(RobotType.MUCKRAKER, spawnDir, 1)) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
/*
        else if (numberofslanderersproduced == 0 && rc.getInfluence() <= 40) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
*/
        else if ((numberofunitsproduced % 7 == 4) && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if ((numberofunitsproduced % 7 == 0 || numberofunitsproduced % 7 == 2 || numberofunitsproduced % 7 == 5) && spawnSafeSlanderer && rc.canBuildRobot(RobotType.SLANDERER, spawnDirSland, slandVal)) {
            unitType = RobotType.SLANDERER;
            spawnDir = spawnDirSland;
            conviction = slandVal;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if ((numberofunitsproduced % 7 == 0 || numberofunitsproduced % 7 == 2 || numberofunitsproduced % 7 == 5) && !spawnSafeSlanderer
                && rc.canBuildRobot(RobotType.POLITICIAN, spawnDir, poliVal)) {
            conviction = poliVal;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if ((numberofunitsproduced % 7 == 1 || numberofunitsproduced % 7 == 3 ||  numberofunitsproduced % 7 == 6) && rc.canBuildRobot(RobotType.MUCKRAKER, spawnDir, 1)) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (numberofunitsproduced == 0 && rc.getInfluence() > 0) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        if (rc.canBuildRobot(unitType, spawnDir, conviction)) {
            rc.buildRobot(unitType, spawnDir, conviction);
            if (unitType != RobotType.SLANDERER) {
                RobotInfo builtRobot = rc.senseRobotAtLocation(rc.getLocation().add(spawnDir));
                producedUnitIDs.add(builtRobot.getID());
            }
        }
    }

    static Boolean safeToSpawnSlanderer() throws GameActionException {
        RobotInfo[] friendlies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
        int nearbyPoliticians = 0, nearbySlanderers = 0, nearbynotPoliticians = 0;

        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            int influence = robot.influence;
            if (type == RobotType.POLITICIAN && influence < 50) {
                nearbyPoliticians++;
            }
            else if(type == RobotType.SLANDERER){
                nearbySlanderers++;
            }
        }

        for (RobotInfo robot : enemies) {
            RobotType type = robot.getType();
            if (type != RobotType.POLITICIAN) {
                nearbynotPoliticians++;
            }
        }

        if (nearbyPoliticians * 4 < nearbySlanderers || nearbynotPoliticians != 0) {
            return false;
        } else {
            return true;
        }
    }

    static Direction getOptimalSpawn() throws GameActionException {
        Direction optimalDir = Direction.SOUTH;
        double optimalCost = Double.MIN_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double cost = rc.sensePassability(adj);
                //System.out.println("Cost: " + cost);
                if (cost > optimalCost && rc.canBuildRobot(RobotType.SLANDERER, dir, 1)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }

    static Direction getOptimalSpawnSlanderer() throws GameActionException {
        Direction optimalDir = Direction.SOUTH;
        double optimalCost = Double.MAX_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double cost = rc.sensePassability(adj);
                //System.out.println("Cost: " + cost);
                if (cost < optimalCost && rc.canBuildRobot(RobotType.SLANDERER, dir, 1)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }

    static int getOptimalSlandererVal() throws GameActionException {
        int upgradethreshold = 0;
        if (turnssinceattacked < 5) {
            upgradethreshold = numberofslanderersproduced;
        }
        if (upgradethreshold >= 30) {
            upgradethreshold = 29;
        }
        for (int i = 0; i < slanderervals.length - upgradethreshold; i++) {
            if (slanderervals[i] <= rc.getInfluence()) {
                return slanderervals[i];
            }
        }
        return 0;
    }

    static int getOptimalPoliVal() throws GameActionException {
        int optimalVal = 14;
        if (numberofunitsproduced > 30) {
            optimalVal = (rc.getInfluence())/50 + 14;
        }
        if (optimalVal >=30) {
            optimalVal =29;
        }

        return optimalVal;
    }

    static int getOptimalBigPoliVal() throws GameActionException {
        int optimalVal = 100;
        if (numberofunitsproduced > 30) {
            optimalVal = (rc.getInfluence())/8 + 50;
        }
        if (optimalVal >= 551) {
            optimalVal = 550;
        }

        return optimalVal;
    }

    static boolean shouldSpawnPoli() throws GameActionException {
        if (rc.getInfluence() > peakInfluence) {
            peakInfluence = rc.getInfluence();
            return true;
        }
        else {
            return false;
        }
    }


    static int friendlyVotes, prevBid, winStreak, loseStreak, stalledRounds = 0;
    //bids for the ec
    static void bidVote() throws GameActionException{
        int currentInfluence = rc.getInfluence();
        int round = rc.getRoundNum();
        int newBid;

        if(rc.getTeamVotes()>friendlyVotes){
            if(winStreak<25) winStreak++;
            loseStreak = 0;
            //System.out.println("I won the last vote! Winstreak: " + winStreak);
        }
        else{
            winStreak = 0;
            if(loseStreak<25) loseStreak++;
            //System.out.println("I lost the last vote :((. Losestreak: " + loseStreak);
        }
        friendlyVotes = rc.getTeamVotes();

        //amount of continuous rounds we have not bid for - reset the prevBid & loseStreak;
        if(stalledRounds>=4) {
            prevBid = 2;
            loseStreak = 0;
            stalledRounds = 0;
        }

        int threshold = currentInfluence/5; //our maximum we are willing to bid
        if(friendlyVotes > 750)// || round-friendlyVotes > 1500)//if either we or enemy have already won the bidding game? unless ties...
        {
            //System.out.println("The election has already been decided.");
        }
        else if(winStreak == 0){ // we lost the last vote...
            double iCoef = 2; //1.97+currentInfluence/40000; //more is more aggro bidding

            int antiPreserve;
            if(currentInfluence<100)
                antiPreserve = 0;
            else if(currentInfluence < 1000)
                antiPreserve = 1;
            else if(currentInfluence<10000)
                antiPreserve = 2;
            else
                antiPreserve = (int) Math.floor(Math.log10(currentInfluence));

            newBid = prevBid + (int) Math.ceil(Math.pow( (1/iCoef),(-loseStreak+1 - antiPreserve) ));
            //System.out.println("loseStreak: " + loseStreak + " prevBid:  " + prevBid + " newBid: " + newBid);
            //increasing doubley (loseStreak is increasing while prevBid is increasing)


            if(newBid < 1 && rc.canBid(1)){
                rc.bid(1);
                prevBid = 1;
                stalledRounds = 0;
                //System.out.println("Lost last vote, newBid<1 so lets bid the minimum, 1");
            }
            else if(newBid < threshold && rc.canBid(newBid)){
                //dont want to be bankrupting ourselves so we have a threshold (max value we are willing to bid)

                rc.bid(newBid);
                prevBid = newBid;
                stalledRounds = 0;
                //System.out.println("Last vote lost, and we are less than the threshold, bid: " + newBid);
            }
            else if(newBid >= threshold && threshold>prevBid && rc.canBid(threshold)){
                //bid the max we are willing to, also if its greater than our last bid
                newBid = threshold;
                rc.bid(newBid);
                prevBid = newBid;
                stalledRounds = 0;
                //System.out.println("Last vote lost, and we are greater than the threshold, bid: " + newBid);
            }
            else{
                loseStreak--; //we already know we lost - no need to bid higher next time...
                stalledRounds++;
                //System.out.println("We lost the last vote, but we arent willing to bid more than last time so we bid 0");
            }
        }
        else{// we won the last vote!
            double dCoef = 1.7; //less is more aggro bidding

            int preserve;
            if(currentInfluence<10000)
                preserve = 0;
            else if(currentInfluence<1000000)
                preserve = 1;
            else
                preserve = 3;

            newBid = prevBid + (int) Math.ceil(-1*Math.pow( (1/dCoef),(-winStreak+2+preserve) ) );
            //System.out.println("winStreak: " + winStreak + " prevBid:  " + prevBid + " newBid: " + newBid);
            //decreasing doubley (winStreak is increasing while prevBid is decreasing)

            if(newBid < 1 && rc.canBid(1)){
                rc.bid(1);
                prevBid = 1;
                //System.out.println("Won last vote, newBid<1 so lets bid the minimum, 1");
            }
            else if(rc.canBid(newBid) && newBid < threshold){
                rc.bid(newBid);
                prevBid = newBid;
                //System.out.println("Won last vote, lets bid " + newBid);
            }
            else{
                //we lost (no bid)
            }

        }

    }

    static boolean containsLocationFromFlag(int neutralFlag, int[] arr) {
        int[] decodedTgt = decodeFlag(neutralFlag);
        for (int i = arr.length; --i >= 0;) {
            int[] decoded = decodeFlag(arr[i]);
            if (decoded[1] == decodedTgt[1] && decoded[2] == decodedTgt[2]) {
                return true;
            }
        }
        return false;
    }

    static void updateFlags(int newFlag, int[] arr) {
        int[] decodedTgt = decodeFlag(newFlag);
        for (int i = arr.length; --i >= 0;) {
            int[] decoded = decodeFlag(arr[i]);
            if (decoded[1] == decodedTgt[1] && decoded[2] == decodedTgt[2]) {
                arr[i] = newFlag;
            }
        }
    }
}