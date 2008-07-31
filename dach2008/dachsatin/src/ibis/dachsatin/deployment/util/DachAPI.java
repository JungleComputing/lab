package ibis.dachsatin.deployment.util;

import ibis.util.RunProcess;

import java.util.Arrays;
import java.util.LinkedList;

public class DachAPI {

	private String dachApi = "/home/dach911/dach_api/dach_api";

	private String [] getProblem = { dachApi, "--get_problem", "" };
	private String [] returnResult = { dachApi, "--check_ans", "", "" };
	
	public DachAPI() { 
		// empty 
	}
	
	public DachAPI(String get, String ret) { 
		getProblem[0] = get;
		returnResult[0] = ret;
	}
	
	public Problem getProblem(String name, String homeDir) throws Exception { 
		
		getProblem[2] = name;
		
		System.out.println("DACH_API: Retrieving problem using " + Arrays.toString(getProblem));
		
		RunProcess p = new RunProcess(getProblem);
		p.run();
		int result = p.getExitStatus();
		
		if (result != 0) { 
			throw new Exception("DACH_API: Failed to retrieve problem!");			
		}

		String output = new String(p.getStdout()).trim();
		
		System.out.println("DACH_API: Got problem: " +  output);
		
		int index = output.indexOf(' ');
		
		if (index <= 0) { 
			throw new Exception("DACH_API: Failed to understand output!");
		}

		String ID = output.substring(0, index).trim();
		String dir = output.substring(index).trim();
		
		return new Problem(ID, dir, homeDir);
	}
	
	public String provideResult(Problem problem) throws Exception { 
		
		returnResult[2] = problem.ID;
		returnResult[3] = problem.outputFile;
		
		System.out.println("DACH_API: Returning problem using " + Arrays.toString(returnResult));
		
		RunProcess p = new RunProcess(returnResult);
		p.run();
		int result = p.getExitStatus();
		
		if (result != 0) { 
			throw new Exception("DACH_API: Failed return result " + problem.ID + " " + problem.outputFile);			
		}

		String res = new String(p.getStdout()).trim();
		
		System.out.println("DACH_API: Reply was: " + res);
		
		return res;
		
	}
	
	public LinkedList<Problem> getProblems(LinkedList<String> names, String homeDir) throws Exception { 
		
		LinkedList<Problem> result = new LinkedList<Problem>();
		
		for (String s : names) { 
			result.add(getProblem(s, homeDir));
		}
		
		return result;
	}
	
	public LinkedList<String> returnResults(LinkedList<Problem> problems) throws Exception { 
		
		LinkedList<String> result = new LinkedList<String>();
		
		for (Problem p : problems) { 
			result.add(provideResult(p));
		}
		
		return result;
	}
}
