/*
 * Copyright (c) 1998, 2001, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

class ModificationWatchpointSpec extends WatchpointSpec {
  ModificationWatchpointSpec(ReferenceTypeSpec refSpec, String fieldId)
      throws MalformedMemberNameException {
    super(refSpec, fieldId);
  }

  /** The 'refType' is known to match, return the EventRequest. */
  @Override
  EventRequest resolveEventRequest(ReferenceType refType) throws NoSuchFieldException {
    Field field = refType.fieldByName(fieldId);
    EventRequestManager em = refType.virtualMachine().eventRequestManager();
    EventRequest wp = em.createModificationWatchpointRequest(field);
    wp.setSuspendPolicy(suspendPolicy);
    wp.enable();
    return wp;
  }

  @Override
  public String toString() {
    return MessageOutput.format(
        "watch modification of", new Object[] {refSpec.toString(), fieldId});
  }
}
