package competitor.util;

import snowbound.api.Tile;

public class GatherGoal extends Goal
{

    private Tile m_tile;
    public GatherGoal(Tile tile)
    {
        m_goal = GOAL.GATHER;
        m_tile = tile;
    }
    
    public Tile getTile()
    {
        return m_tile;
    }
}
