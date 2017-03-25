package uk.ac.nott.cs.g53dia.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import uk.ac.nott.cs.g53dia.demo.DemoTankerHelper;
import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.demo.Position;
import uk.ac.nott.cs.g53dia.library.*;

/**
 * A simple example tanker that chooses actions.
 *
 * @author Julian Zappala
 */
/*
 *
 * Copyright (c) 2011 Julian Zappala
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
public class DemoTanker extends Tanker
{
	public final static int MAP_TRAVELLABLE_BOUND = ((Tanker.MAX_FUEL - 1) / 2);
	public final static int MAP_BOUND = MAP_TRAVELLABLE_BOUND * 2 + 1;
	public final static Position FUEL_STATION_POS = new Position(0,0);
	
	Cell[][] currentCellData;
	boolean[][] cellDiscovered = new boolean[MAP_BOUND][MAP_BOUND];
	long currentTimeStep;
	Position activeTaskPosition;
	HashMap<Well, Position> wellList;
	HashMap<Position, Station> stationList;
	HashMap<Task, Position> tasks;
	ArrayList<PlanSequence> plansQueue;
	Position playerPos;
	boolean isWholeMapDiscovered = false;

    public DemoTanker()
	{
		wellList = new HashMap<Well,Position>();
		stationList = new HashMap<Position,Station>();
		tasks = new HashMap<Task,Position>();
		plansQueue = new ArrayList<PlanSequence>();
		playerPos = new Position( 0, 0 );
		
		for( int i = 0; i < MAP_TRAVELLABLE_BOUND; i++ )
		{
			for( int j = 0; j < MAP_TRAVELLABLE_BOUND; j++ )
			{
				cellDiscovered[i][j] = false;
			}
		}
	}

	/*
	 * Scanner v1.0
	 *
	 * Scan Surroundings, get all informations that can be useful
	 */
	private void scanSurroundings()
	{
		Cell[][] rows = currentCellData;

		for( int row = 0; row < rows.length; row++ )
		{
			for( int col = 0; col < rows.length; col++ )
			{
				Cell cell = rows[col][row];
				Position pos = DemoTankerHelper.getPositionFromView( row, col, playerPos );
				Position discoveredCell = DemoTankerHelper.convertToUnsignedPos( pos );
				cellDiscovered[discoveredCell.x][discoveredCell.y] = true;

				switch( DemoTankerHelper.getCellType(cell) )
				{
					case Well:
					Well w = (Well) cell;
					if( !wellList.containsKey(w) )
					{
						wellList.put( w, pos );
					}
					break;

					case Station:
					Station s = (Station) cell;
					if( !DemoTankerHelper.isStationStored(pos, stationList) )
					{
						stationList.put( pos, s );
					}

					Task t = s.getTask();

					if( t != null && !t.isComplete() && !tasks.containsKey(t) )
					{
						tasks.put( t, pos );
					}
					break;

					case EmptyCell:
					case FuelPump:
					case DefaultCell:
					default:
					break;
				}
			}
		}
		
		if( !isWholeMapDiscovered )
		{
			isWholeMapDiscovered = DemoTankerHelper.isWholeMapDiscovered(cellDiscovered);
		}
	}
	
	/*
	 * If Fuel Level is reaching the bottom threshold, forget all tasks and go directly
	 * to Fuel Station to refuel.
	 */
	private void fuel()
	{
		if( plansQueue.isEmpty() || plansQueue.get(0).getPlanType() != PlanType.Refuel )
		{
			int distance = DemoTankerHelper.calculateDistance( playerPos, FUEL_STATION_POS );
			
			if( getFuelLevel() - distance <= 1 )
			{
				System.out.println(">>>>>>>>> ADD FUEL PLAN");
				
				ArrayList<Plan> newPlans = new ArrayList<Plan>();
				newPlans.add( new Plan(PlanType.Refuel, FUEL_STATION_POS) );
				
				PlanSequence planSeq = new PlanSequence( newPlans, PlanType.Refuel );
				plansQueue.add( 0, planSeq );
			}
		}
	}

	/*
	 * When no activities are to be done, do recon randomly.
	 */
	private void recon()
	{
		if( plansQueue.isEmpty() )
		{
			int direction;
			if( !isWholeMapDiscovered )
			{
				direction = DemoTankerHelper.getDirectionMostAreaDiscovered(cellDiscovered, playerPos);
				if( direction == -1 )
				{
					System.err.println("ERROR: Failed to find undiscovered part of map's direction");
					DemoTankerHelper.printCellDiscovered(cellDiscovered, playerPos);
					DemoTankerHelper.halt();
				}
			}
			else
			{
				direction = (int) (Math.random() * 8);
			}
			
			Position newPos = playerPos.clone();
			DemoTankerHelper.playerMoveUpdatePosition(newPos, direction);
			
			ArrayList<Plan> planSeq = new ArrayList<Plan>();
			planSeq.add( new Plan(PlanType.Recon, newPos) );

			System.out.println(">>>>>>>>> ADD RECON PLAN");
			PlanSequence newPlans = new PlanSequence( planSeq, PlanType.Recon );
			plansQueue.add( newPlans );
		}
	}

	/*
	 * Add Task to Plan Queue if no Water Delivery Plan in Plan Queue
	 */
	private void task()
	{
		if( DemoTankerHelper.noWaterDeliveryPlan(plansQueue) )
		{
			ArrayList<Plan> deliveryPlanSet = new ArrayList<Plan>();
			Iterator<Entry<Task, Position>> iter = tasks.entrySet().iterator();
			
			while( iter.hasNext() )
			{
				Entry<Task, ?> pair = ((Entry<Task, Position>) iter.next());
				Task task = (Task) pair.getKey();
				Position taskPos = tasks.get(task);
				Well nearestWell = DemoTankerHelper.getNearestWell(playerPos, wellList);
				Position wellPos = wellList.get( nearestWell );
				
				/*
				 *  Several Scenarios:
				 *  - DeliverWater
				 *  - Refuel, DeliverWater
				 *  - LoadWater, DeliverWater
				 *  - Refuel, LoadWater, DeliverWater
				 *  - Refuel, LoadWater, Refuel, DeliverWater (should this even be done?)
				 */

				if( !task.isComplete() )
				{
					if( getWaterLevel() >= task.getWaterDemand() )
					{
						// DeliverWater
						int playerToStation = DemoTankerHelper.calculateDistance( playerPos, taskPos );
						int stationToFuelS = DemoTankerHelper.calculateDistance( taskPos, FUEL_STATION_POS );
						
						if( getFuelLevel() - (playerToStation + stationToFuelS) >= 1 )
						{
							deliveryPlanSet.add( new DeliveryPlan(PlanType.DeliverWater, taskPos, task) );
							break;
						}
					}
					else
					{
						int playerToWell = DemoTankerHelper.calculateDistance( playerPos, wellPos );
						int FuelSToWell = DemoTankerHelper.calculateDistance( FUEL_STATION_POS, wellPos );
						int wellToStation = DemoTankerHelper.calculateDistance( wellPos, taskPos );
						int stationToFuelS = DemoTankerHelper.calculateDistance( taskPos, FUEL_STATION_POS );
						
						if( getFuelLevel() - (playerToWell + wellToStation + stationToFuelS) >= 1 )
						{
							// LoadWater, DeliverWater
							System.out.println("Player To Well: " + playerToWell);
							System.out.println("Well To Station: " + wellToStation);
							System.out.println("Station To Fuel: " + stationToFuelS);
							
							deliveryPlanSet.add( new Plan(PlanType.LoadWater,wellPos) );
							deliveryPlanSet.add( new DeliveryPlan(PlanType.DeliverWater,taskPos,task) );
							break;
						}
						else if( Tanker.MAX_FUEL - (FuelSToWell + wellToStation + stationToFuelS) >= 1 )
						{
							// Refuel, LoadWater, DeliverWater
							System.out.println("Fuel To Well: " + FuelSToWell);
							System.out.println("Well To Station: " + wellToStation);
							System.out.println("Station To Fuel: " + stationToFuelS);

							deliveryPlanSet.add( new Plan(PlanType.Refuel,DemoTanker.FUEL_STATION_POS) );
							deliveryPlanSet.add( new Plan(PlanType.LoadWater,wellPos) );
							deliveryPlanSet.add( new DeliveryPlan(PlanType.DeliverWater,taskPos,task) );
							break;
						}
					}
				}
				else
				{ 
					iter.remove();
				}
			}
			
			if( !deliveryPlanSet.isEmpty() )
			{
				PlanSequence newPlans = new PlanSequence(deliveryPlanSet, PlanType.DeliverWater);
				DemoTankerHelper.insertDeliveryPlan( newPlans, plansQueue );
			}
		}
	}
	
	public void deliberateRefill()
	{
		if( getWaterLevel() != Tanker.MAX_WATER )
		{
			
		}
		
		if( getFuelLevel() != Tanker.MAX_FUEL )
		{
			
		}
	}

    public Action senseAndAct(Cell[][] view, long timestep)
	{
		currentCellData = view;
		currentTimeStep = timestep;

		// Debug Purpose: Check if Position (generated) is Equal to Point (default)
		verifyPlayerPos();
		
		// -- Update Surroundings Data --
		scanSurroundings();
		
		// -- Update the Plans Queue --
		task();
		fuel();
		recon();
		// ----------------------------

		debug();
		
		Action act = null;
		
		if( plansQueue.size() > 0 )
		{
			PlanSequence planSeq = plansQueue.get(0);
			act = planSeq.runPlan(playerPos);
			if( planSeq.isTaskDone() )
			{
				plansQueue.remove( planSeq );
			}
		}
		else
		{
			// SHOULD NEVER REACHED HERE, MIGHT BE POTENTIAL LOOP
			System.err.println("ERROR: EMPTY PLANS");
			DemoTankerHelper.halt();
		}
		
		System.out.println("Action: " + act.toString());

		return act;
    }

    /*
	private Action doNothing()
	{
		return new MoveTowardsAction( getCurrentCell(currentCellData).getPoint() );
	}
	*/

	private void verifyPlayerPos()
	{
		Point storedPos = getCurrentCell(currentCellData).getPoint();

		if( !DemoTankerHelper.checkIfPointEqualPosition(storedPos, playerPos) )
		{
			System.err.println("ERR: Player Position is not the same as the actual point!");
			System.err.println("STORED: " + storedPos.toString() );
			System.err.println("ACTUAL: " + playerPos.toString() );
		}
	}
	
	public void debug()
	{
		System.out.println("===== TIMESTEP: " + currentTimeStep + " =====" );
		System.out.println("Position: \t\t" + playerPos.toString());
		System.out.println("Station Found: \t\t" + stationList.size());
		System.out.println("Well Found: \t\t" + wellList.size());
		System.out.println("Task Waiting: \t\t" + tasks.size());
		System.out.println("Whole Map Discovered: \t" + isWholeMapDiscovered);
		System.out.println("Fuel Level: \t\t" + getFuelLevel());
		System.out.println("Plans in Queue: \t" + plansQueue.size());
		System.out.println("Distance to FS: \t" + DemoTankerHelper.calculateDistance(playerPos, FUEL_STATION_POS));
		DemoTankerHelper.printPlans( plansQueue );
	}
}
