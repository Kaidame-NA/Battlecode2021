package testplayer;

import battlecode.common.*;

import java.util.ArrayList;

public class EnlightenmentCenter extends RobotPlayer{


    static int numberofunits = rc.getRobotCount();
    static int numberofunitsproduced, numberofmuckrakersproduced,
    numberofslanderersproduced, numberofpoliticiansproduced, numberofattackingunitsproduced = 0;
    static double buildcooldown;
    static int ecIDTag = rc.getID() % 128;

    static void setup() throws GameActionException {
        turnCount = rc.getRoundNum();
        buildcooldown = Math.ceil(2/rc.sensePassability(rc.getLocation()));
    }

    static void run() throws GameActionException {
        spawn();
        //System.out.println(rc.getInfluence());

        //only bidVote if we are not in overflow poli producing mode
        if( rc.getEmpowerFactor(rc.getTeam(), 10) < 1.3 ) bidVote();
        
        RobotInfo[] nearbyUnits = rc.senseNearbyRobots();
        for (int i = nearbyUnits.length; --i >= 0;) {
            if (nearbyUnits[i].getTeam() == rc.getTeam()) {
                if (rc.canGetFlag(nearbyUnits[i].getID())) {
                    int[] flagContents = decodeFlag(rc.getFlag(nearbyUnits[i].getID()));
                    if (flagContents[0] == ENEMY_EC_FOUND) {
                        rc.setFlag(encodeFlag(ATTACK_ENEMY, flagContents[1], flagContents[2], ecIDTag));
                    } else if (flagContents[0] == NEUTRAL_EC_FOUND) {
                        rc.setFlag(encodeFlag(ATTACK_NEUTRAL, flagContents[1], flagContents[2], ecIDTag));
                    }
                }
            }
        }
    }

