package competitor;

import static snowbound.api.util.Utility.any;
import static snowbound.api.util.Utility.min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import snowbound.api.AI;
import snowbound.api.Action;
import snowbound.api.Agent;
import snowbound.api.Base;
import snowbound.api.CaptureAction;
import snowbound.api.Direction;
import snowbound.api.GatherAction;
import snowbound.api.MoveAction;
import snowbound.api.Pathfinding;
import snowbound.api.Perk;
import snowbound.api.Position;
import snowbound.api.ShoutAction;
import snowbound.api.SpawnAction;
import snowbound.api.Stat;
import snowbound.api.ThrowAction;
import snowbound.api.Tile;
import snowbound.api.Turn;
import snowbound.api.Unit;
import snowbound.api.util.BaseHasOwner;
import snowbound.api.util.ManhattanDistance;
import snowbound.api.util.Owned;
import snowbound.api.util.TileHasSnow;
import snowbound.api.util.TileIsPassable;
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
    private int                           numOfLayers   = 0;

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
        // System.err.println("MAX UNITS: " + maxUnits);
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
                    // if (numOfPitchers < 1)
                    // {
                    // numOfPitchers++;
                    // //System.out.println("Spawning LAYERS");
                    // perks.put(unit, Perk.PITCHER);
                    // } else
//                    if (numOfLayers < 1)
//                    {
//                        numOfLayers++;
//                        // //System.out.println("Spawning LAYERS");
//                        perks.put(unit, Perk.LAYERS);
//                    } else
                    {
                        perks.put(unit, Perk.CLEATS);
                    }
                    break;
                }
                case 5:
                case 6:
                {
                    if (numOfPitchers <= 1)
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

        // //System.err.println(perks.get(player).toString());
        goals.put(player, new EmptyGoal());
        Collection<Tile> spawns = Utility.retain(
                turn.tilesAt(turn.myTeam().spawns()), new TileIsPassable(turn));

        return new SpawnAction(any(spawns).position(), perks.get(player));

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
                    // //System.out.println("PITCHER: Doing Nothing");
                    if (pitcher.snowballs() < Unit.statistic(Stat.CAPACITY,
                            pitcher.perk()))
                    {
                        Tile snow = Utility.nearest(
                                Utility.retain(Utility.retain(turn.tiles(),
                                        new TileHasSnow()), new TileIsPassable(
                                        turn)), pitcher);
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
                    // //System.out.println("PITCHER: FIGHTING");
                    if (pitcher.snowballs() == 0)
                    {
                        goal = new EmptyGoal();
                        break;
                    }

                    // Attack in range
                    FightGoal g = new FightGoal(turn.current(((FightGoal) goal)
                            .getUnit()));
                    goal = g;
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
                    // //System.out.println("PITCHER: GATHERING SNOW");
                    GatherGoal g = new GatherGoal(
                            turn.current(((GatherGoal) goal).getTile()));
                    if (pitcher.snowballs() == Unit.statistic(Stat.CAPACITY,
                            pitcher.perk()))
                    {
                        // //System.out.println(pitcher);
                        Unit enemy = Utility
                                .nearest(turn.enemyUnits(), pitcher);

                        goal = new FightGoal(enemy);
                        break;
                    }
                    boolean enemysnear = !pitcher.enemyUnitsInMoveRange()
                            .isEmpty();
                    Tile currentTile = turn.tileAt(pitcher);

                    if (!enemysnear
                            && turn.hasBaseAt(currentTile)
                            && !turn.baseAt(currentTile).isOwnedBy(
                                    turn.myTeam()))
                    {
                        goal = new BaseGoal(turn.baseAt(currentTile));

                        break;
                    }

                    Tile endTile = g.getTile();
                    if (endTile == null)
                    {
                        action = new ShoutAction("Tile no exist");
                        break;
                    }
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
        Action action = null;
        while (action == null)
        {
            switch (goal.getGoal())
            {
                case NONE:
                {
                    Collection<Base> bases = turn.allBases();
                    bases = Utility.filter(bases, new Owned(turn.myTeam()));

                    bases = Utility
                            .ordered(bases, new ManhattanDistance(cleat));

                    if (bases.isEmpty())
                    {
                        action = new ShoutAction("No uncaptured Bases");
                        break;
                    }

                    goal = new BaseGoal((Base) bases.toArray()[0]);

                    break;
                }
                case BASE:
                {
                    goal = new BaseGoal(turn.current(((BaseGoal) goal)
                            .getBase()));
                    BaseGoal g = (BaseGoal) goal;

                    Set<Position> basePoses = g.getBase().coverage();
                    Set<Unit> enemyUnits = turn.enemyUnits();
                    ArrayList<Position> positions = new ArrayList<>();
                    for (Unit e : enemyUnits)
                    {
                        positions.add(e.position());
                    }

                    Set<Position> eOnB = Utility
                            .intersect(positions, basePoses);

                    if (!eOnB.isEmpty() || eOnB.size() != 0)
                    {
                        // System.out.println("New Base needed");
                        Collection<Base> bases = turn.allBases();
                        bases = Utility.filter(bases, new Owned(turn.myTeam()));

                        bases = Utility.ordered(bases, new ManhattanDistance(
                                cleat));

                        if (bases.isEmpty())
                        {
                            action = new ShoutAction("No uncaptured Bases");
                            break;
                        }
                        // System.out.println("Found new Base1");
                        goal = new BaseGoal((Base) bases.toArray()[1]);
                        // System.out.println("Found new Base2");
                        break;
                    }

                    Tile currentTile = turn.tileAt(cleat);
                    if (turn.hasBaseAt(currentTile)
                            && !turn.baseAt(currentTile).isOwnedBy(
                                    turn.myTeam()))
                    {

                        action = new CaptureAction();
                        if (turn.baseAt(currentTile).equals(g.getBase()))
                            goal = new EmptyGoal();
                        break;

                    }

                    if (!paths.containsKey(cleat) || turnCount % 3 == 0
                            || paths.get(cleat).isEmpty())
                    {
                        List<Position> path = Pathfinding.getPath(turn,
                                cleat.position(), any(g.getBase().coverage()));
                        Set<Tile> goodMoves = Utility.retain(
                                turn.tilesAt(cleat.positionsInMoveRange()),
                                new TileIsPassable(turn));
                        Tile target = null;
                        if (path.size() == 1)
                        {
                            target = min(goodMoves, new ManhattanDistance(g
                                    .getBase().position()));
                            path.add(target.position());
                        }
                        paths.put(cleat, path);
                    }

                    Position newPos = null;

                    int pathlength = paths.get(cleat).size();
                    for (int i = 0; i < Unit.statistic(Stat.MOVE, cleat.perk())
                            && i < pathlength; i++)
                    {
                        Position testPos = paths.get(cleat).get(0);
                        if (new TileIsPassable(turn).eval(turn.tileAt(testPos))
                                || testPos.equals(cleat.position()))
                        {
                            newPos = paths.get(cleat).remove(0);
                        }
                    }

                    action = new MoveAction(newPos);

                    break;
                }
                default:
                    break;

            }
            goals.put(cleat, goal);
        }
        return action;
    }
}
