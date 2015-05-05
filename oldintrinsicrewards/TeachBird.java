package intrinsicrewards;

import java.awt.Color;
import java.util.List;

import burlap.behavior.singleagent.*;
import burlap.domain.singleagent.gridworld.*;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.*;
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
import burlap.oomdp.auxiliary.common.NullTermination;

public class TeachBird {

	BirdBox			 			birdBox;
	Domain						domain;
	StateParser 				sp;
	RewardFunction 				rf, fitnessRF, openRF, paperRF;
	TerminalFunction			tf;
	StateConditionTest			goalCondition;
	State 						initialState;
	DiscreteStateHashFactory	hashingFactory;

	public static final int MAXSTEPS = 10000;

	public TeachBird(){

		//create the domain
		birdBox = new BirdBox();
		domain = birdBox.generateDomain();

		//create the state parser
		sp = new GridWorldStateParser(domain);

		//define the task
	    rf = new FitnessRF();
		fitnessRF = new FitnessRF();
		openRF = new OpenRF(1, 5);
		paperRF = new PaperRF(.7, .3, -.01, -.05, .2, .1, -.02);
		tf = new NullTermination();
		goalCondition = new TFGoalCondition(tf);


		BirdStateGenerator stateGen = new BirdStateGenerator();
		initialState = stateGen.generateState();

		//set up the state hashing system
		hashingFactory = new DiscreteStateHashFactory();
		hashingFactory.setAttributesForClass(BirdBox.CLASSAGENT,
				domain.getObjectClass(BirdBox.CLASSAGENT).attributeList);


		//add visual observer
		//VisualActionObserver observer = new VisualActionObserver(domain,
		//	birdBox.getVisualizer());
		//observer.setFrameDelay(100);
		//((SADomain)this.domain).setActionObserverForAllAction(observer);
		//observer.initGUI();


	}


