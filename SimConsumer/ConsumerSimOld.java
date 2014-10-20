import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Attribute.AttributeType;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.TransitionProbability;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.TerminalExplorer;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StateRenderLayer;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;

public class ConsumerSim implements DomainGenerator {

	//wtp = willingness-to-pay
    public static final String ATTWTP = "wtp";
    public static final String ATTDECISIONHISTORY = "decisionhistory";
    public static final String ATTPRICEHISTORY = "pricehistory";
    public static final String ATTPRICE = "price";

    public static final String CLASSCONSUMER = "consumer";
    public static final String CLASSTRANSACTION = "transaction";

    public static final String ACTIONPASS = "pass";
    public static final String ACTIONBUY = "buy";
    public static final String ACTIONQUIT = "quit";

    public static final String PFAT = "at";

    @Override
    public Domain generateDomain() {

        SADomain domain = new SADomain();

        Attribute wtpatt = new Attribute(domain, ATTWTP, AttributeType.INT);
        wtpatt.setLims(0, 100);
        Attribute decisionhistoryatt = new Attribute(domain, ATTDECISIONHISTORY, AttributeType.INTARRAY);
        Attribute pricehistoryatt = new Attribute(domain, ATTPRICEHISTORY, AttributeType.DOUBLEARRAY);
        Attribute priceatt = new Attribute(domain, ATTPRICE, AttributeType.REAL);
        priceatt.setLims(0, 100);

        ObjectClass consumerClass = new ObjectClass(domain, CLASSCONSUMER);
        consumerClass.addAttribute(wtpatt);

        ObjectClass transactionClass = new ObjectClass(domain, CLASSTRANSACTION);
        transactionClass.addAttribute(priceatt);

        new Decision(ACTIONPASS, domain, 0);
        new Decision(ACTIONBUY, domain, 1);
        new Decision(ACTIONQUIT, domain, 2);

		//TODO: WHAT IS THIS?
        new AtLocation(domain);

        return domain;
    }
	//TODO: FINISH This
    public static State getExampleState(Domain domain){
        State s = new State();
        ObjectInstance agent = new ObjectInstance(domain.getObjectClass(CLASSAGENT), "agent0");
        agent.setValue(ATTY, 0);

        ObjectInstance location = new ObjectInstance(domain.getObjectClass(CLASSLOCATION), "location0");
        location.setValue(ATTX, 10);
        location.setValue(ATTY, 10);

        s.addObject(agent);
        s.addObject(location);

        return s
    }

    protected class Decision extends Action{

        //0: pass; 1: buy; 2:quit;
        protected double [] decisionProbs = new double[3];

        public Movement(String actionName, Domain domain, int decision){
            super(actionName, domain, "");
            for(int i = 0; i < 4; i++){
                if(i == decision){
                    decisionProbs[i] = 1.;
                }
                else{
                    decisionProbs[i] = 0/2.;
                }
            }
        }

        @Override
        protected State performActionHelper(State s, String[] params) {

            //get agent and current position
            ObjectInstance consumer = s.getFirstObjectOfClass(CLASSCONSUMER);
            int curWtp = consumer.getDiscValForAttribute(ATTWTP);
            int curDecisionHistory = consumer.getDiscValForAttribute(ATTDECISIONHISTORTY);
            int curPriceHistory = consumer.getDiscValForAttribute(ATTPRICEHISTORTY);

            //sample directon with random roll
            double r = Math.random();
            double sumProb = 0.;
            int dec = 0;
            for(int i = 0; i < this.decisionProbs.length; i++){
                sumProb += this.decisionProbs[i];
                if(r < sumProb){
                    dec = i;
                    break; //found direction
                }
            }

            //get resulting history
            int [] newPos = this.moveResult(curX, curY, dir);

            //set the new position
            agent.setValue(ATTX, newPos[0]);
            agent.setValue(ATTY, newPos[1]);

            //return the state we just modified
            return s;
        }


