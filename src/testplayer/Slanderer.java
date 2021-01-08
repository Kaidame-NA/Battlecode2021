package testplayer;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import java.util.LinkedList;

public class Slanderer extends RobotPlayer {
    static LinkedList<Integer> ECIDs;
    static LinkedList<MapLocation> ECLocations;

    static void setup() throws GameActionException {
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = 0; ++i < possibleECs.length;) {
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECIDs.add(possibleECs[i].getID());
                ECLocations.add(possibleECs[i].getLocation());
            }
        }
    }

    static void run() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
}
