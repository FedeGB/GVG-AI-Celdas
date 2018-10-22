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
//    private Comparator<Theory> comparatorUtility;
//    private Comparator<Theory> comparatorSuccess;
    private Theorizer theorizer;
    private Vector2d finishPos;
    private List<Integer> vertexPath;

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
        ArrayList<Observation>[] portals = so.getPortalsPositions();
        Vector2d position = portals[0].get(0).position;
        position.x = position.x / so.getBlockSize();
        position.y = position.y / so.getBlockSize();
        finishPos = position;
        theorizer = new Theorizer(finishPos);
        planifier = new Planifier(theories);
        if(planifier.foundFinishPoint()) {
            planifier.buildGraph();
        }
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
        Perception currentPerception = new Perception(currentState);
        System.out.println(currentPerception);
        if(lastState == null && lastAction == null) {
            lastState = currentState;
            lastPosition = currentPosition;
            if(planifier.foundFinishPoint()) {
                Theory auxiliar = new Theory();
                auxiliar.setCurrentState(currentPerception.getLevel());
                vertexPath = planifier.getShortestPath(auxiliar.hashCodeOnlyCurrentState());
                actionToTake = planifier.getNextActionOnPath(vertexPath, auxiliar.hashCodeOnlyCurrentState());
                if(actionToTake != null) {
                    lastAction = actionToTake;
                    return actionToTake;
                }
            }
            lastAction = actionToTake;
            return actionToTake;
        }
        Perception lastPerception = new Perception(lastState);
        Theory localTheory = new Theory();
        localTheory.setCurrentState(lastPerception.getLevel());
        localTheory.setAction(lastAction);
        localTheory.setPredictedState(currentPerception.getLevel());
        if(planifier.foundFinishPoint()) {
            actionToTake = planifier.getNextActionOnPath(vertexPath, localTheory.hashCodeOnlyPredictedState());
            if(actionToTake != null) {
                lastState = currentState;
                lastAction = actionToTake;
                lastPosition = theorizer.getAvatarPositionFixed(currentState);
                return actionToTake;
            }
        }
        boolean selectNewTheory = false;
        List<Theory> listOfTheories = theories.getSortedListForCurrentState(localTheory);
        List<Types.ACTIONS> actionsDone = new ArrayList<>();
        if(!listOfTheories.isEmpty()) {
            for(Theory theo : listOfTheories) {
                actionsDone.add(theo.getAction());
            }
            if(actionsDone.size() == actions.size()) { // All actions done
                selectNewTheory = true;
            } else { // Not all done, maybe explore
                if(!theorizer.shouldExplore()) {
                    selectNewTheory = true;
                }
            }
        }

        theories = theorizer.updateResultsTheories(theories, localTheory, currentState, !currentPosition.equals(lastPosition),true);
        if(selectNewTheory) {
            Theory selected =  theorizer.evaluateBestCandidate(theories.getSortedListForCurrentState(localTheory));
            actionToTake = selected.getAction();
            System.out.println("selected theory");
            System.out.println(actionToTake);
        } else {
            ArrayList<Types.ACTIONS> leftOver = new ArrayList<>(actions);
            leftOver.removeAll(actionsDone);
            if(leftOver.isEmpty()) leftOver = actions;
            actionToTake = theorizer.getRandomActionFromList(leftOver);
        }

        lastState = currentState;
        lastAction = actionToTake;
        lastPosition = theorizer.getAvatarPositionFixed(currentState);
        return actionToTake;

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
            theories = theorizer.updateResultsTheories(theories, lastTheory, stateObs, true, IsAlive);
            try {
                TheoryPersistant.save(theories);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
