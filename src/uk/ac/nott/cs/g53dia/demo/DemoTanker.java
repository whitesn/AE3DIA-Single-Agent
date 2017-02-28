package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;

import java.util.ArrayList;

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
public class DemoTanker extends Tanker {

	final int LOW_FUEL_THRESHOLD = (MAX_FUEL / 2) + 10;
	Action act;
	Cell[][] currentCellData;
	long currentTimeStep;
	Task activeTask;

	ArrayList<Well> wellList;
	ArrayList<Station> stationList;
	ArrayList<Task> tasks;

	boolean gotFirstStation = false;

	public enum CellType {
		EmptyCell,
		FuelPump,
		Well,
		Station,
		DefaultCell
	}

    public DemoTanker()
	{
		wellList = new ArrayList<Well>();
		stationList = new ArrayList<Station>();
		tasks = new ArrayList<Task>();
		act = null;
		activeTask = null;
	}

	/*
	 * =========  This Section contain Helper Functions ============
	 */
	private boolean isCurrentCellFuelStation()
	{
		return (getCurrentCell(currentCellData) instanceof FuelPump);
	}

	private boolean isFuelLow()
	{
		return getFuelLevel() < LOW_FUEL_THRESHOLD;
	}

	private CellType getCellType( Cell c )
	{
		if( c instanceof Well )
		{
			return CellType.Well;
		}
		else if( c instanceof EmptyCell )
		{
			return CellType.EmptyCell;
		}
		else if( c instanceof FuelPump )
		{
			return CellType.FuelPump;
		}
		else if( c instanceof Station )
		{
			return CellType.Station;
		}

		return CellType.DefaultCell;
	}

	private void debugHalt()
	{
		while( true );
	}

	private void getCoord( String point, int coord[] )
	{
		int openBracketLoc = point.indexOf("(");
		int commaLoc = point.indexOf(",");
		int closingBracketLoc = point.indexOf(")");

		coord[0] = Integer.parseInt( point.substring(openBracketLoc + 1, commaLoc) );
		coord[1] = Integer.parseInt( point.substring(commaLoc + 2, closingBracketLoc) );
	}

	private int calculateDistance( Cell c1, Cell c2 )
	{
		int[] coord1 = new int[2];
		int[] coord2= new int[2];

		getCoord( c1.getPoint().toString(), coord1 );
		getCoord( c2.getPoint().toString(), coord2 );

		return Math.abs( coord1[0] - coord2[0] ) + Math.abs( coord1[1] - coord2[1] );
	}

	private Well getNearestWell()
	{
		int minDistance = Integer.MAX_VALUE;
		Well nearestWell = null;
		Cell playerPos = getCurrentCell(currentCellData);

		for( Well w : wellList )
		{
			int dis = calculateDistance( playerPos, w );
			if( minDistance > dis )
			{
				minDistance = dis;
				nearestWell = w;
			}
		}

		return nearestWell;
	}
	// ======== Helper Functions Ends Here =========================

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
				switch( getCellType(col) )
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
	 * Fuel Action v1.0
	 *
	 * If Fuel Level is reaching the bottom threshold, forget all tasks and go directly
	 * to Fuel Station to refuel.
	 */
	private Action fuelRepleneshingAction()
	{
		Action a = null;

		if( isFuelLow() )
		{
			if( isCurrentCellFuelStation() )
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
	 * Complete Task v1.0
	 *
	 * Find nearest well, refill water, send
	 */
	private Action completeTask()
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
				Well nearestWell = getNearestWell();

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

		if( (act = fuelRepleneshingAction()) != null )
		{
			return act;
		}

		if( (act = completeTask()) != null )
		{
			return act;
		}

		if( (act = recon()) != null )
		{
			return act;
		}

		return act;
    }
}
