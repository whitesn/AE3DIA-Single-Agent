package test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import uk.ac.nott.cs.g53dia.demo.Plan;
import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.demo.PlanSequence;
import uk.ac.nott.cs.g53dia.demo.Position;

public class TestPlanSequence {
	
	@Test
	public void testGetLastPlanPosition()
	{
		Position pos1 = new Position( 10, 20 );
		Position pos2 = new Position( 50, 40 );
		
		Plan p1 = new Plan( PlanType.DeliverWater, pos1 );
		Plan p2 = new Plan( PlanType.Refuel, pos2 );
		
		ArrayList<Plan> planSeq = new ArrayList<Plan>();
		planSeq.add( p1 );
		planSeq.add( p2 );
		
		PlanSequence plans = new PlanSequence( planSeq, PlanType.DeliverWater );
		
		assertEquals( pos2, plans.getLastPlanPosition() );
	}

}
