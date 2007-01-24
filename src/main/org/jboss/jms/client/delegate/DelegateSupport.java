/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
package org.jboss.jms.client.delegate;

import java.io.Serializable;

import org.jboss.aop.Advised;
import org.jboss.aop.Dispatcher;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.aop.metadata.SimpleMetaData;
import org.jboss.aop.util.PayloadKey;
import org.jboss.jms.client.state.HierarchicalState;
import org.jboss.jms.server.remoting.MessagingMarshallable;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;

/**
 * Base class for all client-side delegate classes.
 *
 * Client-side delegate classes provide an empty implementation of the appropriate delegate
 * interface. The classes are advised using JBoss AOP to provide the client side advice stack.
 * The methods in the delegate class will never actually be invoked since they will either be
 * handled in the advice stack or invoked on the server before reaching the delegate.
 *
 * The delegates are created on the server and serialized back to the client. When they arrive on
 * the client, the init() method is called which causes the advices to be bound to the advised
 * class.
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public abstract class DelegateSupport implements Interceptor, Serializable, Initializable
{
   // Constants ------------------------------------------------------------------------------------

   private static final long serialVersionUID = 8005108339439737469L;

   private static final Logger log = Logger.getLogger(DelegateSupport.class);

   private static boolean trace = log.isTraceEnabled();

   // Attributes -----------------------------------------------------------------------------------

   // This is set on the server.
   protected int id;

   // This is set on the client.
   // The reason we don't use the meta-data to store the state for the delegate is to avoid the
   // extra HashMap lookup that would entail. This can be significant since the state could be
   // queried for many aspects in an a single invocation.
   protected transient HierarchicalState state;

   // Static ---------------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   public DelegateSupport(int id)
   {
      this.id = id;
      this.state = null;
   }

   public DelegateSupport()
   {
      this(Integer.MIN_VALUE);
   }

   // Interceptor implementation -------------------------------------------------------------------

   public String getName()
   {
      // Neede a meaninful name to change the aop stack programatically (HA uses that)
      return this.getClass().getName();
   }

   /**
    * DelegateSupport also acts as an interceptor - the last interceptor in the chain which invokes
    * on the server.
    */
   public Object invoke(Invocation invocation) throws Throwable
   {
      String methodName = ((MethodInvocation)invocation).getMethod().getName();

      invocation.getMetaData().addMetaData(Dispatcher.DISPATCHER,
                                           Dispatcher.OID,
                                           new Integer(id),
                                           PayloadKey.AS_IS);

      Client client = getClient();
      byte version = getState().getVersionToUse().getProviderIncrementingVersion();
      MessagingMarshallable request = new MessagingMarshallable(version, invocation);

      // select invocations ought to be sent "one way" for increased performance
      
      //TODO polymorphism: shouldn't this be ClientSessionDelegate::invoke rather than the super class?? 
      if ("changeRate".equals(methodName))
      {
         if (trace) { log.trace(this + " invoking " + methodName + "(..) asynchronously on server"); }

         client.invokeOneway(request);

         if (trace) { log.trace(this + " asynchronously invoked " + methodName + "(..) on server, no response expected"); }

         return null;
      }
      else
      {
         if (trace) { log.trace(this + " invoking " + methodName + "(..) synchronously on server"); }

         Object o = client.invoke(request, null);

         if (trace) { log.trace(this + " got server response for " + methodName + "(..): " + o); }

         MessagingMarshallable response = (MessagingMarshallable)o;
         return response.getLoad();
      }
   }

   // Initializable implemenation ------------------------------------------------------------------

   public void init()
   {
      ((Advised)this)._getInstanceAdvisor().appendInterceptor(this);
   }

   // Public ---------------------------------------------------------------------------------------

   public HierarchicalState getState()
   {
      return state;
   }

   public void setState(HierarchicalState state)
   {
      this.state = state;
   }

   public int getID()
   {
      return id;
   }

   /**
    * During HA events, delegates corresponding to new enpoints on the new server are created and
    * the state of those delegates has to be transfered to the "failed" delegates. For example, a
    * "failed" connection delegate will have to assume the ID of the new connection endpoint, the
    * new RemotingConnection instance, etc.
    */
   public void synchronizeWith(DelegateSupport newDelegate) throws Exception
   {
      id = newDelegate.getID();
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected SimpleMetaData getMetaData()
   {
      return ((Advised)this)._getInstanceAdvisor().getMetaData();
   }

   protected abstract Client getClient() throws Exception;


   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}
