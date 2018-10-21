package ar.uba.fi.celdas;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.*;

public class Theorizer {

    protected Random randomGenerator;
    private Comparator<Theory> comparatorUtility;
    private Comparator<Theory> comparatorSuccess;
    private Vector2d finishPos;

    Theorizer(Vector2d finalPos) {
        randomGenerator = new Random();
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
        finishPos = finalPos;
    }

    public void evaluateSituation(Theories theories, StateObservation currentState) {

    }

    public Theories updateResultsTheories(Theories theories, Theory lastTheory, StateObservation currentState, boolean moved, boolean IsAlive) {

        Map<Integer, List<Theory>> mapaTeorias = theories.getTheories();
        if(theories.existsTheory(lastTheory)) {
            List<Theory> equalTheories = mapaTeorias.get(lastTheory.hashCodeOnlyCurrentState());
            for (final ListIterator<Theory> teoiter = equalTheories.listIterator(); teoiter.hasNext(); ) {
                final Theory teo = teoiter.next();
                if (teo.equals(lastTheory)) { // iguales
                    teo.setUsedCount(teo.getUsedCount() + 1);
                    if(IsAlive) {
                        teo.setSuccessCount(teo.getSuccessCount() + 1);
                    }
                    teoiter.set(teo);
                } else { // similares
                    teo.setUsedCount(teo.getUsedCount() + 1);
                    teoiter.set(teo);
                }
            }
            mapaTeorias.put(lastTheory.hashCodeOnlyCurrentState(), equalTheories);
            theories.setTheories(mapaTeorias);
        } else {
            List<Theory> equalTheories = mapaTeorias.get(lastTheory.hashCodeOnlyCurrentState());
            if(equalTheories != null && !equalTheories.isEmpty()) {
                for (final ListIterator<Theory> teoiter = equalTheories.listIterator(); teoiter.hasNext(); ) {
                    final Theory teo = teoiter.next();
                    teo.setUsedCount(teo.getUsedCount() + 1);
                    teoiter.set(teo);
                }
                mapaTeorias.put(lastTheory.hashCodeOnlyCurrentState(), equalTheories);
                theories.setTheories(mapaTeorias);
            }
            if(IsAlive) {
                lastTheory.setUsedCount(1);
                if(moved) {
                    lastTheory.setUtility(getUtilityBasedOnRef(currentState));
                    lastTheory.setSuccessCount(1);
                } else {
                    lastTheory.setSuccessCount(0);
                    lastTheory.setUtility(0);
                }
            } else {
                lastTheory.setUtility(0);
                lastTheory.setUsedCount(1);
                lastTheory.setSuccessCount(0);
            }
            try {
                theories.add(lastTheory);
            } catch (Exception e) {
//                e.printStackTrace();
                // Should not happen since we checked if the theory existed before
            }
        }
        return theories;
    }

    public Types.ACTIONS getRandomActionFromList(List<Types.ACTIONS> listOfActions) {
        int index = randomGenerator.nextInt(listOfActions.size());
        Types.ACTIONS actionToTake = listOfActions.get(index);
        System.out.println("Accion random");
        System.out.println(actionToTake);
        return actionToTake;
    }

    public boolean shouldExplore() {
        // par explora, impar no explora
        return (randomGenerator.nextInt(100) % 2) == 0;
    }

    public Vector2d getAvatarPositionFixed(StateObservation stateObs) {
        Vector2d avatarPos = stateObs.getAvatarPosition();
        if(avatarPos.x < 0 && avatarPos.y < 0) { // Correction after taking portal
            avatarPos = finishPos;
        }
        avatarPos.x = avatarPos.x / stateObs.getBlockSize();
        avatarPos.y = avatarPos.y / stateObs.getBlockSize();
        return avatarPos;
    }

    public char[][] getState(char[][] level, Vector2d avatarPos) {
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

    public char[][] getPredictedAvatarStateByAction(StateObservation currentState, Vector2d avatarCurPos, Types.ACTIONS actionToTake) {
        Vector2d predictedPos = avatarCurPos.copy();
        switch (actionToTake) {
            case ACTION_UP:
                avatarCurPos.subtract(0, 1);
                break;
            case ACTION_DOWN:
                avatarCurPos.add(0,1);
                break;
            case ACTION_LEFT:
                avatarCurPos.subtract(1, 0);
                break;
            case ACTION_RIGHT:
                avatarCurPos.add(1, 0);
                break;
            default:
                break;
        }
        Perception percept = new Perception(currentState);
        return getState(percept.getLevel(), predictedPos);
    }

    public char[][] getReducedState(StateObservation so) {
        Perception percept = new Perception(so);
        return getState(percept.getLevel(), getAvatarPositionFixed(so));
    }

    public float getUtilityBasedOnRef(StateObservation predicted) {
        Vector2d refPos = getAvatarPositionFixed(predicted);
        double distance = refPos.dist(finishPos);
        System.out.println(refPos);
        System.out.println(distance);
        return 1000 / (float)(1 + distance);
    }

    public Theory evaluateBestCandidate(List<Theory> listToUse) {
        listToUse.sort(comparatorUtility);
        Theory selected;
        if(shouldExplore()) {
            selected = listToUse.get(randomGenerator.nextInt(listToUse.size()));
        } else {
            selected = listToUse.get(0);
        }

        return selected;
    }

}
