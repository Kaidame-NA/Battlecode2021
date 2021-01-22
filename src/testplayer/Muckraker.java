package testplayer;

import battlecode.common.*;
import com.sun.org.apache.bcel.internal.generic.RETURN;

import java.util.ArrayList;
import java.util.LinkedList;

public class Muckraker extends RobotPlayer{
    static int[] ECIDs = new int[20];
    static MapLocation[] ECLocations = new MapLocation[20];
    static int currentHomeEC = -1;
    static int role;
    static final int SCOUTING = 0;
    static final int ATTACKING = 1;
    static final int GLITCHED = 2;
    static int homeECx;
    static int homeECy;
    static MapLocation target;
    static int stuckCounter;
    static int[] homeECFlagContents;
    static int scoutedEnemyMuckID;

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
        if (currentHomeEC >=0) {
            homeECx = ECLocations[currentHomeEC].x;
            homeECy = ECLocations[currentHomeEC].y;
            rc.setFlag(0);
            role = SCOUTING;
        } else {
            role = GLITCHED;
        }
    }

    static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int distToHome = 0;
        //RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy);
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
        for (RobotInfo robot : rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy)) {
            if (robot.getType() == RobotType.SLANDERER) {
                tryMove(getPathDirTo(robot.getLocation()));
                break;
            }
        }
        if (currentHomeEC != -1) {
            if (rc.canGetFlag(ECIDs[currentHomeEC])) {
                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs[currentHomeEC]));
                distToHome = rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]);
            }
        }
        //System.out.println("Checkpoint 3: " + Clock.getBytecodeNum());
        if (role == SCOUTING) {
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
            //System.out.println("Checkpoint Scout A: " + Clock.getBytecodeNum());
            if (!rc.canSenseRobot(scoutedEnemyMuckID)) {
                rc.setFlag(0);
            }
            for (int i = enemiesInRange.length; --i >= 0;) {
                RobotInfo unit = enemiesInRange[i];
                if (unit.getType() == RobotType.MUCKRAKER) {
                    rc.setFlag(encodeFlag(0, unit.getLocation().x - homeECx, unit.getLocation().y - homeECy, Math.min(unit.getConviction(), 255)));
                    scoutedEnemyMuckID = unit.getID();
                    break;
                }
            }
            //System.out.println("Checkpoint Scout A2: " + Clock.getBytecodeNum());
            for (int i = unitsInRange.length; --i >= 0;) {
                RobotInfo unit = unitsInRange[i];
                if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == enemy) {
                    rc.setFlag(encodeFlag(ENEMY_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, Math.min(unit.getConviction(), 255)));
                    break;
                }
                else if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == Team.NEUTRAL) {
                    rc.setFlag(encodeFlag(NEUTRAL_EC_FOUND, unit.location.x - homeECx, unit.location.y - homeECy, Math.min(unit.getConviction(), 255)));
                    break;
                }
            }

            if (homeECFlagContents != null) {
                MapLocation tgtedEC = new MapLocation(homeECx + homeECFlagContents[1], homeECy + homeECFlagContents[2]);
                if (rc.canSenseLocation(tgtedEC)) {
                    RobotInfo tgt = rc.senseRobotAtLocation(tgtedEC);
                    if (tgt.getType() == RobotType.ENLIGHTENMENT_CENTER && tgt.getTeam() == rc.getTeam()
                            && tgt.getLocation().equals(tgtedEC) && !contains(tgt.getID(), ECIDs)) {
                        role = SCOUTING;
                        rc.setFlag(encodeFlag(SECURED_EC, tgtedEC.x - homeECx, tgtedEC.y - homeECy, 0));
                    }
                }
            }
            //System.out.println("Checkpoint Scout B: " + Clock.getBytecodeNum());
            if (shouldSpread()) {
                tryMove(getPathDirSpread());
            } else if (rc.getID() % 2 == 0){
                tryMove(randomDirection());
            } else {
                tryMove(awayFromLocation(ECLocations[currentHomeEC]));
            }
            //System.out.println("Checkpoint Scout C: " + Clock.getBytecodeNum());
        } else if (role == ATTACKING) {
            //System.out.println("Checkpoint Attack A: " + Clock.getBytecodeNum());
            if (rc.canSenseLocation(target)) {
                RobotInfo tgt = rc.senseRobotAtLocation(target);
                if (tgt.getType() == RobotType.ENLIGHTENMENT_CENTER && tgt.getTeam() == rc.getTeam()
                        && tgt.getLocation().equals(target) && !contains(tgt.getID(), ECIDs)) {
                    role = SCOUTING;
                    rc.setFlag(encodeFlag(SECURED_EC, target.x - homeECx, target.y - homeECy, 0));
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

/*
            if (rc.getID() % 5 == 3 || rc.getID() % 5 == 4) {
                tryMove(getAlternatePathDirToEnemyEC(target));
            }*/
            //System.out.println("Checkpoint Attack C: " + Clock.getBytecodeNum());
            
        } else if (role == GLITCHED) {
            if (shouldSpread()) {
                tryMove(getPathDirSpread());
            } else if (rc.getID() % 2 == 0){
                tryMove(randomDirection());
            } else {
                tryMove(awayFromLocation(ECLocations[currentHomeEC]));
            }
        }
        //System.out.println("Checkpoint 4: " + Clock.getBytecodeNum());
        if (homeECFlagContents != null) {
            //if its an attack command, attack
            int[] ownFlag = decodeFlag(rc.getFlag(rc.getID()));
            if (homeECFlagContents[0] == ENEMY_EC_FOUND && (rc.getID() % 5 <= 2 || rc.getConviction() > 1)) {

                rc.setFlag(rc.getFlag(ECIDs[currentHomeEC]));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            } /*else if (homeECFlagContents[0] == NEUTRAL_EC_FOUND && ownFlag[1] == homeECFlagContents[1] &&
                    homeECFlagContents[2] == ownFlag[2]) {
                rc.setFlag(0);
                role = SCOUTING;
                //to prevent positive feedback loop keep track of attacked neutral ecs in ec.java
            }*/ else if (homeECFlagContents[0] == SECURED_EC && ownFlag[1] == homeECFlagContents[1]
                    && ownFlag[2] == homeECFlagContents[2]){
                rc.setFlag(0);
                role = SCOUTING;
            }
        }
        //System.out.println("Checkpoint 6: " + Clock.getBytecodeNum());
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

    static Direction getPathDirSpread() throws GameActionException {
        Team friendly = rc.getTeam();
        RobotInfo[] friendlies = rc.senseNearbyRobots(-1, friendly);
        int numberofnearbyfriendlies = friendlies.length;

        numberofnearbyfriendlies = friendlies.length > 10 ? 10 : numberofnearbyfriendlies; // cap at 10

        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                double cost = - (rc.getType().actionCooldown/pass);
                /*
                if (nearbyecs.size() != 0) {
                    MapLocation spreadfromecone = nearbyecs.get(0).getLocation();
                    cost += (Math.pow(spreadfromecone.x - adj.x, 2) + Math.pow(spreadfromecone.y - adj.y, 2));
                }

                 */
                for(int i = numberofnearbyfriendlies; --i>=0;)
                {
                    MapLocation spreadFrom = friendlies[i].getLocation();
                    cost += (30.0/rc.getLocation().distanceSquaredTo(spreadFrom)) * ((Math.abs(adj.x - spreadFrom.x) + Math.abs(adj.y - spreadFrom.y))
                            - (Math.abs(rc.getLocation().x - spreadFrom.x) + Math.abs(rc.getLocation().y - spreadFrom.y)))
                            /*+ (Math.abs(adj.x - homeECx) - Math.abs(rc.getLocation().x - homeECx) +
                            Math.abs(adj.y - homeECy) - Math.abs(rc.getLocation().y - homeECy))*/;
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
