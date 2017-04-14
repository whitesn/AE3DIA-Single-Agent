package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.Action;
import uk.ac.nott.cs.g53dia.library.DeliverWaterAction;
import uk.ac.nott.cs.g53dia.library.MoveAction;
import uk.ac.nott.cs.g53dia.library.Task;

public class DeliveryPlan extends Plan {

	Task deliveryTask;
	
	public DeliveryPlan(PlanType pt, Position planLocation, Task deliveryTask) {
		super(pt, planLocation);
		this.deliveryTask = deliveryTask;
	}

	public Action runPlan( Position playerPos )
	{
		if( isPlayerArrived(playerPos) )
		{
			isTaskDone = true;
			return new DeliverWaterAction( deliveryTask );
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
	}
	
	
}
