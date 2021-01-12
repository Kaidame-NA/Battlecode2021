package testplayer;
import battlecode.common.*;

import java.util.HashSet;
import java.util.ArrayList;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static int turnCount;
    static boolean politicianCreated = false;
    static MapLocation startLocation;
    static int closestDistToTarget = 9999;
    static int movesSinceClosest = 0;

    //SIGNAL CODES
    static final int ENEMY_EC_FOUND = 1;
    static final int NEUTRAL_EC_FOUND = 2;
    static final int ATTACK_ENEMY = 3;
    static final int ATTACK_NEUTRAL = 4;
    static final int CONVERTED_FLAG = 5;
    static final int DEFEND = 6;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;
        startLocation = rc.getLocation();

        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        switch (rc.getType()) {
            case POLITICIAN: Politician.setup(); break;
            case SLANDERER: Slanderer.setup(); break;
            case MUCKRAKER: Muckraker.setup(); break;
        }

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
                    case POLITICIAN:           Politician.run();          break;
                    case SLANDERER:            Slanderer.run();           break;
                    case MUCKRAKER:            Muckraker.run();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */

    public static HashSet<MapLocation> banList = new HashSet(); //CREATE THE BANLIST
    static boolean tryMove(Direction dir) throws GameActionException {
        //System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            banList.add(rc.getLocation());
            return true;
        } else return false;
    }

    /**
     * Finds the optimal direction to move in to reach a target square.
     * Factors in distance between target square and the surrounding squares at the current location and the passability of surrounding squares.
     *
     * @param tgt The target location to move to
     * @return Direction that is most optimal to move in.
     * @throws GameActionException
     */

    static Direction getPathDirTo(MapLocation tgt) throws GameActionException {

        double distanceWeight = 1; // Change the multiplier for distance
        double passabilityWeight = 1; // Change the multiplier for passability

        if (rc.getLocation().equals(tgt)) {
            banList.clear();
            return Direction.CENTER;
        }
        Direction optimalDir = Direction.CENTER;
        double optimalCost = Double.MAX_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double pass = rc.sensePassability(adj);
                double cost = Math.pow((rc.getType().actionCooldown/pass), 2) * passabilityWeight + //rc.getCooldownTurns()
                        (Math.abs(tgt.x - adj.x) - Math.abs(tgt.x - rc.getLocation().x) +
                                Math.abs(tgt.y - adj.y) - Math.abs(tgt.y - rc.getLocation().y)) * distanceWeight;
                //System.out.println("Cost: " + cost);
                //System.out.println("Direction: " + dir);
                if (cost < optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        int localClosestDist = rc.adjacentLocation(optimalDir).distanceSquaredTo(tgt);
        if (localClosestDist < closestDistToTarget) {
            closestDistToTarget = localClosestDist;
            movesSinceClosest = 0;
        } else if (optimalDir != Direction.CENTER){
            movesSinceClosest ++;
        }
        if (movesSinceClosest > 6 && rc.canMove(rc.getLocation().directionTo(tgt))) {
            optimalDir = rc.getLocation().directionTo(tgt);
        }

        return optimalDir;
    }

    static int encodeFlag(int msg, int x, int y, int extraInfo) {
        String flag = padBinary(Integer.toBinaryString(msg), 3);
        if (x >= 0) {
            flag += "0" + padBinary(Integer.toBinaryString(x), 6);
        } else {
            flag += "1" + padBinary(Integer.toBinaryString(Math.abs(x)), 6);
        }
        if (y >= 0) {
            flag += "0" + padBinary(Integer.toBinaryString(y), 6);
        } else {
            flag += "1" + padBinary(Integer.toBinaryString(Math.abs(y)), 6);
        }
        flag += padBinary(Integer.toBinaryString(extraInfo), 7);
        return Integer.parseInt(flag, 2);
    }

    static int[] decodeFlag(int flag) {
        int[] flagContents = new int[4];
        String stringFlag = Integer.toBinaryString(flag);
        stringFlag = padBinary(stringFlag, 24);
        flagContents[0] = Integer.parseInt(stringFlag.substring(0, 3), 2);
        if (stringFlag.charAt(3) == '0') {
            flagContents[1] = Integer.parseInt(stringFlag.substring(4, 10), 2);
        } else {
            flagContents[1] = -Integer.parseInt(stringFlag.substring(4, 10), 2);
        }
        if (stringFlag.charAt(10) == '0') {
            flagContents[2] = Integer.parseInt(stringFlag.substring(11, 17), 2);
        } else {
            flagContents[2] = -Integer.parseInt(stringFlag.substring(11, 17), 2);
        }
        flagContents[3] = Integer.parseInt(stringFlag.substring(17), 2);
        return flagContents;
    }

    static String padBinary(String str, int tgtLength) {
        tgtLength -= str.length();
        String output = "";
        for(int i = tgtLength; --i >= 0;)
        {
            output+="0";
        }
        return output + str;
    }

    static Direction awayFromCreationEC() {
        MapLocation curr = rc.getLocation();
        //System.out.println(startLocation + " " + curr);
        return curr.directionTo(curr.subtract(curr.directionTo(startLocation)));
    }
    
    static boolean shouldSpread() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(25, friendly);
        if (friendlies.length != 0)
            return true;
        else {
            return false;
        }
    }
    
    static Direction getPathDirSpread() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(25, friendly);
        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }
        int numberofnearbyfriendlies = friendlies.length;

        Direction optimalDir = Direction.CENTER;
        double optimalCost = Double.MIN_VALUE;
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double pass = rc.sensePassability(adj);
                double cost = - (rc.getType().actionCooldown/pass);

                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost += Math.pow((Math.abs(spreadfromecone.x - adj.x) + Math.abs(spreadfromecone.y - adj.y)), 2);
                }
                if (numberofnearbyfriendlies > 0) {
                    MapLocation spreadfromone = friendlies[0].getLocation();
                    cost += Math.abs(spreadfromone.x - adj.x) + Math.abs(spreadfromone.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 2) {
                    MapLocation spreadfromtwo = friendlies[1].getLocation();
                    cost += Math.abs(spreadfromtwo.x - adj.x) + Math.abs(spreadfromtwo.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 3) {
                    MapLocation spreadfromthree = friendlies[2].getLocation();
                    cost += Math.abs(spreadfromthree.x - adj.x) + Math.abs(spreadfromthree.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 4) {
                    MapLocation spreadfromfour = friendlies[3].getLocation();
                    cost += Math.abs(spreadfromfour.x - adj.x) + Math.abs(spreadfromfour.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 5) {
                    MapLocation spreadfromfive = friendlies[4].getLocation();
                    cost += Math.abs(spreadfromfive.x - adj.x) + Math.abs(spreadfromfive.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 6) {
                    MapLocation spreadfromsix = friendlies[5].getLocation();
                    cost += Math.abs(spreadfromsix.x - adj.x) + Math.abs(spreadfromsix.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 7) {
                    MapLocation spreadfromseven = friendlies[6].getLocation();
                    cost += Math.abs(spreadfromseven.x - adj.x) + Math.abs(spreadfromseven.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 8) {
                    MapLocation spreadfromeight = friendlies[7].getLocation();
                    cost += Math.abs(spreadfromeight.x - adj.x) + Math.abs(spreadfromeight.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 9) {
                    MapLocation spreadfromnine = friendlies[8].getLocation();
                    cost += Math.abs(spreadfromnine.x - adj.x) + Math.abs(spreadfromnine.y - adj.y);
                }
                if (numberofnearbyfriendlies >= 10) {
                    MapLocation spreadfromten = friendlies[9].getLocation();
                    cost += Math.abs(spreadfromten.x - adj.x) + Math.abs(spreadfromten.y - adj.y);
                }
                    if (cost > optimalCost && rc.canMove(dir)) {
                        optimalDir = dir;
                        optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }
    
    static Direction getPathDirToEnemyEC(MapLocation tgt) throws GameActionException {

        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(30, friendly);
        ArrayList<RobotInfo> nearbypoliticians = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.POLITICIAN) {
                nearbypoliticians.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }

        if (rc.getLocation().equals(tgt)) {
            banList.clear();
            return Direction.CENTER;
        }
        Direction optimalDir = Direction.CENTER;
        double optimalCost = Double.MAX_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double pass = rc.sensePassability(adj);
                double cost = Math.pow((rc.getType().actionCooldown/pass), 2) +
                        (Math.abs(tgt.x - adj.x) + Math.abs(tgt.y - adj.y));

                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromecone.x - adj.x) + Math.abs(spreadfromecone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() != 0) {
                    MapLocation spreadfrompoliticianone = nearbypoliticians.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianone.x - adj.x) + Math.abs(spreadfrompoliticianone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 1) {
                    MapLocation spreadfrompoliticiantwo = nearbypoliticians.get(1).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticiantwo.x - adj.x) + Math.abs(spreadfrompoliticiantwo.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 2) {
                    MapLocation spreadfrompoliticianthree = nearbypoliticians.get(2).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianthree.x - adj.x) + Math.abs(spreadfrompoliticianthree.y - adj.y)), 3);
                }

                if (cost < optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        int localClosestDist = rc.adjacentLocation(optimalDir).distanceSquaredTo(tgt);
        if (localClosestDist < closestDistToTarget) {
            closestDistToTarget = localClosestDist;
            movesSinceClosest = 0;
        } else if (optimalDir != Direction.CENTER){
            movesSinceClosest ++;
        }
        if (movesSinceClosest > 6 && rc.canMove(rc.getLocation().directionTo(tgt))) {
            optimalDir = rc.getLocation().directionTo(tgt);
        }
        return optimalDir;
    }

    static Direction getAlternatePathDirToEnemyEC(MapLocation tgt) throws GameActionException {

        Team friendly = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] friendlies = rc.senseNearbyRobots(30, friendly);
        RobotInfo[] enemies = rc.senseNearbyRobots(30, enemy);

        ArrayList<RobotInfo> nearbypoliticians = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.POLITICIAN) {
                nearbypoliticians.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyenemyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : enemies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyenemyecs.add(robot);
            }
        }

        if (rc.getLocation().equals(tgt)) {
            banList.clear();
            return Direction.CENTER;
        }
        Direction optimalDir = Direction.CENTER;
        double optimalCost = Double.MAX_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double pass = rc.sensePassability(adj);
                double cost = Math.pow((rc.getType().actionCooldown/pass), 2) +
                        (Math.abs(tgt.x - adj.x) + Math.abs(tgt.y - adj.y));

                if (nearbyenemyecs.size() != 0) {
                    MapLocation spreadfromenemyecone = nearbyenemyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromenemyecone.x - adj.x) + Math.abs(spreadfromenemyecone.y - adj.y)), 5);
                }

                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromecone.x - adj.x) + Math.abs(spreadfromecone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() != 0) {
                    MapLocation spreadfrompoliticianone = nearbypoliticians.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianone.x - adj.x) + Math.abs(spreadfrompoliticianone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 1) {
                    MapLocation spreadfrompoliticiantwo = nearbypoliticians.get(1).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticiantwo.x - adj.x) + Math.abs(spreadfrompoliticiantwo.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 2) {
                    MapLocation spreadfrompoliticianthree = nearbypoliticians.get(2).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianthree.x - adj.x) + Math.abs(spreadfrompoliticianthree.y - adj.y)), 3);
                }

                if (cost < optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        int localClosestDist = rc.adjacentLocation(optimalDir).distanceSquaredTo(tgt);
        if (localClosestDist < closestDistToTarget) {
            closestDistToTarget = localClosestDist;
            movesSinceClosest = 0;
        } else if (optimalDir != Direction.CENTER){
            movesSinceClosest ++;
        }
        if (movesSinceClosest > 6 && rc.canMove(rc.getLocation().directionTo(tgt))) {
            optimalDir = rc.getLocation().directionTo(tgt);
        }
        return optimalDir;
    }

    static Direction getAlternatePathTwoDirToEnemyEC(MapLocation tgt) throws GameActionException {

        Team friendly = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] friendlies = rc.senseNearbyRobots(25, friendly);
        RobotInfo[] enemies = rc.senseNearbyRobots(25, enemy);

        ArrayList<RobotInfo> nearbypoliticians = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.POLITICIAN) {
                nearbypoliticians.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyenemyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : enemies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyenemyecs.add(robot);
            }
        }

        if (rc.getLocation().equals(tgt)) {
            banList.clear();
            return Direction.CENTER;
        }
        Direction optimalDir = Direction.CENTER;
        double optimalCost = Double.MAX_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double pass = rc.sensePassability(adj);
                double cost = Math.pow((rc.getType().actionCooldown/pass), 2) +
                        (Math.abs(tgt.x - adj.x) + Math.abs(tgt.y - adj.y));

                if (nearbyenemyecs.size() != 0) {
                    MapLocation spreadfromenemyecone = nearbyenemyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromenemyecone.x - adj.x) + Math.abs(spreadfromenemyecone.y - adj.y)), 5);
                }

                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromecone.x - adj.x) + Math.abs(spreadfromecone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() != 0) {
                    MapLocation spreadfrompoliticianone = nearbypoliticians.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianone.x - adj.x) + Math.abs(spreadfrompoliticianone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 1) {
                    MapLocation spreadfrompoliticiantwo = nearbypoliticians.get(1).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticiantwo.x - adj.x) + Math.abs(spreadfrompoliticiantwo.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 2) {
                    MapLocation spreadfrompoliticianthree = nearbypoliticians.get(2).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianthree.x - adj.x) + Math.abs(spreadfrompoliticianthree.y - adj.y)), 3);
                }

                if (cost < optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        int localClosestDist = rc.adjacentLocation(optimalDir).distanceSquaredTo(tgt);
        if (localClosestDist < closestDistToTarget) {
            closestDistToTarget = localClosestDist;
            movesSinceClosest = 0;
        } else if (optimalDir != Direction.CENTER){
            movesSinceClosest ++;
        }
        if (movesSinceClosest > 6 && rc.canMove(rc.getLocation().directionTo(tgt))) {
            optimalDir = rc.getLocation().directionTo(tgt);
        }
        return optimalDir;
    }

    static Direction getAlternatePathThreeDirToEnemyEC(MapLocation tgt) throws GameActionException {

        Team friendly = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] friendlies = rc.senseNearbyRobots(20, friendly);
        RobotInfo[] enemies = rc.senseNearbyRobots(20, enemy);

        ArrayList<RobotInfo> nearbypoliticians = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.POLITICIAN) {
                nearbypoliticians.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyenemyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : enemies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyenemyecs.add(robot);
            }
        }

        if (rc.getLocation().equals(tgt)) {
            banList.clear();
            return Direction.CENTER;
        }
        Direction optimalDir = Direction.CENTER;
        double optimalCost = Double.MAX_VALUE;
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj)) {
                double pass = rc.sensePassability(adj);
                double cost = Math.pow((rc.getType().actionCooldown/pass), 2) +
                        (Math.abs(tgt.x - adj.x) + Math.abs(tgt.y - adj.y));

                if (nearbyenemyecs.size() != 0) {
                    MapLocation spreadfromenemyecone = nearbyenemyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromenemyecone.x - adj.x) + Math.abs(spreadfromenemyecone.y - adj.y)), 5);
                }

                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfromecone.x - adj.x) + Math.abs(spreadfromecone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() != 0) {
                    MapLocation spreadfrompoliticianone = nearbypoliticians.get(0).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianone.x - adj.x) + Math.abs(spreadfrompoliticianone.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 1) {
                    MapLocation spreadfrompoliticiantwo = nearbypoliticians.get(1).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticiantwo.x - adj.x) + Math.abs(spreadfrompoliticiantwo.y - adj.y)), 3);
                }

                if (nearbypoliticians.size() > 2) {
                    MapLocation spreadfrompoliticianthree = nearbypoliticians.get(2).getLocation();
                    cost -= Math.pow((Math.abs(spreadfrompoliticianthree.x - adj.x) + Math.abs(spreadfrompoliticianthree.y - adj.y)), 3);
                }

                if (cost < optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        int localClosestDist = rc.adjacentLocation(optimalDir).distanceSquaredTo(tgt);
        if (localClosestDist < closestDistToTarget) {
            closestDistToTarget = localClosestDist;
            movesSinceClosest = 0;
        } else if (optimalDir != Direction.CENTER){
            movesSinceClosest ++;
        }
        if (movesSinceClosest > 6 && rc.canMove(rc.getLocation().directionTo(tgt))) {
            optimalDir = rc.getLocation().directionTo(tgt);
        }
        return optimalDir;
    }

    //returns the direction to move away from a location
    static Direction awayFromLocation(MapLocation loc) throws GameActionException {
        MapLocation curr = rc.getLocation();
        System.out.println("Away From Location: " + loc + " Current Location: " + curr);
        return curr.directionTo(curr.subtract(curr.directionTo(loc)));
    }

    //runs the direction to run away from the first enemy seen; otherwise return randomdir (for now)
    static Direction awayFromEnemies() throws GameActionException {
        RobotInfo[] Enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
        for(RobotInfo info: Enemies){
            return awayFromLocation(info.location);
        }
        return randomDirection();
    }
}
