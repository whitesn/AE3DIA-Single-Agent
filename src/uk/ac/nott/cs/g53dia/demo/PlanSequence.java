package uk.ac.nott.cs.g53dia.demo;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.library.Action;
import uk.ac.nott.cs.g53dia.library.Well;

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
			Helper.halt();
		}
		
		this.planSeq = planSeq;
		this.pt = pt;
	}

	public Position getLastPlanPosition()
	{
		return planSeq.get(planSeq.size() - 1).getPlanPosition();
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
	
	public Position getDeliveryPlanPosition()
	{
		Position pos = null;
		
		if( pt == PlanType.DeliverWater )
		{
			for( Plan p : planSeq )
			{
				if( p.planType == PlanType.DeliverWater )
				{
					pos = p.getPlanPosition();
					break;
				}
			}
		}
			
		return pos;
	}
	
	private SimpleEntry<Integer,ArrayList<Plan>> loadWaterAlternative( Position playerPos, DemoTanker dt )
	{
		HashMap<Well, Position> wellList = dt.wellList;
		SimpleEntry<Integer,ArrayList<Plan>> lwAlt = null;
		
		int addBeforeVal = Integer.MIN_VALUE; 
		int addAfterVal = Integer.MIN_VALUE;
		
		Well w = Helper.getNearestWell( wellList, playerPos );
		
		if( w != null )
		{
			Position pos = wellList.get(w);
			Plan p = new Plan( PlanType.LoadWater, pos );
			
			@SuppressWarnings("unchecked")
			ArrayList<Plan> addBeforeSeq = (ArrayList<Plan>) planSeq.clone();
			addBeforeSeq.add( 0, p );
			PlanSequence addBeforePlan = new PlanSequence( addBeforeSeq, pt );
			if( Helper.isPlanDoable(addBeforePlan, dt) )
			{
				addBeforeVal = Helper.calculatePlanSequenceValue( addBeforePlan, dt );
			}
			
			@SuppressWarnings("unchecked")
			ArrayList<Plan> addAfterSeq = (ArrayList<Plan>) planSeq.clone();
			addAfterSeq.add( p );
			PlanSequence addAfterPlan = new PlanSequence( addAfterSeq, pt );
			if( Helper.isPlanDoable(addAfterPlan, dt) )
			{
				addAfterVal = Helper.calculatePlanSequenceValue( addAfterPlan, dt );
			}
			
			if( addBeforeVal < addAfterVal )
			{
				lwAlt = new SimpleEntry<Integer, ArrayList<Plan>>( addAfterVal, addBeforeSeq );
			}
			else
			{
				lwAlt = new SimpleEntry<Integer, ArrayList<Plan>>( addBeforeVal, addAfterSeq );
			}
		}
		
		return lwAlt;
	}
	
	private SimpleEntry<Integer,ArrayList<Plan>> refuelAlternative( Position playerPos, DemoTanker dt )
	{
		SimpleEntry<Integer,ArrayList<Plan>> refuelAlt = null;
		
		Plan p = new Plan( PlanType.Refuel, DemoTanker.FUEL_STATION_POS );
		
		int addBeforeVal = Integer.MIN_VALUE; 
		int addAfterVal = Integer.MIN_VALUE;
		
		@SuppressWarnings("unchecked")
		ArrayList<Plan> addBeforeSeq = (ArrayList<Plan>) planSeq.clone();
		addBeforeSeq.add( 0, p );
		PlanSequence addBeforePlan = new PlanSequence( addBeforeSeq, pt );	
		if( Helper.isPlanDoable(addBeforePlan, dt) )
		{
			addBeforeVal = Helper.calculatePlanSequenceValue( addBeforePlan, dt );
		}
		
		@SuppressWarnings("unchecked")
		ArrayList<Plan> addAfterSeq = (ArrayList<Plan>) planSeq.clone();
		addBeforeSeq.add( p );
		PlanSequence addAfterPlan = new PlanSequence( addAfterSeq, pt );
		if( Helper.isPlanDoable(addAfterPlan, dt) )
		{
			addAfterVal = Helper.calculatePlanSequenceValue( addAfterPlan, dt );
		}
		
		if( addBeforeVal < addAfterVal )
		{
			refuelAlt = new SimpleEntry<Integer, ArrayList<Plan>>( addAfterVal, addAfterSeq );
		}
		else
		{
			refuelAlt = new SimpleEntry<Integer, ArrayList<Plan>>( addBeforeVal, addBeforeSeq );
		}
			
		return refuelAlt;
	}
	
	public void alternativePlan( Position playerPos, DemoTanker dt )
	{
		if( planSeq.size() == 1 )
		{
			int initialValue = Helper.calculatePlanSequenceValue( this, dt );
	
			SimpleEntry<Integer,ArrayList<Plan>> lwAlt;
			SimpleEntry<Integer,ArrayList<Plan>> refuelAlt;
	
			switch( pt )
			{
			case Recon:
				lwAlt = loadWaterAlternative(playerPos, dt);
				refuelAlt = refuelAlternative(playerPos, dt);
				
				SimpleEntry<Integer,ArrayList<Plan>> bestAltPlan = (lwAlt != null && lwAlt.getKey() > refuelAlt.getKey()) ? lwAlt : refuelAlt;
				
				if( bestAltPlan.getKey() > initialValue )
				{
					planSeq = bestAltPlan.getValue();
				}
				break;
				
			case Refuel:
				lwAlt = loadWaterAlternative(playerPos, dt);
				if( lwAlt != null && lwAlt.getKey() > initialValue )
				{
					planSeq = lwAlt.getValue();
				}
				break;
				
			default:
				break;
			}
		}
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
