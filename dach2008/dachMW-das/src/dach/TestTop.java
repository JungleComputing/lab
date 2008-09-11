package dach;

public class TestTop {

	public static void main(String [] args) { 
		
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		
		if (MiscUtils.getMachineLoad(out, err) == 0) { 
			System.out.println(out);
		} else { 
			System.out.println(out);
		}
	}
	
}
