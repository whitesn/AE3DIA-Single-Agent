package uk.ac.nott.cs.g53dia.demo;

import java.util.ArrayList;
import java.util.Iterator;

import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.library.Action;

public class PlanSequence {

	ArrayList<Plan> planSeq;
	PlanType pt;
	
	public PlanSequence( ArrayList<Plan> planSeq, PlanType pt )
	{
		if( planSeq.size() == 0 )
		{
			System.err.println("ERR: Empty Plan Sequence being initialized");
			System.err.println( planSeq );
			System.err.println("Plan Type: " + pt);
			DemoTankerHelper.halt();
		}
		
		this.planSeq = planSeq;
		this.pt = pt;
	}
	
	public PlanType getPlanType()
	{
		return pt;
	}
	
	public Action runPlan( Position playerPos )
	{
		Iterator<Plan> iter = planSeq.iterator();
		
		while( iter.hasNext() )
		{
			Plan p = iter.next();

			if( !p.isTaskDone() )
			{
				Action a = p.runPlan(playerPos);
				
				if( p.isTaskDone() )
				{
					iter.remove();
				}
				
				return a;
			}
		}
		
		return null;
	}
	
	public boolean isTaskDone()
	{
		for( Plan p : planSeq )
		{
			if( !p.isTaskDone() )
			{
				return false;
			}
		}
		
		return true;
	}
	
	public boolean alternativeTask( Plan altPlan )
	{
		return true;
	}
	
	public void printPlans()
	{
		System.out.print("[ ");
		for( int i = 0; i < planSeq.size(); i++ )
		{
			Plan p = planSeq.get(i);
			System.out.print( p.toString() );
			if( i != planSeq.size() - 1 )
			{
				System.out.print(" || ");
			}
		}
		System.out.print(" ]");
	}
}
