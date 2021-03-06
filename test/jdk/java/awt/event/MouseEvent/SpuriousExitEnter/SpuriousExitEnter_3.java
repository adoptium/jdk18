/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
  @test
  @key headful
  @bug 6479820
  @library ../../../regtesthelpers
  @build Util
  @summary verify that enter/exit events still comes correctly
  @author andrei dmitriev: area=awt.event
  @run main SpuriousExitEnter_3
*/

/**
 * SpuriousExitEnter_3.java
 *
            "There is a plain JFrame with JButton in it.",
            "Let A area is the area inside JButton.",
            "Let B area is the area inside JFrame but not inside JButton.",
            "Let C area is the area outside JFrame.",
            "Now check that the correct events and are in the correct number generates when you",
            "move the pointer between those areas.",
            " 1) Verify that the Enter and Exit events comes to JButton and JFrame when ",
            " you move the pointer between A and B areas.",
            " 2) Verify that the Enter and Exit events comes to JButton when you",
            " move the pointer between A to C.",
            " 3) Verify that the Enter and Exit events comes to JFrame when you",
            " move the pointer between B to C.",
 */

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import test.java.awt.regtesthelpers.Util;

import javax.swing.JButton;
import javax.swing.JFrame;

public class SpuriousExitEnter_3 {
    static JFrame frame;
    static JButton jbutton;

    static Frame frame1;
    static Button button1;

    static final Robot r = Util.createRobot();

    static volatile EnterExitAdapter frameAdapter;
    static volatile EnterExitAdapter buttonAdapter;
    static volatile Point centerA;
    static volatile Point centerB;
    static volatile Point centerC_1 ;
    static volatile Point centerC_2;

    public static void testCase(Window w, Component comp) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(()-> {
            frameAdapter = new EnterExitAdapter(w);
            buttonAdapter = new EnterExitAdapter(comp);

            w.addMouseListener(frameAdapter);
            comp.addMouseListener(buttonAdapter);

            w.setSize(200, 200);
            w.add(comp, BorderLayout.NORTH);
            w.setLocationRelativeTo(null);
            w.setVisible(true);
        });

        r.waitForIdle();
        r.delay(1000);

        EventQueue.invokeAndWait(()-> {
            centerA = new Point(comp.getLocationOnScreen().x + comp.getWidth() / 2,
                    comp.getLocationOnScreen().y + comp.getHeight() / 2);
            centerB = new Point(w.getLocationOnScreen().x + w.getWidth() / 2,
                    w.getLocationOnScreen().y + w.getHeight() / 2);
            //for moving from A outside: don't cross the A area. Move straight to the right.
            centerC_1 = new Point(w.getLocationOnScreen().x + w.getWidth() + 20,  //go right off the border
                    comp.getLocationOnScreen().y + comp.getHeight() / 2); //don't cross the A area!

            //for moving from B outside: don't cross the B area. Move straight to the bottom.
            centerC_2 = new Point(w.getLocationOnScreen().x + w.getWidth() / 2,
                    w.getLocationOnScreen().y + w.getHeight() + 20); //go below the bottom border
        });

        //A and B areas
        Util.pointOnComp(comp, r);
        Util.waitForIdle(r);
        frameAdapter.zeroCounters();
        buttonAdapter.zeroCounters();

        moveBetween(r, centerA, centerB);
        checkEvents(frameAdapter, 1, 1);
        checkEvents(buttonAdapter, 1, 1);

        //A and C areas
        Util.pointOnComp(comp, r);
        Util.waitForIdle(r);
        frameAdapter.zeroCounters();
        buttonAdapter.zeroCounters();
        moveBetween(r, centerA, centerC_1);
        checkEvents(frameAdapter, 0, 0);
        checkEvents(buttonAdapter, 1, 1);

        //B and C areas
        Util.pointOnComp(w, r);
        Util.waitForIdle(r);
        frameAdapter.zeroCounters();
        buttonAdapter.zeroCounters();
        moveBetween(r, centerB, centerC_2);
        checkEvents(frameAdapter, 1, 1);
        checkEvents(buttonAdapter, 0, 0);
        w.setVisible(false);
        Util.waitForIdle(r);
    }


    public static void main(String []s) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(() -> {
            frame = new JFrame("SpuriousExitEnter_3_LW");
            jbutton = new JButton("jbutton");

            frame1 = new Frame("SpuriousExitEnter_3_HW");
            button1  = new Button("button");
        });

        try {
            testCase(frame,  jbutton); //LW case
            testCase(frame1, button1); //HW case
        } finally {
            EventQueue.invokeLater(frame::dispose);
            EventQueue.invokeLater(frame1::dispose);
        }
    }

    private static void moveBetween(Robot r, Point first, Point second) {
        Util.waitForIdle(r);
        Util.mouseMove(r, first, second);
        Util.waitForIdle(r);
        Util.mouseMove(r, second, first);
        Util.waitForIdle(r);
    }

    // component should get exactly entered of Entered events and exited of Exited events.
    private static void checkEvents(EnterExitAdapter adapter, int entered, int exited) {
        if (adapter.getEnteredEventCount() != entered ||
            adapter.getExitedEventCount() != exited)
        {
            throw new RuntimeException(adapter.getTarget().getClass().getName()+": incorrect event number: Entered got: " +
                                       adapter.getEnteredEventCount() +" expected : " + entered
                                       + ". Exited got : " + adapter.getExitedEventCount() + " expected : "
                                       + exited);
        }
    }

}


class EnterExitAdapter extends MouseAdapter {
    private final Component target;
    private volatile int enteredEventCount = 0;
    private volatile int exitedEventCount = 0;

    public EnterExitAdapter(Component target) {
        this.target = target;
    }

    public Component getTarget(){
        return target;
    }
    public int getEnteredEventCount(){
        return enteredEventCount;
    }

    public int getExitedEventCount(){
        return exitedEventCount;
    }

    public void zeroCounters(){
        System.out.println("Zeroing on " +target.getClass().getName());
        enteredEventCount = 0;
        exitedEventCount = 0;
    }

    public void mouseEntered(MouseEvent e){
        System.out.println("Entered on " + e.getSource().getClass().getName());
        enteredEventCount ++;
    }
    public void mouseExited(MouseEvent e){
        System.out.println("Exited on " + e.getSource().getClass().getName());
        exitedEventCount ++;
    }
}
