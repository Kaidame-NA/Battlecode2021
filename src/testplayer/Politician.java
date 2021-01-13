package testplayer;

import battlecode.common.*;

import java.awt.*;
import java.util.LinkedList;
import java.util.Map;

public class Politician extends RobotPlayer{
    static LinkedList<Integer> ECIDs = new LinkedList<Integer>();
    static LinkedList<MapLocation> ECLocations = new LinkedList<MapLocation>();
    static final int SCOUTING = 0;
    static final int ATTACKING = 1;
    static final int RETURNING = 2;
    static final int CONVERTED = 3;
    static final int FOLLOW = 4;
    static int role;
    static MapLocation target;
    static int[] homeECFlagContents;
    static int homeECx;
    static int homeECy;
    static int homeECIDTag;
    static int scoutingFlag;
    static int muckrakersInRange;
    static int trailedMuckrakerID;

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
            homeECIDTag = ECIDs.get(0) % 128;
            scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
            rc.setFlag(scoutingFlag);
            role = SCOUTING;
            if (rc.getEmpowerFactor(rc.getTeam(), 0) > 1.3 && rc.getConviction() > 200
                && rc.canEmpower(2)) {
                rc.empower(2);
            }
        }
    }

    static void run() throws GameActionException {
        if (ECIDs.isEmpty() && ECLocations.isEmpty()) {
            role = CONVERTED;
            rc.setFlag(encodeFlag(CONVERTED_FLAG, 0, 0, 0));
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy);
        muckrakersInRange = 0;
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        if (role != CONVERTED) {
            if (rc.canGetFlag(ECIDs.get(0))) {
                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs.get(0)));
            } //maybe something about if cant then its taken
        }
        //follow muckrakers if small poli
        for (int i = enemiesInRange.length; --i >= 0;) {
            if (enemiesInRange[i].getType() == RobotType.MUCKRAKER && rc.getConviction() < 30
                && !ECLocations.isEmpty() && rc.getLocation().distanceSquaredTo(ECLocations.get(0)) < 1000) {
                if (trailedMuckrakerID == 0 && role != RETURNING && notTrailed(trailedMuckrakerID, friendlyInRange)) {
                    trailedMuckrakerID = enemiesInRange[i].getID();
                    role = FOLLOW;
                    rc.setFlag(0);
                }
            }
        }
        for (int i = attackable.length; --i >= 0;) {
            if (attackable[i].getType() == RobotType.MUCKRAKER) {
                muckrakersInRange ++;
            }
        }

        if (role == SCOUTING) {
            //go to point along direction of creation
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            for (int i = unitsInRange.length; --i >= 0;) {
                RobotInfo unit = unitsInRange[i];
                if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == enemy) {
                    rc.setFlag(encodeFlag(ENEMY_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, homeECIDTag));
                    role = RETURNING;
                }
                else if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == Team.NEUTRAL) {
                    if (rc.getConviction()*rc.getEmpowerFactor(rc.getTeam(), 0) - 10 > unit.getConviction()) {
                        target = unit.getLocation();
                        role = ATTACKING;
                    }
                    rc.setFlag(encodeFlag(NEUTRAL_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, homeECIDTag));
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
                    if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == rc.getTeam()
                        && unit.getLocation().equals(target)) {
                        ECIDs.addFirst(unit.getID());
                        ECLocations.addFirst(target);
                        homeECx = ECLocations.get(0).x;
                        homeECy = ECLocations.get(0).y;
                        homeECIDTag = ECIDs.get(0) % 128;
                        role = SCOUTING;
                        scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
                        rc.setFlag(scoutingFlag);
                        if (role != CONVERTED) {
                            if (rc.canGetFlag(ECIDs.get(0))) {
                                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs.get(0)));
                            } //maybe something about if cant then its taken
                        }
                    }
                }
            }
            if (rc.getLocation().distanceSquaredTo(target) < actionRadius
                    && rc.getConviction()*rc.getEmpowerFactor(rc.getTeam(), 0) > 10
            && rc.canEmpower(rc.getLocation().distanceSquaredTo(target)) && role == ATTACKING) {
                if (rc.getLocation().distanceSquaredTo(target) <= 2
                        || (rc.getCooldownTurns() < 1 && !rc.canMove(rc.getLocation().directionTo(target))
                        && movesSinceClosest > 4)) {
                    rc.empower(rc.getLocation().distanceSquaredTo(target));
                }
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
                    if (flagContents[0] != CONVERTED_FLAG && friendlyInRange[i].getType() != RobotType.SLANDERER
                        && flagContents[3] == homeECIDTag) {
                        if (flag == scoutingFlag) {
                            role = SCOUTING;
                            rc.setFlag(flag);
                        } else if (flagContents[0] == ATTACK_ENEMY && rc.getConviction() > 110) {
                            target = new MapLocation(homeECx + flagContents[1],
                                    homeECy + flagContents[2]);
                            rc.setFlag(flag);
                            role = ATTACKING;
                        } else if (flag == rc.getFlag(rc.getID())) {
                            role = SCOUTING;
                            rc.setFlag(scoutingFlag);
                        }
                    }
                    //depending on signal code, change role and target
                }
            }
            if (rc.canSenseLocation(target)) {
                rc.setFlag(scoutingFlag);
                role = SCOUTING;
            }
        } else if (role == CONVERTED) { //right now they just run around and kamikaze
            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
            }
            if (rc.canMove(getPathDirSpread()) && shouldSpread()) {
                rc.move(getPathDirSpread());
            }
        } else if (role == FOLLOW) {
            if (rc.canSenseRobot(trailedMuckrakerID)) {
                target = rc.senseRobot(trailedMuckrakerID).getLocation();
                if (!ECLocations.isEmpty()
                        && rc.getLocation().distanceSquaredTo(ECLocations.get(0)) < 200
                        && rc.canEmpower(rc.getLocation().distanceSquaredTo(rc.senseRobot(trailedMuckrakerID).getLocation()))) {
                    rc.empower(rc.getLocation().distanceSquaredTo(rc.senseRobot(trailedMuckrakerID).getLocation()));
                } else if (muckrakersInRange > 1 && rc.canEmpower(actionRadius)) {
                    rc.empower(actionRadius);
                } else if (!ECLocations.isEmpty() && rc.getLocation().distanceSquaredTo(ECLocations.get(0)) > 1000) {
                    trailedMuckrakerID = 0;
                    rc.setFlag(scoutingFlag);
                    role = SCOUTING;
                }
                tryMove(getPathDirTo(target));
            } else {
                trailedMuckrakerID = 0;
                rc.setFlag(scoutingFlag);
                role = SCOUTING;
            }
        }

        //reading home ec flag info
        if (homeECFlagContents != null) {
            //if its an attack command, attack
            if ((homeECFlagContents[0] == ATTACK_ENEMY || homeECFlagContents[0] == ATTACK_NEUTRAL) &&
                rc.getConviction() > 110) {
                rc.setFlag(rc.getFlag(ECIDs.get(0)));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            }
        }

        //relay, mobile robot/robot comms
        if (role != RETURNING && role != CONVERTED && role != FOLLOW) {
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
                            friendlyInRange[i].getType() != RobotType.SLANDERER && flagContents[3] == homeECIDTag) {
                        if (!(friendlyInRange[i].getConviction() < 110 && role == ATTACKING)) {
                            rc.setFlag(flag);
                            target = ECLocations.get(0);
                            role = RETURNING;
                        }
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
        }
        if (rc.getFlag(rc.getID()) == scoutingFlag) {
            role = SCOUTING;
        }

        for (int i = friendlyInRange.length; --i >= 0; ) {
            if (friendlyInRange[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECIDs.add(friendlyInRange[i].getID());
                ECLocations.add(friendlyInRange[i].getLocation());
                if (ECIDs.size() == 1 && ECLocations.size() == 1) {
                    homeECx = ECLocations.get(0).x;
                    homeECy = ECLocations.get(0).y;
                    homeECIDTag = ECIDs.get(0) % 128;
                    scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
                    rc.setFlag(scoutingFlag);
                    role = SCOUTING;
                }
            }
        }

    }

    static boolean notTrailed(int trailedMuckrakerID, RobotInfo[] friendlies) throws GameActionException {
        if (rc.canSenseRobot(trailedMuckrakerID)) {
            MapLocation tgtLoc = rc.senseRobot(trailedMuckrakerID).getLocation();
            for (int i = friendlies.length; --i >= 0;) {
                if (friendlies[i].getType() == RobotType.POLITICIAN
                        && friendlies[i].getLocation().distanceSquaredTo(tgtLoc) < RobotType.POLITICIAN.sensorRadiusSquared
                        && friendlies[i].getConviction() < 30) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
