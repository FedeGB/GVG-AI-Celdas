package ar.uba.fi.celdas;

import ontology.Types;

public class ActionCounter {

    public int amount;
    public Types.ACTIONS action;

    ActionCounter(Types.ACTIONS action) {
        amount = 0;
        action = action;
    }

    public void sumAction() {
        amount++;
    }

}
