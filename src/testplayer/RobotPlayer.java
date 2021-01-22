package testplayer;
import battlecode.common.*;

import java.util.*;

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
    static final int SECURED_EC = 3;
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
            case ENLIGHTENMENT_CENTER: EnlightenmentCenter.setup(); break;
        }
        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
/*
            if(turnCount >= 500)
            {
                rc.resign();
            }


*/

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
            turnCount += 1;
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
        String flag = padBinary(Integer.toBinaryString(msg), 2);
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
        flag += padBinary(Integer.toBinaryString(extraInfo), 8);
        return Integer.parseInt(flag, 2);
    }

    static int[] decodeFlag(int flag) {
        int[] flagContents = new int[4];
        String stringFlag = Integer.toBinaryString(flag);
        stringFlag = padBinary(stringFlag, 24);
        flagContents[0] = Integer.parseInt(stringFlag.substring(0, 2), 2);
        if (stringFlag.charAt(2) == '0') {
            flagContents[1] = Integer.parseInt(stringFlag.substring(3, 9), 2);
        } else {
            flagContents[1] = -Integer.parseInt(stringFlag.substring(3, 9), 2);
        }
        if (stringFlag.charAt(9) == '0') {
            flagContents[2] = Integer.parseInt(stringFlag.substring(10, 16), 2);
        } else {
            flagContents[2] = -Integer.parseInt(stringFlag.substring(10, 16), 2);
        }
        flagContents[3] = Integer.parseInt(stringFlag.substring(16), 2);
        return flagContents;
    }

    static String padBinary(String str, int tgtLength) {
        return new String(new char[tgtLength - str.length()]).replace('\0', '0') + str;
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
        /*
        ArrayList<RobotInfo> nearbyfriendlies = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            int id = robot.getID();
            if (type == RobotType.MUCKRAKER || type == RobotType.POLITICIAN) {
                nearbyfriendlies.add(robot);
            }
        }
         */
        int numberofnearbyfriendlies = friendlies.length;

        numberofnearbyfriendlies = friendlies.length > 10 ? 10 : numberofnearbyfriendlies; // cap at 10

        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                double cost = - (rc.getType().actionCooldown/pass);
                /*
                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost += (Math.pow(spreadfromecone.x - adj.x, 2) + Math.pow(spreadfromecone.y - adj.y, 2));
                }
                 */
                for(int i = numberofnearbyfriendlies; --i>=0;)
                {
                    MapLocation spreadFrom = friendlies[i].getLocation();
                    cost += (Math.pow(spreadFrom.x - adj.x, 2) + Math.pow(spreadFrom.y - adj.y, 2));
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
        //System.out.println("Away From Location: " + loc + " Current Location: " + curr);
        return curr.directionTo(curr.subtract(curr.directionTo(loc)));
    }

    //runs the direction to run away from the first enemy seen; otherwise return randomdir (for now)
    static Direction awayFromEnemies() throws GameActionException {
        RobotInfo[] Enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
        for(RobotInfo info: Enemies){
            return optimalDirection( awayFromLocation(info.location) );
        }
        return randomDirection();//eventually change to "normal" slanderer or bot movement
    }

    static Direction optimalDirection(Direction dir) throws GameActionException{
        SortedMap<Double, Direction> directions
                = new TreeMap<Double, Direction>(Collections.reverseOrder()); //sorted high to low passability

        if(rc.canSenseLocation( rc.getLocation().add(dir) ) )
            directions.put(rc.sensePassability( rc.getLocation().add(dir) ), dir);
        if(rc.canSenseLocation( rc.getLocation().add(dir.rotateLeft()) ) )
            directions.put(rc.sensePassability( rc.getLocation().add(dir.rotateLeft()) ), dir.rotateLeft());
        if(rc.canSenseLocation( rc.getLocation().add(dir.rotateRight()) ) )
            directions.put(rc.sensePassability( rc.getLocation().add(dir.rotateRight()) ), dir.rotateRight());

        for (Map.Entry mapElement : directions.entrySet() ) {
            //Getting the Key
            //double key = (double)mapElement.getKey();

            // Finding the value
            Direction value = (Direction) mapElement.getValue();

            if(rc.canMove(value) ){
                return value;
            }
        }
        return Direction.CENTER;
    }

    static Direction slanderersSafe() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(20, friendly);
        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbypolis = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.POLITICIAN) {
                nearbypolis.add(robot);
            }
        }

        int numberofnearbypolis = nearbypolis.size();

        numberofnearbypolis = nearbypolis.size() > 10 ? 10 : numberofnearbypolis; // cap at 10

        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                //double cost = - (rc.getType().actionCooldown/pass);
                double cost = 0;
                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost -= Math.abs(2.25 - Math.sqrt(Math.pow(spreadfromecone.x - adj.x, 2) + Math.pow(spreadfromecone.y - adj.y, 2)));
                }
                //technically this code below shouldnt ever be run, but its here just in case
                if (nearbyecs.size() == 0) {
                    for(int i = numberofnearbypolis; --i>=0;)
                    {
                        MapLocation spreadTo = nearbypolis.get(i).getLocation();
                        cost += Math.sqrt(Math.pow(spreadTo.x - adj.x, 2) + Math.pow(spreadTo.y - adj.y, 2));
                    }
                    cost += (rc.getType().actionCooldown/pass);
                }


                if (cost > optimalCost && rc.canMove(dir)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        };
        return optimalDir;
    }

    static Direction polisring() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(25, friendly);
        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }

        ArrayList<RobotInfo> nearbyslanderers = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.SLANDERER) {
                nearbyslanderers.add(robot);
            }
        }
        int numberofnearbyslanderers = nearbyslanderers.size();

        numberofnearbyslanderers = nearbyslanderers.size() > 10 ? 10 : numberofnearbyslanderers; // cap at 10

        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                //double cost = - (rc.getType().actionCooldown/pass);
                double cost = 0;

                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost -= Math.abs(4.75 - Math.sqrt(Math.pow(spreadfromecone.x - adj.x, 2) + Math.pow(spreadfromecone.y - adj.y, 2)));
                }
                /*
                for(int i = numberofnearbyslanderers; --i>=0;)
                {
                    MapLocation spreadTo = nearbyslanderers.get(i).getLocation();
                    cost += .1 * Math.sqrt(Math.pow(spreadTo.x - adj.x, 2) + Math.pow(spreadTo.y - adj.y, 2));
                }
                 */
                if (cost > optimalCost && rc.canMove(dir)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        if (nearbyecs.size() == 0) { // && numberofnearbyslanderers ==0 if adding commented stuff above
            optimalDir = getPathDirSpread();
        }
        return optimalDir;
    }

    static boolean ecinrange() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, friendly);
        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }
        if (nearbyecs.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
}