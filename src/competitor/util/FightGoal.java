package competitor.util;

import snowbound.api.Unit;

public class FightGoal extends Goal
{
    Unit m_unit;
    
    public FightGoal(Unit u)
    {
        m_goal = GOAL.FIGHT;
        m_unit = u;
    }
    
    public Unit getUnit()
    {
        return m_unit;
    }

}
