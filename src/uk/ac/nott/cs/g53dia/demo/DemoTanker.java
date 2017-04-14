package uk.ac.nott.cs.g53dia.demo;

import java.util.ArrayList;
import java.util.HashMap;

import uk.ac.nott.cs.g53dia.demo.Helper;
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

	public final static int VAL_WATER = 40;
	public final static int VAL_WATER_DELIVERED = 90;
	public final static int VAL_TASK_DELIVERED = 5000;
	public final static int VAL_FUEL = 80;
	public final static int VAL_FUEL_BURNED = 80;
	public final static int VAL_TIMESTEP = 130;
	public final static int VAL_AREA_DISCOVERED = 100;

	public final static int STATION_RECON_TOP_DATA_TAKE = 8;

	Cell[][] currentCellData;
	boolean[][] cellDiscovered = new boolean[MAP_BOUND][MAP_BOUND];
	long currentTimeStep;
	HashMap<Well, Position> wellList;
	HashMap<Position, Station> stationList;
	HashMap<Task, Position> tasks;
	ArrayList<PlanSequence> plansQueue;
	Position playerPos;
	boolean isWholeMapDiscovered = false;

	ArrayList<Position> reconPos = null;
	int reconPosEntry = 0;

    public DemoTanker()
	{
		wellList = new HashMap<Well,Position>();
		stationList = new HashMap<Position,Station>();
		tasks = new HashMap<Task,Position>();
		plansQueue = new ArrayList<PlanSequence>();
		playerPos = new Position( 0, 0 );

		for( int i = 0; i < MAP_BOUND; i++ )
		{
			for( int j = 0; j < MAP_BOUND; j++ )
			{
				cellDiscovered[i][j] = false;
			}
		}
	}

	private void scanSurroundings()
	{
		Cell[][] rows = currentCellData;

		for( int row = 0; row < rows.length; row++ )
		{
			for( int col = 0; col < rows.length; col++ )
			{
				Cell cell = rows[col][row];
				Position pos = Helper.getPositionFromView( row, col, playerPos );
				Position discoveredCell = Helper.convertToUnsignedPos( pos );
				cellDiscovered[discoveredCell.x][discoveredCell.y] = true;

				switch( Helper.getCellType(cell) )
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
					if( !Helper.isStationStored(pos, stationList) )
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
			isWholeMapDiscovered = Helper.isWholeMapDiscovered(cellDiscovered);

			if( isWholeMapDiscovered )
			{
				reconPos = Helper.getMostStationRecon( stationList );
			}
		}
	}

	private boolean isRefueling()
	{
		return !plansQueue.isEmpty() && plansQueue.get(0).getPlanType() == PlanType.Refuel;
	}

	private void fuel()
	{
		if( !isRefueling() )
		{
			int distance = Helper.calculateDistance( playerPos, FUEL_STATION_POS );

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

	private void recon()
	{
		if( plansQueue.isEmpty() )
		{
			Position reconToPosition = null;

			if( !isWholeMapDiscovered )
			{
				reconToPosition = Helper.getReconLocation( cellDiscovered, playerPos );
				if( reconToPosition == null )
				{
					System.err.println("ERROR: Failed to find undiscovered part of map's direction");
					Helper.printCellDiscovered(cellDiscovered, playerPos);
					Helper.halt();
				}
			}
			else
			{
				reconToPosition = reconPos.get(reconPosEntry);
				if( ++reconPosEntry >= reconPos.size() )
				{
					reconPosEntry = 0;
				}
			}

			ArrayList<Plan> planSeq = new ArrayList<Plan>();

			if( reconToPosition.isEqualCoord(playerPos) )
			{
				planSeq.add( new IdlePlan(PlanType.Idle, playerPos, getCurrentCell(currentCellData).getPoint()) );
			}
			else
			{
				planSeq.add( new Plan(PlanType.Recon, reconToPosition) );
			}

			PlanSequence newPlans = new PlanSequence( planSeq, PlanType.Recon );
			plansQueue.add( newPlans );
		}
	}

	private void task()
	{
		if( !isRefueling() && Helper.noWaterDeliveryPlan(plansQueue) )
		{
			PlanSequence waterDeliveryPlan = DeliveryPlanner.generateDeliveryPlan( this );

			if( waterDeliveryPlan != null )
			{
				Helper.insertDeliveryPlan(waterDeliveryPlan, plansQueue);
			}
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
			planSeq.alternativePlan(playerPos, this);
			act = planSeq.runPlan(playerPos);
			if( planSeq.isTaskDone() )
			{
				plansQueue.remove( planSeq );
			}
		}
		else
		{
			// SHOULD NEVER REACHED HERE, MIGHT BE POTENTIAL LOOP
			System.err.println("ERR: EMPTY PLANS");
			Helper.halt();
		}

		System.out.println("Action: " + act.toString());

		return act;
    }

	private void verifyPlayerPos()
	{
		Point storedPos = getCurrentCell(currentCellData).getPoint();

		if( !Helper.checkIfPointEqualPosition(storedPos, playerPos) )
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
		System.out.println("Distance to FS: \t" + Helper.calculateDistance(playerPos, FUEL_STATION_POS));
		Helper.printPlans( plansQueue );
	}
}
