package test;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.ac.nott.cs.g53dia.demo.DemoTanker;
import uk.ac.nott.cs.g53dia.demo.DemoTankerHelper;
import uk.ac.nott.cs.g53dia.demo.*;
import uk.ac.nott.cs.g53dia.library.*;

public class TestDemoTankerHelper {
	
	@Test
	public void testIsFuelLow() 
	{
		int fuel = DemoTanker.LOW_FUEL_THRESHOLD - 1;
		assertTrue( DemoTankerHelper.isFuelLow(fuel) );
		
		int sufficientFuel = DemoTanker.LOW_FUEL_THRESHOLD + 1;
		assertFalse( DemoTankerHelper.isFuelLow(sufficientFuel) );
	}

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
}
