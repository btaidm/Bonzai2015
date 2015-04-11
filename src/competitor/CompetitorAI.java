package competitor;

import static snowbound.api.util.Utility.any;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import snowbound.api.AI;
import snowbound.api.Action;
import snowbound.api.Agent;
import snowbound.api.Base;
import snowbound.api.CaptureAction;
import snowbound.api.GatherAction;
import snowbound.api.MoveAction;
import snowbound.api.Pathfinding;
import snowbound.api.Perk;
import snowbound.api.Position;
import snowbound.api.SpawnAction;
import snowbound.api.Stat;
import snowbound.api.ThrowAction;
import snowbound.api.Tile;
import snowbound.api.Turn;
import snowbound.api.Unit;
import snowbound.api.util.ManhattanDistance;
import snowbound.api.util.Owned;
import snowbound.api.util.TileHasSnow;
import snowbound.api.util.Utility;

import competitor.util.BaseGoal;
import competitor.util.EmptyGoal;
import competitor.util.FightGoal;
import competitor.util.GatherGoal;
import competitor.util.Goal;

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
        System.err.println("MAX UNITS: " + maxUnits);
        Collection<Unit> myRoster = Utility.intersect(turn.roster(),
                turn.myUnits());
        for (Unit unit : myRoster)
        {
            switch (maxUnits)
            {
                case 1:
                case 2:
                {
                    perks.put(unit, Perk.CLEATS);
                    break;
                }
                case 3:
                case 4:
                {
                    if (numOfPitchers < 2)
                    {
                        numOfPitchers++;
                        System.out.println("Spawning LAYERS");
                        perks.put(unit, Perk.LAYERS);
                    } else
                    {
                        perks.put(unit, Perk.CLEATS);
                    }
                    break;
                }
                case 5:
                case 6:
                {
                    if (numOfPitchers <= 2)
                    {
                        numOfPitchers++;
                        perks.put(unit, Perk.LAYERS);
                    } else
                    {
                        perks.put(unit, Perk.CLEATS);
                    }
                    break;
                }
                default:
                {
                    perks.put(unit, Perk.CLEATS);
                    break;
                }
            }
        }
        init = true;
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
                action = pitcherMovement(player, turn);
                break;
            case NONE:
                break;
            case PITCHER:
                action = pitcherMovement(player, turn);
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

        System.err.println(perks.get(player).toString());
        goals.put(player, new EmptyGoal());
        return new SpawnAction(any(turn.myTeam().spawns()), perks.get(player));

    }

    private Action pitcherMovement(Unit pitcher, Turn turn)
    {
        Goal goal = goals.get(pitcher);
        Action action = null;
        while (action == null)
        {
            switch (goal.getGoal())
            {
                case NONE:
                {
                    System.out.println("PITCHER: Doing Nothing");
                    if (pitcher.snowballs() < Unit.statistic(Stat.CAPACITY,
                            Perk.PITCHER))
                    {
                        Tile snow = Utility
                                .nearest(Utility.retain(turn.tiles(),
                                        new TileHasSnow()), pitcher);
                        goal = new GatherGoal(snow);
                        break;
                    } else
                    {
                        Unit enemy = Utility
                                .nearest(turn.enemyUnits(), pitcher);
                        goal = new FightGoal(enemy);
                        break;
                    }

                }
                case BASE:
                    action = new CaptureAction();
                    goal = new EmptyGoal();
                    break;
                case DEFEND:
                    break;
                case FIGHT:
                {
                    System.out.println("PITCHER: FIGHTING");
                    if (pitcher.snowballs() == 0)
                    {
                        goal = new EmptyGoal();
                        break;
                    }

                    // Attack in range
                    FightGoal g = new FightGoal(turn.current(((FightGoal) goal)
                            .getUnit()));
                    Set<Unit> inRange = pitcher.enemyUnitsInThrowRange();
                    if (!inRange.isEmpty())
                    {
                        Unit attack = any(inRange);
                        action = new ThrowAction(attack);
                        break;
                    }
                    // Check for uncaptured base
                    Tile currentTile = turn.tileAt(pitcher);

                    if (turn.hasBaseAt(currentTile)
                            && !turn.baseAt(currentTile).isOwnedBy(
                                    turn.myTeam()))
                    {
                        goal = new BaseGoal(turn.baseAt(currentTile));

                        break;
                    }

                    // Finally Move
                    if (!paths.containsKey(pitcher) || turnCount % 3 == 0
                            || paths.get(pitcher).isEmpty())
                    {
                        List<Position> path = Pathfinding.getPath(turn,
                                pitcher.position(), g.getUnit().position());
                        paths.put(pitcher, path);
                    }
                    Position newPos = null;

                    int pathlength = paths.get(pitcher).size();
                    for (int i = 0; i < Unit.statistic(Stat.MOVE,
                            pitcher.perk())
                            && i < pathlength; i++)
                        newPos = paths.get(pitcher).remove(0);
                    action = new MoveAction(newPos);
                    break;
                }
                case GATHER:
                {
                    System.out.println("PITCHER: GATHERING SNOW");
                    GatherGoal g = new GatherGoal(
                            turn.current(((GatherGoal) goal).getTile()));
                    if (pitcher.snowballs() >= Unit.statistic(Stat.CAPACITY,
                            Perk.PITCHER))
                    {
                        Unit enemy = Utility
                                .nearest(turn.enemyUnits(), pitcher);
                        goal = new FightGoal(enemy);
                        break;
                    }
                    boolean enemysnear = !pitcher.enemyUnitsInMoveRange().isEmpty();
                    Tile currentTile = turn.tileAt(pitcher);

                    if (!enemysnear && turn.hasBaseAt(currentTile)
                            && !turn.baseAt(currentTile).isOwnedBy(
                                    turn.myTeam()))
                    {
                        goal = new BaseGoal(turn.baseAt(currentTile));

                        break;
                    }

                    Tile endTile = g.getTile();
                    if (currentTile.snow() > 0)
                    {
                        action = new GatherAction();
                        break;
                    }
                    if (pitcher.position().equals(endTile.position()))
                    {
                        if (endTile.snow() > 0)
                        {
                            action = new GatherAction();
                            break;
                        } else
                        {
                            goal = new EmptyGoal();
                            break;
                        }
                    }

                    if (!paths.containsKey(pitcher) || turnCount % 3 == 0
                            || paths.get(pitcher).isEmpty())
                    {
                        List<Position> path = Pathfinding.getPath(turn,
                                pitcher.position(), endTile.position());
                        paths.put(pitcher, path);
                    }
                    Position newPos = null;

                    int pathlength = paths.get(pitcher).size();
                    for (int i = 0; i < Unit.statistic(Stat.MOVE,
                            pitcher.perk())
                            && i < pathlength; i++)
                        newPos = paths.get(pitcher).remove(0);
                    action = new MoveAction(newPos);
                    break;
                }
                default:
                {
                    return null;
                }

            }
            goals.put(pitcher, goal);
        }
        return action;
    }

    private Action cleatMovement(Unit cleat, Turn turn)
    {
        Goal goal = goals.get(cleat);
        while (true)
        {
            switch (goal.getGoal())
            {
                case BASE:
                {

                    Base b = turn.baseAt(cleat.position());
                    if (((BaseGoal) goal).getBase().isOwnedBy(turn.myTeam()))
                    {
                        goals.put(cleat, new EmptyGoal());
                    }
                    if (b != null && !b.isOwnedBy(turn.myTeam()))
                    {
                        Action action = new CaptureAction();
                        if (b.equals(((BaseGoal) goal).getBase()))
                            goals.put(cleat, new EmptyGoal());
                        return action;
                    } else
                    {
                        if (turnCount % 3 == 0 || paths.get(cleat).isEmpty())
                        {
                            List<Position> path = Pathfinding.getPath(turn,
                                    cleat.position(), ((BaseGoal) goal)
                                            .getBase().position());
                            paths.put(cleat, path);
                        }
                        Position newPos = null;

                        int pathlength = paths.get(cleat).size();
                        for (int i = 0; i < Unit.statistic(Stat.MOVE,
                                cleat.perk())
                                && i < pathlength; i++)
                            newPos = paths.get(cleat).remove(0);
                        return new MoveAction(newPos);

                    }
                }
                case DEFEND:
                    return null;
                case FIGHT:
                    return null;
                case NONE:
                {
                    Base b = turn.baseAt(cleat.position());
                    if (b != null && !b.isOwnedBy(turn.myTeam()))
                        return new CaptureAction();

                    Set<Base> nonCapBase = Utility.filter(turn.allBases(),
                            new Owned(turn.myTeam()));

                    if (nonCapBase.isEmpty())
                        return null;

                    List<Base> sortBase = Utility.ordered(nonCapBase,
                            new ManhattanDistance(cleat.position()));
                    Goal g = null;

                    if (sortBase.size() > 1)
                        g = new BaseGoal(sortBase.get(1));
                    else
                        g = new BaseGoal(sortBase.get(0));

                    goals.put(cleat, g);
                    List<Position> path = Pathfinding.getPath(turn, cleat
                            .position(), ((BaseGoal) g).getBase().position());
                    paths.put(cleat, path);
                    Position newPos = null;

                    int pathlength = paths.get(cleat).size();
                    for (int i = 0; i < Unit.statistic(Stat.MOVE, cleat.perk())
                            && i < pathlength; i++)
                        newPos = paths.get(cleat).remove(0);
                    return new MoveAction(newPos);

                }
                default:
                    return null;

            }
        }

    }

}
