package testplayer;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import java.util.LinkedList;

public class Slanderer extends RobotPlayer {
    static LinkedList<Integer> ECIDs = new LinkedList<Integer>();
    static LinkedList<MapLocation> ECLocations = new LinkedList<MapLocation>();

    static void setup() throws GameActionException {
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = possibleECs.length; --i >= 0;) {
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECIDs.add(possibleECs[i].getID());
                ECLocations.add(possibleECs[i].getLocation());
            }
        }
    }

    static void run() throws GameActionException {
        if (tryMove(randomDirection())) {
            //System.out.println("I moved!");
        }
    }
}
