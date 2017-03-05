package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;
import uk.ac.nott.cs.g53dia.demo.Position;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DemoTankerHelper
{
	// MAX_DISTANCE_TRAVELLED = MAX_FUEL - COST_ACTION_FUEL / 2 (Roundtrip Travel) - VIEW_RANGE
	public static int RECON_TRAVEL_BOUND = DemoTanker.MAP_TRAVELLABLE_BOUND - Tanker.VIEW_RANGE;
	
    public enum CellType {
        EmptyCell,
        FuelPump,
        Well,
        Station,
        DefaultCell
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

    public static int calculateDistance( Position p1, Position p2 )
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

    /*
     * Map the Cell[][] view from (col,row)(0,0) to actual 
     * Position (-12,12) (top left) relative to the playerPos
     */
    public static Position getPositionFromView( int row, int col, Position playerPos )
    {
    	int x = col - Tanker.VIEW_RANGE;
    	int y = Tanker.VIEW_RANGE - row;
        return new Position( playerPos.x + x, playerPos.y + y );
    }
    
    public static Position convertToUnsignedPos( Position p )
    {
    	int x = (DemoTanker.MAP_BOUND / 2) + p.x;
    	int y = (DemoTanker.MAP_BOUND / 2) - p.y;
    	
    	x = (x >= DemoTanker.MAP_BOUND) ? DemoTanker.MAP_BOUND - 1 : x;
    	y = (y >= DemoTanker.MAP_BOUND) ? DemoTanker.MAP_BOUND - 1 : y;
    	x = (x < 0) ? 0 : x;
    	y = (y < 0) ? 0 : y;
    	
    	return new Position(x,y);
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
    
    private static int calculateGainForArea( boolean[][] cellDiscovered, Position pos )
    {
    	int gain = 0;

		for( int row = pos.y-Tanker.VIEW_RANGE; row <= pos.y+Tanker.VIEW_RANGE; row++ )
		{
			for( int col = pos.x-Tanker.VIEW_RANGE; col <= pos.x+Tanker.VIEW_RANGE; col++ )
			{
				Position p = new Position(col,row);
				p = convertToUnsignedPos( p );
				gain += ( cellDiscovered[p.x][p.y] ) ? 0 : 1;
			}
		}
		
		return gain;
    }
    
    public static int getDirectionMostAreaDiscovered( boolean[][] cellDiscovered, Position playerPos )
    {
    	int maxGain = 0;
    	int maxGainDirection = -1;
    	
    	for( int direction = 0; direction < 8; direction++ )
    	{
    		Position newPos = playerPos.clone();
    		playerMoveUpdatePosition( newPos, direction );
    		int gain = calculateGainForArea( cellDiscovered, newPos );
			if( gain > maxGain )
			{
				maxGain = gain;
				maxGainDirection = direction;
			}
    	}
    	
    	if( maxGainDirection == -1 )
    	{
    		for( int row = -RECON_TRAVEL_BOUND; row <= RECON_TRAVEL_BOUND; row++ )
    		{
    			for( int col = -RECON_TRAVEL_BOUND; col <= RECON_TRAVEL_BOUND; col++ )
    			{
    				Position pos = new Position(col,row);
    	    		int gain = calculateGainForArea( cellDiscovered, pos );
    	    		if( gain > maxGain )
    	    		{
    	    			maxGain = gain;
    	    			maxGainDirection = getDirectionToward( playerPos, pos );
    	    		}
    			}
    		}
    	}
    	
    	return maxGainDirection;
    }

    public static boolean isWholeMapDiscovered( boolean[][] cellDiscovered )
    {
    	for( int i = 0; i < cellDiscovered.length; i++ )
    	{
    		for( int j = 0; j < cellDiscovered.length; j++ )
    		{
    			if( !cellDiscovered[i][j] )
    				return false;
    		}
    	}
    	
    	return true;
    }
    
	public static boolean noWaterDeliveryPlan( ArrayList<Plan> plans )
	{
		for( Plan p : plans )
		{
			if( p.getPlanType() == Plan.PlanType.DeliverWater )
			{
				return false;
			}
		}
		
		return true;
	}
	
	public static void insertDeliveryPlan( ArrayList<Plan> deliveryPlanSet, ArrayList<Plan> plans )
	{
		if( plans.isEmpty() )
		{
			plans.addAll( deliveryPlanSet );
		}
		else
		{
			for( int i = 0; i < plans.size(); i++ )
			{
				Plan plan = plans.get(i);
				if( !(plan.getPlanType() == Plan.PlanType.Refuel) )
				{
					plans.addAll(i, deliveryPlanSet);
					break;
				} 
				else if( i == plans.size() - 1 )
				{
					plans.addAll( deliveryPlanSet );
				}
			}
		}
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
		/*
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
	}
	
	
	private static Position getActualPositionFromRowCol( int row, int col )
	{
		int x = col - DemoTanker.MAP_BOUND / 2;
		int y = DemoTanker.MAP_BOUND / 2 - row;
		return new Position(x,y);
	}
	
	public static void printCellDiscovered( boolean[][] cellDiscovered, Position playerPos )
	{
		for( int row = 0; row < cellDiscovered.length; row++ )
		{
			for( int col = 0; col < cellDiscovered.length; col++ )
			{
				Position curCell = getActualPositionFromRowCol( row, col );
				if( curCell.isEqualCoord(playerPos) )
				{
					System.out.print("O ");
				}
				else
				{
					if( cellDiscovered[col][row] )
					{
						System.out.print("X ");
					}
					else
					{
						System.out.print(". ");
					}
				}
			}
			System.out.println();
		}
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
	
	public static void printPlans( ArrayList<Plan> plans )
	{
		System.out.print("Plans: ");
		for( Plan plan : plans )
		{
			System.out.print( plan.toString() + " || " );
		}
		System.out.println();
	}
}
