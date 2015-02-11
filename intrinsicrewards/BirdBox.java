package intrinsicrewards;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
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

public class BirdBox implements DomainGenerator {

    public static final String ATTX = "x";
    public static final String ATTY = "y";
    public static final String ATTHUNGRY = "hungry";
    public static final String ATTOPEN = "open";
    public static final String ATTFILLED = "filled";

    public static final String CLASSAGENT = "agent";
    public static final String CLASSBOX = "box";

    public static final String ACTIONNORTH = "north";
    public static final String ACTIONSOUTH = "south";
    public static final String ACTIONEAST = "east";
    public static final String ACTIONWEST = "west";
    public static final String ACTIONOPEN = "open";
    public static final String ACTIONEAT = "eat";

    public static final String PFAT = "at";

	public static double LEARNINGRATE = 1;
	public static double DISCOUNTFACTOR = 0.99;
	public static double EPSILONGREEDY = 0;
	public static double CLOSEPROB = 0.1;


    //ordered so first dimension is x
    protected int [][] map = new int[][]{
            {0,0,0,0,0,0,0},
            {0,0,0,1,0,0,0},
            {0,0,0,1,0,0,0},
            {1,1,1,1,1,1,0},
            {0,0,0,1,0,0,0},
            {0,0,0,0,0,0,0},
            {0,0,0,1,0,0,0},
    };

	public BirdBox(){
		super();
	}

	public int[][] getMap(){
		int[][] copy = new int[map.length][map[0].length];
		for(int i = 0; i < map.length; i++){
			for(int j = 0; j < map[i].length; j++){
				copy[i][j] = map[i][j];
			}
		}
		return copy;
	}

    @Override
    public Domain generateDomain() {

        SADomain domain = new SADomain();

        Attribute xatt = new Attribute(domain, ATTX, AttributeType.INT);
        xatt.setLims(0, 10);

        Attribute yatt = new Attribute(domain, ATTY, AttributeType.INT);
        yatt.setLims(0, 10);

		Attribute hungryatt = new Attribute(domain, ATTHUNGRY, AttributeType.BOOLEAN);
		Attribute openatt = new Attribute(domain, ATTOPEN, AttributeType.DISC);
		String[] openArr = {"open", "half-open", "closed"};
		openatt.setDiscValues(openArr);
		Attribute filledatt = new Attribute(domain, ATTFILLED, AttributeType.BOOLEAN);

        ObjectClass agentClass = new ObjectClass(domain, CLASSAGENT);
        agentClass.addAttribute(xatt);
        agentClass.addAttribute(yatt);
        agentClass.addAttribute(hungryatt);

        ObjectClass boxClass = new ObjectClass(domain, CLASSBOX);
        boxClass.addAttribute(xatt);
        boxClass.addAttribute(yatt);
        boxClass.addAttribute(openatt);
        boxClass.addAttribute(filledatt);

        new Movement(ACTIONNORTH, domain, 0);
        new Movement(ACTIONSOUTH, domain, 1);
        new Movement(ACTIONEAST, domain, 2);
        new Movement(ACTIONWEST, domain, 3);
        new Eat(ACTIONEAT, domain);
        new Open(ACTIONOPEN, domain);

        new AtLocation(domain);

        return domain;
    }

	public static void main(String [] args){

        BirdBox gen = new BirdBox();
        Domain domain = gen.generateDomain();

        State initialState = BirdBox.getExampleState(domain);

        //TerminalExplorer exp = new TerminalExplorer(domain);
        //exp.exploreFromState(initialState);


        Visualizer v = gen.getVisualizer();
        VisualExplorer exp = new VisualExplorer(domain, v, initialState);

        exp.addKeyAction("w", ACTIONNORTH);
        exp.addKeyAction("s", ACTIONSOUTH);
        exp.addKeyAction("d", ACTIONEAST);
        exp.addKeyAction("a", ACTIONWEST);
        exp.addKeyAction("e", ACTIONEAT);
        exp.addKeyAction("o", ACTIONOPEN);

        exp.initGUI();


    }


