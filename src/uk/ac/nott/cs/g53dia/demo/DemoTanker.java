package uk.ac.nott.cs.g53dia.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.nott.cs.g53dia.demo.DemoTankerHelper;
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
	public final static int LOW_FUEL_THRESHOLD = (MAX_FUEL / 2) + 10;

	Cell[][] currentCellData;
	long currentTimeStep;
	Task activeTask;
	Position activeTaskPosition;
	HashMap<Well, Position> wellList;
	HashMap<Position, Station> stationList;
	HashMap<Task, Position> tasks;
	Position playerPos;

    public DemoTanker()
	{
		wellList = new HashMap<Well,Position>();
		stationList = new HashMap<Position,Station>();
		tasks = new HashMap<Task,Position>();
		activeTask = null;
		playerPos = new Position( 0, 0 );
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
		Cell[][] rows = currentCellData;

		for( int row = 0; row < rows.length; row++ )
		{
			for( int col = 0; col < rows[row].length; col++ )
			{
				Cell cell = rows[col][row];

				Position pos = DemoTankerHelper.getPositionFromView( row, col, playerPos );
				if( !DemoTankerHelper.checkIfPointEqualPosition(cell.getPoint(), pos) )
				{
					System.err.println("ERR: Position Verification mismatch!");
					System.err.println("CELL: " + cell.getPoint().toString() );
					System.err.println("DETECTED: " + pos.toString() );
					DemoTankerHelper.halt();
				}

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

		if( DemoTankerHelper.isFuelLow( getFuelLevel()) )
		{
			if( DemoTankerHelper.getCellType(getCurrentCell(currentCellData)) == DemoTankerHelper.CellType.FuelPump )
			{
				a = new RefuelAction();
			}
			else
			{
				int direction = DemoTankerHelper.getDirectionToward( playerPos, new Position(0,0) );

				if( direction >= 0 )
				{
					DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
					a = new MoveAction( direction );
				}
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
		int direction = (int)(Math.random()*8);
		DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
		return new MoveAction( direction );
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
			if( getWaterLevel() != MAX_WATER )
			{
				Well nearestWell = DemoTankerHelper.getNearestWell( playerPos, wellList );

				if( nearestWell != null )
				{
					Position wellPoint = wellList.get( nearestWell );

					if( wellPoint.isEqualCoord(playerPos) )
					{
						a = new LoadWaterAction();
					}
					else
					{
						int direction = DemoTankerHelper.getDirectionToward( playerPos, wellPoint );
						DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
						a = new MoveAction( direction );
					}
				}
			}
			else
			{
				if( activeTaskPosition.isEqualCoord(playerPos) )
				{
					a = new DeliverWaterAction( activeTask );
					tasks.remove( activeTask );
				}
				else
				{

					int direction = DemoTankerHelper.getDirectionToward(playerPos, activeTaskPosition);
					DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
					a = new MoveAction( direction );
				}
			}
		}

		return a;
	}

	/*
	 * Debugging purposes, do nothing
	 */
	private Action doNothing()
	{
		return new MoveTowardsAction( getCurrentCell(currentCellData).getPoint() );
	}

	private void verifyPlayerPos()
	{
		Point storedPos = getCurrentCell(currentCellData).getPoint();

		if( !DemoTankerHelper.checkIfPointEqualPosition(storedPos, playerPos) )
		{
			System.err.println("ERR: Player Position is not the same as the actual point!");
			System.err.println("STORED: " + storedPos.toString() );
			System.err.println("ACTUAL: " + playerPos.toString() );
			DemoTankerHelper.halt();
		}
	}

	public void updateActiveTask()
	{
		if( activeTask == null || activeTask.isComplete() )
		{
			Iterator<Entry<Task, Position>> iter = tasks.entrySet().iterator();

			while( iter.hasNext() )
			{
				Map.Entry<Task,Position> entry = iter.next();
				Task task = entry.getKey();

	    		if( !task.isComplete() && DemoTankerHelper.isReachableWithinFuelThreshold( new Position(0,0), tasks.get(task)) )
	    		{
	    			activeTask = task;
	    			activeTaskPosition = tasks.get(task);
	    			return;
	    		}
	    		else
	    		{
	    			iter.remove();
	    		}
			}

			activeTask = null;
		}
	}

    public Action senseAndAct(Cell[][] view, long timestep)
	{
		Action act = null;
		currentCellData = view;
		currentTimeStep = timestep;

		verifyPlayerPos();
		scanSurroundings();
		updateActiveTask();

		act = ( act == null ) ? fuel() : act;
		act = ( act == null ) ? task() : act;
		act = ( act == null ) ? recon() : act;

		return act;
    }
}
