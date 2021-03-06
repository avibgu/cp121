package algorithm;

import java.util.Vector;

import problem.Problem;

public class BnBDAC extends BnBICDAC{

	public BnBDAC() {
		super();
	}

	public BnBDAC(Problem problem){
		super(problem);
	}

	@Override
	protected void init(Problem problem){
		super.init(problem);
	}

	@Override
	protected void calcSumMinIC(int i) {

		_sum_min_ic = 0;

		for (int j = i + 1; j < _problem.getN(); j++){

			int min =  + _dac.get(j).get(0);

			for (int k = 1; k < _problem.getD(); k++){

				int x = _dac.get(j).get(k);

				if (x < min)
					min = x;
			}

			_sum_min_ic += min;
		}
	}

	@Override
	protected void updateIC(Vector<Vector<Integer>> remainingDom, int i, Integer v, int x) {
		calcSumMinIC(i);
	}

	@Override
	protected void restoreIC(Vector<Vector<Integer>> remainingDom, int i, Integer v) {
		return;
	}

	@Override
	public String toString(){
		return "BnBDAC";
	}
}
