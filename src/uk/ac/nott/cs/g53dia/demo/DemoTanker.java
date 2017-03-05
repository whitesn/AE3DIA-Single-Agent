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
	public final static int MAP_TRAVELLABLE_BOUND = ((Tanker.MAX_FUEL - 1) / 2);
	public final static int MAP_BOUND = MAP_TRAVELLABLE_BOUND * 2 + 1;
	
	public boolean isWholeMapDiscovered = false;
	
	Cell[][] currentCellData;
	boolean[][] cellDiscovered = new boolean[MAP_BOUND][MAP_BOUND];
	long currentTimeStep;
	Task activeTask;
	Position activeTaskPosition;
	HashMap<Well, Position> wellList;
	HashMap<Position, Station> stationList;
	HashMap<Task, Position> tasks;
	ArrayList<Action> actionQueue;
	Position playerPos;

    public DemoTanker()
	{
		wellList = new HashMap<Well,Position>();
		stationList = new HashMap<Position,Station>();
		tasks = new HashMap<Task,Position>();
		actionQueue = new ArrayList<Action>();
		activeTask = null;
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
				return getMoveActionToPos( new Position(0,0) );
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
		Action a = null;
		int direction;
		
		if( !isWholeMapDiscovered )
		{
			direction = DemoTankerHelper.getDirectionMostAreaDiscovered(cellDiscovered, playerPos);
			if( direction == -1 )
			{
				System.out.println("Failed to find undiscovered part of map direction");
				DemoTankerHelper.printCellDiscovered(cellDiscovered, playerPos);
				DemoTankerHelper.halt();
			}
			a = new MoveAction( direction );
		}
		else
		{
			direction = (int) (Math.random() * 8);
			a = new MoveAction( direction );
		}
		
		DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
		
		return a;
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
						a = getMoveActionToPos( wellPoint );
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
					a = getMoveActionToPos( activeTaskPosition );
				}
			}
		}

		return a;
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
    
    private Action getMoveActionToPos( Position target )
    {
		int direction = DemoTankerHelper.getDirectionToward( playerPos, target );

		if( direction >= 0 )
		{
			DemoTankerHelper.playerMoveUpdatePosition( playerPos, direction );
			return new MoveAction( direction );
		}
		else
		{
			return doNothing();
		}
    }

	private Action doNothing()
	{
		return new MoveTowardsAction( getCurrentCell(currentCellData).getPoint() );
	}
}
