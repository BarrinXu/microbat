/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

import microbat.codeanalysis.runtime.jpda.bdi.ExecutionManager;
import microbat.codeanalysis.runtime.jpda.bdi.VMNotInterruptedException;
import microbat.codeanalysis.runtime.jpda.expr.ExpressionParser;
import microbat.codeanalysis.runtime.jpda.expr.ParseException;

public class MonitorTool extends JPanel {

  private static final long serialVersionUID = -645235951031726647L;
  private ExecutionManager runtime;
  private ContextManager context;

  private JList list;

  public MonitorTool(Environment env) {
    super(new BorderLayout());
    this.runtime = env.getExecutionManager();
    this.context = env.getContextManager();

    list = new JList(env.getMonitorListModel());
    list.setCellRenderer(new MonitorRenderer());

    JScrollPane listView = new JScrollPane(list);
    add(listView);

    // Create listener.
    MonitorToolListener listener = new MonitorToolListener();
    list.addListSelectionListener(listener);
    // ### remove listeners on exit!
  }

  private class MonitorToolListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent e) {
      int index = list.getSelectedIndex();
      if (index != -1) {}
    }
  }

  private Value evaluate(String expr)
      throws ParseException,
          InvocationException,
          InvalidTypeException,
          ClassNotLoadedException,
          IncompatibleThreadStateException {
    ExpressionParser.GetFrame frameGetter =
        new ExpressionParser.GetFrame() {
          @Override
          public StackFrame get() throws IncompatibleThreadStateException {
            try {
              return context.getCurrentFrame();
            } catch (VMNotInterruptedException exc) {
              throw new IncompatibleThreadStateException();
            }
          }
        };
    return ExpressionParser.evaluate(expr, runtime.vm(), frameGetter);
  }

  private class MonitorRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
        JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

      // ### We should indicate the current thread independently of the
      // ### selection, e.g., with an icon, because the user may change
      // ### the selection graphically without affecting the current
      // ### thread.

      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value == null) {
        this.setText("<unavailable>");
      } else {
        String expr = (String) value;
        try {
          Value result = evaluate(expr);
          this.setText(expr + " = " + result);
        } catch (ParseException exc) {
          this.setText(expr + " ? " + exc.getMessage());
        } catch (IncompatibleThreadStateException exc) {
          this.setText(expr + " ...");
        } catch (Exception exc) {
          this.setText(expr + " ? " + exc);
        }
      }
      return this;
    }
  }
}
