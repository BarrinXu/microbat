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

package microbat.codeanalysis.runtime.jpda.tty;

import java.util.ArrayList;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;

abstract class EventRequestSpec {

  final ReferenceTypeSpec refSpec;

  int suspendPolicy = EventRequest.SUSPEND_ALL;

  EventRequest resolved = null;
  ClassPrepareRequest prepareRequest = null;

  EventRequestSpec(ReferenceTypeSpec refSpec) {
    this.refSpec = refSpec;
  }

  /** The 'refType' is known to match, return the EventRequest. */
  abstract EventRequest resolveEventRequest(ReferenceType refType) throws Exception;

  /**
   * @return If this EventRequestSpec matches the 'refType' return the cooresponding EventRequest.
   *     Otherwise return null.
   */
  synchronized EventRequest resolve(ClassPrepareEvent event) throws Exception {
    if ((resolved == null) && (prepareRequest != null) && prepareRequest.equals(event.request())) {

      resolved = resolveEventRequest(event.referenceType());
      prepareRequest.disable();
      Env.vm().eventRequestManager().deleteEventRequest(prepareRequest);
      prepareRequest = null;

      if (refSpec instanceof PatternReferenceTypeSpec) {
        PatternReferenceTypeSpec prs = (PatternReferenceTypeSpec) refSpec;
        if (!prs.isUnique()) {
          /*
           * Class pattern event requests are never
           * considered "resolved", since future class loads
           * might also match.
           * Create and enable a new ClassPrepareRequest to
           * keep trying to resolve.
           */
          resolved = null;
          prepareRequest = refSpec.createPrepareRequest();
          prepareRequest.enable();
        }
      }
    }
    return resolved;
  }

  synchronized void remove() {
    if (isResolved()) {
      Env.vm().eventRequestManager().deleteEventRequest(resolved());
    }
    if (refSpec instanceof PatternReferenceTypeSpec) {
      PatternReferenceTypeSpec prs = (PatternReferenceTypeSpec) refSpec;
      if (!prs.isUnique()) {
        /*
         * This is a class pattern.  Track down and delete
         * all EventRequests matching this spec.
         * Note: Class patterns apply only to ExceptionRequests,
         * so that is all we need to examine.
         */
        ArrayList<ExceptionRequest> deleteList = new ArrayList<ExceptionRequest>();
        for (ExceptionRequest er : Env.vm().eventRequestManager().exceptionRequests()) {
          if (prs.matches(er.exception())) {
            deleteList.add(er);
          }
        }
        Env.vm().eventRequestManager().deleteEventRequests(deleteList);
      }
    }
  }

  private EventRequest resolveAgainstPreparedClasses() throws Exception {
    for (ReferenceType refType : Env.vm().allClasses()) {
      if (refType.isPrepared() && refSpec.matches(refType)) {
        resolved = resolveEventRequest(refType);
      }
    }
    return resolved;
  }

  synchronized EventRequest resolveEagerly() throws Exception {
    try {
      if (resolved == null) {
        /*
         * Not resolved.  Schedule a prepare request so we
         * can resolve later.
         */
        prepareRequest = refSpec.createPrepareRequest();
        prepareRequest.enable();

        // Try to resolve in case the class is already loaded.
        resolveAgainstPreparedClasses();
        if (resolved != null) {
          prepareRequest.disable();
          Env.vm().eventRequestManager().deleteEventRequest(prepareRequest);
          prepareRequest = null;
        }
      }
      if (refSpec instanceof PatternReferenceTypeSpec) {
        PatternReferenceTypeSpec prs = (PatternReferenceTypeSpec) refSpec;
        if (!prs.isUnique()) {
          /*
           * Class pattern event requests are never
           * considered "resolved", since future class loads
           * might also match.  Create a new
           * ClassPrepareRequest if necessary and keep
           * trying to resolve.
           */
          resolved = null;
          if (prepareRequest == null) {
            prepareRequest = refSpec.createPrepareRequest();
            prepareRequest.enable();
          }
        }
      }
    } catch (VMNotConnectedException e) {
      // Do nothing. Another resolve will be attempted when the
      // VM is started.
    }
    return resolved;
  }

  /**
   * @return the eventRequest this spec has been resolved to, null if so far unresolved.
   */
  EventRequest resolved() {
    return resolved;
  }

  /**
   * @return true if this spec has been resolved.
   */
  boolean isResolved() {
    return resolved != null;
  }

  protected boolean isJavaIdentifier(String s) {
    if (s.length() == 0) {
      return false;
    }

    int cp = s.codePointAt(0);
    if (!Character.isJavaIdentifierStart(cp)) {
      return false;
    }

    for (int i = Character.charCount(cp); i < s.length(); i += Character.charCount(cp)) {
      cp = s.codePointAt(i);
      if (!Character.isJavaIdentifierPart(cp)) {
        return false;
      }
    }

    return true;
  }

  String errorMessageFor(Exception e) {
    if (e instanceof IllegalArgumentException) {
      return (MessageOutput.format("Invalid command syntax"));
    } else if (e instanceof RuntimeException) {
      // A runtime exception that we were not expecting
      throw (RuntimeException) e;
    } else {
      return (MessageOutput.format("Internal error; unable to set", this.refSpec.toString()));
    }
  }
}
