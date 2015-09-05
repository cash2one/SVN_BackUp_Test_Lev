/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.resource.mail;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@Remote(StatelessMail.class)
public class StatelessMailBean
   implements StatelessMail
{
   
   @Resource(name="MyDefaultMail")
   private Session mailSession;
   
   @Resource(lookup="java:jboss/mail/Default")
   private Session session;
   
   // injected via xml descriptor
   private Session dsSession;
   
   public void testMail() throws NamingException
   {
      Context initCtx = new InitialContext();
      Context myEnv = (Context) initCtx.lookup("java:comp/env");
      
      // JavaMail Session
      Object obj = myEnv.lookup("MyDefaultMail");
      if ((obj instanceof javax.mail.Session) == false)
         throw new NamingException("DefaultMail is not a javax.mail.Session");
   }
   
   public void testMailInjection()
   {
      mailSession.getProperties();
      session.getProperties();
      dsSession.getProperties();
   }

}
