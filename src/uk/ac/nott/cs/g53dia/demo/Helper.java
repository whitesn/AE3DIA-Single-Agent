package uk.ac.nott.cs.g53dia.demo;

import uk.ac.nott.cs.g53dia.library.*;
import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.demo.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Helper
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

	public static Well getNearestWell( HashMap<Well,Position> wellList,
			Position playerPos )
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

    /*
     * Map the Position from (0,0) to (49,49)
     */
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

    public static Position getReconLocation( boolean[][] cellDiscovered, Position playerPos )
    {
    	int maxGain = 0;
    	Position maxGainPosition = null;

    	/*
    	 * Get Best Gain from Single Step
    	 */
    	for( int direction = 0; direction < 8; direction++ )
    	{
    		Position newPos = playerPos.clone();
    		playerMoveUpdatePosition( newPos, direction );
    		int gain = calculateGainForArea( cellDiscovered, newPos );
			if( gain > maxGain )
			{
				maxGain = gain;
				maxGainPosition = newPos;
			}
    	}

    	/*
    	 * If no gain, then find the best area to be travelled to on the map
    	 */
    	if( maxGainPosition == null )
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
    	    			maxGainPosition = pos.clone();
    	    		}
    			}
    		}
    	}

    	return maxGainPosition;
    }

	public static ArrayList<Position> getMostStationRecon( HashMap<Position, Station> stationList )
    {
    	boolean[][] nonStationCells = new boolean[DemoTanker.MAP_BOUND][DemoTanker.MAP_BOUND];

    	for( int i = 0; i < DemoTanker.MAP_BOUND; i++ )
    	{
    		for( int j = 0; j < DemoTanker.MAP_BOUND; j++ )
    		{
    			nonStationCells[i][j] = true;
    		}
    	}

    	for( Position pos : stationList.keySet() )
    	{
    		Position stationCell = convertToUnsignedPos( pos );
    		nonStationCells[stationCell.x][stationCell.y] = false;
    	}

    	Map<Position,Integer> map = new HashMap<Position, Integer>();

    	for( int row = -RECON_TRAVEL_BOUND; row <= RECON_TRAVEL_BOUND; row++ )
		{
			for( int col = -RECON_TRAVEL_BOUND; col <= RECON_TRAVEL_BOUND; col++ )
			{
				Position pos = new Position(col,row);
	    		int gain = calculateGainForArea( nonStationCells, pos );
	    		map.put(pos, gain);
			}
		}

    	Set<Entry<Position, Integer>> set = map.entrySet();
    	List<Entry<Position,Integer>> list = new ArrayList<Entry<Position, Integer>>(set);
    	Collections.sort( list, new Comparator<Map.Entry<Position, Integer>>()
        {
            public int compare( Map.Entry<Position, Integer> o1, Map.Entry<Position, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        });


    	ArrayList<Position> bestReconPos = new ArrayList<Position>();

    	Position firstPos = list.get(0).getKey();
    	bestReconPos.add( firstPos );

    	for( int i = 0; i < DemoTanker.STATION_RECON_TOP_DATA_TAKE; i++ )
    	{
    		bestReconPos.add( list.get(i).getKey() );
    	}

	    return bestReconPos;
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

	public static boolean noWaterDeliveryPlan( ArrayList<PlanSequence> plansQueue )
	{
		for( PlanSequence p : plansQueue )
		{
			if( p.getPlanType() == PlanType.DeliverWater )
			{
				return false;
			}
		}

		return true;
	}

	private static void clearReconPlan( ArrayList<PlanSequence> plansQueue )
	{
		Iterator<PlanSequence> iter = plansQueue.iterator();

		while( iter.hasNext() )
		{
			PlanSequence p = iter.next();

			if( p.getPlanType() == PlanType.Recon )
			{
				iter.remove();
			}
		}
	}

	public static void insertDeliveryPlan( PlanSequence newDeliveryPlan, ArrayList<PlanSequence> plansQueue )
	{
		if( plansQueue.isEmpty() )
		{
			plansQueue.add( newDeliveryPlan );
		}
		else
		{
			for( int i = 0; i < plansQueue.size(); i++ )
			{
				PlanSequence planSeq = plansQueue.get(i);
				if( !(planSeq.getPlanType() == PlanType.Refuel) )
				{
					plansQueue.add(i, newDeliveryPlan);
					break;
				}
				else if( i == plansQueue.size() - 1 )
				{
					plansQueue.add( newDeliveryPlan );
					break;
				}
			}

			clearReconPlan( plansQueue );
		}
	}

	public static int calculatePlanValue( Position playerPos, Plan plan, DemoTanker dt )
	{
    	int value = 0;
    	int timestepUsed = calculateDistance( plan.getPlanPosition(), playerPos );
    	value -= timestepUsed;
    	value -= (timestepUsed * DemoTanker.VAL_FUEL_BURNED);

    	switch( plan.getPlanType() )
    	{
    	case DeliverWater:
    		Task deliveryTask = ((DeliveryPlan) plan).deliveryTask;
    		int deliveryPoint = deliveryTask.getRequired() * DemoTanker.VAL_WATER_DELIVERED;
    		value += deliveryPoint;
    		break;

    	case LoadWater:
    		int waterPoint = (Tanker.MAX_WATER - dt.getWaterLevel()) * DemoTanker.VAL_WATER;
    		value += waterPoint;
    		break;

    	case Refuel:
    		int fuelPoint = (Tanker.MAX_FUEL - dt.getFuelLevel()) * DemoTanker.VAL_FUEL;
    		value += fuelPoint;
    		break;

    	case Recon:
    		int areaGain = calculateGainForArea( dt.cellDiscovered, plan.getPlanPosition() );
    		value += areaGain * DemoTanker.VAL_AREA_DISCOVERED;
    		break;

		default:
			break;
    	}

    	return value;
	}

	/*
	 * Calculate the Value of a Sequence of a Plan, based on the value defined in DemoTanker.
	 */
    public static int calculatePlanSequenceValue( PlanSequence plans, DemoTanker dt )
    {
    	int value = 0;
    	Position curPos = dt.playerPos;

    	for( Plan plan : plans.planSeq )
    	{
    		value += calculatePlanValue( curPos, plan, dt );
    		curPos = plan.getPlanPosition().clone();
    	}

    	return value;
    }

    public static int calculatePlanSequenceFuelCost( Position playerPos, PlanSequence plans )
    {
    	int fuelCost = 0;
    	Position curPos = playerPos.clone();

    	for( Plan plan : plans.planSeq )
    	{
    		fuelCost += calculateDistance( curPos, plan.getPlanPosition() );
    		curPos = plan.getPlanPosition().clone();
    	}

    	return fuelCost;
    }

    public static boolean isPlanDoable( PlanSequence newPlans, DemoTanker dt )
    {
    	int fuel = dt.getFuelLevel();
    	int planCost = calculatePlanSequenceFuelCost( dt.playerPos, newPlans );

    	if( !(newPlans.pt != PlanType.Refuel) )
    	{
    		planCost += calculateDistance( newPlans.getLastPlanPosition(),
    				DemoTanker.FUEL_STATION_POS );
    	}

    	return fuel > planCost;
    }

    /*
     * Debugging Purposes
     * All Functions below are only for debugging purposes
     */

	public static Action doNothing( Point playerPoint )
	{
		return new MoveTowardsAction( playerPoint );
	}

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

	public static void printPlans( ArrayList<PlanSequence> plansQueue )
	{
		System.out.print("Plans: ");
		for( PlanSequence planSeq : plansQueue )
		{
			planSeq.printPlans();
			System.out.println();
		}
		System.out.println();
	}
}
