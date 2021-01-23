package testplayer;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class Slanderer extends RobotPlayer {
    static int[] ECIDs = new int[20];
    static MapLocation[] ECLocations = new MapLocation[20];
    static int currentHomeEC = -1;
    static int homeECx;
    static int homeECy;
    static int[] homeECFlagContents;
    static MapLocation enemyEC;
    static MapLocation lastScoutedEnemyMuck;

    static void setup() throws GameActionException {
        turnCount = rc.getRoundNum();
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = possibleECs.length; --i >= 0;) {
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                currentHomeEC ++;
                ECIDs[currentHomeEC] = possibleECs[i].getID();
                ECLocations[currentHomeEC] = possibleECs[i].getLocation();
            }
        }
        homeECx = ECLocations[currentHomeEC].x;
        homeECy = ECLocations[currentHomeEC].y;
    }

    static void run() throws GameActionException {
        if (currentHomeEC != -1) {
            if (rc.canGetFlag(ECIDs[currentHomeEC])) {
                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs[currentHomeEC]));
            }
        }
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        for (int i = 0; i < friendlyInRange.length; i ++) {
            RobotInfo unit = friendlyInRange[i];
            if (rc.canGetFlag(unit.getID())) {
                int flag = rc.getFlag(unit.getID());
                int[] decoded = decodeFlag(flag);
                if (decoded[0] == 0 && decoded[1] != 0) {
                    lastScoutedEnemyMuck = new MapLocation(decoded[1] + homeECx, decoded[2] + homeECy);
                    rc.setFlag(flag);
                    break;
                }
            }
        }
        if (homeECFlagContents[0] == ENEMY_EC_FOUND) {
            enemyEC = new MapLocation(homeECx + homeECFlagContents[1], homeECy + homeECFlagContents[2]);
        }
        if (ecinrange()) {
            tryMove(slanderersSafev2());
        } else {
            tryMove(getPathDirTo(ECLocations[currentHomeEC]));
        }

        enemyEC = null;
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

    static Direction slanderersSafev2() throws GameActionException {
        MapLocation home = ECLocations[currentHomeEC];
        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;

        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                //double cost = - (rc.getType().actionCooldown/pass);
                double cost = 0;
                double radius = 2.25;
                //double distancefromhomeeq = Math.abs(radius - Math.sqrt(Math.pow(home.x - adj.x, 2) + Math.pow(home.y - adj.y, 2)));
                cost -= Math.abs(radius - Math.sqrt(Math.pow(home.x - adj.x, 2) + Math.pow(home.y - adj.y, 2)));
                cost += radius * pass;
                if (lastScoutedEnemyMuck != null) {
                    cost += Math.abs(Math.sqrt(Math.pow(lastScoutedEnemyMuck.x - adj.x, 2) + Math.pow(lastScoutedEnemyMuck.y - adj.y, 2)));
                } else if (!(enemyEC == null)) {
                    cost += Math.abs(Math.sqrt(Math.pow(enemyEC.x - adj.x, 2) + Math.pow(enemyEC.y - adj.y, 2)));
                }
                if (cost > optimalCost && rc.canMove(dir)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }
}

