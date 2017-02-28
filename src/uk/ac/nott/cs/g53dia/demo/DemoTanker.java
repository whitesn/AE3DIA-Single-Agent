package uk.ac.nott.cs.g53dia.demo;
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
public class DemoTanker extends Tanker {

	final int LOW_FUEL_THRESHOLD = MAX_FUEL / 2;
	Action act = null;
	Cell[][] currentCell;
	long currentTimeStep;

    public DemoTanker() { }

	private boolean isCurrentCellFuelStation()
	{
		return (getCurrentCell(currentCell) instanceof FuelPump);
	}

	private boolean isFuelLow()
	{
		return getFuelLevel() < LOW_FUEL_THRESHOLD;
	}

	private Action fuelRepleneshingAction()
	{
		if( isFuelLow() )
		{
			if( isCurrentCellFuelStation() )
			{
				System.out.println("Refueling Gas...");
				return new RefuelAction();
			}
			else
			{
				System.out.println("Going to fuel station...");
				return new MoveTowardsAction(FUEL_PUMP_LOCATION);
			}
		}

		return null;
	}

    public Action senseAndAct(Cell[][] view, long timestep) {

		act = null;
		currentCell = view;
		currentTimeStep = timestep;

		if( (act = fuelRepleneshingAction()) != null )
		{
			return act;
		}

        System.out.println("Moving randomly...");
        return new MoveAction((int)(Math.random()*8));
    }
}
