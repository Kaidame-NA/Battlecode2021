package testplayer;

import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

public class Politician extends RobotPlayer{
    static int[] ECIDs = new int[20];
    static MapLocation[] ECLocations = new MapLocation[20];
    static int currentHomeEC = -1;
    static final int SCOUTING = 0;
    static final int ATTACKING = 1;
    static final int CONVERTED = 2;
    static final int FOLLOW = 3;
    static final int OVERFLOW = 4;
    static int role;
    static MapLocation target;
    static int[] homeECFlagContents;
    static int homeECx;
    static int homeECy;
    static int muckrakersInRange;
    static int trailedMuckrakerID;
    static int scoutedEnemyMuckID;

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
        if (currentHomeEC == -1) {
            role = CONVERTED;
            rc.setFlag(0);
        } else {
            homeECx = ECLocations[currentHomeEC].x;
            homeECy = ECLocations[currentHomeEC].y;
            rc.setFlag(0);
            role = SCOUTING;
            if (rc.getEmpowerFactor(rc.getTeam(), 10) > 2 && rc.getConviction() > 1000) {
                rc.setFlag(0);
                role = OVERFLOW;
            }
        }
    }

    static void run() throws GameActionException {
        if (currentHomeEC == -1) {
            role = CONVERTED;
        }
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int distToHome = 0;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, enemy);
        muckrakersInRange = 0;
        RobotInfo[] friendlyInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam());
        if (role != CONVERTED && role != OVERFLOW) {
            if (rc.canGetFlag(ECIDs[currentHomeEC])) {
                homeECFlagContents = decodeFlag(rc.getFlag(ECIDs[currentHomeEC]));
                distToHome = rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]);
            } //maybe something about if cant then its taken
        }
        //follow muckrakers if small poli
        for (int i = enemiesInRange.length; --i >= 0;) {
            if (enemiesInRange[i].getType() == RobotType.MUCKRAKER && rc.getConviction() < 30
                && ECLocations[0] != null && rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]) < 500) {
                RobotInfo unit = enemiesInRange[i];
                if (trailedMuckrakerID == 0 && role != OVERFLOW && notTrailed(unit.getID(), friendlyInRange)) {
                    trailedMuckrakerID = unit.getID();
                    role = FOLLOW;
                    rc.setFlag(encodeFlag(0, unit.getLocation().x - homeECx, unit.getLocation().y-homeECy, enemiesInRange[i].getID() % 256));
                    break;
                }
            }
        }
        for (int i = attackable.length; --i >= 0;) {
            if (attackable[i].getType() == RobotType.MUCKRAKER) {
                muckrakersInRange ++;
            }
        }
        if (role == OVERFLOW) {
            if (rc.canEmpower(2)) {
                rc.empower(2);
            }
        } else if (role == SCOUTING) {
            //go to point along direction of creation
            RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
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
            if ((rc.getID() % 2 == 0) && rc.getInfluence() < 30) {
                tryMove(polisringv2());
            }
            else if (shouldSpread()) {
                tryMove(getPathDirSpread());
            } else {
                tryMove(awayFromLocation(ECLocations[currentHomeEC]));
            }

        } else if (role == ATTACKING) {
            //only attacks target location atm, no reaction to other units on the way
            if (rc.canSenseLocation(target)) {
                RobotInfo[] unitsInRange = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared);
                for (int i = unitsInRange.length; --i >= 0;) {
                    RobotInfo unit = unitsInRange[i];
                    if (unit.getType() == RobotType.ENLIGHTENMENT_CENTER && unit.getTeam() == rc.getTeam()
                        && unit.getLocation().equals(target) && !contains(unit.getID(), ECIDs)) {
                        role = SCOUTING;
                        rc.setFlag(encodeFlag(SECURED_EC, target.x - homeECx, target.y - homeECy, 0));
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
            if ((rc.getID() % 2 == 0) && rc.getInfluence() < 30) {
                tryMove(polisringv2());
            }
            else {
                tryMove(getPathDirTo(target));
            }
        } else if (role == CONVERTED) { //right now they just run around and kamikaze
            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
            }

            if (shouldSpread()) {
                tryMove(getPathDirSpread());
            } else {
                tryMove(randomDirection());
            }

        } else if (role == FOLLOW) {
            if (rc.canSenseRobot(trailedMuckrakerID) && notTrailed(trailedMuckrakerID, friendlyInRange)) {
                RobotInfo trailed = rc.senseRobot(trailedMuckrakerID);
                target = trailed.getLocation();
                rc.setFlag(encodeFlag(0, trailed.getLocation().x - homeECx,
                        trailed.getLocation().y-homeECy, trailedMuckrakerID % 256));
                if (ECLocations[0] != null
                        && rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]) < 196
                        && rc.canEmpower(rc.getLocation().distanceSquaredTo(rc.senseRobot(trailedMuckrakerID).getLocation()))) {
                    rc.empower(rc.getLocation().distanceSquaredTo(rc.senseRobot(trailedMuckrakerID).getLocation()));
                } else if (muckrakersInRange > 1 && rc.canEmpower(actionRadius)) {
                    rc.empower(actionRadius);
                } else if (ECLocations[0] != null && rc.getLocation().distanceSquaredTo(ECLocations[currentHomeEC]) > 500) {
                    trailedMuckrakerID = 0;
                    rc.setFlag(0);
                    role = SCOUTING;
                }
                tryMove(getPathDirTo(target));
            } else {
                trailedMuckrakerID = 0;
                rc.setFlag(0);
                role = SCOUTING;
            }
        }

        //reading home ec flag info
        if (homeECFlagContents != null) {
            //if its an attack command, attack
            int[] ownFlag = decodeFlag(rc.getFlag(rc.getID()));
            if (((homeECFlagContents[0] == ENEMY_EC_FOUND &&
                rc.getConviction() > 29) || (homeECFlagContents[0] == NEUTRAL_EC_FOUND
                    && ((homeECFlagContents[3] == 255 && rc.getConviction() > 510)
                    || (homeECFlagContents[3] < 255 && rc.getConviction() > homeECFlagContents[3] + 10))))
                && role != FOLLOW && role != OVERFLOW && ownFlag[0] != SECURED_EC) {
                rc.setFlag(rc.getFlag(ECIDs[currentHomeEC]));
                target = new MapLocation(homeECx + homeECFlagContents[1],
                        homeECy + homeECFlagContents[2]);
                role = ATTACKING;
            } else if (homeECFlagContents[0] == SECURED_EC && ownFlag[1] == homeECFlagContents[1]
                    && ownFlag[2] == homeECFlagContents[2]){
                rc.setFlag(0);
                role = SCOUTING;
            }
        }

        for (int i = friendlyInRange.length; --i >= 0; ) {
            if (friendlyInRange[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                if (currentHomeEC == -1) {
                    currentHomeEC ++;
                    ECIDs[currentHomeEC] = friendlyInRange[i].getID();
                    ECLocations[currentHomeEC] = friendlyInRange[i].getLocation();
                    homeECx = ECLocations[currentHomeEC].x;
                    homeECy = ECLocations[currentHomeEC].y;
                    rc.setFlag(0);
                    role = SCOUTING;
                }
            }
        }
    homeECFlagContents = null;
    }

    static boolean notTrailed(int trailedMuckrakerID, RobotInfo[] friendlies) throws GameActionException {
        if (rc.canSenseRobot(trailedMuckrakerID)) {
            MapLocation tgtLoc = rc.senseRobot(trailedMuckrakerID).getLocation();
            for (int i = friendlies.length; --i >= 0;) {
                if (friendlies[i].getType() == RobotType.POLITICIAN) {
                    if (friendlies[i].getLocation().distanceSquaredTo(tgtLoc)
                            <= RobotType.POLITICIAN.sensorRadiusSquared) {
                        if (rc.canGetFlag(friendlies[i].getID())) {
                            int[] flag = decodeFlag(rc.getFlag(friendlies[i].getID()));
                            if (flag[3] == trailedMuckrakerID % 256) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
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
        RobotInfo[] friendlies = rc.senseNearbyRobots(25, friendly);
        ArrayList<RobotInfo> nearbyecs = new ArrayList<RobotInfo>();
        for (RobotInfo robot : friendlies) {
            RobotType type = robot.getType();
            if (type == RobotType.ENLIGHTENMENT_CENTER) {
                nearbyecs.add(robot);
            }
        }
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
                    cost += (25.0/rc.getLocation().distanceSquaredTo(spreadFrom)) * ((Math.abs(adj.x - spreadFrom.x) + Math.abs(adj.y - spreadFrom.y))
                            - (Math.abs(rc.getLocation().x - spreadFrom.x) + Math.abs(rc.getLocation().y - spreadFrom.y)));
                  /*  if (role != CONVERTED) {
                        cost += (Math.abs(adj.x - homeECx) - Math.abs(rc.getLocation().x - homeECx) +
                                Math.abs(adj.y - homeECy) - Math.abs(rc.getLocation().y - homeECy));
                    }*/
                }
                if (cost > optimalCost && rc.canMove(dir)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }

    static Direction polisringv2() throws GameActionException {
        MapLocation home = ECLocations[currentHomeEC];
        Direction optimalDir = Direction.CENTER;
        double optimalCost = - Double.MAX_VALUE;

        if (turnCount % 10 == 0) {
            banList.clear();
        }
        for (Direction dir: directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.canMove(dir)) {
                double pass = rc.sensePassability(adj);
                //double cost = - (rc.getType().actionCooldown/pass);
                double cost = 0;
                double radius = 6.2;
                cost -= Math.abs(radius - Math.sqrt(Math.pow(home.x - adj.x, 2) + Math.pow(home.y - adj.y, 2)));
                cost += radius * pass;
                if (cost > optimalCost && rc.canMove(dir) && !banList.contains(adj)) {
                    optimalDir = dir;
                    optimalCost = cost;
                }
            }
        }
        return optimalDir;
    }
}
