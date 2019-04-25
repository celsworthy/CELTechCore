/*******************************************************************************
 * Copyright (c) 2019 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christoph Caks <ccaks@bestsolution.at> - initial API and implementation
 *******************************************************************************/
package celtech.coreUI.controllers.panels;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AARenderer {

    private volatile boolean alive = true;
    private Thread t;

    public void start() {
        if (t == null) {
            t = new Thread(this::run);
            t.setDaemon(true);
            t.setName(getClass().getSimpleName());
            alive = true;
            t.start();
        }
    }

    public void stop() {
        alive = false;
        try {
            t.join();
            t = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isAlive() {
        return alive;
    }

    protected abstract void run();

    public static String loadTextResource(String resource) {

        StringBuilder textSource = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(AARenderer.class.getResourceAsStream(resource)))) {
            String line;
            while((line = reader.readLine()) != null) {
                textSource.append(line)
                          .append("\n");
            }
            
        } catch(IOException e) {
            System.err.println("Error accessing Resource " + resource + "! This will most likely result in a crash!");
            e.printStackTrace();
        }

        return textSource.toString();
    }
}
