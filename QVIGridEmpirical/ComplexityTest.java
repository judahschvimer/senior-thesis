import java.awt.Color;
import java.util.ArrayList;
import java.util.Scanner;

import burlap.behavior.singleagent.*;
import burlap.domain.singleagent.gridworld.*;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.*;
import burlap.oomdp.stochasticgames.*;
import burlap.behavior.stochasticgame.*;
import burlap.oomdp.singleagent.common.*;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.singleagent.learning.*;
import burlap.behavior.singleagent.learning.tdmethods.*;
import burlap.behavior.singleagent.planning.*;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.*;
import burlap.behavior.singleagent.planning.deterministic.informed.Heuristic;
import burlap.behavior.singleagent.planning.deterministic.informed.astar.AStar;
import burlap.behavior.singleagent.planning.deterministic.uninformed.bfs.BFS;
import burlap.behavior.singleagent.planning.deterministic.uninformed.dfs.DFS;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.oomdp.visualizer.Visualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.auxiliary.common.ConstantStateGenerator;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.performance.LearningAlgorithmExperimenter;
import burlap.behavior.singleagent.auxiliary.performance.PerformanceMetric;
import burlap.behavior.singleagent.auxiliary.performance.TrialMode;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.*;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.PolicyGlyphPainter2D.PolicyGlyphRenderStyle;
import burlap.oomdp.singleagent.common.VisualActionObserver;

public class ComplexityTest {

	public static void main(String[] args) {


		String outputPath = "output/";
		ComplexityTest example = new ComplexityTest();
		int numRewardFunctions = 6;
		int maxSize = 200;
		double numTrials = 1000;
		ArrayList<ArrayList<Double>> averages = new ArrayList<>(numRewardFunctions);

		ArrayList<RewardConfig> rfs = new ArrayList<RewardConfig>();
		rfs.add(new RewardConfig("GoalBasedRF", 0, -1, 0, 0.99));
		rfs.add(new RewardConfig("GoalBasedRF", 0, -1, 0, 1.0));
		rfs.add(new RewardConfig("GoalBasedRF", 1, 0, 0, 0.99));
		rfs.add(new RewardConfig("GoalBasedRF", 1, 0, 0, 1.0));
		//rfs.add(new RewardConfig("GoalBasedRF", 5, -0.3, 0, 0.99));
		//rfs.add(new RewardConfig("GoalBasedRF", 0, -1, 1, 0.99));
		rfs.add(new RewardConfig("GoalBasedRF", 1, 0, 1, 0.99));
		rfs.add(new RewardConfig("GoalBasedRF", 1, 0, 1, 1.0));
		//rfs.add(new RewardConfig("GoalBasedRF", 5, -0.3, 1, 0.99));

		for(int i = 0; i < numRewardFunctions; i++){
			averages.add(new ArrayList<Double>(maxSize-2));
			System.out.println("RF " + i + " is " + rfs.get(i));
		}

		for(int n = 2; n < maxSize; n += 20){
			for(int i = 0; i < rfs.size(); i++){
				double totalSteps = 0;

				for (int t = 0; t < numTrials; t++){
					//create the domain
					GridWorldDomain gwdg = new GridWorldDomain(n, n);
					Domain domain = gwdg.generateDomain();

					//define the task
					TerminalFunction tf = new SinglePFTF(domain.getPropFunction(GridWorldDomain.PFATLOCATION));
					StateConditionTest goalCondition = new TFGoalCondition(tf);

					//create the state parser
					StateParser sp = new GridWorldStateParser(domain);

					/*VisualActionObserver observer = new VisualActionObserver(domain,
					GridWorldVisualizer.getVisualizer(gwdg.getMap()));
					((SADomain)domain).setActionObserverForAllAction(observer);
					observer.setFrameDelay(50);
					observer.initGUI();*/

					//set up the initial state of the task
					State initialState = GridWorldDomain.getOneAgentOneLocationState(domain);
					GridWorldDomain.setAgent(initialState, 0, 0);
					GridWorldDomain.setLocation(initialState, 0, n-1, n-1);

					//set up the state hashing system
					DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
					hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
							domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);

					RewardFunction rf = CreateRewardFunction("GoalBasedRF", goalCondition, rfs.get(i).GoalReward, rfs.get(i).StepReward);
					LearningAgent agent = new QLearning(domain, rf, tf, 0.99, hashingFactory, rfs.get(i).InitQ, 1.0);

					EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState);
					//ea.writeToFile(String.format("%se%03d", outputPath, i), sp);
					totalSteps += ea.numTimeSteps();

					//new Scanner(System.in).nextLine();
					//observer.dispose();
				}

				double average = totalSteps/numTrials;
				averages.get(i).add(average);
				System.out.println(n + " x " + n + " with RF " + i + ": " + average);

			}
		}

		for(int i = 0; i < averages.size(); i++){
			System.out.println(rfs.get(i));
			for(Double avg: averages.get(i)){
				System.out.println(avg);
			}
		}

	}

	private static RewardFunction CreateRewardFunction(String rfType, StateConditionTest goalCondition, double goalReward, double stepReward) {
			switch(rfType){
				case "GoalBasedRF":
					return new GoalBasedRF(goalCondition, goalReward, stepReward);
				case "UniformCostRF":
					return new UniformCostRF();
				default:
					return new GoalBasedRF(goalCondition, goalReward, stepReward);
			}

	}

	private static class RewardConfig {
		public String RewardType;
		public double GoalReward;
		public double StepReward;
		public double InitQ;
		public double Discount;

		public RewardConfig(String rewardType, double goalReward, double stepReward, double initQ, double discount){
			this.RewardType = rewardType;
			this.GoalReward = goalReward;
			this.StepReward = stepReward;
			this.InitQ = initQ;
			this.Discount = discount;
		}
		@Override
		public String toString() {
		    return String.format(RewardType + " | Goal Reward: " + this.GoalReward + " StepReward: " + this.StepReward + " InitQ: " + this.InitQ + " Discount: " + this.Discount);
		}
	}
}