        @Override
        public List<TransitionProbability> getTransitions(State s, String [] params){

            //get agent and current position
            ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
            int curX = agent.getDiscValForAttribute(ATTX);
            int curY = agent.getDiscValForAttribute(ATTY);

            List<TransitionProbability> tps = new ArrayList<TransitionProbability>(4);
            TransitionProbability noChangeTransition = null;
            for(int i = 0; i < this.directionProbs.length; i++){
                int [] newPos = this.moveResult(curX, curY, i);
                if(newPos[0] != curX || newPos[1] != curY){
                    //new possible outcome
                    State ns = s.copy();
                    ObjectInstance nagent = ns.getFirstObjectOfClass(CLASSAGENT);
                    nagent.setValue(ATTX, newPos[0]);
                    nagent.setValue(ATTY, newPos[1]);

                    //create transition probability object and add to our list of outcomes
                    tps.add(new TransitionProbability(ns, this.directionProbs[i]));
                }
                else{
                    //this direction didn't lead anywhere new
                    //if there are existing possible directions
                    //that wouldn't lead anywhere, aggregate with them
                    if(noChangeTransition != null){
                        noChangeTransition.p += this.directionProbs[i];
                    }
                    else{
                        //otherwise create this new state and transition
                        noChangeTransition = new TransitionProbability(s.copy(),
                                     this.directionProbs[i]);
                        tps.add(noChangeTransition);
                    }
                }
            }


            return tps;
        }


        protected int [] moveResult(int curX, int curY, int direction){

            //first get change in x and y from direction using 0: north; 1: south; 2:east; 3: west
            int xdelta = 0;
            int ydelta = 0;
            if(direction == 0){
                ydelta = 1;
            }
            else if(direction == 1){
                ydelta = -1;
            }
            else if(direction == 2){
                xdelta = 1;
            }
            else{
                xdelta = -1;
            }

            int nx = curX + xdelta;
            int ny = curY + ydelta;

            int width = ExampleGridWorld.this.map.length;
            int height = ExampleGridWorld.this.map[0].length;

            //make sure new position is valid (not a wall or off bounds)
            if(nx < 0 || nx >= width || ny < 0 || ny >= height ||
                ExampleGridWorld.this.map[nx][ny] == 1){
                nx = curX;
                ny = curY;
            }


            return new int[]{nx,ny};

        }


    }


    protected class AtLocation extends PropositionalFunction{

        public AtLocation(Domain domain){
            super(PFAT, domain, new String []{CLASSAGENT,CLASSLOCATION});
        }

        @Override
        public boolean isTrue(State s, String[] params) {
            ObjectInstance agent = s.getObject(params[0]);
            ObjectInstance location = s.getObject(params[1]);

            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            int lx = location.getDiscValForAttribute(ATTX);
            int ly = location.getDiscValForAttribute(ATTY);

            return ax == lx && ay == ly;
        }



    }

    public static class ExampleRF implements RewardFunction{

        int goalX;
        int goalY;

        public ExampleRF(int goalX, int goalY){
            this.goalX = goalX;
            this.goalY = goalY;
        }

        @Override
        public double reward(State s, GroundedAction a, State sprime) {

            //get location of agent in next state
            ObjectInstance agent = sprime.getFirstObjectOfClass(CLASSAGENT);
            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            //are they at goal location?
            if(ax == this.goalX && ay == this.goalY){
                return 100.;
            }

            return -1;
        }


    }

    public static class ExampleTF implements TerminalFunction{

        int goalX;
        int goalY;

        public ExampleTF(int goalX, int goalY){
            this.goalX = goalX;
            this.goalY = goalY;
        }

        @Override
        public boolean isTerminal(State s) {

            //get location of agent in next state
            ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
            int ax = agent.getDiscValForAttribute(ATTX);
            int ay = agent.getDiscValForAttribute(ATTY);

            //are they at goal location?
            if(ax == this.goalX && ay == this.goalY){
                return true;
            }

            return false;
        }



    }



    public static void main(String [] args){

        ExampleGridWorld gen = new ExampleGridWorld();
        Domain domain = gen.generateDomain();

        State initialState = ExampleGridWorld.getExampleState(domain);

        //TerminalExplorer exp = new TerminalExplorer(domain);
        //exp.exploreFromState(initialState);


        Visualizer v = gen.getVisualizer();
        VisualExplorer exp = new VisualExplorer(domain, v, initialState);

        exp.addKeyAction("w", ACTIONNORTH);
        exp.addKeyAction("s", ACTIONSOUTH);
        exp.addKeyAction("d", ACTIONEAST);
        exp.addKeyAction("a", ACTIONWEST);

        exp.initGUI();


    }

}