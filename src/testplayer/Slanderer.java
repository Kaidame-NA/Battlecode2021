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
        if (homeECFlagContents[0] == ENEMY_EC_FOUND) {
            enemyEC = new MapLocation(homeECFlagContents[1], homeECFlagContents[2]);
        }
        if (ecinrange()) {
            tryMove(slanderersSafe());
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
}