    public static State getExampleState(Domain domain){
        State s = new State();
        ObjectInstance agent = new ObjectInstance(domain.getObjectClass(CLASSAGENT), "agent0");
        agent.setValue(ATTX, 0);
        agent.setValue(ATTY, 6);
        agent.setValue(ATTHUNGRY, true);

        ObjectInstance box0 = new ObjectInstance(domain.getObjectClass(CLASSBOX), "box0");
        box0.setValue(ATTX, 6);
        box0.setValue(ATTY, 6);
        box0.setValue(ATTOPEN, "closed");
        box0.setValue(ATTFILLED, true);

		ObjectInstance box1 = new ObjectInstance(domain.getObjectClass(CLASSBOX), "box1");
        box1.setValue(ATTX, 0);
        box1.setValue(ATTY, 0);
        box1.setValue(ATTOPEN, "closed");
        box1.setValue(ATTFILLED, true);

        s.addObject(agent);
        s.addObject(box0);
        s.addObject(box1);

        return s;
    }

    public StateRenderLayer getStateRenderLayer(){
        StateRenderLayer rl = new StateRenderLayer();
        rl.addStaticPainter(new WallPainter());
        rl.addObjectClassPainter(CLASSBOX, new BoxPainter());
        rl.addObjectClassPainter(CLASSAGENT, new AgentPainter());

        return rl;
    }

    public Visualizer getVisualizer(){
        return new Visualizer(this.getStateRenderLayer());
    }

	protected class Open extends Action{
        public Open(String actionName, Domain domain){
			super(actionName, domain, "");
		}

        @Override
        protected State performActionHelper(State s, String[] params) {
            ObjectInstance box0 = s.getObject("box0");
            int box0X = box0.getDiscValForAttribute(ATTX);
            int box0Y = box0.getDiscValForAttribute(ATTY);

			ObjectInstance box1 = s.getObject("box1");
            int box1X = box1.getDiscValForAttribute(ATTX);
            int box1Y = box1.getDiscValForAttribute(ATTY);

            ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
            int curX = agent.getDiscValForAttribute(ATTX);
            int curY = agent.getDiscValForAttribute(ATTY);
			agent.setValue(ATTHUNGRY, true);

			String box1Open = box1.getStringValForAttribute(ATTOPEN);
			if (box1Open.equals("half-open")){
				box1.setValue(ATTOPEN, "open");
				box1.setValue(ATTFILLED, false);
			}

			String box0Open = box0.getStringValForAttribute(ATTOPEN);
			if (box0Open.equals("half-open")){
				box0.setValue(ATTOPEN, "open");
				box0.setValue(ATTFILLED, false);
			}


			ObjectInstance currBox = null;
			if(curX == box0X && curY == box0Y){
				currBox = box0;
			}
			else if (curX == box1X && curY == box1Y){
				currBox = box1;
			}
			if (currBox != null){
				String currBoxOpen = currBox.getStringValForAttribute(ATTOPEN);
				boolean currBoxFilled = currBox.getBooleanValue(ATTFILLED);
				if (currBoxOpen.equals("closed")){
					currBox.setValue(ATTOPEN, "half-open");
				}
			}

			if (box0Open.equals("open")){
				double r = Math.random();
				if (r < CLOSEPROB){
					box0.setValue(ATTOPEN, "closed");
					box0.setValue(ATTFILLED, true);
				}
			}
			if (box1Open.equals("open")){
				double r = Math.random();
				if (r < CLOSEPROB){
					box1.setValue(ATTOPEN, "closed");
					box1.setValue(ATTFILLED, true);
				}
			}

			return s;

		}

