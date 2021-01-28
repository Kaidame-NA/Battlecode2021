package sprint2;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import sprint2.RobotPlayer;

import java.util.LinkedList;

public class Slanderer extends RobotPlayer {
    static int[] ECIDs = new int[20];
    static MapLocation[] ECLocations = new MapLocation[20];
    static int currentHomeEC = -1;
    static int homeECx;
    static int homeECy;
    static int[] homeECFlagContents;
    static MapLocation closestEnemyMuckPos;

    static void setup() throws GameActionException {
        turnCount = rc.getRoundNum();
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = possibleECs.length; --i >= 0;){
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
        if (homeECFlagContents != null) {
            if (homeECFlagContents[0] == 0 && homeECFlagContents[1] != 0) {
                closestEnemyMuckPos = new MapLocation(homeECx + homeECFlagContents[1], homeECy + homeECFlagContents[2]);
            }
        }
        if (ecinrange()) {
            tryMove(slanderersSafe());
        } else {
            tryMove(getPathDirTo(ECLocations[currentHomeEC]));
        }
    }
}

