package e12optimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class OptimizingE12 {
	private static enum E_SERIES_TYPE {
		E12, E24
	};

	private static final double[] SERIES_E12 = { 1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2 };
	private static final double[] SERIES_E24 = { 1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0, 3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5,
			8.2, 9.1 };

	public static double root(double num, double root) {
		return Math.pow(Math.E, Math.log(num) / root);
	}

	public static void main(String[] args) {
		double[] multipliers = { 1e2, 1e3, 1e4, 1e5, 1e6 };

		E_SERIES_TYPE e_series_type = E_SERIES_TYPE.E12;

		// formula to find zero
		// String leftHand = "1.25*(1+rl/rh)+iq*rl-27.8"; // gg's vref=1.28V using a lm317
		// String leftHand = "25.4*rh/(rl+rh)-4.096"; // voltage divider formula
		String leftHand = "20*log10(1+r1/r2)";

		String rightHand = "24";
		String minimisation = "abs(" + leftHand + "-(" + rightHand + "))";
		HashSet<String> variables = detectVariables(minimisation);
		Expression minim_expression = new ExpressionBuilder(minimisation) //
				.variables(variables) //
				.build();

//		String difference = "(" + leftHand + ")-(" + rightHand + ")";
		Expression result_expression = new ExpressionBuilder(leftHand) //
				.variables(variables) //
				.build();

		minim_expression.setVariable("iq", 50e-6);

		HashMap<String, DoubleArrayList> varValues = new HashMap<>();
		for (String variable : variables) {
			varValues.put(variable, createValues(multipliers, e_series_type));
		}

		ArrayList<DoubleArrayList> results = calculateExpressionError(varValues, minim_expression, result_expression);
		sortResultsAscending(results);
		printResults(results);
	}

	private static void printResults(ArrayList<DoubleArrayList> results) {
		System.out.println("results shown by descending error");
		System.out.println("error\tfunction\tvar_0\t...\tvar_n");
		for (DoubleArrayList res : results) {
			System.out.print((res.getDouble(0)) + "\t");
			System.out.print((res.getDouble(1)) + "\t");
			for (int i = 2; i < res.size(); i++) {
				// print as a whole number (standard e-series value)
				int v = (int) Math.round(res.getDouble(i));
				System.out.print(v + "\t");
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

	private static ArrayList<DoubleArrayList> calculateExpressionError(HashMap<String, DoubleArrayList> varValues, Expression minim_expression,
			Expression result_expression) {
		ArrayList<DoubleArrayList> results = new ArrayList<DoubleArrayList>();

		int numVars = varValues.size();
		assert numVars == 2;

		ArrayList<String> varNames = new ArrayList<String>(varValues.keySet());

		String var0_name = varNames.get(0);
		DoubleArrayList var0_values = varValues.get(var0_name);

		String var1_name = varNames.get(1);
		DoubleArrayList var1_values = varValues.get(var1_name);

		for (double d0 : var0_values) {
			minim_expression.setVariable(var0_name, d0);
			result_expression.setVariable(var0_name, d0);

			for (double d1 : var1_values) {
				minim_expression.setVariable(var1_name, d1);
				result_expression.setVariable(var1_name, d1);

				double error = minim_expression.evaluate();
				double result = result_expression.evaluate();
				// each result is of the form error result var0 ... varN
				DoubleArrayList curResult = new DoubleArrayList(4);
				curResult.add(error);
				curResult.add(result);
				curResult.add(d0);
				curResult.add(d1);
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

	private static DoubleArrayList createValues(double[] multipliers, E_SERIES_TYPE e_series_type) {
		DoubleArrayList values = new DoubleArrayList();
		double[] valseries = null;
		if (e_series_type == E_SERIES_TYPE.E12) {
			valseries = SERIES_E12;
		} else if (e_series_type == E_SERIES_TYPE.E24) {
			valseries = SERIES_E24;
		} else {
			throw new RuntimeException("unrecgonized E_SERIES_TYPE " + e_series_type);
		}
		for (double val : valseries) {
			for (double mul : multipliers) {
				double v = val * mul;
				values.add(v);
			}
		}
		return values;
	}

}
