package testplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import battlecode.common.Team;

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
        }
        else{
            wonLastVote = false;
            winStreak = 0;
            loseStreak++;
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
            if(newBid < threshold && rc.canBid(newBid)){
                //dont want to be bankrupting ourselves so we have a threshold (max value we are willing to bid)

                rc.bid(newBid);
                prevBid = newBid;
            }
            else if(newBid >= threshold && threshold>prevBid && rc.canBid(threshold)){
                //bid the max we are willing to, also if its greater than our last bid
                newBid = threshold;
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
