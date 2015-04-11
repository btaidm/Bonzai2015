package competitor;

import static snowbound.api.util.Utility.any;
import static snowbound.api.util.Utility.difference;
import static snowbound.api.util.Utility.min;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import competitor.util.EmptyGoal;
import competitor.util.Goal;
import snowbound.api.*;
import snowbound.api.util.ManhattanDistance;
import snowbound.api.util.Owned;
import snowbound.api.util.Predicate;
import snowbound.api.util.Utility;

@Agent(name = "Headcrabs")
public class CompetitorAI extends AI
{

    private boolean             init          = false;

    private int                 maxUnits;

    private int                 numOfCleats   = 0;
    private int                 numOfBucket   = 0;
    private int                 numOfPitchers = 0;

    private HashMap<Unit, Goal> goals;
    private HashMap<Unit, Perk> perks;

    public CompetitorAI()
    {
        this.goals = new HashMap<>();
        this.perks = new HashMap<>();
    }

    private void setup(Turn turn)
    {
        if (init)
            return;
        maxUnits = turn.myUnits().size();

        for (Unit unit : turn.roster())
        {
            perks.put(unit, Perk.CLEATS);
        }
    }

    /**
	 *
	 **/
    public Action action(Turn turn)
    {
        setup(turn);

        Unit player = turn.actor();
        Action action = null;

        action = spawn(player, turn);
        if (action != null)
            return action;

        switch (player.perk())
        {
            case BUCKET:
                break;
            case CLEATS:
            {
                action = cleatMovement(player, turn);
                break;
            }
            case LAYERS:
                break;
            case NONE:
                break;
            case PITCHER:
                break;
            default:
                break;

        }

        return action;
    }

    private Action spawn(Unit player, Turn turn)
    {
        if (player.isSpawned())
            return null;

        goals.put(player, new EmptyGoal());
        return new SpawnAction(any(turn.myTeam().spawns()), perks.get(player));

    }
    
    private Action cleatMovement(Unit cleat, Turn turn)
    {
        Goal goal = goals.get(cleat);
        switch(goal.getGoal())
        {
            case BASE:
                break;
            case DEFEND:
                break;
            case FIGHT:
                break;
            case NONE:
                Set<Base> nonCapBase = Utility.filter(turn.allBases(), new Owned(turn.myTeam()));
                List<Base> sortBase = Utility.ordered(nonCapBase, new ManhattanDistance(cleat.position()));
                break;
            default:
                break;
            
        }
        return null;
    }

}
