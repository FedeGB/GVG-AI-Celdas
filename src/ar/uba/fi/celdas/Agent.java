package ar.uba.fi.celdas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import javax.lang.model.element.TypeElement;

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
    protected Planifier planifier;
    private Theories theories;
    private Types.ACTIONS lastAction;
    private StateObservation lastState;
    private Vector2d lastPosition;
    private Comparator<Theory> comparatorUtility;
    private Comparator<Theory> comparatorSuccess;
    private Theorizer theorizer;
    private Vector2d finishPos;
    /**
     * List of available actions for the agent
     */
    protected ArrayList<Types.ACTIONS> actions;
    
    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        randomGenerator = new Random();
        actions = so.getAvailableActions();
        lastState = null;
        lastAction = null;
        lastPosition = null;
        try {
            theories = TheoryPersistant.load();
        } catch (FileNotFoundException e) {
            theories = new Theories();
        }
        planifier = new Planifier(so);
        theorizer = new Theorizer();
        ArrayList<Observation>[] portals = so.getPortalsPositions();
        Vector2d position = portals[0].get(0).position;
        position.mul(1/(float)so.getBlockSize());
        finishPos = position;
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        StateObservation currentState = stateObs.copy();
        Types.ACTIONS actionToTake = theorizer.getRandomActionFromList(actions);
        Vector2d currentPosition = theorizer.getAvatarPositionFixed(currentState);
        if(lastState == null && lastAction == null) {
            lastState = currentState;
            lastAction = actionToTake;
            lastPosition = currentPosition;
            return actionToTake;
        }
        Perception lastPerception = new Perception(lastState);
        Perception currentPerception = new Perception(currentState);
        Theory localTheory = new Theory();
        localTheory.setCurrentState(lastPerception.getLevel());
        localTheory.setAction(lastAction);
        localTheory.setPredictedState(currentPerception.getLevel());
        boolean buildNewLocalTheory = false;
        boolean selectNewTheory = false;
        List<Theory> listOfTheories = theories.getSortedListForCurrentState(localTheory);
        List<Types.ACTIONS> actionsDone = new ArrayList<>();
        if(!listOfTheories.isEmpty()) {
            for(Theory theo : listOfTheories) {
                actionsDone.add(theo.getAction());
            }
            if(actionsDone.size() == actions.size()) { // All actions done
                selectNewTheory = true;
                // TODO: Select theory and update counter
            } else { // Not all done, maybe explore
                if(!theorizer.shouldExplore()) {
                    selectNewTheory = true;
//                    buildNewLocalTheory = true;
                }
            }
        } else {
            buildNewLocalTheory = true;
        }

        if(buildNewLocalTheory) {
            localTheory.setUsedCount(1);
            if(currentPosition.equals(lastPosition)) {
                localTheory.setSuccessCount(0);
                localTheory.setUtility(0);
            } else {
                localTheory.setSuccessCount(1);
                localTheory.setUtility(getUtilityBasedOnRef(currentState));
            }
            try {
                theories.add(localTheory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(selectNewTheory) {
            // TODO: Select theory and update counter
        } else {
            ArrayList<Types.ACTIONS> leftOver = new ArrayList<>(actions);
            leftOver.removeAll(actionsDone);
            actionToTake = theorizer.getRandomActionFromList(leftOver);
        }

        lastState = currentState;
        lastAction = actionToTake;
        lastPosition = theorizer.getAvatarPositionFixed(currentState);
        return actionToTake;

    }

    public float getUtilityBasedOnRef(StateObservation predicted) {
        Vector2d refPos = theorizer.getAvatarPositionFixed(predicted);
        double distance = finishPos.dist(refPos);
        return 1000 / (float)(1 + distance);
    }

    public void result(StateObservation stateObs, ElapsedCpuTimer elapsedCpuTimer)
    {
        boolean GameOver = stateObs.isGameOver();
        boolean IsAlive = stateObs.isAvatarAlive();
        if(GameOver) {
            Perception lastPerception = new Perception(lastState);
            Perception currentPerception = new Perception(stateObs);
            Theory lastTheory = new Theory();
            lastTheory.setCurrentState(lastPerception.getLevel());
            lastTheory.setAction(lastAction);
            lastTheory.setPredictedState(currentPerception.getLevel());
            theories = theorizer.updateResultsTheories(theories, lastTheory, IsAlive);
            try {
                TheoryPersistant.save(theories);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
