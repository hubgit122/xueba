package ustc.ssqstone.xueba;

class Point
{
	public double x, y;
	
	public Point(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Point substract(Point p)
	{
		return new Point(this.x - p.x, this.y - p.y);
	}
	
	public Point add(Point p)
	{
		return new Point(this.x + p.x, this.y + p.y);
	}
}

public class PointArray
{
	public Point points[];
	public int index;	
	public int depth;
	
	public Point result;
	
	PointArray(int depth)
	{
		if (depth%2==1)
		{
			depth+=1;
		}
		if (depth<=0)
		{
			depth = 8;
		}
		points = new Point[depth];
		index = 0;
		result =new Point(0, 0);
		this.depth = depth;
	}
	
	Point push(Point p)
	{
		result = result.add(points[index].substract(points[(index+depth/2)%depth]));
		points[index] = p;
		result = result.substract(points[index].substract(points[(index+depth/2)%depth]));
		
		index = ++index%depth;
		
		return result;
	}
}