		@Override
        public List<TransitionProbability> getTransitions(State s, String [] params){

			State ns = s.copy();
            ObjectInstance nagent = ns.getFirstObjectOfClass(CLASSAGENT);
			ObjectInstance nbox0 = ns.getObject("box0");
			ObjectInstance nbox1 = ns.getObject("box1");

			List<TransitionProbability> tpsWithBoxes = new ArrayList<TransitionProbability>(16);
			nagent.setValue(ATTHUNGRY, true);

			String box0Open = nbox0.getStringValForAttribute(ATTOPEN);
			String box1Open = nbox1.getStringValForAttribute(ATTOPEN);
			if (box0Open.equals("half-open")){
				nbox0.setValue(ATTOPEN, "open");
				nbox0.setValue(ATTFILLED, false);
			}
			if (box1Open.equals("half-open")){
				nbox1.setValue(ATTOPEN, "open");
				nbox1.setValue(ATTFILLED, false);
			}

			State nnsOC = ns.copy();
			State nnsOO = ns.copy();

			if (box0Open.equals("open")){
				ObjectInstance nnbox0 = nnsOC.getObject("box0");
				nnbox0.setValue(ATTOPEN, "closed");
				nnbox0.setValue(ATTFILLED, true);
			}

			State nnsOCOC = nnsOC.copy();
			State nnsOCOO = nnsOC.copy();
			State nnsOOOC = nnsOO.copy();
			State nnsOOOO = nnsOO.copy();

			if (box1Open.equals("open")){
				ObjectInstance nnbox1OC = nnsOCOC.getObject("box1");
				nnbox1OC.setValue(ATTOPEN, "closed");
				nnbox1OC.setValue(ATTFILLED, true);

				ObjectInstance nnbox1OO = nnsOOOC.getObject("box1");
				nnbox1OO.setValue(ATTOPEN, "closed");
				nnbox1OO.setValue(ATTFILLED, true);
			}

			tpsWithBoxes.add(new TransitionProbability(nnsOCOC, .1*.1));
			tpsWithBoxes.add(new TransitionProbability(nnsOCOO, .1*.9));
			tpsWithBoxes.add(new TransitionProbability(nnsOOOC, .9*.1));
			tpsWithBoxes.add(new TransitionProbability(nnsOOOO, .9*.9));

			for(TransitionProbability tp: tpsWithBoxes){
				State currS = tp.s;
				ObjectInstance lagent = currS.getFirstObjectOfClass(CLASSAGENT);
				int curX = lagent.getDiscValForAttribute(ATTX);
				int curY = lagent.getDiscValForAttribute(ATTY);

				ObjectInstance lbox0 = currS.getObject("box0");
				int box0X = lbox0.getDiscValForAttribute(ATTX);
				int box0Y = lbox0.getDiscValForAttribute(ATTY);

				ObjectInstance lbox1 = currS.getObject("box1");
				int box1X = lbox1.getDiscValForAttribute(ATTX);
				int box1Y = lbox1.getDiscValForAttribute(ATTY);

				ObjectInstance currBox = null;
				if(curX == box0X && curY == box0Y){
					currBox = lbox0;
				}
				else if (curX == box1X && curY == box1Y){
					currBox = lbox1;
				}
				if (currBox != null){
					String currBoxOpen = currBox.getStringValForAttribute(ATTOPEN);
					boolean currBoxFilled = currBox.getBooleanValue(ATTFILLED);
					if (currBoxOpen.equals("closed")){
						currBox.setValue(ATTOPEN, "half-open");
					}
				}
			}


            return tpsWithBoxes;
		}
	}

	protected class Eat extends Action{
        public Eat(String actionName, Domain domain){
			super(actionName, domain, "");
		}

        @Override
        protected State performActionHelper(State s, String[] params) {
            ObjectInstance box0 = s.getObject("box0");
            int box0X = box0.getDiscValForAttribute(ATTX);
            int box0Y = box0.getDiscValForAttribute(ATTY);

			ObjectInstance box1 = s.getObject("box1");
            int box1X = box1.getDiscValForAttribute(ATTX);
            int box1Y = box1.getDiscValForAttribute(ATTY);

            ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
            int curX = agent.getDiscValForAttribute(ATTX);
            int curY = agent.getDiscValForAttribute(ATTY);
			agent.setValue(ATTHUNGRY, true);

			ObjectInstance currBox = null;
			if(curX == box0X && curY == box0Y){
				currBox = box0;
			}
			else if (curX == box1X && curY == box1Y){
				currBox = box1;
			}
			if (currBox != null){
				String currBoxOpen = currBox.getStringValForAttribute(ATTOPEN);
				boolean currBoxFilled = currBox.getBooleanValue(ATTFILLED);
				if (currBoxOpen.equals("half-open") && currBoxFilled ){
					agent.setValue(ATTHUNGRY, false);
					currBox.setValue(ATTFILLED, false);
				}
			}

			String box1Open = box1.getStringValForAttribute(ATTOPEN);
			if (box1Open.equals("open")){
				double r = Math.random();
				if (r < CLOSEPROB){
					box1.setValue(ATTOPEN, "closed");
					box1.setValue(ATTFILLED, true);
				}
			}
			else if (box1Open.equals("half-open")){
				box1.setValue(ATTOPEN, "open");
				box1.setValue(ATTFILLED, false);
			}

			String box0Open = box0.getStringValForAttribute(ATTOPEN);
			if (box0Open.equals("open")){
				double r = Math.random();
				if (r < CLOSEPROB){
					box0.setValue(ATTOPEN, "closed");
					box0.setValue(ATTFILLED, true);
				}
			}
			else if (box0Open.equals("half-open")){
				box0.setValue(ATTOPEN, "open");
				box0.setValue(ATTFILLED, false);
			}


			return s;
		}

