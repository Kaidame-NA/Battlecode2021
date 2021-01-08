package testplayer;
import battlecode.common.*;

import java.util.HashSet;

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

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
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
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
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
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
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
                System.out.println("Cost: " + cost);
                System.out.println("Direction: " + dir);
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
        if (movesSinceClosest > 6) {
            optimalDir = rc.getLocation().directionTo(tgt);
        }

        return optimalDir;
    }

    static int encodeFlag(int msg, int x, int y, int extraInfo) {
        String flag = padBinary(Integer.toBinaryString(msg), 4);
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
        flag += padBinary(Integer.toBinaryString(extraInfo), 6);
        return Integer.parseInt(flag, 2);
    }

    static int[] decodeFlag(int flag) {
        int[] flagContents = new int[4];
        String stringFlag = Integer.toBinaryString(flag);
        stringFlag = padBinary(stringFlag, 24);
        flagContents[0] = Integer.parseInt(stringFlag.substring(0, 4), 2);
        if (stringFlag.charAt(4) == '0') {
            flagContents[1] = Integer.parseInt(stringFlag.substring(5, 11), 2);
        } else {
            flagContents[1] = -Integer.parseInt(stringFlag.substring(5, 11), 2);
        }
        if (stringFlag.charAt(11) == '0') {
            flagContents[2] = Integer.parseInt(stringFlag.substring(12, 18), 2);
        } else {
            flagContents[2] = -Integer.parseInt(stringFlag.substring(12, 18), 2);
        }
        flagContents[3] = Integer.parseInt(stringFlag.substring(18), 2);
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
}
