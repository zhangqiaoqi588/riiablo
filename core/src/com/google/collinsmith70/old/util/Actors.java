package com.google.collinsmith70.old.util;

import com.badlogic.gdx.scenes.scene2d.Actor;

public class Actors {
    private Actors() {

    }

    public static void centerAt(Actor a, float x, float y) {
        a.setPosition(
            x - a.getWidth() / 2,
            y - a.getHeight() / 2
        );
    }
}