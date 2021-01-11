package testplayer;

import battlecode.common.*;

public class EnlightenmentCenter extends RobotPlayer{

    static int neutralAttackTurnCounter = 0;
    static void run() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        if (rc.canBid(1)) {
            rc.bid(1);
        }
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
                rc.buildRobot(RobotType.POLITICIAN, dir, influence);
            } else {
                break;
            }
        }
        RobotInfo[] nearbyUnits = rc.senseNearbyRobots();
        for (int i = nearbyUnits.length; --i >= 0;) {
            if (nearbyUnits[i].getTeam() == rc.getTeam()) {
                if (rc.canGetFlag(nearbyUnits[i].getID())) {
                    int[] flagContents = decodeFlag(rc.getFlag(nearbyUnits[i].getID()));
                    if (flagContents[0] == ENEMY_EC_FOUND) {
                        rc.setFlag(encodeFlag(ATTACK_ENEMY, flagContents[1], flagContents[2], 0));
                    } else if (flagContents[0] == NEUTRAL_EC_FOUND) {
                        rc.setFlag(encodeFlag(ATTACK_ENEMY, flagContents[1], flagContents[2], 0));
                    }
                }
            }
        }
        if (decodeFlag(rc.getFlag(rc.getID()))[0] == NEUTRAL_EC_FOUND) {
            if (neutralAttackTurnCounter > 5) {
                rc.setFlag(0);
                neutralAttackTurnCounter = 0;
            }
            neutralAttackTurnCounter ++;
        }
    }

        static boolean wonLastVote;
        static int friendlyVotes, prevBid, winStreak, loseStreak = 0;
        //bids for the ec
    static void bidVote() throws GameActionException{
        int round = rc.getRoundNum();
        int newBid;

        if(rc.getTeamVotes()>friendlyVotes){
            wonLastVote = true;
            winStreak++;
            loseStreak = 0;
            System.out.println("I won the last vote! Winstreak: " + winStreak);
        }
        else{
            wonLastVote = false;
            winStreak = 0;
            loseStreak++;
            System.out.println("I lost the last vote :((. Losestreak: " + loseStreak);
        }
        friendlyVotes = rc.getTeamVotes();


        if(friendlyVotes > 1500 || round-friendlyVotes > 1500)//if either we or enemy have already won the bidding game
        {
            rc.bid(0);
        }
        else if(!wonLastVote){ // we lost the last vote...
            double iCoef = 2;

            newBid = prevBid + (int) Math.ceil(Math.pow( (1/iCoef),(-loseStreak+1) ));
            //increasing doubley (loseStreak is increasing while prevBid is increasing)

            int threshold = rc.getInfluence()/5; //our maximum we are willing to bid
            if(newBid < 1 && rc.canBid(1)){
                rc.bid(1);
                prevBid = 1;
                System.out.println("Lost last vote, newBid<1 so lets bid the minimum, 1");
            }
            else if(newBid < threshold && rc.canBid(newBid)){
                //bid the max we are willing to, also if its greater than our last bid

                rc.bid(newBid);
                prevBid = newBid;
            }
        }
        else{// we won the last vote!
            double dCoef = 1.7; //changeable

            newBid = (int) Math.ceil(Math.pow( (-1/dCoef),(-winStreak+2) ) + prevBid);
            //decreasing doubley (winStreak is increasing while prevBid is decreasing)

            if(newBid < 1 && rc.canBid(1)){
                rc.bid(1);
            }
            else if(rc.canBid(newBid)){
                rc.bid(newBid);
            }

        }

    }
}
