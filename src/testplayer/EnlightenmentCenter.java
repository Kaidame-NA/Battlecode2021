package testplayer;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class EnlightenmentCenter extends RobotPlayer{


    static int numberofunits = rc.getRobotCount();
    static int numberofunitsproduced, numberofmuckrakersproduced,
    numberofslanderersproduced, numberofpoliticiansproduced, numberofattackingunitsproduced = 0;
    static double buildcooldown;
    static HashSet<Integer> producedUnitIDs = new HashSet<Integer>();
    static boolean attacking;
    static int tgtConviction = 0;
    //static HashSet<MapLocation> neutralAttackedECs = new HashSet<MapLocation>();
    static int slanderervals[] = {949,902,855,810,766,724,683,643,605,568,532,497,463,431,399,368,339,310,
            282,255,228,203,178,154,130,107,85,63,41};
    static int peakInfluence = 86;
    static int effectiveTurn = 0;
    static int wavecount = 7;



    static void setup() throws GameActionException {
        turnCount = rc.getRoundNum();
        buildcooldown = Math.ceil(2/rc.sensePassability(rc.getLocation()));
    }

    static void run() throws GameActionException {
        HashSet<Integer> producedUnitsCopy = (HashSet<Integer>) producedUnitIDs.clone();
        //System.out.println("check 1: " + Clock.getBytecodeNum());
        for (Integer id : producedUnitsCopy) {
            if (rc.canGetFlag(id)) {
                int[] ownFlag = decodeFlag(rc.getFlag(rc.getID()));
                int[] flag = decodeFlag(rc.getFlag(id));
                if (flag[0] == NEUTRAL_EC_FOUND && (!attacking || (flag[1] == ownFlag[1] && flag[2] == ownFlag[2]))) {
                    tgtConviction = flag[3]; //check for switching attack target in this file
                    rc.setFlag(rc.getFlag(id));
                    attacking = true;
                    //some logic about spawning correct poli size
                } else if (flag[0] == ENEMY_EC_FOUND && (!attacking || (flag[1] == ownFlag[1] && flag[2] == ownFlag[2]))) {
                    rc.setFlag(rc.getFlag(id));
                    tgtConviction = flag[3];
                    attacking = true;
                    //some logic about spawning correct poli size
                } else if (flag[0] == SECURED_EC) {
                    if (ownFlag[1] == flag[1] && ownFlag[2] == flag[2]) {
                        rc.setFlag(encodeFlag(flag[0], flag[1], flag[2], flag[3]));
                        attacking = false;
                    }
                }
            } else {
                producedUnitIDs.remove(id);
            }
        }
        //System.out.println("check 2: " + Clock.getBytecodeNum());
        spawn();
        //System.out.println("check 3: " + Clock.getBytecodeNum());
        //only bidVote if we are not in overflow poli producing mode NVM FOR NOW
        bidVote();
        //System.out.println("check 4: " + Clock.getBytecodeNum());

    }

    static void spawn() throws GameActionException{
        double turnslost = numberofattackingunitsproduced * buildcooldown;
        Direction spawnDir = getOptimalSpawn();
        RobotType unitType = RobotType.POLITICIAN;
        int conviction = 0;
        if (rc.getEmpowerFactor(rc.getTeam(), 10) > 1.35) {
            if (rc.getInfluence() > 49) {
                if (rc.getInfluence() < 1000000) {
                    conviction = rc.getInfluence();
                } else {
                    conviction = rc.getInfluence() - 100000;
                }
                numberofunitsproduced++;
                numberofpoliticiansproduced++;
            }
        }

        //overflow
        else if (rc.getInfluence() >= 10000 && numberofunitsproduced % 3 == 0) {
            conviction = 26;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 3 == 1 || numberofunitsproduced % 3 == 2)) {
            conviction = 400;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        //defend

        else if (enemyRushDefense() && rc.getInfluence() >= 26) {
            conviction = 26;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }
        else if (enemyRushDefense()) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        //attack

        else if (numberofattackingunitsproduced < 50 && rc.getInfluence() >= 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            conviction = 400;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            numberofattackingunitsproduced++;
        }
        else if ((effectiveTurn % 7 == 0 || effectiveTurn % 7 == 3) && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 26) &&
                numberofattackingunitsproduced < 50 && rc.getInfluence() < 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            conviction = 26;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (safeToSpawnSlanderer() && numberofattackingunitsproduced < 50 && rc.getInfluence() < 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND
         && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), getOptimalSlandererVal())) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = getOptimalSlandererVal();
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (numberofattackingunitsproduced < 50 && rc.getInfluence() < 400 && decodeFlag(rc.getFlag(rc.getID()))[0] == ENEMY_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
            numberofattackingunitsproduced++;
        }

        //neutral

        else if (tgtConviction == 255 && rc.getInfluence() >= 511 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            conviction = 511;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            numberofattackingunitsproduced++;
        }

        else if (tgtConviction < 255 && rc.getInfluence() >= tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            conviction = tgtConviction + 11;
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            numberofattackingunitsproduced++;
        }

        else if ((effectiveTurn % 7 == 0 || effectiveTurn % 7 == 3) && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 26) &&
                rc.getInfluence() < tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            conviction = 26;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (safeToSpawnSlanderer() && rc.getInfluence() < tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND
        && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), getOptimalSlandererVal())) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = getOptimalSlandererVal();
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (rc.getInfluence() < tgtConviction + 11 && decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
            numberofattackingunitsproduced++;
        }

        //build 1
        else if (turnCount == 1) {
            unitType = RobotType.SLANDERER;
            conviction = 130;
            spawnDir = getOptimalSpawnSlanderer();
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }
        else if (turnCount == 3 || turnCount == 5) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if ((turnCount == 7)) {
            conviction = 23;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if ((effectiveTurn % 7 == 0 || effectiveTurn % 7 == 3) && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 26)) {
            conviction = 26;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (wavecount < 7) {
            if (rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 24)) {
                conviction = 24;
                numberofunitsproduced++;
                numberofpoliticiansproduced++;
                wavecount--;
            }
            else if (rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
                unitType = RobotType.MUCKRAKER;
                conviction = 1;
                numberofunitsproduced++;
                numberofmuckrakersproduced++;
            }
            if (wavecount == 0)
                wavecount = 7;
        }

        else if (shouldSpawnPoli() && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 24)) {
            conviction = 24;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
            wavecount = 6;
        }

        else if (safeToSpawnSlanderer() && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), getOptimalSlandererVal())) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = getOptimalSlandererVal();
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        /*
        else if (effectiveturn < 100 && numberofunitsproduced % 4 == 0) {
            conviction = 14;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 150 && numberofunitsproduced % 4 == 0) {
            conviction = 16;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 200 && numberofunitsproduced % 4 == 0) {
            conviction = 18;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 250 && numberofunitsproduced % 4 == 0) {
            conviction = 20;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 300 && numberofunitsproduced % 4 == 0) {
            conviction = 22;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 300 && (numberofunitsproduced % 4 == 1 || numberofunitsproduced % 4 == 3 || numberofunitsproduced % 4 == 0)) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn < 300 && safeToSpawnSlanderer() && numberofunitsproduced % 4 == 2) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = 100;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn < 300 && numberofunitsproduced % 4 == 2) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        //phase 2

        else if (effectiveturn >= 300 && effectiveturn <= 350 && numberofunitsproduced % 3 == 0) {
            conviction = 24;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 350 && effectiveturn <= 500 && numberofunitsproduced % 3 == 0) {
            conviction = 26;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 300 && effectiveturn <= 500 && numberofunitsproduced % 3 == 1) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn >= 300 && effectiveturn <= 500 && safeToSpawnSlanderer() && numberofunitsproduced % 3 == 2) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = 150;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn >= 300 && effectiveturn <= 500 && numberofunitsproduced % 3 == 2) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        //phase 3
        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofmuckrakersproduced/3 >= 26 && numberofunitsproduced % 3 == 0 ) {
            conviction = numberofmuckrakersproduced/3;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }


        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofmuckrakersproduced/3 <= 26 && numberofunitsproduced % 3 == 0) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofunitsproduced % 3 == 1) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn > 500 && effectiveturn < 1100 && safeToSpawnSlanderer() && rc.getInfluence() >= 200 && numberofunitsproduced % 3 == 2) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = 300;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofunitsproduced % 3 == 2) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        //phase 4
        else if (effectiveturn >= 1100 && safeToSpawnSlanderer() && numberofunitsproduced % 3 == 0 && rc.getInfluence() > 200) {
            unitType = RobotType.SLANDERER;
            spawnDir = getOptimalSpawnSlanderer();
            conviction = 500;
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 0 && rc.getInfluence() > 40 && rc.getInfluence() <= 200) {
            conviction = numberofmuckrakersproduced/2;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 0 && rc.getInfluence() <= 40) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 2) {
            conviction = numberofmuckrakersproduced/2;
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 1) {
            unitType = RobotType.MUCKRAKER;
            conviction = 1;
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        */

        if (rc.canBuildRobot(unitType, spawnDir, conviction)) {
            rc.buildRobot(unitType, spawnDir, conviction);
            effectiveTurn ++;
            if (unitType != RobotType.SLANDERER) {
                RobotInfo builtRobot = rc.senseRobotAtLocation(rc.getLocation().add(spawnDir));
                producedUnitIDs.add(builtRobot.getID());
            }
        }
    }

    static Boolean enemyRushDefense() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy);

        if (enemies.length > 0) {
            return true;
        } else {
            return false;
        }
    }


    static Boolean safeToSpawnSlanderer() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, friendly);
        ArrayList<RobotInfo> nearbypoliticians = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            int influence = robot.influence;
            if (type == RobotType.POLITICIAN && influence < 50) {
                nearbypoliticians.add(robot);
            }
        }
        ArrayList<RobotInfo> nearbyslanderers = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.SLANDERER) {
                nearbyslanderers.add(robot);
            }
        }

        if (nearbypoliticians.size() * 4 < nearbyslanderers.size()) {
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
        for (int i = 0; i < slanderervals.length; i++) {
            if (slanderervals[i] <= rc.getInfluence()) {
                return slanderervals[i];
            }
        }
        return 0;
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
}
