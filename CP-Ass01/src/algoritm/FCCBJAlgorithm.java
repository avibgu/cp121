package algoritm;

import java.util.Stack;
import java.util.Vector;

import problem.Problem;

public class FCCBJAlgorithm extends CBJAlgorithm {

	protected	Vector<Stack<Vector<Integer>>>	_reductions;
	protected	Vector<Stack<Integer>>			_pastFc;
	protected	Vector<Stack<Integer>>			_futureFc;

	public FCCBJAlgorithm() {
		super();
	}

	public FCCBJAlgorithm(Problem problem) {
		super(problem);
	}

	@Override
	protected void init(Problem problem) {

		super.init(problem);

		_reductions = new Vector<Stack<Vector<Integer>>>(_problem.getN());
		_pastFc = new Vector<Stack<Integer>>(_problem.getN());
		_futureFc = new Vector<Stack<Integer>>(_problem.getN());
		
		for (int i = 0; i < _problem.getN(); i++){
			
			_reductions.add(new Stack<Vector<Integer>>());
			
			_pastFc.add(new Stack<Integer>());
			_futureFc.add(new Stack<Integer>());
		}
	}

	@Override
	public int label(int i) {
		
		_consistent = false;
		
		while  (!_currentDomain.get(i).isEmpty() && !_consistent){

			_problem.setVi(i, _currentDomain.get(i).firstElement());//Always going for the first
			_consistent = true;		
			int j;
			for(j = i+1; j < _problem.getN() && _consistent; j++){
				_consistent = checkForward(i, j);
			}
			if(!_consistent){
				_currentDomain.get(i).remove(0);//Hard coded 0 
				undoReductions(i);
				_confSets.get(i).addAll(_pastFc.get(j-1));
			}
		
		}

		return (_consistent) ? i + 1 : i;
	}
	
	@Override
	public int unlabel(int i) {

		int h = getHFromI(i);
		
		// TODO 	if (-1 != h){ ... }
		
		_confSets.get(h).addAll(_confSets.get(i));
		_confSets.get(h).addAll(_pastFc.get(i));
		_confSets.get(h).remove(h);//TODO:Possibly a bug here
		
		for(int j = i;j >= h + 1;j--){
			_confSets.get(j).clear();
			undoReductions(j);
			updateCurrentDomain(j);
		}
		
		undoReductions(h);
		_currentDomain.get(h).remove(_problem.getV().get(h));
		_consistent = !_currentDomain.get(h).isEmpty();
		
		return h;
	}
	
	@Override
	protected int getHFromI(int i) {
		
		int tMax = -2;
		
		for(Integer tX :_pastFc.get(i)){
			if(tX > tMax)
				tMax = tX;
		}
		if(tMax < _confSets.get(i).last())
			tMax = _confSets.get(i).last();

		return tMax;
	}
	
	
	public boolean checkForward(int i, int j){

		Vector<Integer> tReduction = new Vector<Integer>();

		int size = _currentDomain.get(j).size();

		for (int k = 0; k < size; k++){

			_problem.getV().set(j, _currentDomain.get(j).get(k));

			if(!_problem.check(i, _problem.getV().get(i), j, _problem.getV().get(j)))
				tReduction.add(_problem.getV().get(j));
		}

		if (!tReduction.isEmpty()){

			_currentDomain.get(j).removeAll(tReduction);
			_reductions.get(j).push(tReduction);
			_futureFc.get(i).push(j);
			_pastFc.get(j).push(i);
		}

		return !_currentDomain.get(j).isEmpty();	
	}

	public void undoReductions(int i){

		Vector<Integer> tReduction = null;

		for (Integer j: _futureFc.get(i)){

			tReduction = _reductions.get(j).pop();
			_currentDomain.get(j).addAll(tReduction);
			_pastFc.get(j).pop();
		}

		_futureFc.get(i).clear();
	}

	public void updateCurrentDomain(int i){

		super.restoreCurrentDomain(i);

		for (Vector<Integer> tReduction : _reductions.get(i)) {
			_currentDomain.get(i).removeAll(tReduction);
		}
	}
}

