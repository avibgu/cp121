package ext.sim.agents;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import bgu.dcr.az.api.agt.*;
import bgu.dcr.az.api.ano.*;
import bgu.dcr.az.api.ds.ImmutableSet;
import bgu.dcr.az.api.tools.*;
import bgu.dcr.az.api.ano.WhenReceived;

@Algorithm(name = "ABT", useIdleDetector = false)
public class ABTAgent extends SimpleAgent {

//	http://azapi-test.googlecode.com/svn/trunk/bin/documentation/javadoc/index.html
	
	private Assignment				agent_view				= null;
	private Integer					current_value			= null;
	
	private Vector<Assignment>		nogoods					= null;	// TODO: is that a good representation?..
	private Map<Integer,Assignment> nogoodsPerRemovedValue	= null;	// TODO: experimental..
	
	@Override
	public void start() {
		
		current_value = getDomainSize() + 1;
		
		for (Integer d : getDomain())
			if (d < current_value)
				current_value = d;

		agent_view = new Assignment();	// TODO: should we add current_value to agent_view?... i don't think..
		
		nogoods = new Vector<Assignment>();
		
		nogoodsPerRemovedValue = new HashMap<Integer, Assignment>();
		
		// KICK START THE ALGORITHM..
		send("OK", current_value).toAllAgentsAfterMe();
	}
	
	@WhenReceived("OK")
	public void handleOK(int value) {

		int sender = getCurrentMessage().getSender();
		
		agent_view.assign(sender, value);
		removeNonConsistentNoGoods(sender, value);
		checkAgentView();
	}

	@WhenReceived("NOGOOD")
	public void handleNOGOOD(Assignment noGood) {
		
		int old_value = current_value;
		
		if (isNogoodConsistentWithAgentView(noGood) &&
			noGood.isConsistentWith(getId(), current_value, getProblem())){
			
			nogoods.add(noGood);				// TODO: "store noGood" check if ok..
			nogoodsPerRemovedValue.put(current_value, noGood);	// TODO: experimental.. what to do when already have nogood for some value?..
			
			addNewNeighborsFromNogood(noGood);
			checkAgentView();
		}
		
		if (old_value == current_value)
			 send("OK", current_value).to(getCurrentMessage().getSender());
	}

	private void checkAgentView() {

		if (!agent_view.isConsistentWith(getId(), current_value, getProblem())){
			
			int d = getValueFromDWhichConsistentWithAgentView();
			
			if (-1 == d)	//	There is no value in D which consistent with agent_view..
				backtrack();
			
			else{
				
				current_value = getValueFromDWhichConsistentWithAgentView();
				send("OK", current_value).toAllAgentsAfterMe();	// TODO: is it going to send the message to the low_priority_neighbors??.. i think it is..
			}
		}
	}

	private void backtrack() {
		
		Assignment noGood = resolveInconsistentSubset();
		
		if (noGood.getNumberOfAssignedVariables() == 0){
			
			send("NO_SOLUTION").toAllAgentsAfterMe(); // TODO: is this sufficient?...
			finish();
			return;
		}
		
		int lowerPriorityVar = -1;
		
		for (Integer v : noGood.assignedVariables())
			if (v > lowerPriorityVar)
				lowerPriorityVar = v;
		
		send("NOGOOD", noGood).to(lowerPriorityVar);
		
		agent_view.unassign(lowerPriorityVar);
		
		removeNogoodsThatContainThisVariable(lowerPriorityVar, noGood.getAssignment(lowerPriorityVar));
		
		checkAgentView();
	}

	private void removeNonConsistentNoGoods(int sender, int value) {

		// TODO: is that what we want to do?..
		
		Vector<Assignment> toRemove = new Vector<Assignment>();
		
		for (Assignment a : nogoods)
			if (a.isAssigned(sender) && a.getAssignment(sender) != value)
				toRemove.add(a);
		
		nogoods.removeAll(toRemove);
		
		// TODO: experimental..
		for (Integer val : nogoodsPerRemovedValue.keySet()){
			
			Assignment a = nogoodsPerRemovedValue.get(val);
			
			if (a.isAssigned(sender) && a.getAssignment(sender) != value)
				nogoodsPerRemovedValue.remove(val);			
		}
	}

	private boolean isNogoodConsistentWithAgentView(Assignment noGood) {
		
		ImmutableSet<Integer> noGoodVariables = noGood.assignedVariables();
		ImmutableSet<Integer> agentViewVariables = agent_view.assignedVariables();
		
		for (Integer v : noGoodVariables){
			
			if (!agentViewVariables.contains(v))
				continue;
			
			else if (noGood.getAssignment(v.intValue()) != agent_view.getAssignment(v.intValue()))
				return false;
		}
		
		return true;
	}

	private void addNewNeighborsFromNogood(Assignment noGood) {

		ImmutableSet<Integer> noGoodVariables = noGood.assignedVariables();
		Set<Integer> neighbors = getNeighbors();
		
		for (Integer v : noGoodVariables){
			
			if (!neighbors.contains(v)){
				
				send("ADD_NEIGHBOR").to(v);
				agent_view.assign(v, noGood.getAssignment(v));
			}
		}
	}

	@WhenReceived("ADD_NEIGHBOR")
	public void handleADDNEIGHBOR(){
		getNeighbors().add(getCurrentMessage().getSender());
		// TODO: add the sender also to the agent_view or something else??..
	}
	
	private int getValueFromDWhichConsistentWithAgentView() {

		for (Integer v : getDomain())
			if (agent_view.isConsistentWith(getId(), v, getProblem()))
				return v.intValue();
		
		return -1;
	}
	
	private Assignment resolveInconsistentSubset() {
		// TODO WTF??... something which related to DBT??..
		
//		Assignment nogood = new Assignment();
//		
//		for (Assignment a : nogoods)
//			if (a.assignedVariables().contains(getId()) &&
//					a.getAssignment(getId()) == current_value)
//				nogood.as
		
		return nogoodsPerRemovedValue.get(current_value);
	}
	
	private void removeNogoodsThatContainThisVariable(int var, int val) {
		
		// TODO: is that what we want to do?..
		
		Vector<Assignment> toRemove = new Vector<Assignment>();
		
		for (Assignment a : nogoods)
			if (a.assignedVariables().contains(var) &&
					a.getAssignment(var) == val)
				toRemove.add(a);
		
		nogoods.removeAll(toRemove);
		
		// TODO: experimental..

		for (Integer value : nogoodsPerRemovedValue.keySet()){
			
			Assignment a = nogoodsPerRemovedValue.get(value);
			
			if (a.isAssigned(var) && a.getAssignment(var) == value)
				nogoodsPerRemovedValue.remove(value);	
		}
	}

	@WhenReceived("NO_SOLUTION")
	public void handleNOSOLUTION(){
		finish();	// TODO: is this sufficient?..
	}

	
}
