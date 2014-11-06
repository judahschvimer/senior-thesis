package ConsumerExperiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.auxiliary.StateEnumerator;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.pomdp.BeliefMDPPolicyAgent;
import burlap.behavior.singleagent.pomdp.qmdp.QMDP;
import burlap.behavior.singleagent.pomdp.wrappedmdpalgs.BeliefSarsa;
import burlap.behavior.singleagent.pomdp.wrappedmdpalgs.BeliefSparseSampling;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.singleagent.pomdp.BeliefState;
import burlap.oomdp.singleagent.pomdp.ObservationFunction;
import burlap.oomdp.singleagent.pomdp.PODomain;
import burlap.oomdp.singleagent.pomdp.POEnvironment;

public class ConsumerPOMDP implements DomainGenerator {
	public enum Distribution {
		Normal, Exponential, Uniform
	}

	//wtp = willingness-to-pay
    public static final String ATTWTP = "wtp";
    public static final String ATTDECISION = "decision";
    public static final String ATTPRICE = "price";

    public static final String CLASSOBSERVATION = "observation";
    public static final String CLASSCONSUMER = "consumer";

	public static final String VALBUY = "buy";
	public static final String VALPASS = "pass";
	public static final String VALLEAVE = "leave";

	public static final String OBBUY = "buy";
	public static final String OBPASS = "pass";
	public static final String OBLEAVE = "leave";

	public static final int PMIN = 0;
	public static final int PMAX = 10;

	public static final Distribution DIST = Distribution.Normal;
	public static final int MEAN = 4;
	public static final int STDEV = 2;

	public ConsumerPOMDP(){

	}

    @Override
    public Domain generateDomain() {

        PODomain domain = new PODomain();

        Attribute wtpAtt = new Attribute(domain, ATTWTP, AttributeType.INT);
        wtpAtt.setLims(PMIN, PMAX);
        Attribute decisionAtt = new Attribute(domain, ATTDECISION, AttributeType.DISC);
        decisionAtt.setDiscValues(new String[]{OBBUY, OBLEAVE});

		ObjectClass observationClass = new ObjectClass(domain, CLASSOBSERVATION);
        observationClass.addAttribute(decisionAtt);

        ObjectClass consumerClass = new ObjectClass(domain, CLASSCONSUMER);
        consumerClass.addAttribute(wtpAtt);

        for(int p = PMIN; p <= PMAX; p++){
			new SetPriceAction("price" + p, domain, p);
		}

		new ConsumerObservations(domain);

		StateEnumerator senum = new StateEnumerator(domain, new DiscreteStateHashFactory());
        for(int p = PMIN; p <= PMAX; p++){
			senum.getEnumeratedID(this.consumerPriceState(domain, p));
		}
		domain.setStateEnumerator(senum);


        return domain;

	}

	public static State consumerPriceState(PODomain domain, int price){
		State s = new State();
		ObjectInstance o = new ObjectInstance(domain.getObjectClass(CLASSCONSUMER), CLASSCONSUMER);
		o.setValue(ATTWTP, price);
		s.addObject(o);
		return s;
	}
	public static BeliefState getInitialBeliefState(PODomain domain){
		BeliefState bs = new BeliefState(domain);
		bs.initializeBeliefsUniformly();
		return bs;
	}

	private static int getSampleWTP(){
		Random gen = new Random();
		int wtp = 0;
		switch (DIST) {
			case Normal:
				wtp = (int) (gen.nextGaussian()*STDEV+MEAN);
				break;
			case Exponential:
				wtp =  (int) (-1)*((int) (((double) MEAN)*Math.log(gen.nextDouble())));
				break;
			case Uniform:
				wtp = PMIN + gen.nextInt(PMAX-PMIN);
			default:
				break;
		}
		wtp = Math.min(wtp, PMAX);
		wtp = Math.max(wtp, PMIN);
		return wtp;
	}

    protected class SetPriceAction extends Action{
		private int price;

        public SetPriceAction(String actionName, Domain domain, int price){
            super(actionName, domain, "");
			this.price = price;
        }

