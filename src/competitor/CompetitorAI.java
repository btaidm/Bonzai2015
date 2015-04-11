package competitor;

import static snowbound.api.util.Utility.any;
import static snowbound.api.util.Utility.difference;
import static snowbound.api.util.Utility.min;

import java.util.HashMap;
import java.util.Map;

import snowbound.api.*;
import snowbound.api.util.ManhattanDistance;

@Agent(name = "Headcrabs")
public class CompetitorAI extends AI{
	
	private Map<Unit, Base> goals;
	
	public CompetitorAI(){
		this.goals = new HashMap<>();
	}
	
	/**
	 *
	 **/
	public Action action(Turn turn) {
		
		Unit player = turn.actor();
		
		if(!player.isSpawned())
		{
			return new SpawnAction(any(player.team().spawns()), Perk.CLEATS);
		}
		
		return null;
	}

}
