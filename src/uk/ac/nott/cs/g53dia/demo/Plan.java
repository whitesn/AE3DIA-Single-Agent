package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;

public class Plan {
	
	 public enum PlanType {
        DeliverWater,
        LoadWater,
        Refuel,
        Recon
    }
	 
	PlanType planType;
	Position planLocation;
	Task deliveryTask;
	boolean isTaskDone;
	
	public Plan( PlanType pt, Position planLocation, Task deliveryTask )
	{
		this.planType = pt;
		this.planLocation = planLocation;
		this.deliveryTask = deliveryTask;
		this.isTaskDone = false;
	}
	
	private boolean isPlayerArrived( Position playerPos )
	{
		return this.planLocation.isEqualCoord( playerPos );
	}
	
	public boolean isTaskDone()
	{
		return isTaskDone;
	}
	
	public PlanType getPlanType()
	{
		return planType;
	}
	
	public Action runPlan( Position playerPos )
	{
		if( isPlayerArrived(playerPos) )
		{
			isTaskDone = true;
			
			switch( planType )
			{
			case DeliverWater:
				return new DeliverWaterAction( deliveryTask );
				
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
			int direction = DemoTankerHelper.getDirectionToward(playerPos, planLocation);
			DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
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
		return "[" + planType + "]," + planLocation.toString();
	}
}