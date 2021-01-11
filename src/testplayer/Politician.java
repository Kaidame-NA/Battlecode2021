package testplayer;

import battlecode.common.*;

import java.util.LinkedList;
import java.util.Map;

public class Politician extends RobotPlayer{
    static LinkedList<Integer> ECIDs = new LinkedList<Integer>();
    static LinkedList<MapLocation> ECLocations = new LinkedList<MapLocation>();
    static final int SCOUTING = 0;
    static final int ATTACKING = 1;
    static final int RETURNING = 2;
    static final int CONVERTED = 3;
    static int role;
    static MapLocation target;
    static int[] homeECFlagContents;
    static int homeECx;
    static int homeECy;

    static void setup() throws GameActionException {
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = possibleECs.length; --i >= 0;) {
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECIDs.add(possibleECs[i].getID());
                ECLocations.add(possibleECs[i].getLocation());
            }
        }
        if (ECIDs.isEmpty() && ECLocations.isEmpty()) {
            role = CONVERTED;
            rc.setFlag(encodeFlag(CONVERTED_FLAG, 0, 0, 0));
        } else {
            homeECx = ECLocations.get(0).x;
            homeECy = ECLocations.get(0).y;
            role = SCOUTING;
        }
    }

    static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        if (role != CONVERTED && !ECIDs.isEmpty()) {
            if (rc.canGetFlag(ECIDs.get(0))) {
                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs.get(0)));
            } //maybe something about if cant then its taken
        }
        if (role == SCOUTING) {
            //go to point along direction of creation
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            for (int i = unitsInRange.length; --i >= 0;) {
                RobotInfo unit = unitsInRange[i];
                if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == enemy) {
                    rc.setFlag(encodeFlag(ENEMY_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, 0));
                    role = RETURNING;
                }
                else if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == Team.NEUTRAL) {
                    rc.setFlag(encodeFlag(NEUTRAL_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, 0));
                    role = RETURNING;
                }
            }
            if (rc.canMove(getPathDirSpread()) && shouldSpread()) {
                rc.move(getPathDirSpread());
            }
        } else if (role == ATTACKING) {
            //only attacks target location atm, no reaction to other units on the way
            if (rc.canSenseLocation(target)) {
                RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
                for (int i = unitsInRange.length; --i >= 0;) {
                    RobotInfo unit = unitsInRange[i];
                    if (unit.type == RobotType.ENLIGHTENMENT_CENTER && unit.team == rc.getTeam()) {
                        ECIDs.addFirst(unit.getID());
                        ECLocations.addFirst(target);
                        homeECx = ECLocations.get(0).x;
                        homeECy = ECLocations.get(0).y;
                        role = SCOUTING;
                    }
                }
            }
            if (rc.getLocation().distanceSquaredTo(target) < actionRadius && rc.getConviction() > 10
            && rc.canEmpower(rc.getLocation().distanceSquaredTo(target))) {
                rc.empower(rc.getLocation().distanceSquaredTo(target));
            }
            tryMove(getPathDirTo(target));
        } else if (role == RETURNING) {
            target = ECLocations.get(0);
            tryMove(getPathDirTo(target));
            for (int i = friendlyInRange.length; --i >= 0;) {
                //otherside of relay, if you are delivering and farther away, let closer bot assume info and you
                //take its flag and commands(unless its an attack)
                if (friendlyInRange[i].getLocation().distanceSquaredTo(target)
                        < rc.getLocation().distanceSquaredTo(target) && rc.canGetFlag(friendlyInRange[i].getID())) {
                    int flag = rc.getFlag(friendlyInRange[i].getID());
                    int[] flagContents = decodeFlag(flag);
                    if (flagContents[0] != CONVERTED_FLAG && friendlyInRange[i].getType() != RobotType.SLANDERER) {
                        rc.setFlag(flag);
                    }
                    //depending on signal code, change role and target
                }
            }
        } else if (role == CONVERTED) { //right now they just run around and kamikaze
            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
            }
            tryMove(randomDirection());
        }
        //relay, mobile robot/robot comms
        if (role != RETURNING && role != CONVERTED) {
            //if you are not returning info and a friendly is near with flag
            for (int i = friendlyInRange.length; --i >= 0; ) {
                if (rc.canGetFlag(friendlyInRange[i].getID())) {
                    int flag = rc.getFlag(friendlyInRange[i].getID());
                    int[] flagContents = decodeFlag(flag);
                    //relay info if you are closer to home ec
                    if (!ECLocations.isEmpty()
                            && friendlyInRange[i].getLocation().distanceSquaredTo(ECLocations.get(0))
                            > rc.getLocation().distanceSquaredTo(ECLocations.get(0)) &&
                            (flagContents[0] == ENEMY_EC_FOUND || flagContents[0] == NEUTRAL_EC_FOUND) &&
                            friendlyInRange[i].getType() == RobotType.POLITICIAN) {
                        rc.setFlag(flag);
                        target = ECLocations.get(0);
                        role = RETURNING;
                    }  //otherwise if its an attack command go attack
                    /*
                    else if (flagContents[0] == ATTACK_ENEMY) {
                        rc.setFlag(flag);
                        target = new MapLocation(homeECx + flagContents[1],
                                homeECy + flagContents[2]);
                        role = ATTACKING;
                    }*/
                }
            }
            if (attackable.length != 0 && rc.canEmpower(actionRadius) && rc.getInfluence() < 27) {
                rc.empower(actionRadius);
            }
        }
        //reading home ec flag info
        if (homeECFlagContents != null) {
            //if its an attack command, attack
            if (homeECFlagContents[0] == ATTACK_ENEMY) {
                rc.setFlag(rc.getFlag(ECIDs.get(0)));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            }
        }
    }

}
