package ar.uba.fi.celdas;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Planifier {

    private Theories theories;
    protected Random randomGenerator;
    protected ArrayList<Types.ACTIONS> actions;
    private Vector2d finishPos;
    private Types.ACTIONS lastAction;
    private StateObservation lastState;
    private Comparator<Theory> comparatorUtility;
    private Comparator<Theory> comparatorSuccess;
    private Map<Integer, Integer> repetition;
    private Map<Vector2d, ArrayList<ActionCounter>> actionOnPoint;
    private ArrayList<Theory> uselessTheories;
    private int randomCount = 0;
    private int mejorCount = 0;

    public Planifier(StateObservation so) {
        randomGenerator = new Random();
        actions = so.getAvailableActions();
        ArrayList<Observation>[] portals = so.getPortalsPositions();
        Vector2d position = portals[0].get(0).position;
        position.mul(1/50.0);
        finishPos = position;
        lastState = null;
        lastAction = null;
        uselessTheories = new ArrayList<>();
        comparatorUtility = (left, right) -> {
            if (left.getUtility() < right.getUtility()) {
                return 1;
            } else if(left.getUtility() > right.getUtility()) {
                return -1;
            }
            return 0;
        };
        comparatorSuccess = (left, right) -> {
            if ((float)left.getSuccessCount() / left.getUsedCount() < (float)right.getSuccessCount() / right.getUsedCount()) {
                return 1;
            } else if((float)left.getSuccessCount() / left.getUsedCount() > (float)right.getSuccessCount() / right.getUsedCount()) {
                return -1;
            }
            return 0;
        };
        repetition = new HashMap<>();
        actionOnPoint = new HashMap<>();
        try {
            theories = TheoryPersistant.load();
        } catch (FileNotFoundException e) {
            theories = new Theories();
        }
    }

    public Types.ACTIONS getNextAction(StateObservation stateObs) {
        StateObservation currentState = stateObs.copy();
        Types.ACTIONS actionToTake = Types.ACTIONS.ACTION_UP;
        if(lastState == null && lastAction == null) {
//            updateActionCounter(currentState, actionToTake);
            lastState = currentState;
            lastAction = actionToTake;
            return actionToTake;
        }
        Perception previousPerception = new Perception(lastState);
        Perception currentPerception = new Perception(currentState);
        System.out.println(currentPerception.toString());

        Theory localTheory = new Theory();
        Vector2d avatarPreviousPosition = getAvatarPositionFixed(lastState);
        localTheory.setCurrentState(getState(previousPerception.getLevel(), avatarPreviousPosition));
        localTheory.setAction(lastAction);
        localTheory.setPredictedState(getPredictedState(currentState));
//        ArrayList<Types.ACTIONS> viableActions = getViableActions(currentState,new Perception(currentState));
//        boolean theoryUseless = false;
//        if(viableActions.size() == 1) {
//            uselessTheories.add(localTheory);
//            theoryUseless = true;
//        }
        if(/*!theoryUseless && */theories.getTheories().isEmpty()) {
            localTheory.setUsedCount(1);
            localTheory.setSuccessCount(1);
            localTheory.setUtility(getUtilityBasedOnRef(currentState));
            try {
                theories.add(localTheory);
            } catch (Exception e) {
                // Should never happen since there are no theories!
            }

        } else /*if(!theoryUseless) */{
            Map<Integer, List<Theory>> mapaTeorias = theories.getTheories();
            boolean recentlyAdded = false;
            if(!theories.existsTheory(localTheory)) {
                localTheory.setUsedCount(1);
                localTheory.setSuccessCount(1);
                localTheory.setUtility(getUtilityBasedOnRef(currentState));
                try {
                    theories.add(localTheory);
                    recentlyAdded = true;
                } catch (Exception e) {
                    // Should never happen since there are no equal theories!
                }
            }
            if(mapaTeorias.containsKey(localTheory.hashCodeOnlyCurrentState())) {
                List<Theory> equalTheories = mapaTeorias.get(localTheory.hashCodeOnlyCurrentState());
                for(final ListIterator<Theory> teoiter = equalTheories.listIterator(); teoiter.hasNext();) {
                    final Theory teo = teoiter.next();
                    if(teo.equals(localTheory) && !recentlyAdded) { // iguales
                        teo.setSuccessCount(teo.getSuccessCount() + 1);
                        teo.setUsedCount(teo.getUsedCount() + 1);
                        teoiter.set(teo);
                    } else { // similares
                        teo.setUsedCount(teo.getUsedCount() + 1);
                        teoiter.set(teo);
                    }
                }
                mapaTeorias.put(localTheory.hashCodeOnlyCurrentState(), equalTheories);
            } else {
                localTheory.setUsedCount(1);
                localTheory.setSuccessCount(1);
                localTheory.setUtility(getUtilityBasedOnRef(currentState));
                try {
                    theories.add(localTheory);
                } catch (Exception e) {
                    // Should never happen since there are no equal theories!
                }
            }

        }

        ArrayList<Theory> wantedList = getWantedList(theories, localTheory);
        List<Theory> toUse = null;
        Theory best = null;
        Types.ACTIONS actionToAvoid = null;
        for(Theory theo : wantedList) {
            toUse = hasPathTheCurrentState(currentState, theo);
            if(toUse != null && !toUse.isEmpty()) {
                best = evaluateBestCandidate(toUse, currentState);
//                for(Theory useless : uselessTheories) {
//                    if(best != null && java.util.Arrays.deepEquals(useless.getPredictedState(), best.getPredictedState())) {
//                        actionToAvoid = best.getAction();
//                        best = null;
//
//                    }
//                }
            }
            if(best != null) {
                System.out.println("Accion mejor");
                System.out.println((float) best.getSuccessCount() / best.getUsedCount());
                actionToTake = best.getAction();
                System.out.println(actionToTake);
                mejorCount++;
                break;
            }
        }
        if(toUse == null || toUse.isEmpty() || best == null) {
            ArrayList<Types.ACTIONS> viable = getViableActions(currentState, new Perception(currentState));
//            if(actionToAvoid != null) {
//                viable.remove(actionToAvoid);
//            }
//                ArrayList<ActionCounter> counterCurrent = actionOnPoint.get(avatarCurrentPosition);
//                int maxValueCounter = 0;
//                for(Types.ACTIONS option : viable) {
//                    for(ActionCounter counter : counterCurrent) {
//                        if(option == counter.action && counter.amount > maxValueCounter) {
//                            maxValueCounter = counter.amount;
//                            actionToTake = counter.action;
//                        }
//                    }
//                }
            int index = randomGenerator.nextInt(viable.size());
            actionToTake = viable.get(index);
            System.out.println("Accion random");
            System.out.println(actionToTake);
            randomCount++;
        }
        try {
            TheoryPersistant.save(theories);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Integer times;
        if(best != null) {
            times = repetition.get(best.hashCode());
            if(times == null) times = 0;
            times++;
            repetition.put(best.hashCode(), times);
        } else {
            Theory forRepetition = new Theory();
            forRepetition.setCurrentState(getPredictedState(currentState));
            forRepetition.setAction(actionToTake);
            StateObservation pred = currentState.copy();
            pred.advance(actionToTake);
            forRepetition.setPredictedState(getPredictedState(pred));
            times = repetition.get(forRepetition.hashCode());
            if(times == null) times = 0;
            times++;
            repetition.put(forRepetition.hashCode(), times);
        }
//        updateActionCounter(currentState, actionToTake);
        lastState = currentState;
        lastAction = actionToTake;
        System.out.println("Random: "  + randomCount);
        System.out.println("Mejor: " + mejorCount);
        return actionToTake;
    }

    private ArrayList<Types.ACTIONS> getViableActions(StateObservation stateObs, Perception perception) {
        ArrayList<Types.ACTIONS> adjPos = new ArrayList<>(actions);
        char curPos;
        for(Types.ACTIONS action : actions) {
            Vector2d position = this.getActionPosition(stateObs, action);
            curPos = perception.getAt((int)position.y, (int)position.x);
            if(curPos == 'w' || curPos == 't') {
                adjPos.remove(action);
            }
        }
        return adjPos;
    }

    private Vector2d getActionPosition(StateObservation stateObs, Types.ACTIONS action) {
        Vector2d avatarPos = stateObs.getAvatarPosition();
        if(action == Types.ACTIONS.ACTION_DOWN) {
            avatarPos.add(0,50);
        } else if(action == Types.ACTIONS.ACTION_UP) {
            avatarPos.subtract(0, 50);
        } else if(action == Types.ACTIONS.ACTION_LEFT) {
            avatarPos.subtract(50, 0);
        } else {
            avatarPos.add(50, 0);
        }
        avatarPos.mul(1/50.0);
        return avatarPos;
    }

    private Vector2d getAvatarPositionFixed(StateObservation stateObs) {
        Vector2d avatarPos = stateObs.getAvatarPosition();
        avatarPos.mul(1/50.0);
        return avatarPos;
    }
    private float getUtilityBasedOnRef(StateObservation predicted) {
        Vector2d refPos = getAvatarPositionFixed(predicted);
        double distance = finishPos.dist(refPos);
        return 1 / (float)(1 + distance);
    }

    private char[][] getState(char[][] level, Vector2d avatarPos) {
        char[][] stateReturn = new char[3][3];
        int xState = 0;
        int yState = 0;
        for(int x = -1; x < 2; x++) {
            for(int y = -1; y < 2; y++) {
                if((xState == 1) && (yState == 0 || yState == 2)) {
                    stateReturn[yState][xState] = level[(int) avatarPos.y + y][(int) avatarPos.x + x];
                } else if((xState == 0 || xState == 2) && yState == 1) {
                    stateReturn[yState][xState] = level[(int) avatarPos.y + y][(int) avatarPos.x + x];
                } else {
                    stateReturn[yState][xState] = 'F';
                }
//                stateReturn[yState][xState] = level[(int) avatarPos.y + y][(int) avatarPos.x + x];
//                if(stateReturn[yState][xState] == 'A') stateReturn[yState][xState] = '.';
                yState++;
            }
            xState++;
            yState = 0;
        }
        return stateReturn;
    }

    private char[][] getPredictedState(StateObservation so) {
        Perception predictedPerception = new Perception(so);
        return getState(predictedPerception.getLevel(), getAvatarPositionFixed(so));
    }

    private ArrayList<Theory> getWantedList(Theories theories, Theory local) {
         List<Theory> allTheory = theories.getTheories().get(local.hashCodeOnlyCurrentState());
        ArrayList<Theory> wantedList = new ArrayList<>(allTheory);
         wantedList.sort(comparatorSuccess);
         return wantedList;
    }

    private List<Theory> hasPathTheCurrentState(StateObservation current, Theory theo) {
        Map<Integer, List<Theory>> map = theories.getTheories();
        List<Theory> listToUse = new ArrayList<>();
        Theory actual = new Theory();
        Perception actualPer = new Perception(current);
        char[][] actualState = getState(actualPer.getLevel(), getAvatarPositionFixed(current));
        actual.setCurrentState(actualState);
        List<Theory> found = map.get(actual.hashCodeOnlyCurrentState());
        if(found != null) {
            for(Theory foundTheory : found) {
                if(foundTheory.hashCodeOnlyCurrentState() == theo.hashCodeOnlyPredictedState()) {
                    System.out.println("theories were found");
                    listToUse.add(foundTheory);
                }
            }
        }
        return listToUse;
    }

    private Theory evaluateBestCandidate(List<Theory> listToUse, StateObservation currentState) {
        Theory bestTheory = null;
        listToUse.sort(comparatorUtility);

        for (Theory eval : listToUse) {
            ArrayList<Types.ACTIONS> viable = getViableActions(currentState, new Perception(currentState));
            if(!viable.contains(eval.getAction())) continue;
            if(lastAction == eval.getAction()) continue;
            Integer times = repetition.get(eval.hashCode());
            if(times == null) times = 0;
            if(times > 10) {
                continue; // skip overuse
            }
            bestTheory = eval;
            break;
        }
        return bestTheory;
    }

    private void updateActionCounter(StateObservation currentState, Types.ACTIONS actionToTake) {
        Vector2d posActual = getAvatarPositionFixed(currentState);
        ArrayList<ActionCounter> actionList = actionOnPoint.get(posActual);
        if(actionList == null) {
            actionList = new ArrayList<>();
            ActionCounter newActionC = new ActionCounter(actionToTake);
            newActionC.sumAction();
            actionList.add(newActionC);
        } else {
            for(ActionCounter counter : actionList) {
                if(counter.action == actionToTake) {
                    counter.sumAction();
                }
            }
        }
        actionOnPoint.put(posActual, actionList);
    }

}