	public void visualize(String outputPath){
		Visualizer v = birdBox.getVisualizer();
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v,
								domain, sp, outputPath);
	}

	public void QLearning(String outputPath, RewardFunction qrf, double qinit, int numEpisodes){

		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}

		TerminalFunction qtf = new NullTermination();

		//discount= 0.99; initialQ=0.0; learning rate=0.9
		QLearning agent = new QLearning(domain, qrf, qtf, BirdBox.DISCOUNTFACTOR, hashingFactory, qinit, BirdBox.LEARNINGRATE);
		//Policy p = new GreedyQPolicy((QComputablePlanner)agent);
		agent.setNumEpisodesToStore(numEpisodes);
		//run learning for 100 episodes
		for(int i = 0; i < numEpisodes; i++){
			EpisodeAnalysis ea = agent.runLearningEpisodeFrom(initialState, MAXSTEPS);
			ea.writeToFile(String.format("%se%03d", outputPath, i), sp);
			//this.valueFunctionVisualize((QComputablePlanner)agent, p);
		}

		System.out.println("FITNESS");
		for (EpisodeAnalysis ea: agent.getAllStoredLearningEpisodes()){
			printFitness(ea, 1);
		}
		System.out.println("BOTH OPEN");
		for (EpisodeAnalysis ea: agent.getAllStoredLearningEpisodes()){
			printOpenness(ea, 1);
		}
		EpisodeAnalysis ea = agent.getLastLearningEpisode();
		for (int i = ea.maxTimeStep()-1; i>=0; i--){
			State s = ea.getState(i);
			System.out.println(s.getStateDescription());
			double topQ = -999999;
			AbstractGroundedAction topAction = null;
			for(QValue q : agent.getQs(s)){
				System.out.println(sprintQ(q));
				if(q.q > topQ){
					topAction = q.a;
					topQ = q.q;
				}
			}
			System.out.println("BEST: " + topAction);
			try {System.in.read();} catch(Exception e){};
		}
	}


	private String sprintQ(QValue q){
		return "Action: " + q.a + " Q: " + q.q;
	}

	private boolean isOnBox(State s){
		ObjectInstance agent = s.getFirstObjectOfClass(BirdBox.CLASSAGENT);
		ObjectInstance box0 = s.getObject("box0");
		ObjectInstance box1 = s.getObject("box1");
		int agentX = agent.getDiscValForAttribute(BirdBox.ATTX);
		int agentY = agent.getDiscValForAttribute(BirdBox.ATTY);
		int box0X = box0.getDiscValForAttribute(BirdBox.ATTX);
		int box0Y = box0.getDiscValForAttribute(BirdBox.ATTY);
		int box1X = box1.getDiscValForAttribute(BirdBox.ATTX);
		int box1Y = box1.getDiscValForAttribute(BirdBox.ATTY);

		if( agentX == box0X && agentY == box0Y){
			return true;
		}
		if( agentX == box1X && agentY == box1Y){
			return true;
		}
		return false;

	}

	private void printFitness(EpisodeAnalysis ea, int step){
		int max = ea.maxTimeStep();
		int t = 0;
		for (int i = 0; i < max; i+=step) {
			if (!ea.getState(i).getFirstObjectOfClass(BirdBox.CLASSAGENT).getBooleanValue(BirdBox.ATTHUNGRY)){
				t++;
			}
			System.out.print(t + ",");
		}
		System.out.println("");
	}

	private void printOpenness(EpisodeAnalysis ea, int step){
		int max = ea.maxTimeStep();
		int t = 0;
		for (int i = 0; i < max; i+=step) {
			if (ea.getState(i).getObject("box0").getStringValForAttribute(BirdBox.ATTOPEN).equals("open") &&
					ea.getState(i).getObject("box1").getStringValForAttribute(BirdBox.ATTOPEN).equals("open")){
				t++;
			}
			System.out.print(t + ",");
		}
		System.out.println("");
	}

	public void ValueIterationExample(String outputPath){

		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}


		OOMDPPlanner planner = new ValueIteration(domain, rf, tf, 0.99, hashingFactory,
								0.001, 100);
		planner.planFromState(initialState);

		//create a Q-greedy policy from the planner
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);

		//record the plan results to a file
		p.evaluateBehavior(initialState, rf, tf).writeToFile(outputPath + "planResult", sp);

		//visualize the value function and policy
		this.valueFunctionVisualize((QComputablePlanner)planner, p);

	}


	public void valueFunctionVisualize(QComputablePlanner planner, Policy p){
		List <State> allStates = StateReachability.getReachableStates(initialState,
			(SADomain)domain, hashingFactory);
		LandmarkColorBlendInterpolation rb = new LandmarkColorBlendInterpolation();
		rb.addNextLandMark(0., Color.RED);
		rb.addNextLandMark(1., Color.BLUE);

		StateValuePainter2D svp = new StateValuePainter2D(rb);
		svp.setXYAttByObjectClass(BirdBox.CLASSAGENT, BirdBox.ATTX,
			BirdBox.CLASSAGENT, BirdBox.ATTY);

		PolicyGlyphPainter2D spp = new PolicyGlyphPainter2D();
		spp.setXYAttByObjectClass(BirdBox.CLASSAGENT, BirdBox.ATTX,
			BirdBox.CLASSAGENT, BirdBox.ATTY);
		spp.setActionNameGlyphPainter(BirdBox.ACTIONNORTH, new ArrowActionGlyph(0));
		spp.setActionNameGlyphPainter(BirdBox.ACTIONSOUTH, new ArrowActionGlyph(1));
		spp.setActionNameGlyphPainter(BirdBox.ACTIONEAST, new ArrowActionGlyph(2));
		spp.setActionNameGlyphPainter(BirdBox.ACTIONWEST, new ArrowActionGlyph(3));
		spp.setRenderStyle(PolicyGlyphRenderStyle.DISTSCALED);

		ValueFunctionVisualizerGUI gui = new ValueFunctionVisualizerGUI(allStates, svp, planner);
		gui.setSpp(spp);
		gui.setPolicy(p);
		gui.setBgColor(Color.GRAY);
		gui.initGUI();
	}


	public void experimenterAndPlotter(){

		//custom reward function for more interesting results
		final RewardFunction fitnessRF = new FitnessRF();
		final RewardFunction paperRF = new PaperRF(.7, .3, -.01, -.05, .2, .1, -.02);
		final RewardFunction paperFitnessRF = new PaperRF(1, 1, 0, 0, 0, 0, 0);
		final RewardFunction badRF = new PaperRF(0, 0, 1, 1, 1, 1, 1);
		final RewardFunction openRF = new PaperRF(1, 1, 0, 0, .5, .5, 0);
		final RewardFunction openXRF = new PaperRF(1, 1, 0, 0, .1, .1, 0);
		final RewardFunction simplePenaltyRF = new PaperRF(1, 1, -.05, -.05, .1, .1, -.05);
		final RewardFunction simplePenaltyX1RF = new PaperRF(5, 5, -.05, -.05, .1, .1, -.05);
		final RewardFunction simplePenaltyX2RF = new PaperRF(1, 1, -.5, -.5, .1, .1, -.5);
		final RewardFunction openPenaltyRF = new PaperRF(1, 1, -.05, -.05, 0, 0, -05);
		final RewardFunction simpleOpenRF = new OpenRF(.2, 1);
		final TerminalFunction nullTF = new NullTermination();

		/**
		 * Create factories for Q-learning agent
		 */
		LearningAgentFactory learningFactoryFitness = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Fitness Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, fitnessRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactoryPaperFitness = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Paper Fitness Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, paperFitnessRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactoryBad = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Bad Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, badRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactoryOpen = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Open Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, openRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactoryPaper = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Paper Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, paperRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactorySimpleOpen = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Simple Open Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, simpleOpenRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactoryOpenX = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Open X Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, openXRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactorySimplePenalty = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Simple Penalty Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, simplePenaltyRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactorySimplePenaltyX1 = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Simple Penalty X1 Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, simplePenaltyX1RF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactorySimplePenaltyX2 = new LearningAgentFactory() {


			@Override
			public String getAgentName() {
				return "Q Simple Penalty X2 Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, simplePenaltyX2RF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};

		LearningAgentFactory learningFactoryOpenPenalty = new LearningAgentFactory() {

			@Override
			public String getAgentName() {
				return "Q Open Penalty Reward";
			}

			@Override
			public LearningAgent generateAgent() {
				//Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double qInit, double learningRate
				return new QLearning(domain, openPenaltyRF, nullTF, BirdBox.DISCOUNTFACTOR, hashingFactory, 0.0, BirdBox.LEARNINGRATE, MAXSTEPS);
			}
		};



		StateGenerator sg = new BirdStateGenerator();

		LearningAlgorithmExperimenter exp = new LearningAlgorithmExperimenter((SADomain)this.domain,
			fitnessRF, sg, 20, 1,
			learningFactoryFitness,
			learningFactoryPaper,
			learningFactoryOpen,
			learningFactorySimpleOpen,
			learningFactoryPaperFitness,
			learningFactoryBad
			);

		exp.setUpPlottingConfiguration(500, 250, 2, 1700,
			TrialMode.MOSTRECENTANDAVERAGE,
			PerformanceMetric.CUMULATIVEREWARDPERSTEP);

		exp.startExperiment();

		exp.writeStepAndEpisodeDataToCSV("expData");

	}


	protected static class FitnessRF implements RewardFunction{

        public FitnessRF(){
        }

        @Override
        public double reward(State s, GroundedAction a, State sprime) {

            //get location of agent in next state
            ObjectInstance agent = sprime.getFirstObjectOfClass(BirdBox.CLASSAGENT);
            boolean hungry = agent.getBooleanValue(BirdBox.ATTHUNGRY);

            //are they at goal location?
            if(!hungry){
                return 1;
            }

            return 0;
        }

    }

	protected static class OpenRF implements RewardFunction{
		double openReward;
		double eatReward;

        public OpenRF(double openReward, double eatReward){
			this.openReward = openReward;
			this.eatReward = eatReward;
	 	}

        @Override
        public double reward(State s, GroundedAction a, State sprime) {

            //get location of agent in next state
            ObjectInstance agent = sprime.getFirstObjectOfClass(BirdBox.CLASSAGENT);
            ObjectInstance box0 = sprime.getObject("box0");
            ObjectInstance box1 = sprime.getObject("box1");
            boolean hungry = agent.getBooleanValue(BirdBox.ATTHUNGRY);
            String open0 = box0.getStringValForAttribute(BirdBox.ATTOPEN);
            String open1 = box1.getStringValForAttribute(BirdBox.ATTOPEN);

            //are they at goal location?
            if(!hungry){
                return eatReward;
            }
			if(open1.equals("half-open") || open0.equals("half-open")){
				return openReward;
			}

            return 0;
        }

    }

	protected static class PaperRF implements RewardFunction{

		double soo, son, hoo, hon, hoh, hnh, hnn;

        public PaperRF(double soo, double son, double hoo, double hon, double hoh, double hnh, double hnn){
			this.soo = soo;
			this.son = son;
			this.hoo = hoo;
			this.hon = hon;
			this.hoh = hoh;
			this.hnh = hnh;
			this.hnn = hnn;
		}


        @Override
        public double reward(State s, GroundedAction a, State sprime) {

            //get location of agent in next state
            ObjectInstance agent = sprime.getFirstObjectOfClass(BirdBox.CLASSAGENT);
            ObjectInstance box0 = sprime.getObject("box0");
            ObjectInstance box1 = sprime.getObject("box1");
            boolean hungry = agent.getBooleanValue(BirdBox.ATTHUNGRY);
            String open0 = box0.getStringValForAttribute(BirdBox.ATTOPEN);
            String open1 = box1.getStringValForAttribute(BirdBox.ATTOPEN);


            //are they at goal location?
            if(!hungry){
                if((open0.equals("open") && open1.equals("closed")) || (open1.equals("open") && open0.equals("closed"))){
					return son;
				}
				else if (open0.equals("open") && open1.equals("open")){
					return soo;
				}
				else {
					System.out.println("Bad open hungry combination: [hunger] " + hungry +" [open0] "+ open0 + " [open1] " + open1);
				}
            }
			else{
                if((open0.equals("open") && open1.equals("closed")) || (open1.equals("open") && open0.equals("closed"))){
					return hon;
				}
				else if (open0.equals("open") && open1.equals("open")){
					return hoo;
				}
				else if((open0.equals("open") && open1.equals("half-open")) || (open1.equals("open") && open0.equals("half-open"))){
					return hoh;
				}
				else if((open0.equals("half-open") && open1.equals("closed")) || (open1.equals("half-open") && open0.equals("closed"))){
					return hnh;
				}
				else if (open0.equals("closed") && open1.equals("closed")){
					return hnn;
				}
				else {
					System.out.println("Bad open hungry combination: [hunger] " + hungry +" [open0] "+ open0 + " [open1] " + open1);
				}

			}
			System.out.println("Bad open hungry combination: [hunger] " + hungry +" [open0] "+ open0 + " [open1] " + open1);
            return -999999;
        }

    }


	public static void main(String[] args) {


		TeachBird learner = new TeachBird();
		String outputPath = "output/";


		//uncomment the example you want to see (and comment-out the rest)

		RewardFunction paperRF = new PaperRF(.7, .3, -.01, -.05, .2, .1, -.02);
		RewardFunction fitnessRF = new FitnessRF();
		learner.QLearning(outputPath, fitnessRF, 0, 1);
		//learner.ValueIterationExample(outputPath);
		//learner.experimenterAndPlotter();


		//run the visualizer (only use if you don't use the experiment plotter example)
		//learner.visualize(outputPath);

	}

}