    static void spawn() throws GameActionException{
        double turnslost = numberofattackingunitsproduced * buildcooldown;
        double effectiveturn = turnCount - turnslost;
        if (rc.getEmpowerFactor(rc.getTeam(), 10) > 1.3
                && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), rc.getInfluence())) {
            if (rc.getInfluence() > 49) {
                rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), rc.getInfluence());
                numberofunitsproduced++;
                numberofpoliticiansproduced++;
            }
        }

        //overflow
        else if (rc.getInfluence() >= 10000 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 26)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 26);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (rc.getInfluence() >= 10000 && (numberofunitsproduced % 3 == 1 || numberofunitsproduced % 3 == 2) && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 600)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 600);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        //phase 1
        else if (turnCount == 1 && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 150)) {
            rc.buildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 150);
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (turnCount <= 12 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofmuckrakersproduced++;
            numberofunitsproduced++;
        }

        else if (numberofattackingunitsproduced < 50 && rc.getInfluence() >= 500 && decodeFlag(rc.getFlag(rc.getID()))[0] == ATTACK_ENEMY &&
                rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 500)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 500);
            numberofpoliticiansproduced++;
            numberofunitsproduced++;
            numberofattackingunitsproduced++;
        }

        else if (numberofattackingunitsproduced < 50 && rc.getInfluence() < 500 && decodeFlag(rc.getFlag(rc.getID()))[0] == ATTACK_ENEMY &&
                rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
            numberofattackingunitsproduced++;
        }

        else if (effectiveturn < 150 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 14)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 14);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 300 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 16)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 16);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn < 300 && numberofunitsproduced % 3 == 1 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn < 300 && safeToSpawnSlanderer() && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 100)) {
            rc.buildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 100);
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn < 300 && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        //phase 2

        else if (effectiveturn >= 300 && effectiveturn <= 350 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 18)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 18);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 350 && effectiveturn <= 400 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 20)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 20);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 400 && effectiveturn <= 450 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 22)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 22);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 450 && effectiveturn <= 500 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 24)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), 24);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 300 && effectiveturn <= 500 && numberofunitsproduced % 3 == 1 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn >= 300 && effectiveturn <= 500 && safeToSpawnSlanderer() && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 150)) {
            rc.buildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 150);
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn >= 300 && effectiveturn <= 500 && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        //phase 3
        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofmuckrakersproduced/3 >= 26 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), numberofmuckrakersproduced/3)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), numberofmuckrakersproduced/3);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }


        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofmuckrakersproduced/3 <= 26 && numberofunitsproduced % 3 == 0 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofunitsproduced % 3 == 1 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn > 500 && effectiveturn < 1100 && safeToSpawnSlanderer() && rc.getInfluence() >= 200 && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 300)) {
            rc.buildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 300);
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn > 500 && effectiveturn < 1100 && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
        //phase 4
        else if (effectiveturn >= 1100 && safeToSpawnSlanderer() && numberofunitsproduced % 3 == 0 && rc.getInfluence() > 200 && rc.canBuildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 500)) {
            rc.buildRobot(RobotType.SLANDERER, getOptimalSpawnSlanderer(), 500);
            numberofunitsproduced++;
            numberofslanderersproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 0 && rc.getInfluence() > 40 && rc.getInfluence() <= 200 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), numberofmuckrakersproduced/2)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), numberofmuckrakersproduced/2);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 0 && rc.getInfluence() <= 40 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 2 && rc.canBuildRobot(RobotType.POLITICIAN, getOptimalSpawn(), numberofmuckrakersproduced/2)) {
            rc.buildRobot(RobotType.POLITICIAN, getOptimalSpawn(), numberofmuckrakersproduced/2);
            numberofunitsproduced++;
            numberofpoliticiansproduced++;
        }

        else if (effectiveturn >= 1100 && numberofunitsproduced % 3 == 1 && rc.canBuildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, getOptimalSpawn(), 1);
            numberofunitsproduced++;
            numberofmuckrakersproduced++;
        }
    }

    static Boolean safeToSpawnSlanderer() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, friendly);
        ArrayList<RobotInfo> nearbypoliticians = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.POLITICIAN) {
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

        if (nearbypoliticians.size() > 3 * nearbyslanderers.size()) {
            return true;
        } else {
            return false;
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

    static int friendlyVotes, prevBid, winStreak, loseStreak = 0;
    //bids for the ec
    static void bidVote() throws GameActionException{
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


        if(friendlyVotes > 750)// || round-friendlyVotes > 1500)//if either we or enemy have already won the bidding game? unless ties...
        {
            //System.out.println("The election has already been decided.");
        }
        else if(winStreak == 0){ // we lost the last vote...
            double iCoef = 1.97+rc.getInfluence()/40000; //ramp harder if we have more influence

            newBid = prevBid + (int) Math.ceil(Math.pow( (1/iCoef),(-loseStreak+1) ));
            //System.out.println("loseStreak: " + loseStreak + " prevBid:  " + prevBid + " newBid: " + newBid);
            //increasing doubley (loseStreak is increasing while prevBid is increasing)

            int threshold = rc.getInfluence()/5; //our maximum we are willing to bid
            if(newBid < 1 && rc.canBid(1)){
                rc.bid(1);
                prevBid = 1;
                //System.out.println("Lost last vote, newBid<1 so lets bid the minimum, 1");
            }
            else if(newBid < threshold && rc.canBid(newBid)){
                //dont want to be bankrupting ourselves so we have a threshold (max value we are willing to bid)

                rc.bid(newBid);
                prevBid = newBid;
                //System.out.println("Last vote lost, and we are less than the threshold, bid: " + newBid);
            }
            else if(newBid >= threshold && threshold>prevBid && rc.canBid(threshold)){
                //bid the max we are willing to, also if its greater than our last bid
                newBid = threshold;
                rc.bid(newBid);
                prevBid = newBid;
                //System.out.println("Last vote lost, and we are greater than the threshold, bid: " + newBid);
            }
            else{
                //System.out.println("We lost the last vote, but we arent willing to bid more than last time so we bid 0");
            }
        }
        else{// we won the last vote!
            double dCoef = 1.7; //changeable

            newBid = (int) Math.ceil(-1*Math.pow( (1/dCoef),(-winStreak+2) ) + prevBid);
            //System.out.println("winStreak: " + winStreak + " prevBid:  " + prevBid + " newBid: " + newBid);
            //decreasing doubley (winStreak is increasing while prevBid is decreasing)

            if(newBid < 1 && rc.canBid(1)){
                rc.bid(1);
                prevBid = 1;
                //System.out.println("Won last vote, newBid<1 so lets bid the minimum, 1");
            }
            else if(rc.canBid(newBid)){
                rc.bid(newBid);
                prevBid = newBid;
                //System.out.println("Won last vote, lets bid " + newBid);
            }

        }

    }
}
