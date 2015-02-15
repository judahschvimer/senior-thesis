package intrinsicrewards;

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
import burlap.oomdp.auxiliary.StateGenerator;

import java.util.Random;

public class BirdStateGenerator implements StateGenerator{

	public State generateState(){

        BirdBox gen = new BirdBox();
        Domain domain = gen.generateDomain();

		State s = new State();
        ObjectInstance agent = new ObjectInstance(domain.getObjectClass(BirdBox.CLASSAGENT), "agent0");
        agent.setValue(BirdBox.ATTHUNGRY, true);

        ObjectInstance box0 = new ObjectInstance(domain.getObjectClass(BirdBox.CLASSBOX), "box0");
        box0.setValue(BirdBox.ATTOPEN, "closed");

		ObjectInstance box1 = new ObjectInstance(domain.getObjectClass(BirdBox.CLASSBOX), "box1");
        box1.setValue(BirdBox.ATTOPEN, "closed");

		Random r = new Random(System.currentTimeMillis());

        int i = r.nextInt(4);
		int[][] map = gen.getMap();
		int width = map.length;
		int height = map[0].length;

        switch (i) {
            case 0:
				box0.setValue(BirdBox.ATTX, 0);
				box0.setValue(BirdBox.ATTY, 0);
				break;
            case 1:
				box0.setValue(BirdBox.ATTX, width-1);
				box0.setValue(BirdBox.ATTY, 0);
				break;
            case 2:
				box0.setValue(BirdBox.ATTX, 0);
				box0.setValue(BirdBox.ATTY, height-1);
				break;
            case 3:
				box0.setValue(BirdBox.ATTX, width-1);
				box0.setValue(BirdBox.ATTY, height-1);
				break;
            default:
                break;
		}

		int j = r.nextInt(4);
		while (j == i) {
			j = r.nextInt(4);

		}
        switch (j) {

			case 0:

				box1.setValue(BirdBox.ATTX, 0);
				box1.setValue(BirdBox.ATTY, 0);
				break;
            case 1:
				box1.setValue(BirdBox.ATTX, width-1);
				box1.setValue(BirdBox.ATTY, 0);
				break;
            case 2:
				box1.setValue(BirdBox.ATTX, 0);
				box1.setValue(BirdBox.ATTY, height-1);
				break;
            case 3:
				box1.setValue(BirdBox.ATTX, width-1);
				box1.setValue(BirdBox.ATTY, height-1);
				break;
            default:
                break;
        }

		int x = r.nextInt(width);
		int y = r.nextInt(height);
		while (map[x][y] == 1) {
			x = r.nextInt(width);
			y = r.nextInt(height);
		}
        agent.setValue(BirdBox.ATTX, x);
        agent.setValue(BirdBox.ATTY, y);

        s.addObject(agent);
        s.addObject(box0);
        s.addObject(box1);

        return s;

	}

}
