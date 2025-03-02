/*
 * Copyright (c) 1998, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.sun.jdi.*;

import microbat.codeanalysis.runtime.jpda.event.*;

/** Manage the list of source files. Origin of SourceListener events. */
public class SourceManager {

  // ### TODO: The source cache should be aged, and some cap
  // ### put on memory consumption by source files loaded into core.

  private List<SourceModel> sourceList;
  private SearchPath sourcePath;

  private ArrayList<SourceListener> sourceListeners = new ArrayList<SourceListener>();

  private Map<ReferenceType, SourceModel> classToSource = new HashMap<ReferenceType, SourceModel>();

  private Environment env;

  /** Hold on to it so it can be removed. */
  private SMClassListener classListener = new SMClassListener();

  public SourceManager(Environment env) {
    this(env, new SearchPath(""));
  }

  public SourceManager(Environment env, SearchPath sourcePath) {
    this.env = env;
    this.sourceList = new LinkedList<SourceModel>();
    this.sourcePath = sourcePath;
    env.getExecutionManager().addJDIListener(classListener);
  }

  /** Set path for access to source code. */
  public void setSourcePath(SearchPath sp) {
    sourcePath = sp;
    // Old cached sources are now invalid.
    sourceList = new LinkedList<SourceModel>();
    notifySourcepathChanged();
    classToSource = new HashMap<ReferenceType, SourceModel>();
  }

  public void addSourceListener(SourceListener l) {
    sourceListeners.add(l);
  }

  public void removeSourceListener(SourceListener l) {
    sourceListeners.remove(l);
  }

  private void notifySourcepathChanged() {
    ArrayList<SourceListener> l = new ArrayList<SourceListener>(sourceListeners);
    SourcepathChangedEvent evt = new SourcepathChangedEvent(this);
    for (int i = 0; i < l.size(); i++) {
      l.get(i).sourcepathChanged(evt);
    }
  }

  /** Get path for access to source code. */
  public SearchPath getSourcePath() {
    return sourcePath;
  }

  /** Get source object associated with a Location. */
  public SourceModel sourceForLocation(Location loc) {
    return sourceForClass(loc.declaringType());
  }

  /** Get source object associated with a class or interface. Returns null if not available. */
  public SourceModel sourceForClass(ReferenceType refType) {
    SourceModel sm = classToSource.get(refType);
    if (sm != null) {
      return sm;
    }
    try {
      String filename = refType.sourceName();
      String refName = refType.name();
      int iDot = refName.lastIndexOf('.');
      String pkgName = (iDot >= 0) ? refName.substring(0, iDot + 1) : "";
      String full = pkgName.replace('.', File.separatorChar) + filename;
      File path = sourcePath.resolve(full);
      if (path != null) {
        sm = sourceForFile(path);
        classToSource.put(refType, sm);
        return sm;
      }
      return null;
    } catch (AbsentInformationException e) {
      return null;
    }
  }

  /** Get source object associated with an absolute file path. */
  // ### Use hash table for this?
  public SourceModel sourceForFile(File path) {
    Iterator<SourceModel> iter = sourceList.iterator();
    SourceModel sm = null;
    while (iter.hasNext()) {
      SourceModel candidate = iter.next();
      if (candidate.fileName().equals(path)) {
        sm = candidate;
        iter.remove(); // Will move to start of list.
        break;
      }
    }
    if (sm == null && path.exists()) {
      sm = new SourceModel(env, path);
    }
    if (sm != null) {
      // At start of list for faster access
      sourceList.add(0, sm);
    }
    return sm;
  }

  private class SMClassListener extends JDIAdapter implements JDIListener {

    @Override
    public void classPrepare(ClassPrepareEventSet e) {
      ReferenceType refType = e.getReferenceType();
      SourceModel sm = sourceForClass(refType);
      if (sm != null) {
        sm.addClass(refType);
      }
    }

    @Override
    public void classUnload(ClassUnloadEventSet e) {
      // ### iterate through looking for (e.getTypeName()).
      // ### then remove it.
    }
  }
}
