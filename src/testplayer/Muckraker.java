package testplayer;

import battlecode.common.*;
import com.sun.org.apache.bcel.internal.generic.RETURN;

import java.util.LinkedList;

public class Muckraker extends RobotPlayer{
    static int[] ECIDs = new int[20];
    static MapLocation[] ECLocations = new MapLocation[20];
    static int currentHomeEC = -1;
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
        homeECIDTag = ECIDs[currentHomeEC] % 128;
        scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
        rc.setFlag(scoutingFlag);
        role = SCOUTING;
    }

    static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        int distToHome = rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]);
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
        if (rc.canGetFlag(ECIDs[currentHomeEC])) {
            homeECFlagContents = decodeFlag(rc.getFlag(ECIDs[currentHomeEC]));
        }
        //System.out.println("Checkpoint 3: " + Clock.getBytecodeNum());
        if (role == SCOUTING) {
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            //System.out.println("Checkpoint Scout A: " + Clock.getBytecodeNum());
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
            //System.out.println("Checkpoint Scout B: " + Clock.getBytecodeNum());
            if (shouldSpread()) {
                tryMove(getPathDirSpread());
            }
            //System.out.println("Checkpoint Scout C: " + Clock.getBytecodeNum());
        } else if (role == ATTACKING) {
            //System.out.println("Checkpoint Attack A: " + Clock.getBytecodeNum());
            if (rc.canSenseLocation(target)) {
                RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
                for (int i = unitsInRange.length; --i >= 0;) {
                    RobotInfo unit = unitsInRange[i];
                    if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == rc.getTeam() &&
                        unit.getLocation().equals(target) && !contains(unit.getID(), ECIDs)) {
                        currentHomeEC ++;
                        ECIDs[currentHomeEC] = (unit.getID());
                        ECLocations[currentHomeEC] = (target);
                        homeECx = ECLocations[currentHomeEC].x;
                        homeECy = ECLocations[currentHomeEC].y;
                        homeECIDTag = ECIDs[currentHomeEC] % 128;
                        role = SCOUTING;
                        scoutingFlag = encodeFlag(0, 0, 0, homeECIDTag);
                        rc.setFlag(scoutingFlag);
                        if (rc.canGetFlag(ECIDs[currentHomeEC])) {
                            homeECFlagContents = decodeFlag(rc.getFlag(ECIDs[currentHomeEC]));
                        }
                    }
                }
            }
            //System.out.println("Checkpoint Attack B: " + Clock.getBytecodeNum());
            //attack groups:
            if (rc.getID() % 5 == 0) {
                tryMove(getPathDirToEnemyEC(target));
            }

            if (rc.getID() % 5 == 1) {
                tryMove(getAlternatePathTwoDirToEnemyEC(target));
            }

            if (rc.getID() % 5 == 2) {
                tryMove(getAlternatePathThreeDirToEnemyEC(target));
            }

            if (rc.getID() % 5 == 3 || rc.getID() % 5 == 4) {
                tryMove(getAlternatePathDirToEnemyEC(target));
            }
            //System.out.println("Checkpoint Attack C: " + Clock.getBytecodeNum());
            
        } else if (role == RETURNING){
            target = ECLocations[currentHomeEC];
            tryMove(getPathDirTo(target));
            //System.out.println("Checkpoint Return A: " + Clock.getBytecodeNum());
            for (int i = friendlyInRange.length; --i >= 0;) {
                //otherside of relay, if you are delivering and farther away, let closer bot assume info and you
                //take its flag and commands(unless its an attack)
                if (friendlyInRange[i].getLocation().distanceSquaredTo(target)
                        < distToHome && rc.canGetFlag(friendlyInRange[i].getID())) {
                    int flag = rc.getFlag(friendlyInRange[i].getID());
                    int[] flagContents = decodeFlag(flag);
                    if (flagContents[0] != CONVERTED_FLAG && friendlyInRange[i].getType() != RobotType.SLANDERER
                        && flagContents[3] == homeECIDTag) {
                        if (flag == scoutingFlag) {
                            role = SCOUTING;
                            rc.setFlag(flag);
                            break;
                        } else if (flagContents[0] == ATTACK_ENEMY) {
                            target = new MapLocation(homeECx + flagContents[1],
                                    homeECy + flagContents[2]);
                            rc.setFlag(flag);
                            role = ATTACKING;
                            break;
                        } else if (flag == rc.getFlag(rc.getID())) {
                            role = SCOUTING;
                            rc.setFlag(scoutingFlag);
                            break;
                        }
                    }

                }
            }
           //System.out.println("Checkpoint Return B: " + Clock.getBytecodeNum());
            if (rc.canSenseLocation(target)) {
                rc.setFlag(scoutingFlag);
                role = SCOUTING;
            }
            //System.out.println("Checkpoint Return C: " + Clock.getBytecodeNum());
        }
        //System.out.println("Checkpoint 4: " + Clock.getBytecodeNum());
        if (homeECFlagContents != null) {
            //if its an attack command, attack
            int[] ownFlag = decodeFlag(rc.getFlag(rc.getID()));
            if (homeECFlagContents[0] == ATTACK_ENEMY) {

                rc.setFlag(rc.getFlag(ECIDs[currentHomeEC]));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            } else if (homeECFlagContents[0] == ATTACK_NEUTRAL && ownFlag[1] == homeECFlagContents[1] &&
                    homeECFlagContents[2] == ownFlag[2]) {
                rc.setFlag(scoutingFlag);
                role = SCOUTING;
            }
        }
        //System.out.println("Checkpoint 5: " + Clock.getBytecodeNum());
        if (role != RETURNING) {
            //if you are not returning info and a friendly is near with flag
                if (friendlyInRange.length > 40) {
                    for (int i = friendlyInRange.length/2; --i >= 0; ) {
                        if (friendlyInRange[i].getLocation().distanceSquaredTo(ECLocations[currentHomeEC])
                                > distToHome && friendlyInRange[i].getType() != RobotType.SLANDERER) {
                            if (rc.canGetFlag(friendlyInRange[i].getID())) {
                                int flag = rc.getFlag(friendlyInRange[i].getID());
                                int[] flagContents = decodeFlag(flag);
                                //relay info if you are closer to home ec
                                if (ECLocations[0] != null
                                        && (flagContents[0] == ENEMY_EC_FOUND || flagContents[0] == NEUTRAL_EC_FOUND) &&
                                        flagContents[3] == homeECIDTag) {
                                    rc.setFlag(flag);
                                    target = ECLocations[currentHomeEC];
                                    role = RETURNING;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    for (int i = friendlyInRange.length; --i >= 0; ) {
                        if (friendlyInRange[i].getLocation().distanceSquaredTo(ECLocations[currentHomeEC])
                                > distToHome && friendlyInRange[i].getType() != RobotType.SLANDERER) {
                            if (rc.canGetFlag(friendlyInRange[i].getID())) {
                                int flag = rc.getFlag(friendlyInRange[i].getID());
                                int[] flagContents = decodeFlag(flag);
                                //relay info if you are closer to home ec
                                if (ECLocations[0] != null
                                        && (flagContents[0] == ENEMY_EC_FOUND || flagContents[0] == NEUTRAL_EC_FOUND) &&
                                        flagContents[3] == homeECIDTag) {
                                    rc.setFlag(flag);
                                    target = ECLocations[currentHomeEC];
                                    role = RETURNING;
                                    break;
                                }
                            }
                        }
                    }
                }
        }
        //System.out.println("Checkpoint 6: " + Clock.getBytecodeNum());
        if (rc.getFlag(rc.getID()) == scoutingFlag) {
            role = SCOUTING;
        }
        homeECFlagContents = null;
    }

    static boolean contains(int ecid, int[] arr) {
        for (int i = arr.length; --i >= 0;) {
            if (arr[i] == ecid) {
                return true;
            }
        }
        return false;
    }
}
