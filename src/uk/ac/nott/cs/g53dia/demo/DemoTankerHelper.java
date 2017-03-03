package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;
import uk.ac.nott.cs.g53dia.demo.Position;

import java.util.HashMap;

public class DemoTankerHelper
{
    public enum CellType {
        EmptyCell,
        FuelPump,
        Well,
        Station,
        DefaultCell
    }

	public static boolean isFuelLow( int fuelLevel )
	{
		return fuelLevel < DemoTanker.LOW_FUEL_THRESHOLD;
	}

	public static CellType getCellType( Cell c )
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

    private static int calculateDistance( Position p1, Position p2 )
    {
        return Math.max( Math.abs(p1.x - p2.x), Math.abs(p1.y - p2.y) );
    }

	public static Well getNearestWell( Position playerPos, HashMap<Well,Position> wellList )
	{
		int minDistance = Integer.MAX_VALUE;
		Well nearestWell = null;

        for( Well w : wellList.keySet() )
        {
            Position pos = wellList.get( w );
            int distance = calculateDistance( playerPos, pos );
            if( distance < minDistance )
            {
                minDistance = distance;
                nearestWell = w;
            }
        }

		return nearestWell;
	}

    public static boolean isReachableWithinFuelThreshold( Position p1, Position p2 )
    {
        int distance = calculateDistance( p1, p2 );
        return Tanker.MAX_FUEL - distance > DemoTanker.LOW_FUEL_THRESHOLD;
    }

    private static int getMoveDirection( int moveHorizontal, int moveVertical )
    {
        int moveDirection = -1;

        if( moveVertical > 0 )
        {
            if( moveHorizontal > 0 )
            {
                moveDirection = MoveAction.NORTHEAST;
            }
            else if( moveHorizontal == 0 )
            {
                moveDirection = MoveAction.NORTH;
            }
            else if( moveHorizontal < 0 )
            {
                moveDirection = MoveAction.NORTHWEST;
            }
        }
        else if( moveVertical == 0 )
        {
            if( moveHorizontal > 0 )
            {
                moveDirection = MoveAction.EAST;
            }
            if( moveHorizontal == 0 )
            {
            	moveDirection = -1;
            }
            else if( moveHorizontal < 0 )
            {
                moveDirection = MoveAction.WEST;
            }
        }
        else if( moveVertical < 0 )
        {
            if( moveHorizontal > 0 )
            {
                moveDirection = MoveAction.SOUTHEAST;
            }
            else if( moveHorizontal == 0 )
            {
                moveDirection = MoveAction.SOUTH;
            }
            else if( moveHorizontal < 0 )
            {
                moveDirection = MoveAction.SOUTHWEST;
            }
        }

        return moveDirection;
    }

    public static int getDirectionToward( Position playerPos, Position destination )
    {
        int moveHorizontal = destination.x - playerPos.x;
        int moveVertical = destination.y - playerPos.y;
        return getMoveDirection( moveHorizontal, moveVertical );
    }

    public static void playerMoveUpdatePosition( Position playerPos, int direction )
    {
        switch( direction )
        {
            case MoveAction.NORTH:
            playerPos.y++;
            break;

            case MoveAction.SOUTH:
            playerPos.y--;
            break;

            case MoveAction.WEST:
            playerPos.x--;
            break;

            case MoveAction.EAST:
            playerPos.x++;
            break;

            case MoveAction.NORTHEAST:
            playerPos.y++;
            playerPos.x++;
            break;

            case MoveAction.NORTHWEST:
            playerPos.y++;
            playerPos.x--;
            break;

            case MoveAction.SOUTHEAST:
            playerPos.x++;
            playerPos.y--;
            break;

            case MoveAction.SOUTHWEST:
            playerPos.y--;
            playerPos.x--;
            break;
        }
    }

    public static Position getPositionFromView( int row, int col, Position playerPos )
    {
    	int x = col - Tanker.VIEW_RANGE;
    	int y = Tanker.VIEW_RANGE - row;
        return new Position( playerPos.x + x, playerPos.y + y );
    }

    public static boolean isStationStored( Position newStationPos, 
    		HashMap<Position, Station> stationList )
    {
    	for( Position pos : stationList.keySet() )
    	{
    		if( pos.isEqualCoord(newStationPos) )
    		{
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /*
     * Debugging Purposes
     */

    
    public static int getOppositeDirection( Position playerPos, Position destination )
    {
        int moveHorizontal = playerPos.x - destination.x;
        int moveVertical = playerPos.y - destination.y;
        return getMoveDirection( moveHorizontal, moveVertical );
    }

    public static boolean isStationWithinView( Station s, Cell[][] view )
    {
        for( Cell[] rows : view )
        {
            for( Cell col : rows )
            {
                if( col instanceof Station && s.equals(col) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean checkIfPointEqualPosition( Point p1, Position p2 )
    {
        String pointPos = p1.toString();
		String positionPos = p2.toString();
        return pointPos.equals( positionPos );
    }

	public static void halt()
	{
		while( true );
	}

	public static void printView( Cell[][] cells )
	{
		for( int row = 0; row < cells.length; row++ )
		{
			for( int col = 0; col < cells[row].length; col++ )
			{
				Cell c = cells[col][row];

				switch( getCellType(c) )
				{
				case EmptyCell:
				System.out.print("  |");
				break;

				case FuelPump:
				System.out.print("F |");
				break;

				case Well:
				System.out.print("W |");
				break;

				case Station:
				System.out.print("S |");
				break;

				default:
				break;
				}
			}
			System.out.println();
		}
	}
}
