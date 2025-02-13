/*
 * Copyright (c) 1999, 2004, Oracle and/or its affiliates. All rights reserved.
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

package microbat.codeanalysis.runtime.jpda.event;

import java.util.Collection;
import java.util.EventObject;
import java.util.Iterator;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.event.WatchpointEvent;
import com.sun.jdi.request.EventRequest;

public abstract class AbstractEventSet extends EventObject implements EventSet {

  private static final long serialVersionUID = 2772717574222076977L;
  private final EventSet jdiEventSet;
  final Event oneEvent;

  /** */
  AbstractEventSet(EventSet jdiEventSet) {
    super(jdiEventSet.virtualMachine());
    this.jdiEventSet = jdiEventSet;
    this.oneEvent = eventIterator().nextEvent();
  }

  public static AbstractEventSet toSpecificEventSet(EventSet jdiEventSet) {
    Event evt = jdiEventSet.eventIterator().nextEvent();
    if (evt instanceof LocatableEvent) {
      if (evt instanceof ExceptionEvent) {
        return new ExceptionEventSet(jdiEventSet);
      } else if (evt instanceof WatchpointEvent) {
        if (evt instanceof AccessWatchpointEvent) {
          return new AccessWatchpointEventSet(jdiEventSet);
        } else {
          return new ModificationWatchpointEventSet(jdiEventSet);
        }
      } else {
        return new LocationTriggerEventSet(jdiEventSet);
      }
    } else if (evt instanceof ClassPrepareEvent) {
      return new ClassPrepareEventSet(jdiEventSet);
    } else if (evt instanceof ClassUnloadEvent) {
      return new ClassUnloadEventSet(jdiEventSet);
    } else if (evt instanceof ThreadDeathEvent) {
      return new ThreadDeathEventSet(jdiEventSet);
    } else if (evt instanceof ThreadStartEvent) {
      return new ThreadStartEventSet(jdiEventSet);
    } else if (evt instanceof VMDeathEvent) {
      return new VMDeathEventSet(jdiEventSet);
    } else if (evt instanceof VMDisconnectEvent) {
      return new VMDisconnectEventSet(jdiEventSet);
    } else if (evt instanceof VMStartEvent) {
      return new VMStartEventSet(jdiEventSet);
    } else {
      throw new IllegalArgumentException("Unknown event " + evt);
    }
  }

  public abstract void notify(JDIListener listener);

  // Implement Mirror

  @Override
  public VirtualMachine virtualMachine() {
    return jdiEventSet.virtualMachine();
  }

  public VirtualMachine getVirtualMachine() {
    return jdiEventSet.virtualMachine();
  }

  // Implement EventSet

  /**
   * Returns the policy used to suspend threads in the target VM for this event set. This policy is
   * selected from the suspend policies for each event's request. The one that suspends the most
   * threads is chosen when the event occurs in the target VM and that policy is returned here. See
   * com.sun.jdi.request.EventRequest for the possible policy values.
   *
   * @return the integer suspendPolicy
   */
  public int getSuspendPolicy() {
    return jdiEventSet.suspendPolicy();
  }

  @Override
  public void resume() {
    jdiEventSet.resume();
  }

  @Override
  public int suspendPolicy() {
    return jdiEventSet.suspendPolicy();
  }

  public boolean suspendedAll() {
    return jdiEventSet.suspendPolicy() == EventRequest.SUSPEND_ALL;
  }

  public boolean suspendedEventThread() {
    return jdiEventSet.suspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD;
  }

  public boolean suspendedNone() {
    return jdiEventSet.suspendPolicy() == EventRequest.SUSPEND_NONE;
  }

  /** Return an iterator specific to {@link Event} objects. */
  @Override
  public EventIterator eventIterator() {
    return jdiEventSet.eventIterator();
  }

  // Implement java.util.Set (by pass through)

  /**
   * Returns the number of elements in this set (its cardinality). If this set contains more than
   * <tt>Integer.MAX_VALUE</tt> elements, returns <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of elements in this set (its cardinality).
   */
  @Override
  public int size() {
    return jdiEventSet.size();
  }

  /**
   * Returns <tt>true</tt> if this set contains no elements.
   *
   * @return <tt>true</tt> if this set contains no elements.
   */
  @Override
  public boolean isEmpty() {
    return jdiEventSet.isEmpty();
  }

  /**
   * Returns <tt>true</tt> if this set contains the specified element. More formally, returns
   * <tt>true</tt> if and only if this set contains an element <code>e</code> such that <code>
   * (o==null ? e==null :
   * o.equals(e))</code>.
   *
   * @return <tt>true</tt> if this set contains the specified element.
   */
  @Override
  public boolean contains(Object o) {
    return jdiEventSet.contains(o);
  }

  /**
   * Returns an iterator over the elements in this set. The elements are returned in no particular
   * order (unless this set is an instance of some class that provides a guarantee).
   *
   * @return an iterator over the elements in this set.
   */
  @Override
  public Iterator<Event> iterator() {
    return jdiEventSet.iterator();
  }

  /**
   * Returns an array containing all of the elements in this set. Obeys the general contract of the
   * <tt>Collection.toArray</tt> method.
   *
   * @return an array containing all of the elements in this set.
   */
  @Override
  public Object[] toArray() {
    return jdiEventSet.toArray();
  }

  /**
   * Returns an array containing all of the elements in this set whose runtime type is that of the
   * specified array. Obeys the general contract of the <tt>Collection.toArray(Object[])</tt>
   * method.
   *
   * @param a the array into which the elements of this set are to be stored, if it is big enough {
   *     return jdiEventSet.XXX(); } otherwise, a new array of the same runtime type is allocated
   *     for this purpose.
   * @return an array containing the elements of this set.
   * @throws ArrayStoreException the runtime type of a is not a supertype of the runtime type of
   *     every element in this set.
   */
  @Override
  public <T> T[] toArray(T a[]) {
    return jdiEventSet.toArray(a);
  }

  // Bulk Operations

  /**
   * Returns <tt>true</tt> if this set contains all of the elements of the specified collection. If
   * the specified collection is also a set, this method returns <tt>true</tt> if it is a
   * <i>subset</i> of this set.
   *
   * @param c collection to be checked for containment in this set.
   * @return <tt>true</tt> if this set contains all of the elements of the specified collection.
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    return jdiEventSet.containsAll(c);
  }

  // Make the rest of Set unmodifiable

  @Override
  public boolean add(Event e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends Event> coll) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> coll) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> coll) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