        @Override
        public List<TransitionProbability> getTransitions(State s, String [] params){

			State ns = s.copy();
            ObjectInstance nagent = ns.getFirstObjectOfClass(CLASSAGENT);
            int curX = nagent.getDiscValForAttribute(ATTX);
            int curY = nagent.getDiscValForAttribute(ATTY);

			ObjectInstance nbox0 = ns.getObject("box0");
            int box0X = nbox0.getDiscValForAttribute(ATTX);
            int box0Y = nbox0.getDiscValForAttribute(ATTY);

			ObjectInstance nbox1 = ns.getObject("box1");
            int box1X = nbox1.getDiscValForAttribute(ATTX);
            int box1Y = nbox1.getDiscValForAttribute(ATTY);
			nagent.setValue(ATTHUNGRY, true);

			ObjectInstance currBox = null;
			if(curX == box0X && curY == box0Y){
				currBox = nbox0;
			}
			else if (curX == box1X && curY == box1Y){
				currBox = nbox1;
			}
			if (currBox != null){
				String currBoxOpen = currBox.getStringValForAttribute(ATTOPEN);
				boolean currBoxFilled = currBox.getBooleanValue(ATTFILLED);
				if (currBoxOpen.equals("half-open") && currBoxFilled ){
					nagent.setValue(ATTHUNGRY, false);
					currBox.setValue(ATTFILLED, false);
				}
			}

			List<TransitionProbability> tpsWithBoxes = new ArrayList<TransitionProbability>(16);

			String box0Open = nbox0.getStringValForAttribute(ATTOPEN);
			String box1Open = nbox1.getStringValForAttribute(ATTOPEN);
			if (box0Open.equals("half-open")){
				nbox0.setValue(ATTOPEN, "open");
				nbox0.setValue(ATTFILLED, false);
			}
			if (box1Open.equals("half-open")){
				nbox1.setValue(ATTOPEN, "open");
				nbox1.setValue(ATTFILLED, false);
			}

			State nnsOC = ns.copy();
			State nnsOO = ns.copy();

			if (box0Open.equals("open")){
				ObjectInstance nnbox0 = nnsOC.getObject("box0");
				nnbox0.setValue(ATTOPEN, "closed");
				nnbox0.setValue(ATTFILLED, true);
			}

			State nnsOCOC = nnsOC.copy();
			State nnsOCOO = nnsOC.copy();
			State nnsOOOC = nnsOO.copy();
			State nnsOOOO = nnsOO.copy();

			if (box1Open.equals("open")){
				ObjectInstance nnbox1OC = nnsOCOC.getObject("box1");
				nnbox1OC.setValue(ATTOPEN, "closed");
				nnbox1OC.setValue(ATTFILLED, true);

				ObjectInstance nnbox1OO = nnsOOOC.getObject("box1");
				nnbox1OO.setValue(ATTOPEN, "closed");
				nnbox1OO.setValue(ATTFILLED, true);
			}

			tpsWithBoxes.add(new TransitionProbability(nnsOCOC, .1*.1));
			tpsWithBoxes.add(new TransitionProbability(nnsOCOO, .1*.9));
			tpsWithBoxes.add(new TransitionProbability(nnsOOOC, .9*.1));
			tpsWithBoxes.add(new TransitionProbability(nnsOOOO, .9*.9));

            return tpsWithBoxes;

		}

	}

    protected class Movement extends Action{

        //0: north; 1: south; 2:east; 3: west
        protected double [] directionProbs = new double[4];


        public Movement(String actionName, Domain domain, int direction){
            super(actionName, domain, "");
            for(int i = 0; i < 4; i++){
                if(i == direction){
                    directionProbs[i] = 1 - EPSILONGREEDY;
                }
                else{
                    directionProbs[i] = EPSILONGREEDY / 3.;
                }
            }
        }

