package ConsumerExperiment;
import burlap.behavior.singleagent.*;
import burlap.domain.singleagent.gridworld.*;
import burlap.oomdp.core.*;
import burlap.oomdp.singleagent.*;
import burlap.oomdp.singleagent.common.*;
import burlap.behavior.statehashing.DiscreteStateHashFactory;

public class BasicBehavior {



	ConsumerSim				    gen;
	Domain						domain;
	StateParser					sp;
	RewardFunction				rf;
	TerminalFunction			tf;
	TerminalFunction			gf;
	StateConditionTest			goalCondition;
	State						initialState;
	DiscreteStateHashFactory	hashingFactory;


	public BasicBehavior(){

		//create the domain
		gen = new ConsumerSim();
		domain = gen.generateDomain();

		//create the state parser
		sp = new UniversalStateParser(domain);

		//define the task
		rf = gen.new SellerRF();
		tf = gen.new SellerTF();
		gf = gen.new SellerGF();
		goalCondition = new TFGoalCondition(gf);

		//set up the initial state of the task
		initialState = ConsumerSim.getExampleState(domain);

		//set up the state hashing system
		hashingFactory = new DiscreteMaskHashFactory();
		hashingFactory.setAttributesForClass(GridWorldDomain.CLASSAGENT,
		domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);


	}
}