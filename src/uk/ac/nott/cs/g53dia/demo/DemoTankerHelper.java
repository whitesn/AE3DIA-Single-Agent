package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;
import java.util.ArrayList;

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

	public static void halt()
	{
		while( true );
	}

	public static void getCoord( String point, int coord[] )
	{
		int openBracketLoc = point.indexOf("(");
		int commaLoc = point.indexOf(",");
		int closingBracketLoc = point.indexOf(")");

		coord[0] = Integer.parseInt( point.substring(openBracketLoc + 1, commaLoc) );
		coord[1] = Integer.parseInt( point.substring(commaLoc + 2, closingBracketLoc) );
	}

	public static Well getNearestWell( Cell[][] view )
	{
		int minDistance = Integer.MAX_VALUE;
		Well nearestWell = null;

        int playerPos = view.length / 2;

        for( byte row = 0; row < view.length; row++ )
        {
            for( byte col = 0; col < view.length; col++ )
            {
                Cell c = view[row][col];
                if( getCellType(c) == CellType.Well )
                {
                    int distance = Math.max( Math.abs(row - playerPos), Math.abs(col - playerPos) );
                    if( minDistance > distance )
                    {
                        minDistance = distance;
                        nearestWell = (Well) c;
                    }
                }
            }
        }

		return nearestWell;
	}

    public static Task getFirstActiveTask( ArrayList<Task> tasks )
    {
        for( Task t : tasks )
        {
            if( !t.isComplete() )
            {
                return t;
            }
        }

        return null;
    }

    // Debugging Purposes
    public static int getOppositeDirection( Cell playerPos, Cell direction )
    {
        boolean moveRight;
        boolean moveUp;
		int[] playerCoord = new int[2];
        int[] coord = new int[2];

        getCoord( playerPos.getPoint().toString(), playerCoord );
        getCoord( direction.getPoint().toString(), coord );

        moveRight = (playerCoord[0] < coord[0]) ? false : true;
        moveUp = (playerCoord[1] < coord[1]) ? false : true;

        if( !moveRight && !moveUp )
        {
            return MoveAction.SOUTHWEST;
        } else if( moveRight && !moveUp )
        {
            return MoveAction.SOUTHEAST;
        } else if( !moveRight && moveUp )
        {
            return MoveAction.NORTHWEST;
        }

        return MoveAction.NORTHEAST;
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
}
