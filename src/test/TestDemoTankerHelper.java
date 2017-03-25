package test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import uk.ac.nott.cs.g53dia.demo.*;
import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.library.*;

public class TestDemoTankerHelper {
	
	@Test
	public void testGetCellType()
	{
		Well w = new Well( null );
		
		assertTrue( DemoTankerHelper.getCellType(w) ==
				DemoTankerHelper.CellType.Well );
	}
	
	@Test
	public void testPlayerMoveUpdatePosition()
	{
		Position pos = new Position( 0, 0 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.NORTH );
		assertTrue( pos.x == 0 && pos.y == 1 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.SOUTH );
		assertTrue( pos.x == 0 && pos.y == 0 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.EAST );
		assertTrue( pos.x == 1 && pos.y == 0 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.WEST );
		assertTrue( pos.x == 0 && pos.y == 0 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.NORTHWEST );
		assertTrue( pos.x == -1 && pos.y == 1 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.NORTHEAST );
		assertTrue( pos.x == 0 && pos.y == 2 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.SOUTHEAST );
		assertTrue( pos.x == 1 && pos.y == 1 );
		
		DemoTankerHelper.playerMoveUpdatePosition( pos, MoveAction.SOUTHWEST );
		assertTrue( pos.x == 0 && pos.y == 0 );
	}
	
	@Test
	public void testGetDirectionToward()
	{
		for( int i = 0; i < 8; i++ )
		{
			Position pos = new Position( 0, 0 );
			Position movingPos = new Position( 0, 0 );
			DemoTankerHelper.playerMoveUpdatePosition( movingPos, i );
			assertEquals( i, DemoTankerHelper.getDirectionToward(
					pos, movingPos) );
		}
	}
	
	@Test
	public void testGetPositionFromView()
	{
        Environment env = new Environment((Tanker.MAX_FUEL/2)-5);
        Tanker t = new DemoTanker();
        
        Position playerPos = new Position(0,0);
        Action a = new MoveAction( MoveAction.NORTH );
        
        for( int i = 0; i < 20; i++ )
        {
        	try {
				a.execute(env, t);
				DemoTankerHelper.playerMoveUpdatePosition( playerPos, MoveAction.NORTH );
			} catch (ActionFailedException e) {
				fail();
			}
        }

		Position actualPos;
		Cell[][] view = env.getView(t.getPosition(), Tanker.VIEW_RANGE);
		
		actualPos = DemoTankerHelper.getPositionFromView(0, 0, playerPos);
		assertTrue( DemoTankerHelper.checkIfPointEqualPosition(view[0][0].getPoint(), actualPos) );
		
		actualPos = DemoTankerHelper.getPositionFromView(24, 24, playerPos);
		assertTrue( DemoTankerHelper.checkIfPointEqualPosition(view[24][24].getPoint(), actualPos) );
		
		actualPos = DemoTankerHelper.getPositionFromView(1, 1, playerPos);
		assertFalse( DemoTankerHelper.checkIfPointEqualPosition(view[0][0].getPoint(), actualPos) );
	}
	
	@Test
	public void testConvertToUnsignedPos()
	{
		Position p = new Position(0,0);
		Position actual = new Position(49,49);
		assertTrue( actual.isEqualCoord(DemoTankerHelper.convertToUnsignedPos(p)) );
		
		p.x -= 10;
		actual.x -= 10;
		assertTrue( actual.isEqualCoord(DemoTankerHelper.convertToUnsignedPos(p)) );
	}
	
	@Test
	public void testGetDirectionMostAreaDiscovered()
	{
		boolean[][] cellDiscovered = new boolean[DemoTanker.MAP_BOUND][DemoTanker.MAP_BOUND];
		
		for( int i = 0; i < DemoTanker.MAP_BOUND; i++ )
		{
			for( int j = 0; j < DemoTanker.MAP_BOUND; j++ )
			{
				cellDiscovered[i][j] = false;
			}
		}
		
		Position playerPos = new Position(0,0);
		Position pos = DemoTankerHelper.convertToUnsignedPos( playerPos );
		
		for( int col = 0; col < pos.x; col++ )
		{
			for( int row = 0; row < pos.y; row++ )
			{
				cellDiscovered[col][row] = true;
			}
		}
	
		assertEquals( MoveAction.SOUTHEAST, DemoTankerHelper.getDirectionMostAreaDiscovered(cellDiscovered, playerPos) );
	}
	
	@Test
	public void testIsWholeMapDiscovered()
	{
		boolean[][] cellDiscovered = new boolean[5][5];
		for( int i = 0; i < 5; i++ )
		{
			for( int j = 0; j < 5; j++ )
			{
				cellDiscovered[i][j] = true;
			}
		}
		
		assertTrue( DemoTankerHelper.isWholeMapDiscovered(cellDiscovered) );
		
		cellDiscovered[0][0] = false;
		assertFalse( DemoTankerHelper.isWholeMapDiscovered(cellDiscovered) );

		cellDiscovered[0][0] = true;
		cellDiscovered[4][4] = false;
		assertFalse( DemoTankerHelper.isWholeMapDiscovered(cellDiscovered) );
	}
	
	@Test
	public void testNoWaterDeliveryPlan()
	{
		 ArrayList<PlanSequence> plansQueue = new ArrayList<PlanSequence>();
		 
		 ArrayList<Plan> planSeq = new ArrayList<Plan>();
		 planSeq.add( new Plan(PlanType.Recon, new Position(0,0)) );
		 planSeq.add( new Plan(PlanType.LoadWater, new Position(0,0)) );
		 planSeq.add( new Plan(PlanType.Refuel, new Position(0,0)) );
		 PlanSequence newPlans = new PlanSequence(planSeq, PlanType.Refuel);
		 plansQueue.add( newPlans );
		 
		 assertTrue( DemoTankerHelper.noWaterDeliveryPlan(plansQueue) );

		 planSeq = new ArrayList<Plan>();
		 planSeq.add( new Plan(PlanType.DeliverWater, new Position(0,0)) );
		 newPlans = new PlanSequence(planSeq, PlanType.DeliverWater);
		 plansQueue.add( newPlans );
		 
		 assertFalse( DemoTankerHelper.noWaterDeliveryPlan(plansQueue) );
	}
	
	@Test
	public void testInsertDeliveryPlan()
	{
		ArrayList<PlanSequence> plansQueue = new ArrayList<PlanSequence>();
	
		ArrayList<Plan> deliveryPlanSet = new ArrayList<Plan>();
		deliveryPlanSet.add( new Plan(PlanType.Refuel, DemoTanker.FUEL_STATION_POS) );
		deliveryPlanSet.add( new Plan(PlanType.LoadWater, new Position(0,0)) );
		deliveryPlanSet.add( new Plan(PlanType.DeliverWater, new Position(0,0)) );
		PlanSequence newDeliveryPlan = new PlanSequence(deliveryPlanSet, PlanType.DeliverWater);
		DemoTankerHelper.insertDeliveryPlan(newDeliveryPlan, plansQueue);
		assertEquals( 1, plansQueue.size() );
	}
	
	@Test
	public void testCalculatePlanValue()
	{
		
	}
}
