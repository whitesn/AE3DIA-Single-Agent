package uk.ac.nott.cs.g53dia.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.library.Tanker;
import uk.ac.nott.cs.g53dia.library.Task;
import uk.ac.nott.cs.g53dia.library.Well;

public class DeliveryPlanner {
	
	/*
	 *  - DeliverWater
	 *  - Refuel, DeliverWater
	 *  - LoadWater, DeliverWater
	 *  - Refuel, LoadWater, DeliverWater
	 *  - LoadWater, Refuel, DeliverWater
	 *  - DeliverWater, LoadWater, DeliverWater
	 */
	public final static ArrayList<ArrayList<PlanType>> scenarios = new ArrayList<ArrayList<PlanType>>() 
	{
		private static final long serialVersionUID = 1L;
		{
			add( 
				new ArrayList<PlanType>()
				{
					private static final long serialVersionUID = 1L;
					{
						add( PlanType.DeliverWater );
					}
				}
			);
			
			add( 
					new ArrayList<PlanType>()
					{
						private static final long serialVersionUID = 1L;
						{
							add( PlanType.Refuel );
							add( PlanType.DeliverWater );
						}
					}
			);
			
			add( 
					new ArrayList<PlanType>()
					{
						private static final long serialVersionUID = 1L;
						{
							add( PlanType.LoadWater );
							add( PlanType.DeliverWater );
						}
					}
			);
			
			add( 
					new ArrayList<PlanType>()
					{
						private static final long serialVersionUID = 1L;
						{
							add( PlanType.Refuel );
							add( PlanType.LoadWater );
							add( PlanType.DeliverWater );
						}
					}
			);
			
			add(
					new ArrayList<PlanType>()
					{
						private static final long serialVersionUID = 1L;
						{
							add( PlanType.LoadWater );
							add( PlanType.Refuel );
							add( PlanType.DeliverWater );
						}
					}
			);
			
			add(
					new ArrayList<PlanType>()
					{
						private static final long serialVersionUID = 1L;
						{
							add( PlanType.DeliverWater );
							add( PlanType.LoadWater );
							add( PlanType.DeliverWater );
						}
					}
			);
		}
	};
	
	/*
	 * Simulate the PlanSequence and see if player is able to finish the
	 * sequence without running out of gas, and sufficient water.
	 */
	public static boolean isPlanExecutable( PlanSequence ps, DemoTanker dt )
	{
		int fuel = dt.getFuelLevel();
		int water = dt.getWaterLevel();
		Position playerPos = dt.playerPos.clone();
		
		@SuppressWarnings("unchecked")
		ArrayList<Plan> planSeq = (ArrayList<Plan>) ps.planSeq.clone();

		for( int i = 0; i < planSeq.size(); i++ )
		{
			Plan p = planSeq.get(i);
			
			fuel -= Helper.calculateDistance(playerPos, p.getPlanPosition());
			if( fuel <= 1 )
			{
				return false;
			}
			
			switch( p.planType )
			{
			case Refuel:
				fuel = Tanker.MAX_FUEL;
				break;
				
			case LoadWater:
				water = Tanker.MAX_WATER;
				break;
				
			case DeliverWater:
				int required = ((DeliveryPlan) p).deliveryTask.getRequired();
				water -= required;
				if( water < 0 && i == planSeq.size() - 1 )
				{
					return false;
				}
				break;
				
			default:
				break;
			}
			
			playerPos = p.getPlanPosition().clone();
		}

		fuel -= Helper.calculateDistance(playerPos, DemoTanker.FUEL_STATION_POS);
		if( fuel <= 1 )
		{
			return false;
		}
		
		return true;
	}
	
	/*
	 * Find the best Well to be included in a Plan based on the
	 * sequence of the Plan.
	 */
	public static Well getBestWell( HashMap<Well,Position> wellList, Position taskPos, 
			Position playerPos,	ArrayList<PlanType> planSequence )
	{
		Well bestWell = null;
		int bestDistance = Integer.MAX_VALUE;
		
		for( Well w : wellList.keySet() )
		{
			Position curPos = playerPos.clone();
			int totalDistance = 0;
			
			for( PlanType pt : planSequence )
			{
				switch( pt )
				{
				case DeliverWater:
					totalDistance += Helper.calculateDistance(curPos, taskPos);
					curPos = taskPos.clone();
					break;	
					
				case LoadWater:
					Position wellPos = wellList.get(w);
					totalDistance += Helper.calculateDistance(curPos, wellPos);
					curPos = wellPos.clone();
					break;
					
				case Refuel:
					totalDistance += Helper.calculateDistance(curPos, DemoTanker.FUEL_STATION_POS);
					curPos = DemoTanker.FUEL_STATION_POS;
					break;
				default:
					break;
				}
			}
			
			if( totalDistance < bestDistance )
			{
				bestDistance = totalDistance;
				bestWell = w;
			}
		}
		
		return bestWell;
	}

    public static PlanSequence generateDeliveryPlan( DemoTanker dt )
    {
		HashMap<Task,Position> tasks = dt.tasks;
		HashMap<Well,Position> wellList = dt.wellList;
		Position playerPos = dt.playerPos;

    	PlanSequence bestPlanSequence = null;
    	int highestValue = Integer.MIN_VALUE;
    	
		Iterator<Entry<Task, Position>> iter = tasks.entrySet().iterator();
		
		while( iter.hasNext() )
		{
			Entry<Task, Position> pair = iter.next();
			Task t = pair.getKey();
			Position taskPos = tasks.get(t);

			if( !t.isComplete() )
			{
				int bestScoreScenario = Integer.MIN_VALUE;
				PlanSequence bestScenario = null;
	
				/*
				 * For the Task, calculate the value (score) for all scenarios
				 * listed above in "scenarios", then get the best possible Task
				 * alongside with the PlanSequence.
				 */
				for( ArrayList<PlanType> scenario : scenarios )
				{
					ArrayList<Plan> scenarioPlanSeq = new ArrayList<Plan>();
					Well nearestWell = getBestWell( wellList, taskPos, playerPos, scenario );
					Position wellPos = wellList.get( nearestWell );
					
					for( PlanType pt : scenario )
					{
						Plan p = null;
						
						switch( pt )
						{
						case DeliverWater:
							p = new DeliveryPlan( pt, taskPos, t );
							break;
							
						case LoadWater:
							p = new Plan( pt, wellPos );
							break;
							
						case Refuel:
							p = new Plan( pt, DemoTanker.FUEL_STATION_POS );
							break;
							
						default:
							break;
						}

						scenarioPlanSeq.add( p );
					}
					
					PlanSequence ps = new PlanSequence( scenarioPlanSeq, PlanType.DeliverWater );
					
					if( isPlanExecutable(ps, dt) )
					{
						int value = Helper.calculatePlanSequenceValue(ps, dt);
						
						if( value > bestScoreScenario )
						{
							bestScenario = ps;
							bestScoreScenario = value;
						}
					}
				}
				
				// Update the best score and best Plan Sequence
				if( bestScoreScenario > highestValue )
				{
					highestValue = bestScoreScenario;
					bestPlanSequence = bestScenario;
				}
			}
			else
			{
				iter.remove();
			}
		}
    	
    	return bestPlanSequence;
    }
}
