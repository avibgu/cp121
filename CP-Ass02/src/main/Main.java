package main;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Random;
import java.util.Vector;

import problem.COPProblem;
import problem.MaxCSPProblem;
import problem.NQueensProblem;
import problem.Problem;
import problem.ProblemsSetStats;

import algorithm.Algorithm;
import algorithm.BnB;
import algorithm.BnBDAC;
import algorithm.BnBIC;
import algorithm.BnBICDAC;


public class Main {

	public static final long	RANDOM_SEED			= 1717;

	public static final int		NUM_OF_PROBLEMS		= 50;
	public static final int		NUM_OF_VARIABLES	= 12;
	public static final int		NUM_OF_VALUES		= 5;

	public static final double	P1_MIN				= 0.2;
	public static final double	P1_MAX				= 0.8;
	public static final double	P1_DELTA			= 0.3;

	public static final double	P2_MIN				= 0.1;
	public static final double	P2_MAX				= 0.8;
	public static final double	P2_DELTA			= 0.1;

	public static final double	ZERO_P2				= 0;

	public static final double	P2_2_MIN			= 0.90;
	public static final double	P2_2_MAX			= 0.97;
	public static final double	P2_2_DELTA			= 0.01;

	public static final int		MC_MIN				= 1;
	public static final int		MC_MAX				= 10000;
	public static final int		MC_DELTA			= 10;

	public static final int		ZERO_MC				= 0;

	public static void main(String[] args) throws Exception {

		nQueensTests();
		randomMaxCSPProblemsTests("MaxCSP_02.csv", P1_MIN);
		randomMaxCSPProblemsTests("MaxCSP_05.csv", P1_MIN + P1_DELTA);
		randomMaxCSPProblemsTests("MaxCSP_08.csv", P1_MAX);
		randomCOPProblemsTests("COP.csv");
	}

	private static void nQueensTests() throws Exception {

		Vector<Problem> problems = new Vector<Problem>(1);

		problems.add(new NQueensProblem(8));

		solveProblems(problems, true, true);

	}

	private static void randomMaxCSPProblemsTests(String fileName, double p1) throws Exception {

		Random random = new Random(RANDOM_SEED);

		PrintWriter out = new PrintWriter(fileName);

		out.append(	"\n" +
					"P1," +
					"P2," +
					"Average BnB Assignments," +
					"Average BnBIC Assignments," +
					"Average BnBDAC Assignments," +
					"Average BnBICDAC Assignments," +
					"Average BnB CCs," +
					"Average BnBIC CCs," +
					"Average BnBDAC CCs," +
					"Average BnBICDAC CCs" +
					"\n");

		for (double p2 = P2_MIN; p2 <= P2_MAX; p2 += P2_DELTA)
			out.append(p1 + "," + p2 + "," +
					solveProblems(createMaxCSPProblems(p1, p2, random),
							true, true) + "\n");

		for (double p2 = P2_2_MIN; p2 <= P2_2_MAX; p2 += P2_2_DELTA)
			out.append(p1 + "," + p2 + "," +
					solveProblems(createMaxCSPProblems(p1, p2, random),
							true, true) + "\n");

		out.close();
	}

	private static Vector<Problem> createMaxCSPProblems(double p1, double p2, Random random) {

		Vector<Problem> problems = new Vector<Problem>(NUM_OF_PROBLEMS);

		for (int i = 0; i < NUM_OF_PROBLEMS; i++)
			problems.add(new MaxCSPProblem(NUM_OF_VARIABLES, NUM_OF_VALUES, p1, p2, ZERO_MC, random));

		return problems;
	}

	private static void randomCOPProblemsTests(String fileName) throws Exception {

		Random random = new Random(RANDOM_SEED);

		PrintWriter out = new PrintWriter(fileName);

		for (double p1 = P1_MIN; p1 <= P1_MAX; p1 += P1_DELTA){

			out.append(	"\n" +
						"P1," +
						"MC," +
						"Average BnB Assignments," +
						"Average BnBIC Assignments," +
						"Average BnBDAC Assignments," +
						"Average BnBICDAC Assignments," +
						"Average BnB CCs," +
						"Average BnBIC CCs," +
						"Average BnBDAC CCs," +
						"Average BnBICDAC CCs" +
						"\n");

			for (int mc = MC_MIN; mc <= MC_MAX; mc *= MC_DELTA)
				out.append(p1 + "," + mc + "," +
						solveProblems(createCOPProblems(p1, ZERO_P2, mc, random),
							true, true) + "\n");
		}

		out.close();
	}

	private static Vector<Problem> createCOPProblems(double p1, double p2, int mc, Random random) {

		Vector<Problem> problems = new Vector<Problem>(NUM_OF_PROBLEMS);

		for (int i = 0; i < NUM_OF_PROBLEMS; i++)
			problems.add(new COPProblem(NUM_OF_VARIABLES, NUM_OF_VALUES, p1, p2, mc, random));

		return problems;
	}

	private static ProblemsSetStats solveProblems(Vector<Problem> problems,
			boolean debug, boolean bnb) throws Exception {

		Algorithm BnB = new BnB();
		Algorithm BnBIC = new BnBIC();
		Algorithm BnBDAC = new BnBDAC();
		Algorithm BnBICDAC = new BnBICDAC();

		StringBuffer debugSB = new StringBuffer();

		ProblemsSetStats stats = new ProblemsSetStats();

		for (Problem p: problems){

			debugSB.append("PROBLEM: " + p + "\n");

			if (bnb){

				BnB.solve(p);

				if (!p.isSolved()) debugSB.append("UNSOLVED ");

				stats.addBnBAssignments(new BigInteger(String.valueOf(p.getAssignments())));
				stats.addBnBCCs(new BigInteger(String.valueOf(p.getCCs())));

				debugSB.append("BnB:\n" + p.printSolution() + "\n");
			}

			BnBIC.solve(p);

			if (!p.isSolved()) debugSB.append("UNSOLVED ");

			stats.addBnBICAssignments(new BigInteger(String.valueOf(p.getAssignments())));
			stats.addBnBICCCs(new BigInteger(String.valueOf(p.getCCs())));

			debugSB.append("BnBIC:\n" + p.printSolution() + "\n");

			BnBDAC.solve(p);

			if (!p.isSolved()) debugSB.append("UNSOLVED ");

			stats.addBnBDACAssignments(new BigInteger(String.valueOf(p.getAssignments())));
			stats.addBnBDACCCs(new BigInteger(String.valueOf(p.getCCs())));

			debugSB.append("BnBDAC:\n" + p.printSolution() + "\n");

			BnBICDAC.solve(p);

			if (!p.isSolved()) debugSB.append("UNSOLVED ");

			stats.addBnBICDACAssignments(new BigInteger(String.valueOf(p.getAssignments())));
			stats.addBnBICDACCCs(new BigInteger(String.valueOf(p.getCCs())));

			debugSB.append("BnBICDAC:\n" + p.printSolution() + "\n");
		}

		if (debug) System.out.println(debugSB.toString());

		return stats;
	}
}
