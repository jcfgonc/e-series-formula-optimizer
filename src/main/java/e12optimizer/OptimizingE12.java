package e12optimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class OptimizingE12 {

	private static final double[] SERIES_E12 = { 1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2 };
	@SuppressWarnings("unused")
	private static final double[] SERIES_E24 = { 1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0, 3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2,
			6.8, 7.5, 8.2, 9.1 };

	public static void main(String[] args) {

		double[] multipliers = { 1e2, 1e3, 1e4, 1e5 };

		DoubleArrayList rl_values = createValues(multipliers);
		DoubleArrayList rh_values = createValues(multipliers);

		// formula to find zero
		String expression = "1.28-(1.25*(1+rl/rh)+iq*rl*1)";

		HashSet<String> variables = detectVariables(expression);
		Expression e1 = new ExpressionBuilder(expression) //
				.variables(variables) //
				.build();

		e1.setVariable("iq", 50e-6);

		ArrayList<DoubleArrayList> results = evaluate(rl_values, rh_values, e1);
		sortResultsAscending(results);
		printResults(results);
	}

	private static void printResults(ArrayList<DoubleArrayList> results) {
		System.out.println("error\tr0\t...\trn");
		for (DoubleArrayList res : results) {
			System.out.print((res.getDouble(0)) + "\t");
			for (int i = 1; i < res.size(); i++) {
				System.out.print(((int) Math.round(res.getDouble(i))) + "\t");
			}
			System.out.println();
		}
	}

	private static void sortResultsAscending(ArrayList<DoubleArrayList> results) {
		results.sort(new Comparator<DoubleArrayList>() {
			@Override
			public int compare(DoubleArrayList o1, DoubleArrayList o2) {
				return o1.compareTo(o2);
			}
		});
	}

	private static ArrayList<DoubleArrayList> evaluate(DoubleArrayList rl_values, DoubleArrayList rh_values, Expression e1) {
		ArrayList<DoubleArrayList> results = new ArrayList<DoubleArrayList>();
		for (double rl : rl_values) {
			e1.setVariable("rl", rl);

			for (double rh : rh_values) {
				e1.setVariable("rh", rh);
				double error = Math.abs(e1.evaluate());
				DoubleArrayList curResult = new DoubleArrayList(4);
				curResult.add(error);
				curResult.add(rl);
				curResult.add(rh);
				results.add(curResult);
			}
		}
		return results;
	}

	private static HashSet<String> detectVariables(String expression) {
		boolean blownup = false;
		HashSet<String> variables = new HashSet<String>();
		ExpressionBuilder eb = new ExpressionBuilder(expression);
		do {
			try {
				eb.build(); // try to build it
				blownup = false;
			} catch (Exception e) {
				blownup = true;
				// Unknown function or variable 'x' at pos y in expression 'x'
				String message = e.getMessage();
				if (message.startsWith("Unknown function or variable")) {
					int i1 = message.indexOf("' at", 30);
					String var = message.substring(30, i1);
					variables.add(var);
					eb.variable(var);
				}
			}
		} while (blownup);
		return variables;
	}

	private static DoubleArrayList createValues(double[] multipliers) {
		DoubleArrayList values = new DoubleArrayList();
		for (double val : SERIES_E12) {
			for (double mul : multipliers) {
				double v = val * mul;
				values.add(v);
			}
		}
		return values;
	}

}
