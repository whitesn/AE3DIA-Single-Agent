package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.Action;
import uk.ac.nott.cs.g53dia.library.MoveTowardsAction;
import uk.ac.nott.cs.g53dia.library.Point;

public class IdlePlan extends Plan {
	
	private Point playerPoint;

	public IdlePlan(PlanType pt, Position planLocation, Point playerPoint) {
		super(pt, planLocation);
		this.playerPoint = playerPoint;
	}

	public Action runPlan( Position playerPos )
	{
		this.isTaskDone = true;
		return new MoveTowardsAction( playerPoint );
	}
}