        @Override
        protected State performActionHelper(State s, String[] params) {

            ObjectInstance consumer = s.getFirstObjectOfClass(CLASSCONSUMER);
			int wtp = ConsumerPOMDP.getSampleWTP();
            consumer.setValue(ATTWTP, wtp);

            //return the state we just modified
            return s;
        }


        @Override
        public List<TransitionProbability> getTransitions(State s, String [] params){

			List<TransitionProbability> tps = new ArrayList<TransitionProbability>(PMAX-PMIN);

			for(int i=PMIN; i<PMAX; i++){
				State ns = s.copy();
				ObjectInstance consumer = ns.getFirstObjectOfClass(CLASSCONSUMER);
				consumer.setValue(ATTWTP, i);
				double probability = 1.0/((double)(PMAX-PMIN));
				tps.add(new TransitionProbability(ns, probability));
			}

            return tps;
        }


    }

	public class ConsumerObservations extends ObservationFunction{

		public ConsumerObservations(PODomain domain){
			super(domain);
		}

		@Override
		public List<State> getAllPossibleObservations() {

			List<State> result = new ArrayList<State>(2);

			result.add(this.observationBuy());
			result.add(this.observationLeave());

			return result;
		}

		@Override
		public State sampleObservation(State state, GroundedAction action){
			System.out.println("get obs");
			int price = Integer.parseInt(action.actionName().substring(5));
			int wtp = state.getFirstObjectOfClass(CLASSCONSUMER).getDiscValForAttribute(ATTWTP);
			ObjectInstance observation = state.getFirstObjectOfClass(CLASSOBSERVATION);
			String decision = this.getDecision(price, wtp);

			if(decision.equals(OBBUY)){
				return this.observationBuy();
			}
			else if(decision.equals(OBLEAVE)){
				return this.observationLeave();
			}
			else{
				throw new RuntimeException("Unknown action " + action.actionName() + "; cannot return observation sample.");
			}
		}

		@Override
		public double getObservationProbability(State observation, State state,
				GroundedAction action) {

			int price = Integer.parseInt(action.actionName().substring(5));
			int wtp = state.getFirstObjectOfClass(CLASSCONSUMER).getDiscValForAttribute(ATTWTP);
			String decision = this.getDecision(price, wtp);

			String oVal = observation.getFirstObjectOfClass(CLASSOBSERVATION).getStringValForAttribute(ATTDECISION);

			if(decision.equals(OBBUY)){
				if(oVal.equals(OBBUY)){
					return 1.;
				}
				return 0.;
			}
			else if(decision.equals(OBLEAVE)){
				if(oVal.equals(OBLEAVE)){
					return 1.;
				}
				return 0.;
			}
			else{
				throw new RuntimeException("Unknown action " + action.actionName() + "; cannot return observation probability.");
			}

		}

		protected String getDecision(double price, double wtp){
			if(price < wtp){
				return OBBUY;
			}
			else{
				return OBLEAVE;
			}
			/*else if (price > 2*wtp){
				return OBLEAVE;
			}
			else{
				return OBPASS;
			}*/
        }

		protected State observationBuy(){
			State buy = new State();
			ObjectInstance obB = new ObjectInstance(this.domain.getObjectClass(CLASSOBSERVATION), CLASSOBSERVATION);
			obB.setValue(ATTDECISION, OBBUY);
			buy.addObject(obB);
			return buy;
		}

		/*protected State observationPass(){
			State pass = new State();
			ObjectInstance obP = new ObjectInstance(this.domain.getObjectClass(CLASSOBSERVATION), CLASSOBSERVATION);
			obP.setValue(ATTDECISION, OBPASS);
			pass.addObject(obP);
			return pass;
		}*/

		protected State observationLeave(){
			State leave = new State();
			ObjectInstance obL = new ObjectInstance(this.domain.getObjectClass(CLASSOBSERVATION), CLASSOBSERVATION);
			obL.setValue(ATTDECISION, OBLEAVE);
			leave.addObject(obL);
			return leave;
		}


	}


    public static class SellerRF implements RewardFunction{

