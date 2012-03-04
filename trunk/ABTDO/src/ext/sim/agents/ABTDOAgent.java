package ext.sim.agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import bgu.dcr.az.api.*;
import bgu.dcr.az.api.agt.*;
import bgu.dcr.az.api.ano.*;
import bgu.dcr.az.api.tools.*;
import bgu.dcr.az.api.ano.WhenReceived;
import bgu.dcr.az.api.ds.ImmutableSet;

@Algorithm(name = "ABTDO", useIdleDetector = true)
public class ABTDOAgent extends SimpleAgent {

	// In ABT DO agents send ok? messages to all constraining agents (i.e.,
	// their
	// neighbors in the constraints graph). Although agents might hold in their
	// Agent views
	// assignments of agents with lower priorities, according to their Current
	// order, they
	// eliminate values from their domain only if they violate constraints with
	// higher
	// priority agents.

	private Assignment agent_view = null;
	private Integer current_value = null;
	private Map<Integer, Vector<Assignment>> nogoodsPerRemovedValue = null;

	private Set<Integer> myAllNeighbors = null;
	private Set<Integer> myLowerPriorityNeighbors = null;

	private Order current_order = null;
	private Heuristic heuristic = null;

	@Override
	public void start() {

		assignFirstVariable();

		agent_view = new Assignment();

		nogoodsPerRemovedValue = new HashMap<Integer, Vector<Assignment>>();

		initializeNeighbors();

		current_order = new Order(getNumberOfVariables());

		heuristic = new RandomHeuristic();

		// KICK START THE ALGORITHM..
		send("OK", current_value).toAll(myAllNeighbors); // TODO: to all..

		print(getId() + " sends OK: to all his neighbors with value "
				+ current_value + " from method 'start'");
	}

	private void assignFirstVariable() {

		current_value = getDomainSize() + 1;

		for (Integer d : getDomain())
			if (d < current_value)
				current_value = d;
	}

	private void initializeNeighbors() {

		myAllNeighbors = new HashSet<Integer>();
		myLowerPriorityNeighbors = new HashSet<Integer>();

		for (Integer n : getNeighbors()) {

			myAllNeighbors.add(n);

			if (n > getId())
				myLowerPriorityNeighbors.add(n);
		}
	}

	@WhenReceived("OK")
	public void handleOK(int value) {

		print(getId() + " got OK: from " + getCurrentMessage().getSender()
				+ " with value " + value);

		int sender = getCurrentMessage().getSender();

		agent_view.assign(sender, value);
		removeNonConsistentNoGoods(sender, value);
		checkAgentView();
	}

	private void removeNonConsistentNoGoods(int var, int val) {

		for (Integer key : nogoodsPerRemovedValue.keySet()) {

			Vector<Assignment> tNogoods = nogoodsPerRemovedValue.get(key);

			Vector<Assignment> toRemove = new Vector<Assignment>();

			for (Assignment tNogood : tNogoods)
				if (tNogood.isAssigned(var)
						&& tNogood.getAssignment(var) != val)
					toRemove.add(tNogood);

			tNogoods.remove(toRemove);

			if (tNogoods.isEmpty())
				nogoodsPerRemovedValue.remove(key);
		}
	}

	@WhenReceived("ORDER")
	public void handleORDER(Order received_order) {

		print(getId() + " got ORDER: from " + getCurrentMessage().getSender()
				+ " with order " + received_order);

		if (received_order.compareTo(current_order) > 0) {

			print(getId() + " says that the received_order: " + received_order
					+ " is more up to date than the current_order: "
					+ current_order);

			current_order = received_order;

			// TODO: what parameters?..
			removeNonConsistentNoGoods(getId(), current_value);

			checkAgentView();
		}
	}

	@WhenReceived("NOGOOD")
	public void handleNOGOOD(Assignment noGood) {

		print(getId() + " got NOGOOD: from " + getCurrentMessage().getSender()
				+ " with noGood " + noGood);

		int lowerThanMe = getLowerPriorityAgentThanMeFromNoGood(noGood);

		if (-1 != lowerThanMe) {

			send("NOGOOD", noGood).to(lowerThanMe);

			print(getId()
					+ " sends NOGOOD: to "
					+ lowerThanMe
					+ " because he is lower than me and appearing in the received nogood, from method 'handleNOGOOD'");

			send("OK", current_value).to(getCurrentMessage().getSender());

			print(getId() + " sends OK: to " + getCurrentMessage().getSender()
					+ " with value " + current_value
					+ " from method 'handleNOGOOD'");
		} else {

			if (isNogoodConsistentWithAgentView(noGood)
					&& noGood.getAssignment(getId()) == current_value) {

				storeNogood(noGood);

				addNewNeighborsFromNogood(noGood);
				checkAgentView();
			} else {

				send("OK", current_value).to(getCurrentMessage().getSender());

				print(getId() + " sends OK: to "
						+ getCurrentMessage().getSender() + " with value "
						+ current_value + " from method 'handleNOGOOD'");
			}
		}
	}

	private int getLowerPriorityAgentThanMeFromNoGood(Assignment noGood) {
		
		int minAgent = current_order.getPosition(getId());
		
		ImmutableSet<Integer> nogoodVariables = noGood.assignedVariables();
		
		for (int agent : nogoodVariables)
			if (current_order.getPosition(agent) < minAgent)
				minAgent = agent;
		
		return (getId() == minAgent) ? -1 : minAgent;
	}

	// TODO: check if ok..
	private boolean isNogoodConsistentWithAgentView(Assignment noGood) {

		ImmutableSet<Integer> noGoodVariables = noGood.assignedVariables();
		ImmutableSet<Integer> agentViewVariables = agent_view
				.assignedVariables();

		for (Integer v : noGoodVariables) {

			if (!agentViewVariables.contains(v))
				continue;

			else if (noGood.getAssignment(v.intValue()) != agent_view
					.getAssignment(v.intValue()))
				return false;
		}

		return true;
	}

	private void storeNogood(Assignment noGood) {

		Vector<Assignment> x = nogoodsPerRemovedValue.get(current_value);

		if (null == x) {

			Vector<Assignment> y = new Vector<Assignment>();
			y.add(noGood);
			nogoodsPerRemovedValue.put(current_value, y);
		} else {

			x.add(noGood);
		}
	}

	private void addNewNeighborsFromNogood(Assignment noGood) {

		ImmutableSet<Integer> noGoodVariables = noGood.assignedVariables();

		for (Integer v : noGoodVariables) {

			// TODO: check against all neighbors or not?.. and if so, also with
			// lowerNeighbors??..
			if (!myAllNeighbors.contains(v) && (getId() != v)) {

				send("ADD_NEIGHBOR").to(v);

				print(getId() + " sends ADD_NEIGHBOR: to " + v
						+ " from method 'addNewNeighborsFromNogood'");

				agent_view.assign(v, noGood.getAssignment(v).intValue());
			}
		}
	}

	private void checkAgentView() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onIdleDetected() {
		finish(current_value);
	}

	private void print(String string) {
		// System.err.println(string);
		// System.err.flush();
	}
}
