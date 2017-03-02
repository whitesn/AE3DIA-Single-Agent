package uk.ac.nott.cs.g53dia.demo;

public class Position
{
    public int x;
    public int y;

    public Position( int x, int y )
    {
        this.x = x;
        this.y = y;
    }

    public boolean isEqualCoord( Position p )
    {
        return this.x == p.x && this.y == p.y;
    }
    
    public String toString()
    {
    	return "(" + this.x + ", " + this.y + ")";
    }
}
