package test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import uk.ac.nott.cs.g53dia.demo.DeliveryPlanner;
import uk.ac.nott.cs.g53dia.demo.DemoTanker;
import uk.ac.nott.cs.g53dia.demo.Plan;
import uk.ac.nott.cs.g53dia.demo.Plan.PlanType;
import uk.ac.nott.cs.g53dia.demo.PlanSequence;
import uk.ac.nott.cs.g53dia.demo.Position;

public class TestDeliveryPlanner {

	@Test
	public void testIsPlanExecutable()
	{
		DemoTanker dt = new DemoTanker();
		
		ArrayList<Plan> planSeq = new ArrayList<Plan>();
		Plan p1 = new Plan( PlanType.Recon, new Position(0,48) );
		planSeq.add( p1 );
		PlanSequence ps = new PlanSequence( planSeq, PlanType.Recon );
		assertTrue( DeliveryPlanner.isPlanExecutable(ps, dt) );
		
		planSeq = new ArrayList<Plan>();
		p1 = new Plan( PlanType.Recon, new Position(0,49) );
		planSeq.add( p1 );
		ps = new PlanSequence( planSeq, PlanType.Recon );
		assertTrue( DeliveryPlanner.isPlanExecutable(ps, dt) );
		
		planSeq = new ArrayList<Plan>();
		p1 = new Plan( PlanType.Recon, new Position(0,50) );
		planSeq.add( p1 );
		ps = new PlanSequence( planSeq, PlanType.Recon );
		assertFalse( DeliveryPlanner.isPlanExecutable(ps, dt) );
	}

}