        public SellerRF(){
        }

        @Override
        public double reward(State s, GroundedAction a, State sprime) {

            ObjectInstance observation = sprime.getFirstObjectOfClass(CLASSOBSERVATION);
			System.out.println(s);
			System.out.println(a);
			System.out.println(sprime);
			String curDecision = observation.getStringValForAttribute(ATTDECISION);
			int curPrice = observation.getDiscValForAttribute(ATTPRICE);

            //Did the player buy it?
			switch (curDecision){
				case OBBUY:
					return curPrice;
				case OBPASS:
					return 0;
				case OBLEAVE:
					return 0;
				default:
					throw new RuntimeException("Decision " + curDecision + " does not exist.");
			}
        }


    }

    /*public static class SellerTF implements TerminalFunction{


        public SellerTF(){
        }

        @Override
        public boolean isTerminal(State s) {

            //get location of agent in next state
            ObjectInstance observation = s.getFirstObjectOfClass(CLASSOBSERVATION);
			String curDecision = observation.getStringValForAttribute(ATTDECISION);

            if(curDecision.equals(VALPASS)){
                return false;
            }

            return true;
        }
    }

    public static class SellerGF implements TerminalFunction{


        public SellerGF(){
        }

        @Override
        public boolean isTerminal(State s) {

            //get location of agent in next state
            ObjectInstance observation = s.getFirstObjectOfClass(CLASSOBSERVATION);
			String curDecision = observation.getStringValForAttribute(ATTDECISION);

            if(curDecision.equals(VALBUY)){
                return true;
            }

            return false;
        }
    }*/



    public static void main(String [] args){

		ConsumerPOMDP dgen = new ConsumerPOMDP();
		PODomain domain = (PODomain)dgen.generateDomain();

		RewardFunction rf = new SellerRF();
		TerminalFunction tf = new NullTermination();
		BeliefSarsa sarsa = new BeliefSarsa(domain, rf, tf, 0.99, 20, 1, true, 10., 0.1, 0.5, 10000);
	    //BeliefSparseSampling sparsesampling = new BeliefSparseSampling(domain, rf, tf, 0.99, 3, -1);
		BeliefState bs = ConsumerPOMDP.getInitialBeliefState(domain);

		System.out.println("Begining sarsa planning.");
		sarsa.planFromBeliefState(bs);
		//sparsesampling.planFromBeliefState(bs);
		System.out.println("End sarsa planning.");

		Policy p = new GreedyQPolicy(sarsa);
		//Policy p = new GreedyQPolicy(sparsesampling);

		POEnvironment env = new POEnvironment(domain, rf, tf);
		env.setCurMPDStateTo(bs.sampleStateFromBelief());

		BeliefMDPPolicyAgent agent = new BeliefMDPPolicyAgent(domain, p);
		agent.setEnvironment(env);
		agent.setBeliefState(bs);
		EpisodeAnalysis ea = agent.actUntilTerminalOrMaxSteps(20);

		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			int tval = ea.getState(i).getFirstObjectOfClass(CLASSCONSUMER).getDiscValForAttribute(ATTWTP);
			System.out.println(tval + ": " + ea.getAction(i).toString());
		}

		QMDP qmdp = new QMDP(domain, rf, tf, 0.99, new DiscreteStateHashFactory(), 0.01, 200);
		System.out.println("Beginning QMDP Planning.");
		qmdp.planFromBeliefState(bs);
		System.out.println("Ending QMDP Planning.");
		Policy qp = new GreedyQPolicy(qmdp);

		BeliefMDPPolicyAgent qagent = new BeliefMDPPolicyAgent(domain, qp);
		qagent.setEnvironment(env);
		qagent.setBeliefState(bs);
		ea = qagent.actUntilTerminalOrMaxSteps(20);

		for(int i = 0; i < ea.numTimeSteps()-1; i++){
			int tval = ea.getState(i).getFirstObjectOfClass(CLASSCONSUMER).getDiscValForAttribute(ATTWTP);
			System.out.println(tval + ": " + ea.getAction(i).toString());
		}

    }

}