package competitor.util;

import snowbound.api.Base;

public class BaseGoal extends Goal
{

    private Base m_base;

    public BaseGoal(Base base)
    {
        m_goal = GOAL.BASE;
        m_base = base;
    }

    public Base getBase()
    {
        return m_base;
    }

}
