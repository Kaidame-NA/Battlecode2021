package testplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class EnlightenmentCenter extends RobotPlayer{

    static void run() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        if (rc.canBid(1)) {
            rc.bid(1);
        }
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }
}
