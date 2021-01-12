package testplayer;

import battlecode.common.*;
import com.sun.org.apache.bcel.internal.generic.RETURN;

import java.util.LinkedList;

public class Muckraker extends RobotPlayer{
    static LinkedList<Integer> ECIDs = new LinkedList<Integer>();
    static LinkedList<MapLocation> ECLocations = new LinkedList<MapLocation>();
    static int role;
    static final int SCOUTING = 0;
    static final int RETURNING = 1;
    static final int ATTACKING = 2;
    static int homeECx;
    static int homeECy;
    static MapLocation target;
    static int stuckCounter;
    static int[] homeECFlagContents;
    static int homeECIDTag;
    static int scoutingFlag;

    static void setup() throws GameActionException {
        RobotInfo[] possibleECs = rc.senseNearbyRobots(2, rc.getTeam());
        for (int i = possibleECs.length; --i >= 0;){
            if (possibleECs[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ECIDs.add(possibleECs[i].getID());
                ECLocations.add(possibleECs[i].getLocation());
            }
        }
        homeECx = ECLocations.get(0).x;
        homeECy = ECLocations.get(0).y;
        homeECIDTag = ECIDs.get(0) % 128;
        scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
        rc.setFlag(scoutingFlag);
        role = SCOUTING;
    }

    static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    //System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (rc.canGetFlag(ECIDs.get(0))) {
            homeECFlagContents = decodeFlag(rc.getFlag(ECIDs.get(0)));
        }

        if (role == SCOUTING) {
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            for (int i = unitsInRange.length; --i >= 0;) {
                RobotInfo unit = unitsInRange[i];
                if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == enemy) {
                    rc.setFlag(encodeFlag(ENEMY_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, homeECIDTag));
                    role = RETURNING;
                }
                else if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == Team.NEUTRAL) {
                    rc.setFlag(encodeFlag(NEUTRAL_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, homeECIDTag));
                    role = RETURNING;
                }
            }
            if (rc.canMove(getPathDirSpread()) && shouldSpread()) {
                rc.move(getPathDirSpread());
            }
        } else if (role == ATTACKING) {
            if (rc.canSenseLocation(target)) {
                RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
                for (int i = unitsInRange.length; --i >= 0;) {
                    RobotInfo unit = unitsInRange[i];
                    if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == rc.getTeam() &&
                        unit.getLocation().equals(target)) {
                        ECIDs.addFirst(unit.getID());
                        ECLocations.addFirst(target);
                        homeECx = ECLocations.get(0).x;
                        homeECy = ECLocations.get(0).y;
                        homeECIDTag = ECIDs.get(0) % 128;
                        role = SCOUTING;
                        scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
                        rc.setFlag(scoutingFlag);
                        if (rc.canGetFlag(ECIDs.get(0))) {
                            homeECFlagContents = decodeFlag(rc.getFlag(ECIDs.get(0)));
                        }
                    }
                }
            }
            tryMove(getPathDirToEnemyEC(target));
        } else if (role == RETURNING){
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
                        } else if (flagContents[0] == ATTACK_ENEMY) {
                            target = new MapLocation(homeECx + flagContents[1],
                                    homeECy + flagContents[2]);
                            rc.setFlag(flag);
                            role = ATTACKING;
                        } else if (flag == rc.getFlag(rc.getID())) {
                            role = SCOUTING;
                            rc.setFlag(scoutingFlag);
                        }
                    }

                }
            }
            if (rc.canSenseLocation(target)) {
                rc.setFlag(scoutingFlag);
                role = SCOUTING;
            }
        }

        if (homeECFlagContents != null) {
            //if its an attack command, attack
            int[] ownFlag = decodeFlag(rc.getFlag(rc.getID()));
            if (homeECFlagContents[0] == ATTACK_ENEMY) {

                rc.setFlag(rc.getFlag(ECIDs.get(0)));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            } else if (homeECFlagContents[0] == ATTACK_NEUTRAL && ownFlag[1] == homeECFlagContents[1] &&
                    homeECFlagContents[2] == ownFlag[2]) {
                rc.setFlag(scoutingFlag);
                role = SCOUTING;
            }
        }

        if (role != RETURNING) {
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
                        rc.setFlag(flag);
                        target = ECLocations.get(0);
                        role = RETURNING;
                    }/*
                    else if (flagContents[0] == ATTACK_ENEMY && flagContents[3] == 0) {
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
    }
}
