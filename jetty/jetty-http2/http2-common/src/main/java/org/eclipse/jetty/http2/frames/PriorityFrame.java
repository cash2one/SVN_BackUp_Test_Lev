//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http2.frames;

public class PriorityFrame extends Frame
{
    private final int streamId;
    private final int dependentStreamId;
    private final int weight;
    private final boolean exclusive;

    public PriorityFrame(int streamId, int dependentStreamId, int weight, boolean exclusive)
    {
        super(FrameType.PRIORITY);
        this.streamId = streamId;
        this.dependentStreamId = dependentStreamId;
        this.weight = weight;
        this.exclusive = exclusive;
    }

    public int getStreamId()
    {
        return streamId;
    }

    public int getDependentStreamId()
    {
        return dependentStreamId;
    }

    public int getWeight()
    {
        return weight;
    }

    public boolean isExclusive()
    {
        return exclusive;
    }

    @Override
    public String toString()
    {
        return String.format("%s#%d/#%d{weight=%d,ex=%b}", super.toString(), streamId, dependentStreamId, weight, exclusive);
    }
}
