package testplayer;

import battlecode.common.*;

import java.util.LinkedList;

public class Politician extends RobotPlayer{
    static LinkedList<Integer> ECIDs = new LinkedList<Integer>();
    static LinkedList<MapLocation> ECLocations = new LinkedList<MapLocation>();
    static boolean converted;

    static void setup() throws GameActionException {
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = 0; ++i < possibleECs.length;) {
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECIDs.add(possibleECs[i].getID());
                ECLocations.add(possibleECs[i].getLocation());
            }
        }
        if (ECIDs.isEmpty() && ECLocations.isEmpty()) {
            converted = true;
        }
    }

    static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection())) {
            //System.out.println("I moved!");
        }
    }
}