        @Override
        protected State performActionHelper(State s, String[] params) {

            //get agent and current position
            ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
            int curX = agent.getDiscValForAttribute(ATTX);
            int curY = agent.getDiscValForAttribute(ATTY);
			agent.setValue(ATTHUNGRY, true);

            //sample directon with random roll
            double r = Math.random();
            double sumProb = 0.;
            int dir = 0;
            for(int i = 0; i < this.directionProbs.length; i++){
                sumProb += this.directionProbs[i];
                if(r < sumProb){
                    dir = i;
                    break; //found direction
                }
            }

            //get resulting position
            int [] newPos = this.moveResult(curX, curY, dir);

            //set the new position
            agent.setValue(ATTX, newPos[0]);
            agent.setValue(ATTY, newPos[1]);

			ObjectInstance box0 = s.getObject("box0");
			ObjectInstance box1 = s.getObject("box1");

			String box0Open = box0.getStringValForAttribute(ATTOPEN);
			if (box0Open.equals("open")){
				r = Math.random();
				if (r < CLOSEPROB){
					box0.setValue(ATTOPEN, "closed");
					box0.setValue(ATTFILLED, true);
				}
			}
			else if (box0Open.equals("half-open")){
				box0.setValue(ATTOPEN, "open");
				box0.setValue(ATTFILLED, false);
			}

			String box1Open = box1.getStringValForAttribute(ATTOPEN);
			if (box1Open.equals("open")){
				r = Math.random();
				if (r < CLOSEPROB){
					box1.setValue(ATTOPEN, "closed");
					box1.setValue(ATTFILLED, true);
				}
			}
			else if (box1Open.equals("half-open")){
				box1.setValue(ATTOPEN, "open");
				box1.setValue(ATTFILLED, false);
			}

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

            List<TransitionProbability> tpsWithBoxes = new ArrayList<TransitionProbability>(16);
			for (TransitionProbability tp: tps){
				State ns = tp.s.copy();
				ObjectInstance nagent = ns.getFirstObjectOfClass(CLASSAGENT);
				nagent.setValue(ATTHUNGRY, true);
				ObjectInstance nbox0 = ns.getObject("box0");
				ObjectInstance nbox1 = ns.getObject("box1");

				String box0Open = nbox0.getStringValForAttribute(ATTOPEN);
				String box1Open = nbox1.getStringValForAttribute(ATTOPEN);
				if (box0Open.equals("half-open")){
					nbox0.setValue(ATTOPEN, "open");
					nbox0.setValue(ATTFILLED, false);
				}
				if (box1Open.equals("half-open")){
					nbox1.setValue(ATTOPEN, "open");
					nbox1.setValue(ATTFILLED, false);
				}

				State nnsOC = ns.copy();
				State nnsOO = ns.copy();

				if (box0Open.equals("open")){
					ObjectInstance nnbox0 = nnsOC.getObject("box0");
					nnbox0.setValue(ATTOPEN, "closed");
					nnbox0.setValue(ATTFILLED, true);
				}

				State nnsOCOC = nnsOC.copy();
				State nnsOCOO = nnsOC.copy();
				State nnsOOOC = nnsOO.copy();
				State nnsOOOO = nnsOO.copy();

				if (box1Open.equals("open")){
					ObjectInstance nnbox1OC = nnsOCOC.getObject("box1");
					nnbox1OC.setValue(ATTOPEN, "closed");
					nnbox1OC.setValue(ATTFILLED, true);

					ObjectInstance nnbox1OO = nnsOOOC.getObject("box1");
					nnbox1OO.setValue(ATTOPEN, "closed");
					nnbox1OO.setValue(ATTFILLED, true);
				}

				tpsWithBoxes.add(new TransitionProbability(nnsOCOC, tp.p*.1*.1));
				tpsWithBoxes.add(new TransitionProbability(nnsOCOO, tp.p*.1*.9));
				tpsWithBoxes.add(new TransitionProbability(nnsOOOC, tp.p*.9*.1));
				tpsWithBoxes.add(new TransitionProbability(nnsOOOO, tp.p*.9*.9));
			}

            return tpsWithBoxes;
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

            int width = BirdBox.this.map.length;
            int height = BirdBox.this.map[0].length;

            //make sure new position is valid (not a wall or off bounds)
            if(nx < 0 || nx >= width || ny < 0 || ny >= height ||
                BirdBox.this.map[nx][ny] == 1){
                nx = curX;
                ny = curY;
            }


            return new int[]{nx,ny};

        }


    }


