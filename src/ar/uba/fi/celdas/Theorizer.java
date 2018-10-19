package ar.uba.fi.celdas;

import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

public class Theorizer {

    protected Random randomGenerator;

    Theorizer() {
        randomGenerator = new Random();
    }

    public void evaluateSituation(Theories theories, StateObservation currentState) {

    }

    public Theories updateResultsTheories(Theories theories, Theory lastTheory, boolean IsAlive) {

        if(theories.existsTheory(lastTheory)) {
            Map<Integer, List<Theory>> mapaTeorias = theories.getTheories();
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
            // TODO: Tengo que ver si tienen similares y copiar los valores de used y success sumandole al success de esta y al used en ambas
            // TODO: pasar metodo de ultility aca y ver de usar este metodo para actualizar teorias en general
            if(IsAlive) {
                lastTheory.setUsedCount(1);
                lastTheory.setUtility(1000);
                lastTheory.setSuccessCount(1);
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
        return (randomGenerator.nextInt(1000) % 2) == 0;
    }

    public Vector2d getAvatarPositionFixed(StateObservation stateObs) {
        Vector2d avatarPos = stateObs.getAvatarPosition();
        avatarPos.mul(1/(float)stateObs.getBlockSize());
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

}
