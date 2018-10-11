package ar.uba.fi.celdas;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {
    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;
    /**
     * List of available actions for the agent
     */
    protected ArrayList<Types.ACTIONS> actions;


    protected Theories theories;
    
    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        randomGenerator = new Random();
        actions = so.getAvailableActions();
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
    	
    	
    	//TODO: Replace here the content and create an autonomous agent
    	Perception perception = new Perception(stateObs);
        System.out.println(perception.toString());
//        System.out.println(stateObs.getAvatarPosition());
        int index = randomGenerator.nextInt(actions.size());
        return actions.get(index);
    }

    private ArrayList<Types.ACTIONS> getViableActions(StateObservation stateObs) {
        ArrayList<Vector2d> adjPos = this.getAdjacentPositions(stateObs);
        ArrayList<Types.ACTIONS> selectedActions = new ArrayList<>();

        return selectedActions;
    }

    private ArrayList<Vector2d> getAdjacentPositions(StateObservation stateObs) {
        Vector2d avatarPos = stateObs.getAvatarPosition();
        double unitMove = 50.0;
        ArrayList<Vector2d> adjacents = new ArrayList<>();
        adjacents.add(avatarPos.copy().add(unitMove, 0.0)); // Right
        adjacents.add(avatarPos.copy().subtract(unitMove, 0.0)); // Left
        adjacents.add(avatarPos.copy().add(0.0, unitMove)); // Down
        adjacents.add(avatarPos.copy().subtract(0.0, unitMove)); // Up
        return adjacents;
    }

}
