package uk.ac.nott.cs.g53dia.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import uk.ac.nott.cs.g53dia.demo.DemoTankerHelper;
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
	final static int LOW_FUEL_THRESHOLD = (MAX_FUEL / 2) + 10;
	final ArrayList<Function> routines = new ArrayList<Function>();

	Action act;
	Cell[][] currentCellData;
	long currentTimeStep;
	Task activeTask;
	ArrayList<Well> wellList;
	ArrayList<Station> stationList;
	ArrayList<Task> tasks;

    public DemoTanker()
	{
		wellList = new ArrayList<Well>();
		stationList = new ArrayList<Station>();
		tasks = new ArrayList<Task>();
		act = null;
		activeTask = null;
		routines.add( this::fuel );
		routines.add( this::task );
		routines.add( this::recon );
	}

	@FunctionalInterface
	interface Function {
		Action call();
	}

	/*
	 * Scanner v1.0
	 *
	 * Scan Surroundings, get all informations that can be useful
	 */
	private void scanSurroundings()
	{
		for( Cell[] rows : currentCellData )
		{
			for( Cell col : rows )
			{
				switch( DemoTankerHelper.getCellType(col) )
				{
					case Well:
					if( !wellList.contains((Well) col) )
					{
						wellList.add( (Well) col );
					}
					break;

					case Station:
					Station s = (Station) col;
					if( !stationList.contains(s) )
					{
						stationList.add( s );
					}

					Task t = s.getTask();

					if( t != null && !tasks.contains(t) )
					{
						tasks.add( t );
						System.out.println("========= Task ============");
						System.out.println("Required: " + t.getRequired());
						System.out.println("Deliver to: " + t.getStationPosition());
						System.out.println("Water Demand: " + t.getWaterDemand());
						System.out.println("Is Complete: " + t.isComplete());

						if( activeTask == null )
						{
							activeTask = t;
						}
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
	}

	/*
	 * Fuel v1.0
	 *
	 * If Fuel Level is reaching the bottom threshold, forget all tasks and go directly
	 * to Fuel Station to refuel.
	 */
	private Action fuel()
	{
		Action a = null;

		if( DemoTankerHelper.isFuelLow(getFuelLevel()) )
		{
			if( DemoTankerHelper.getCellType(getCurrentCell(currentCellData)) == DemoTankerHelper.CellType.FuelPump )
			{
				a = new RefuelAction();
			}
			else
			{
				a = new MoveTowardsAction(FUEL_PUMP_LOCATION);
			}
		}

		return a;
	}

	/*
	 * Recon Action v1.0
	 *
	 * When no activities are to be done, do recon randomly.
	 */
	private Action recon()
	{
		return new MoveAction( (int)(Math.random()*8) );
	}

	/*
	 * Task v1.0
	 *
	 * Find nearest well, refill water, send
	 */
	private Action task()
	{
		Action a = null;

		if( activeTask != null )
		{
			if( activeTask.isComplete() )
			{
				activeTask = null;
				return a;
			}

			Point curLocation = getCurrentCell(currentCellData).getPoint();

			if( getWaterLevel() != MAX_WATER )
			{
				Well nearestWell = DemoTankerHelper.getNearestWell( wellList, getCurrentCell(currentCellData) );

				if( nearestWell != null )
				{
					Point nearestWellPoint = nearestWell.getPoint();

					if( nearestWellPoint.equals(curLocation) )
					{
						a = new LoadWaterAction();
					}
					else
					{
						a = new MoveTowardsAction( nearestWellPoint );
					}
				}
			}
			else
			{
				Point destination = activeTask.getStationPosition();

				if( destination.equals(curLocation) )
				{
					a = new DeliverWaterAction(activeTask);
				}
				else
				{
					a = new MoveTowardsAction( destination );
				}
			}
		}

		return a;
	}

    public Action senseAndAct(Cell[][] view, long timestep)
	{
		act = null;
		currentCellData = view;
		currentTimeStep = timestep;

		scanSurroundings();
		System.out.println( "Station Count: " + stationList.size() + " || Well Count : " + wellList.size() );

		for( Function f : routines )
		{
			act = f.call();
			if( act != null ) break;
		}

		return act;
    }
}
