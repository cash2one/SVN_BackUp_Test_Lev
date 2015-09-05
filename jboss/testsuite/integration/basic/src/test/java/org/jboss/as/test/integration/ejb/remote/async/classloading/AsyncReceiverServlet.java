/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.remote.async.classloading;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.ejb.remote.async.RemoteInterface;
/**
 * @author baranowb
 *
 */
@WebServlet(name = "AsyncReceiverServer", urlPatterns = { "/*" })
public class AsyncReceiverServlet extends HttpServlet {

    @EJB(lookup="java:global/wildName/ejbjar/AsyncRemoteEJB!org.jboss.as.test.integration.ejb.remote.async.classloading.AsyncRemote")
    private AsyncRemote remoter;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if(req.getParameter("null")!=null){
                ReturnObject value = remoter.testAsyncNull("NULL!").get();
            } else {
                ReturnObject value = remoter.testAsync("Trololo").get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        } 
        resp.setStatus(200);
        resp.flushBuffer();
    }

}
