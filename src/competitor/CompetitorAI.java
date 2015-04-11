package competitor;

import static snowbound.api.util.Utility.any;
import static snowbound.api.util.Utility.difference;
import static snowbound.api.util.Utility.min;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import competitor.util.BaseGoal;
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

    private boolean                       init          = false;

    private int                           maxUnits;

    private int                           numOfCleats   = 0;
    private int                           numOfBucket   = 0;
    private int                           numOfPitchers = 0;

    private HashMap<Unit, Goal>           goals;
    private HashMap<Unit, Perk>           perks;
    private HashMap<Unit, List<Position>> paths;
    private int                           turnCount     = 0;

    public CompetitorAI()
    {
        this.goals = new HashMap<>();
        this.perks = new HashMap<>();
        paths = new HashMap<>();
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
        turnCount = turn.turns();
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
        switch (goal.getGoal())
        {
            case BASE:
            {
                Base b = turn.baseAt(cleat.position());
                if (b != null && b.isOwnedBy(turn.myTeam()))
                    b = null;

                if (b != null)
                {
                    Action action = new CaptureAction();
                    if (b.equals(((BaseGoal) goal).getBase()))
                        goals.put(cleat, new EmptyGoal());
                    return action;
                } else
                {
                    if (turnCount % 3 == 0)
                    {
                        List<Position> path = Pathfinding.getPath(turn, cleat
                                .position(), ((BaseGoal) goal).getBase()
                                .position());
                        paths.put(cleat, path);
                    }
                    Position newPos = null;

                    try
                    {
                        int pathlength = paths.get(cleat).size();
                        for (int i = 0; i < Unit.statistic(Stat.MOVE,
                                cleat.perk())
                                && i < pathlength; i++)
                            newPos = paths.get(cleat).remove(0);
                        return new MoveAction(newPos);
                    } catch (Exception e)
                    {
                        return new ShoutAction(e.toString());
                    }
                }
            }
            case DEFEND:
                break;
            case FIGHT:
                break;
            case NONE:
            {
                Set<Base> nonCapBase = Utility.filter(turn.allBases(),
                        new Owned(turn.myTeam()));
                if (nonCapBase.isEmpty())
                    break;

                List<Base> sortBase = Utility.ordered(nonCapBase,
                        new ManhattanDistance(cleat.position()));
                Goal g = null;

                if (sortBase.size() > 1)
                    g = new BaseGoal(sortBase.get(1));
                else
                    g = new BaseGoal(sortBase.get(0));

                goals.put(cleat, g);
                List<Position> path = Pathfinding.getPath(turn,
                        cleat.position(), ((BaseGoal) g).getBase().position());
                paths.put(cleat, path);
                Position newPos = null;
                try
                {
                    int pathlength = paths.get(cleat).size();
                    for (int i = 0; i < Unit.statistic(Stat.MOVE, cleat.perk())
                            && i < pathlength; i++)
                        newPos = paths.get(cleat).remove(0);
                    return new MoveAction(newPos);
                } catch (Exception e)
                {
                    return new ShoutAction(e.toString());
                }
            }
            default:
                break;

        }
        return null;
    }

}