    protected class AtLocation extends PropositionalFunction{

        public AtLocation(Domain domain){
            super(PFAT, domain, new String []{CLASSAGENT,CLASSBOX});
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



    protected class WallPainter implements StaticPainter{

        @Override
        public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {

            //walls will be filled in black
            g2.setColor(Color.BLACK);

            //set up floats for the width and height of our domain
            float fWidth = BirdBox.this.map.length;
            float fHeight = BirdBox.this.map[0].length;

            //determine the width of a single cell
            //on our canvas such that the whole map can be painted
            float width = cWidth / fWidth;
            float height = cHeight / fHeight;

            //pass through each cell of our map and if it's a wall, paint a black rectangle on our
            //cavas of dimension widthxheight
            for(int i = 0; i < BirdBox.this.map.length; i++){
                for(int j = 0; j < BirdBox.this.map[0].length; j++){

                    //is there a wall here?
                    if(BirdBox.this.map[i][j] == 1){

                        //left coordinate of cell on our canvas
                        float rx = i*width;

                        //top coordinate of cell on our canvas
                        //coordinate system adjustment because the java canvas
                        //origin is in the top left instead of the bottom right
                        float ry = cHeight - height - j*height;

                        //paint the rectangle
                        g2.fill(new Rectangle2D.Float(rx, ry, width, height));

                    }


                }
            }

        }


    }


    protected class AgentPainter implements ObjectPainter{

        @Override
        public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
                float cWidth, float cHeight) {

            //agent will be filled in gray
            g2.setColor(Color.GRAY);

            //set up floats for the width and height of our domain
            float fWidth = BirdBox.this.map.length;
            float fHeight = BirdBox.this.map[0].length;

            //determine the width of a single cell on our canvas
            //such that the whole map can be painted
            float width = cWidth / fWidth;
            float height = cHeight / fHeight;

            int ax = ob.getDiscValForAttribute(ATTX);
            int ay = ob.getDiscValForAttribute(ATTY);

            //left coordinate of cell on our canvas
            float rx = ax*width;

            //top coordinate of cell on our canvas
            //coordinate system adjustment because the java canvas
            //origin is in the top left instead of the bottom right
            float ry = cHeight - height - ay*height;

            //paint the rectangle
            g2.fill(new Ellipse2D.Float(rx, ry, width, height));


        }



    }

    protected class BoxPainter implements ObjectPainter{

        @Override
        public void paintObject(Graphics2D g2, State s, ObjectInstance ob,
                float cWidth, float cHeight) {

			boolean filled = ob.getBooleanValue(ATTFILLED);
			String open = ob.getStringValForAttribute(ATTOPEN);
			Color color;
			int thickness;
			if (filled) {
				color = Color.RED;
			} else {
				color = Color.LIGHT_GRAY;
			}
			if (open.equals("open")){
				thickness = 0;
			}
			else if (open.equals("half-open")){
				thickness = 10;
			}
			else {
				thickness = 40;
			}


            g2.setColor(color);
			g2.setStroke(new BasicStroke(thickness));


            //set up floats for the width and height of our domain
            float fWidth = BirdBox.this.map.length;
            float fHeight = BirdBox.this.map[0].length;

            //determine the width of a single cell on our canvas
            //such that the whole map can be painted
            float width = cWidth / fWidth;
            float height = cHeight / fHeight;

            int ax = ob.getDiscValForAttribute(ATTX);
            int ay = ob.getDiscValForAttribute(ATTY);

            //left coordinate of cell on our canvas
            float rx = ax*width;

            //top coordinate of cell on our canvas
            //coordinate system adjustment because the java canvas
            //origin is in the top left instead of the bottom right
            float ry = cHeight - height - ay*height;

            //paint the rectangle
            g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			g2.setColor(Color.BLACK);
            g2.draw(new Rectangle2D.Float(rx+(float).5*thickness, ry+(float).5*thickness, width-thickness, height-thickness));
        }
    }
}