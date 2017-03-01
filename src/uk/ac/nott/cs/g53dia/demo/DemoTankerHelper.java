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

	public static void debugHalt()
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

	public static int calculateDistance( Cell c1, Cell c2 )
	{
		int[] coord1 = new int[2];
		int[] coord2 = new int[2];

		getCoord( c1.getPoint().toString(), coord1 );
		getCoord( c2.getPoint().toString(), coord2 );

		return Math.abs( coord1[0] - coord2[0] ) + Math.abs( coord1[1] - coord2[1] );
	}

	public static Well getNearestWell( ArrayList<Well> wellList, Cell playerPos )
	{
		int minDistance = Integer.MAX_VALUE;
		Well nearestWell = null;

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
}
