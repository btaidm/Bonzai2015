package competitor.util;

public abstract class Goal
{

    public enum GOAL
    {
        BASE, FIGHT, DEFEND, NONE
    }

    protected GOAL m_goal;

    public Goal()
    {

    }

    public GOAL getGoal()
    {
        return m_goal;
    }

}
