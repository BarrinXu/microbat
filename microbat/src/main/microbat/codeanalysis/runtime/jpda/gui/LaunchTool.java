/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

package microbat.codeanalysis.runtime.jpda.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;

import microbat.codeanalysis.runtime.jpda.bdi.ExecutionManager;
import microbat.codeanalysis.runtime.jpda.bdi.VMLaunchFailureException;

class LaunchTool {

  private final ExecutionManager runtime;

  private abstract class ArgRep {
    final Connector.Argument arg;
    final JPanel panel;

    ArgRep(Connector.Argument arg) {
      this.arg = arg;
      panel = new JPanel();
      Border etched = BorderFactory.createEtchedBorder();
      Border titled =
          BorderFactory.createTitledBorder(
              etched, arg.description(), TitledBorder.LEFT, TitledBorder.TOP);
      panel.setBorder(titled);
    }

    abstract String getText();

    boolean isValid() {
      return arg.isValid(getText());
    }

    boolean isSpecified() {
      String value = getText();
      return (value != null && value.length() > 0) || !arg.mustSpecify();
    }

    void install() {
      arg.setValue(getText());
    }
  }

  private class StringArgRep extends ArgRep {
    final JTextField textField;

    StringArgRep(Connector.Argument arg, JPanel comp) {
      super(arg);
      textField = new JTextField(arg.value(), 50);
      textField.setBorder(BorderFactory.createLoweredBevelBorder());

      panel.add(new JLabel(arg.label(), SwingConstants.RIGHT));
      panel.add(textField); // , BorderLayout.CENTER);
      comp.add(panel);
    }

    @Override
    String getText() {
      return textField.getText();
    }
  }

  private class BooleanArgRep extends ArgRep {
    final JCheckBox check;

    BooleanArgRep(Connector.BooleanArgument barg, JPanel comp) {
      super(barg);
      check = new JCheckBox(barg.label());
      check.setSelected(barg.booleanValue());
      panel.add(check);
      comp.add(panel);
    }

    @Override
    String getText() {
      return ((Connector.BooleanArgument) arg).stringValueOf(check.getModel().isSelected());
    }
  }

  private LaunchTool(ExecutionManager runtime) {
    this.runtime = runtime;
  }

  private Connector selectConnector() {
    final JDialog dialog = new JDialog();
    Container content = dialog.getContentPane();
    final JPanel radioPanel = new JPanel();
    final ButtonGroup radioGroup = new ButtonGroup();
    VirtualMachineManager manager = Bootstrap.virtualMachineManager();
    List<Connector> all = manager.allConnectors();
    Map<ButtonModel, Connector> modelToConnector =
        new HashMap<ButtonModel, Connector>(all.size(), 0.5f);

    dialog.setModal(true);
    dialog.setTitle("Select Connector Type");
    radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
    for (Connector connector : all) {
      JRadioButton radio = new JRadioButton(connector.description());
      modelToConnector.put(radio.getModel(), connector);
      radioPanel.add(radio);
      radioGroup.add(radio);
    }
    content.add(radioPanel);

    final boolean[] oked = {false};
    JPanel buttonPanel =
        okCancel(
            dialog,
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent event) {
                if (radioGroup.getSelection() == null) {
                  JOptionPane.showMessageDialog(
                      dialog,
                      "Please select a connector type",
                      "No Selection",
                      JOptionPane.ERROR_MESSAGE);
                } else {
                  oked[0] = true;
                  dialog.setVisible(false);
                  dialog.dispose();
                }
              }
            });
    content.add(BorderLayout.SOUTH, buttonPanel);
    dialog.pack();
    dialog.setVisible(true);

    return oked[0] ? modelToConnector.get(radioGroup.getSelection()) : null;
  }

  private void configureAndConnect(final Connector connector) {
    final JDialog dialog = new JDialog();
    final Map<String, Connector.Argument> args = connector.defaultArguments();

    dialog.setModal(true);
    dialog.setTitle("Connector Arguments");
    Container content = dialog.getContentPane();
    JPanel guts = new JPanel();
    Border etched = BorderFactory.createEtchedBorder();
    BorderFactory.createTitledBorder(
        etched, connector.description(), TitledBorder.LEFT, TitledBorder.TOP);
    guts.setBorder(etched);
    guts.setLayout(new BoxLayout(guts, BoxLayout.Y_AXIS));

    //        guts.add(new JLabel(connector.description()));

    final List<ArgRep> argReps = new ArrayList<ArgRep>(args.size());
    for (Connector.Argument arg : args.values()) {
      ArgRep ar;
      if (arg instanceof Connector.BooleanArgument) {
        ar = new BooleanArgRep((Connector.BooleanArgument) arg, guts);
      } else {
        ar = new StringArgRep(arg, guts);
      }
      argReps.add(ar);
    }
    content.add(guts);

    JPanel buttonPanel =
        okCancel(
            dialog,
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent event) {
                for (ArgRep ar : argReps) {
                  if (!ar.isSpecified()) {
                    JOptionPane.showMessageDialog(
                        dialog,
                        ar.arg.label() + ": Argument must be specified",
                        "No argument",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                  }
                  if (!ar.isValid()) {
                    JOptionPane.showMessageDialog(
                        dialog,
                        ar.arg.label() + ": Bad argument value: " + ar.getText(),
                        "Bad argument",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                  }
                  ar.install();
                }
                try {
                  if (runtime.explictStart(connector, args)) {
                    dialog.setVisible(false);
                    dialog.dispose();
                  } else {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "Bad arguments values: See diagnostics window.",
                        "Bad arguments",
                        JOptionPane.ERROR_MESSAGE);
                  }
                } catch (VMLaunchFailureException exc) {
                  JOptionPane.showMessageDialog(
                      dialog, "Launch Failure: " + exc, "Launch Failed", JOptionPane.ERROR_MESSAGE);
                }
              }
            });
    content.add(BorderLayout.SOUTH, buttonPanel);
    dialog.pack();
    dialog.setVisible(true);
  }

  private JPanel okCancel(final JDialog dialog, ActionListener okListener) {
    JPanel buttonPanel = new JPanel();
    JButton ok = new JButton("OK");
    JButton cancel = new JButton("Cancel");
    buttonPanel.add(ok);
    buttonPanel.add(cancel);
    ok.addActionListener(okListener);
    cancel.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent event) {
            dialog.setVisible(false);
            dialog.dispose();
          }
        });
    return buttonPanel;
  }

  static void queryAndLaunchVM(ExecutionManager runtime) throws VMLaunchFailureException {
    LaunchTool lt = new LaunchTool(runtime);
    Connector connector = lt.selectConnector();
    if (connector != null) {
      lt.configureAndConnect(connector);
    }
  }
}
