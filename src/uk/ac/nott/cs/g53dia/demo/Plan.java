package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;

public class Plan {
	
	 public enum PlanType {
        DeliverWater,
        LoadWater,
        Refuel,
        Recon,
        Idle
    }
	 
	protected Position planLocation;
	protected boolean isTaskDone;
	protected PlanType planType;
	
	public Plan( PlanType pt, Position planLocation )
	{
		this.planType = pt;
		this.planLocation = planLocation;
		this.isTaskDone = false;
	}
	
	public boolean isTaskDone()
	{
		return isTaskDone;
	}
	
	public PlanType getPlanType()
	{
		return planType;
	}
	
	public Position getPlanPosition()
	{
		return planLocation;
	}
	
	protected boolean isPlayerArrived( Position playerPos )
	{
		return this.planLocation.isEqualCoord( playerPos );
	}
	
	public Action runPlan( Position playerPos )
	{
		if( isPlayerArrived(playerPos) )
		{
			isTaskDone = true;
			
			switch( planType )
			{
			case LoadWater:
				return new LoadWaterAction();
				
			case Refuel:
				return new RefuelAction();
				
			default:
				break;
			}
		}
		else
		{
			int direction = Helper.getDirectionToward(playerPos, planLocation);
			Helper.playerMoveUpdatePosition( playerPos, direction );
			if( planType == PlanType.Recon )
			{
				isTaskDone = isPlayerArrived( playerPos );
			}
			return new MoveAction( direction );
		}

		return null;
	}
	
	public String toString()
	{
		return "[[" + planType + "]," + planLocation.toString() + "]";
	}
}
